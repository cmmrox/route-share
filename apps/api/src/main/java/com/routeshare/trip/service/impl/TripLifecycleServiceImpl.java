package com.routeshare.trip.service.impl;

import com.routeshare.trip.repository.TripRepository;
import com.routeshare.trip.service.TripLifecycleService;
import com.routeshare.trip.service.TripStartWindowService;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TripLifecycleServiceImpl implements TripLifecycleService {

  private final TripRepository trips;
  private final TripStartWindowService startWindows;

  @Override
  @Transactional
  public long ensureTripForBookedOccurrence(long routeOccurrenceId) {
    // The insert loses harmlessly if another transaction got there first; the read that follows is
    // what decides the answer, so both racers return the same trip.
    trips.insertTripForOccurrence(routeOccurrenceId);
    long tripId =
        trips
            .findTripIdByRouteOccurrenceId(routeOccurrenceId)
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "No occurrence " + routeOccurrenceId + " to create a trip for"));

    // The clock starts from the occurrence's scheduled departure and nothing else. A departure the
    // caller supplied would be a deadline the client chose.
    Instant departsAt =
        trips
            .findScheduledDepartureForOccurrence(routeOccurrenceId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Occurrence " + routeOccurrenceId + " has no scheduled departure"));
    startWindows.open(tripId, departsAt);
    return tripId;
  }
}
