package com.routeshare.passenger.repository;

import com.routeshare.passenger.entity.PassengerDocumentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerDocumentRepository extends JpaRepository<PassengerDocumentEntity, Long> {
  List<PassengerDocumentEntity> findByAppUserIdOrderByIdDesc(long appUserId);

  Optional<PassengerDocumentEntity> findByIdAndAppUserId(long id, long appUserId);
}
