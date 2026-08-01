package com.routeshare.platform.service;

import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.dto.response.PolicySettingResponse;
import java.math.BigDecimal;
import java.util.List;

/**
 * Typed access to the runtime policy surface.
 *
 * <p>Reads are hot — every search result is priced — so values are cached and evicted on write. A
 * missing key is a programming error, not a runtime condition: the migration seeds every key in
 * {@link PolicyKey}, so a lookup that finds nothing means someone added an enum constant without a
 * migration, and that must fail loudly rather than default silently to zero.
 */
public interface PolicySettingService {
  BigDecimal decimal(PolicyKey key);

  int integer(PolicyKey key);

  boolean flag(PolicyKey key);

  String string(PolicyKey key);

  List<PolicySettingResponse> all();

  /** Admin write. Records history and evicts the cache; the caller audits. */
  PolicySettingResponse update(String policyKey, String value, long actorAppUserId);

  /**
   * A fingerprint of the values that price a fare, stored on every quote so an old fare can be
   * explained against the rules that were in force when it was quoted.
   */
  String pricingPolicyVersion();
}
