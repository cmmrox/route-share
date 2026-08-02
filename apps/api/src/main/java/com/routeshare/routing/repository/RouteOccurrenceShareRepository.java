package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RouteOccurrenceShareEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteOccurrenceShareRepository
    extends JpaRepository<RouteOccurrenceShareEntity, Long> {

  Optional<RouteOccurrenceShareEntity> findByRouteOccurrenceId(long routeOccurrenceId);

  Optional<RouteOccurrenceShareEntity> findByShortCode(String shortCode);
}
