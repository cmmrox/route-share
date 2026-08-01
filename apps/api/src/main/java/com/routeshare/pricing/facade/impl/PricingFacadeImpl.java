package com.routeshare.pricing.facade.impl;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.finance.facade.FinanceFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.pricing.domain.FareEngine;
import com.routeshare.pricing.domain.FareQuote;
import com.routeshare.pricing.domain.MatchDiscountTier;
import com.routeshare.pricing.entity.FareQuoteEntity;
import com.routeshare.pricing.facade.PricingFacade;
import com.routeshare.pricing.repository.FareQuoteRepository;
import com.routeshare.vehicle.facade.VehicleFacade;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PricingFacadeImpl implements PricingFacade {
  private static final BigDecimal METERS_PER_KM = BigDecimal.valueOf(1000);

  private final PolicySettingService policy;
  private final VehicleFacade vehicles;
  private final FinanceFacade finance;
  private final FareQuoteRepository quotes;
  private final MeterRegistry meters;
  private final Clock clock;

  @Override
  public FareQuote quoteForMatch(
      Long routeOccurrenceId,
      long vehicleId,
      BigDecimal onRouteMeters,
      BigDecimal matchPercent,
      int seats) {
    BigDecimal ratePerKm =
        vehicles
            .ratePerKmFor(vehicleId)
            .orElseThrow(
                () ->
                    // A trip whose vehicle has no live band has no legal price. Refusing here is
                    // what stops an unpriced car appearing in search with a fare of zero.
                    new GateConflictException(
                        GateCodes.RATE_BAND_NOT_SET,
                        "This vehicle does not have a rate set yet.",
                        "/driver/vehicles"));

    MatchDiscountTier tier =
        MatchDiscountTier.of(
            matchPercent,
            policy.decimal(PolicyKey.MATCH_DISCOUNT_THRESHOLD_HIGH),
            policy.decimal(PolicyKey.MATCH_DISCOUNT_THRESHOLD_MID),
            policy.decimal(PolicyKey.MATCH_DISCOUNT_THRESHOLD_LOW));

    FareQuote quote =
        FareEngine.quote(
            policy.string(PolicyKey.CURRENCY),
            onRouteMeters,
            ratePerKm,
            seats,
            matchPercent,
            tier,
            discountPercentFor(tier),
            policy.decimal(PolicyKey.COMMISSION_PCT),
            finance.activeMinFare().orElse(null),
            clock.instant(),
            policy.pricingPolicyVersion());

    meters.counter("routeshare_fare_quotes_total", "tier", tier.name()).increment();
    if (quote.minFareApplied()) {
      meters.counter("routeshare_min_fare_applied_total").increment();
    }
    meters.summary("routeshare_fare_passenger_pays").record(quote.passengerPays().doubleValue());
    // Quote volume equals search volume, so this is DEBUG on purpose — at INFO it would drown the
    // log the moment anyone searches.
    log.debug(
        "fare quote vehicleId={} meters={} rate={} seats={} tier={} pays={} net={}",
        vehicleId,
        onRouteMeters,
        ratePerKm,
        seats,
        tier,
        quote.passengerPays(),
        quote.driverNet());
    return quote;
  }

  @Override
  @Transactional
  public FareQuote persistForBooking(
      long bookingId,
      Long routeOccurrenceId,
      long vehicleId,
      Long passengerAppUserId,
      BigDecimal onRouteMeters,
      BigDecimal matchPercent,
      int seats) {
    FareQuote quote =
        quoteForMatch(routeOccurrenceId, vehicleId, onRouteMeters, matchPercent, seats);
    quotes.save(toEntity(quote, bookingId, routeOccurrenceId, vehicleId, passengerAppUserId));
    return quote;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<FareQuote> quoteForBooking(long bookingId) {
    return quotes.findFirstByBookingIdOrderByIdDesc(bookingId).map(this::toDomain);
  }

  @Override
  @Transactional
  public FareQuote repriceForActualDistance(long bookingId, BigDecimal actualMeters) {
    FareQuoteEntity original =
        quotes
            .findFirstByBookingIdOrderByIdDesc(bookingId)
            .orElseThrow(() -> new NoSuchElementException("Booking has no fare quote"));

    // Repriced against the ORIGINAL rate, tier and discount, not today's. The passenger travelled
    // less; the terms she booked under have not changed.
    FareQuote repriced =
        FareEngine.quote(
            original.getCurrency(),
            actualMeters,
            original.getRatePerKm(),
            original.getSeats(),
            original.getMatchPercent(),
            MatchDiscountTier.valueOf(original.getMatchTier()),
            original.getDiscountPercent(),
            original.getCommissionPercent(),
            finance.activeMinFare().orElse(null),
            clock.instant(),
            original.getPolicyVersion());

    quotes.save(
        toEntity(
            repriced,
            bookingId,
            original.getRouteOccurrenceId(),
            original.getVehicleId(),
            original.getPassengerAppUserId()));
    return repriced;
  }

  private BigDecimal discountPercentFor(MatchDiscountTier tier) {
    return switch (tier) {
      case HIGH -> policy.decimal(PolicyKey.MATCH_DISCOUNT_TIER_95_PCT);
      case MID -> policy.decimal(PolicyKey.MATCH_DISCOUNT_TIER_75_PCT);
      case LOW -> policy.decimal(PolicyKey.MATCH_DISCOUNT_TIER_45_PCT);
      case BASE -> policy.decimal(PolicyKey.MATCH_DISCOUNT_TIER_BASE_PCT);
    };
  }

  private FareQuoteEntity toEntity(
      FareQuote quote,
      Long bookingId,
      Long routeOccurrenceId,
      Long vehicleId,
      Long passengerAppUserId) {
    var entity = FareQuoteEntity.blank();
    entity.setBookingId(bookingId);
    entity.setRouteOccurrenceId(routeOccurrenceId);
    entity.setVehicleId(vehicleId);
    entity.setPassengerAppUserId(passengerAppUserId);
    entity.setOnRouteDistanceMeters(quote.onRouteDistanceMeters());
    entity.setRatePerKm(quote.ratePerKm());
    entity.setSeats(quote.seats());
    entity.setGrossFare(quote.grossFare());
    entity.setMatchPercent(quote.matchPercent());
    entity.setMatchTier(quote.matchTier().name());
    entity.setDiscountPercent(quote.discountPercent());
    entity.setDiscountAmount(quote.discountAmount());
    entity.setPassengerPays(quote.passengerPays());
    entity.setCommissionPercent(quote.commissionPercent());
    entity.setCommissionAmount(quote.commissionAmount());
    entity.setDriverNet(quote.driverNet());
    entity.setMinFareApplied(quote.minFareApplied());
    entity.setCurrency(quote.currency());
    entity.setPolicyVersion(quote.policyVersion());
    return entity;
  }

  private FareQuote toDomain(FareQuoteEntity entity) {
    return new FareQuote(
        entity.getCurrency(),
        entity.getOnRouteDistanceMeters(),
        entity.getOnRouteDistanceMeters().divide(METERS_PER_KM, 4, java.math.RoundingMode.HALF_UP),
        entity.getRatePerKm(),
        entity.getSeats(),
        entity.getGrossFare(),
        entity.getMatchPercent(),
        MatchDiscountTier.valueOf(entity.getMatchTier()),
        entity.getDiscountPercent(),
        entity.getDiscountAmount(),
        entity.getPassengerPays(),
        entity.getCommissionPercent(),
        entity.getCommissionAmount(),
        entity.getDriverNet(),
        Boolean.TRUE.equals(entity.getMinFareApplied()),
        entity.getQuotedAt(),
        entity.getPolicyVersion());
  }
}
