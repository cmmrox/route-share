package com.routeshare.platform.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.entity.PolicySettingEntity;
import com.routeshare.platform.entity.PolicySettingHistoryEntity;
import com.routeshare.platform.repository.PolicySettingHistoryRepository;
import com.routeshare.platform.repository.PolicySettingRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PolicySettingTest {
  private final PolicySettingRepository settings = mock(PolicySettingRepository.class);
  private final PolicySettingHistoryRepository history = mock(PolicySettingHistoryRepository.class);

  private PolicySettingServiceImpl service(long ttl) {
    return new PolicySettingServiceImpl(settings, history, ttl);
  }

  private PolicySettingEntity setting(String key, String value, String type) {
    var entity = mock(PolicySettingEntity.class);
    when(entity.getPolicyKey()).thenReturn(key);
    when(entity.getValue()).thenReturn(value);
    when(entity.getValueType()).thenReturn(type);
    when(settings.findById(key)).thenReturn(Optional.of(entity));
    return entity;
  }

  @Test
  void aDecimalPolicyReadsAsADecimal() {
    setting("COMMISSION_PCT", "10", "DECIMAL");

    assertThat(service(60).decimal(PolicyKey.COMMISSION_PCT)).isEqualByComparingTo("10");
  }

  @Test
  void anIntegerPolicyReadsAsAnInteger() {
    setting("PICKUP_WAIT_MIN", "5", "INT");

    assertThat(service(60).integer(PolicyKey.PICKUP_WAIT_MIN)).isEqualTo(5);
  }

  @Test
  void anUnseededKeyFailsLoudlyRatherThanDefaultingAPriceToZero() {
    when(settings.findById("COMMISSION_PCT")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service(60).decimal(PolicyKey.COMMISSION_PCT))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not seeded");
  }

  @Test
  void readsAreCachedBecauseEverySearchResultPricesOne() {
    setting("COMMISSION_PCT", "10", "DECIMAL");
    var service = service(60);

    service.decimal(PolicyKey.COMMISSION_PCT);
    service.decimal(PolicyKey.COMMISSION_PCT);
    service.decimal(PolicyKey.COMMISSION_PCT);

    verify(settings, times(1)).findById("COMMISSION_PCT");
  }

  @Test
  void aWriteEvictsTheCacheSoTheNextQuoteUsesTheNewRule() {
    var entity = setting("COMMISSION_PCT", "10", "DECIMAL");
    var service = service(60);
    assertThat(service.decimal(PolicyKey.COMMISSION_PCT)).isEqualByComparingTo("10");
    when(settings.save(any())).thenReturn(entity);

    service.update("COMMISSION_PCT", "12", 1L);
    when(entity.getValue()).thenReturn("12");

    assertThat(service.decimal(PolicyKey.COMMISSION_PCT)).isEqualByComparingTo("12");
  }

  @Test
  void everyWriteIsRecordedWithItsOldAndNewValue() {
    var entity = setting("COMMISSION_PCT", "10", "DECIMAL");
    when(settings.save(any())).thenReturn(entity);

    service(60).update("COMMISSION_PCT", "12", 7L);

    verify(history).save(any(PolicySettingHistoryEntity.class));
  }

  @Test
  void aValueOfTheWrongTypeIsRefused() {
    setting("PICKUP_WAIT_MIN", "5", "INT");

    assertThatThrownBy(() -> service(60).update("PICKUP_WAIT_MIN", "soon", 1L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void anUnknownKeyIsRefused() {
    assertThatThrownBy(() -> service(60).update("MADE_UP_KEY", "1", 1L))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void thePricingVersionChangesWhenAPricingRuleChanges() {
    var commission = setting("COMMISSION_PCT", "10", "DECIMAL");
    setting("MATCH_DISCOUNT_TIER_95_PCT", "10", "DECIMAL");
    setting("MATCH_DISCOUNT_TIER_75_PCT", "8", "DECIMAL");
    setting("MATCH_DISCOUNT_TIER_45_PCT", "5", "DECIMAL");
    setting("MATCH_DISCOUNT_TIER_BASE_PCT", "2.5", "DECIMAL");
    setting("MATCH_DISCOUNT_THRESHOLD_HIGH", "95", "DECIMAL");
    setting("MATCH_DISCOUNT_THRESHOLD_MID", "75", "DECIMAL");
    setting("MATCH_DISCOUNT_THRESHOLD_LOW", "45", "DECIMAL");
    var service = service(0);
    String before = service.pricingPolicyVersion();

    when(commission.getValue()).thenReturn("12");

    // A quote stamped with the old version can still be explained against the rules it was priced
    // under, which is the whole reason the stamp exists.
    assertThat(service.pricingPolicyVersion()).isNotEqualTo(before);
  }
}
