package com.routeshare.pricing.facade;

import com.routeshare.pricing.domain.FareQuote;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * The only way other modules get a price.
 *
 * <p>Every input is server-derived: the on-route distance comes from the matched substring of the
 * driver's stored route line, the rate from the vehicle's assessed band, the discount from policy.
 * <b>No pricing input is ever read from a request body</b> — a client-supplied distance or rate is
 * a free-money bug, and the removed {@code POST /pricing/estimate} was exactly that shape.
 */
public interface PricingFacade {
  /** Prices one candidate or booking. Refuses a vehicle with no live rate band. */
  FareQuote quoteForMatch(
      Long routeOccurrenceId,
      long vehicleId,
      BigDecimal onRouteMeters,
      BigDecimal matchPercent,
      int seats);

  /** Prices and stores the quote against a booking, returning the persisted figures. */
  FareQuote persistForBooking(
      long bookingId,
      Long routeOccurrenceId,
      long vehicleId,
      Long passengerAppUserId,
      BigDecimal onRouteMeters,
      BigDecimal matchPercent,
      int seats);

  /** The quote that priced a booking, for receipts and for the payment commission split. */
  Optional<FareQuote> quoteForBooking(long bookingId);

  /**
   * Reprices a booking on the distance actually travelled and stores the new quote.
   *
   * <p>Slice 05 owns the twice-a-month allowance that decides <em>whether</em> this may be called;
   * this is only the arithmetic.
   */
  FareQuote repriceForActualDistance(long bookingId, BigDecimal actualMeters);
}
