package com.routeshare.location.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.routeshare.location.service.LocationPipelineService;
import com.routeshare.location.service.LocationService;
import org.junit.jupiter.api.Test;

class Phase06ControllerContractTest {
  @Test
  void phase06ControllersExposeDriverPassengerAndAdminEntryPoints() {
    var service = mock(LocationService.class);

    assertThat(new DriverLocationController(mock(LocationPipelineService.class))).isNotNull();
    assertThat(new PassengerLiveTripController(service)).isNotNull();
    assertThat(new AdminLiveTripController(service)).isNotNull();
  }
}
