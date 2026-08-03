package com.routeshare.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.routeshare.location.facade.impl.TripProgressFacadeImpl;
import com.routeshare.location.repository.LocationPipelineRepository;
import com.routeshare.location.service.LocationPipelineService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateTripsNearIT {
  @Test
  void exposesOnlyOfferableRowsReturnedByTheStDwithinQuery() {
    var service = mock(LocationPipelineService.class);
    var repository = mock(LocationPipelineRepository.class);
    var row = mock(LocationPipelineRepository.CandidateRow.class);
    when(row.getTripId()).thenReturn(7L);
    when(row.getRouteFraction()).thenReturn(new BigDecimal("0.42"));
    when(row.getConfidence()).thenReturn("MATCHED");
    when(row.getLatitude()).thenReturn(6.9271);
    when(row.getLongitude()).thenReturn(79.8612);
    when(repository.candidateTripsNear(6.9271, 79.8612, 500)).thenReturn(List.of(row));

    var candidates =
        new TripProgressFacadeImpl(service, repository).candidateTripsNear(6.9271, 79.8612, 500);

    assertThat(candidates)
        .singleElement()
        .satisfies(candidate -> assertThat(candidate.tripId()).isEqualTo(7));
    verify(repository).candidateTripsNear(6.9271, 79.8612, 500);
  }
}
