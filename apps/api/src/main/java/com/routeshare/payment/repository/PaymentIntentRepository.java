package com.routeshare.payment.repository;

import com.routeshare.payment.entity.PaymentIntentEntity;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntentEntity, Long> {
  Optional<PaymentIntentEntity> findFirstByBookingIdAndStatusInOrderByIdDesc(
      long bookingId, java.util.Collection<String> statuses);

  @Modifying
  @Query(
      value =
          """
      UPDATE payment.payment_intent
      SET status = :toStatus,
          updated_at = now()
      WHERE payment_intent_id = :paymentIntentId
        AND status = :fromStatus
      """,
      nativeQuery = true)
  int updateStatus(
      @Param("paymentIntentId") long paymentIntentId,
      @Param("fromStatus") String fromStatus,
      @Param("toStatus") String toStatus);

  @Modifying
  @Query(
      value =
          """
      UPDATE payment.payment_intent
      SET status = :status, updated_at = now()
      WHERE provider_reference = :providerReference
      """,
      nativeQuery = true)
  int updateStatusByProviderReference(
      @Param("providerReference") String providerReference, @Param("status") String status);

  default Optional<PaymentIntentView> transitionStatus(
      long paymentIntentId, String fromStatus, String toStatus) {
    int updated = updateStatus(paymentIntentId, fromStatus, toStatus);
    if (updated != 1) {
      return Optional.empty();
    }
    return findById(paymentIntentId).map(this::toView);
  }

  default Optional<PaymentIntentView> findActiveByBookingId(long bookingId) {
    return findFirstByBookingIdAndStatusInOrderByIdDesc(
            bookingId, java.util.List.of("REQUIRES_CAPTURE", "CAPTURED"))
        .map(this::toView);
  }

  default PaymentIntentView create(
      long bookingId, String providerReference, BigDecimal amount, String currency) {
    return toView(
        save(
            new PaymentIntentEntity(
                null, bookingId, null, providerReference, amount, currency, null)));
  }

  private PaymentIntentView toView(PaymentIntentEntity entity) {
    return new PaymentIntentView(
        entity.getId(),
        entity.getBookingId(),
        entity.getProvider(),
        entity.getProviderReference(),
        entity.getStatus(),
        entity.getAmount(),
        entity.getCurrency());
  }

  @Query(
      value =
          """
      SELECT payment_intent_id AS "paymentIntentId", booking_id AS "bookingId", provider AS "provider",
             provider_reference AS "providerReference", status AS "status", amount AS "amount",
             currency AS "currency", created_at AS "createdAt", updated_at AS "updatedAt"
      FROM payment.payment_intent
      ORDER BY created_at DESC
      LIMIT 200
      """,
      nativeQuery = true)
  java.util.List<PaymentAdminRow> findAdminPayments();

  @Query(
      value =
          """
      SELECT payment_intent_id AS "paymentIntentId", booking_id AS "bookingId", provider AS "provider",
             provider_reference AS "providerReference", status AS "status", amount AS "amount",
             currency AS "currency", created_at AS "createdAt", updated_at AS "updatedAt"
      FROM payment.payment_intent
      WHERE payment_intent_id = :paymentIntentId
      """,
      nativeQuery = true)
  Optional<PaymentAdminRow> findAdminPayment(@Param("paymentIntentId") long paymentIntentId);

  interface PaymentAdminRow {
    Long getPaymentIntentId();

    Long getBookingId();

    String getProvider();

    String getProviderReference();

    String getStatus();

    BigDecimal getAmount();

    String getCurrency();

    java.time.Instant getCreatedAt();

    java.time.Instant getUpdatedAt();
  }

  record PaymentIntentView(
      Long paymentIntentId,
      Long bookingId,
      String provider,
      String providerReference,
      String status,
      BigDecimal amount,
      String currency) {
    public PaymentIntentView(
        String provider,
        String providerReference,
        String status,
        BigDecimal amount,
        String currency) {
      this(null, null, provider, providerReference, status, amount, currency);
    }
  }
}
