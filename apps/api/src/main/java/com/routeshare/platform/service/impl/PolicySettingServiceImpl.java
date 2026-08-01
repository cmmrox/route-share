package com.routeshare.platform.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.dto.response.PolicySettingResponse;
import com.routeshare.platform.entity.PolicySettingEntity;
import com.routeshare.platform.entity.PolicySettingHistoryEntity;
import com.routeshare.platform.repository.PolicySettingHistoryRepository;
import com.routeshare.platform.repository.PolicySettingRepository;
import com.routeshare.platform.service.PolicySettingService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PolicySettingServiceImpl implements PolicySettingService {
  /** The keys that decide a fare. A change to any of them changes what a quote means. */
  private static final List<PolicyKey> PRICING_KEYS =
      List.of(
          PolicyKey.COMMISSION_PCT,
          PolicyKey.MATCH_DISCOUNT_TIER_95_PCT,
          PolicyKey.MATCH_DISCOUNT_TIER_75_PCT,
          PolicyKey.MATCH_DISCOUNT_TIER_45_PCT,
          PolicyKey.MATCH_DISCOUNT_TIER_BASE_PCT,
          PolicyKey.MATCH_DISCOUNT_THRESHOLD_HIGH,
          PolicyKey.MATCH_DISCOUNT_THRESHOLD_MID,
          PolicyKey.MATCH_DISCOUNT_THRESHOLD_LOW);

  private final PolicySettingRepository settings;
  private final PolicySettingHistoryRepository history;
  private final Cache<String, String> cache;

  public PolicySettingServiceImpl(
      PolicySettingRepository settings,
      PolicySettingHistoryRepository history,
      @Value("${routeshare.policy.cache-ttl-seconds:60}") long cacheTtlSeconds) {
    this.settings = settings;
    this.history = history;
    this.cache =
        cacheTtlSeconds <= 0
            ? null
            : Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds)).build();
  }

  @Override
  public BigDecimal decimal(PolicyKey key) {
    return new BigDecimal(raw(key));
  }

  @Override
  public int integer(PolicyKey key) {
    return Integer.parseInt(raw(key).trim());
  }

  @Override
  public boolean flag(PolicyKey key) {
    return Boolean.parseBoolean(raw(key).trim());
  }

  @Override
  public String string(PolicyKey key) {
    return raw(key);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PolicySettingResponse> all() {
    return settings.findAllByOrderByPolicyKeyAsc().stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional
  public PolicySettingResponse update(String policyKey, String value, long actorAppUserId) {
    PolicyKey key = parseKey(policyKey);
    PolicySettingEntity entity =
        settings
            .findById(key.name())
            .orElseThrow(() -> new NoSuchElementException("Unknown policy key: " + policyKey));
    validate(entity.getValueType(), value);

    String previous = entity.getValue();
    entity.setValue(value);
    entity.setUpdatedByAppUserId(actorAppUserId);
    var saved = settings.save(entity);
    history.save(PolicySettingHistoryEntity.of(key.name(), previous, value, actorAppUserId));
    if (cache != null) {
      cache.invalidate(key.name());
    }
    // A rule change is a money change; it belongs in the log as well as the history table.
    log.info(
        "policy setting changed key={} from={} to={} actor={}",
        key.name(),
        previous,
        value,
        actorAppUserId);
    return toResponse(saved);
  }

  @Override
  public String pricingPolicyVersion() {
    StringBuilder fingerprint = new StringBuilder();
    for (PolicyKey key : PRICING_KEYS) {
      fingerprint.append(key.name()).append('=').append(raw(key)).append(';');
    }
    return "v" + Integer.toHexString(fingerprint.toString().hashCode());
  }

  private String raw(PolicyKey key) {
    if (cache == null) {
      return load(key.name());
    }
    return cache.get(key.name(), this::load);
  }

  private String load(String key) {
    return settings
        .findById(key)
        .map(PolicySettingEntity::getValue)
        // Seeded by migration for every enum constant. Absence means an enum constant was added
        // without one, which must fail rather than default a price to zero.
        .orElseThrow(() -> new IllegalStateException("Policy setting is not seeded: " + key));
  }

  private PolicyKey parseKey(String policyKey) {
    try {
      return PolicyKey.valueOf(policyKey.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new NoSuchElementException("Unknown policy key: " + policyKey);
    }
  }

  private void validate(String valueType, String value) {
    try {
      switch (valueType) {
        case "INT" -> Integer.parseInt(value.trim());
        case "DECIMAL" -> new BigDecimal(value.trim());
        case "BOOLEAN" -> {
          if (!"true".equalsIgnoreCase(value.trim()) && !"false".equalsIgnoreCase(value.trim())) {
            throw new IllegalArgumentException("expected true or false");
          }
        }
        default -> {
          /* STRING accepts anything non-blank, which bean validation already enforced */
        }
      }
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Value is not a valid " + valueType + ": " + value);
    }
  }

  private PolicySettingResponse toResponse(PolicySettingEntity entity) {
    return new PolicySettingResponse(
        entity.getPolicyKey(),
        entity.getValue(),
        entity.getValueType(),
        entity.getDescription(),
        entity.getUpdatedAt());
  }
}
