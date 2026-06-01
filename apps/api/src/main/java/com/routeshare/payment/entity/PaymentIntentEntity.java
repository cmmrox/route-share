package com.routeshare.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_intent", schema = "payment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentIntentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_intent_id")
  private Long id;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(insertable = false)
  private String provider;

  @Column(name = "provider_reference")
  private String providerReference;

  private BigDecimal amount;
  private String currency;

  @Column(insertable = false)
  private String status;
}
