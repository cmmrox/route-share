package com.routeshare.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A stored card reference. Only the provider token + safe display fields (brand, last4, expiry) are
 * persisted — never the PAN/CVV. Tokenization happens in the payment gateway.
 */
@Entity
@Table(name = "payment_method", schema = "payment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethodEntity {
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_REMOVED = "REMOVED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_method_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(nullable = false)
  private String provider;

  @Column(nullable = false)
  private String token;

  private String brand;
  private String last4;

  @Column(name = "exp_month")
  private Integer expMonth;

  @Column(name = "exp_year")
  private Integer expYear;

  @Column(name = "is_default", nullable = false)
  private boolean defaultMethod;

  @Column(nullable = false)
  private String status = STATUS_ACTIVE;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static PaymentMethodEntity active(
      long appUserId,
      String provider,
      String token,
      String brand,
      String last4,
      Integer expMonth,
      Integer expYear,
      boolean defaultMethod) {
    var e = new PaymentMethodEntity();
    e.appUserId = appUserId;
    e.provider = provider;
    e.token = token;
    e.brand = brand;
    e.last4 = last4;
    e.expMonth = expMonth;
    e.expYear = expYear;
    e.defaultMethod = defaultMethod;
    e.status = STATUS_ACTIVE;
    return e;
  }
}
