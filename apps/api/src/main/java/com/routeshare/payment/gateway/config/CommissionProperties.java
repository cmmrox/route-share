package com.routeshare.payment.gateway.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Platform commission policy. The default rate applies until per-route/per-driver commission rules
 * (managed by admin in Phase G) override it. Replaces the previously hard-coded 10% in the earnings
 * calculation.
 */
@ConfigurationProperties(prefix = "routeshare.commission")
public record CommissionProperties(BigDecimal defaultRate) {
  public CommissionProperties {
    if (defaultRate == null
        || defaultRate.signum() < 0
        || defaultRate.compareTo(BigDecimal.ONE) > 0) {
      defaultRate = new BigDecimal("0.10");
    }
  }

  public BigDecimal commissionOn(BigDecimal gross) {
    return gross.multiply(defaultRate).setScale(2, java.math.RoundingMode.HALF_UP);
  }
}
