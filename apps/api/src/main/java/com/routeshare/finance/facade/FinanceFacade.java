package com.routeshare.finance.facade;

import java.math.BigDecimal;
import java.util.Optional;

/** What pricing needs from finance, and nothing more. */
public interface FinanceFacade {
  /**
   * The floor a fare may not price below. The only field of the old fare policy that survives the
   * rewrite: base fare, per-km and per-minute all belonged to a model that no longer exists, but a
   * very short overlap still must not produce a fare of four rupees.
   */
  Optional<BigDecimal> activeMinFare();
}
