package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RouteOccurrenceCancellationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteOccurrenceCancellationRepository
    extends JpaRepository<RouteOccurrenceCancellationEntity, Long> {

  Optional<RouteOccurrenceCancellationEntity> findByRouteOccurrenceId(long routeOccurrenceId);
}
