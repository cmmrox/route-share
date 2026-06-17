package com.routeshare.payment.repository;

import com.routeshare.payment.entity.PaymentMethodEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Long> {
  List<PaymentMethodEntity> findByAppUserIdAndStatusOrderByIdDesc(long appUserId, String status);

  Optional<PaymentMethodEntity> findByIdAndAppUserId(long id, long appUserId);

  @Modifying
  @Query("update PaymentMethodEntity m set m.defaultMethod = false where m.appUserId = :appUserId")
  void clearDefaults(@Param("appUserId") long appUserId);
}
