package com.routeshare.routing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.routeshare.routing.dto.request.MatchingSettingsRequest;
import com.routeshare.routing.entity.MatchingSettingsEntity;
import com.routeshare.routing.repository.MatchingSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MatchingSettingsServiceImplTest {
  private final MatchingSettingsRepository repo = mock(MatchingSettingsRepository.class);
  private final MatchingSettingsServiceImpl service = new MatchingSettingsServiceImpl(repo);

  private static final java.util.List<Integer> OFFERED = java.util.List.of(5_000, 10_000, 20_000);

  @Test
  void getReturnsFallbackDefaultsWhenRowMissing() {
    when(repo.current()).thenReturn(Optional.empty());

    var res = service.get();

    // The fallback is the product's stated rule, not a neutral value: a missing settings row must
    // degrade to "20 km, three chips" rather than to a radius nobody chose.
    assertThat(res.defaultTripStartRadiusMeters()).isEqualTo(20_000);
    assertThat(res.allowedTripStartRadiiMeters()).containsExactly(5_000, 10_000, 20_000);
    assertThat(res.maxDepartureWindowMinutes()).isEqualTo(720);
  }

  @Test
  void updatePersistsAndReturnsValues() {
    when(repo.current()).thenReturn(Optional.empty());
    when(repo.save(any(MatchingSettingsEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    var res = service.update(new MatchingSettingsRequest(10_000, 20_000, OFFERED, 90, 300));

    assertThat(res.defaultTripStartRadiusMeters()).isEqualTo(10_000);
    assertThat(res.maxTripStartRadiusMeters()).isEqualTo(20_000);
    assertThat(res.defaultDepartureWindowMinutes()).isEqualTo(90);
  }

  @Test
  void updateRejectsDefaultExceedingMax() {
    assertThatThrownBy(
            () -> service.update(new MatchingSettingsRequest(20_000, 10_000, OFFERED, 90, 300)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void updateRejectsARadiusAboveTheProductCeiling() {
    // 20 km is a product decision with a reason behind it — past that a driver is making a trip
    // for the rider rather than sharing one — so no operator setting may exceed it.
    assertThatThrownBy(
            () ->
                service.update(
                    new MatchingSettingsRequest(
                        20_000, 50_000, java.util.List.of(20_000), 90, 300)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("20 km");
  }

  @Test
  void updateRejectsADefaultTheScreenCannotOffer() {
    // A default that is not one of the chips renders as no chip selected at all.
    assertThatThrownBy(
            () -> service.update(new MatchingSettingsRequest(7_000, 20_000, OFFERED, 90, 300)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("offered options");
  }

  @Test
  void updateRejectsAnOfferedRadiusAboveTheMaximum() {
    assertThatThrownBy(
            () ->
                service.update(
                    new MatchingSettingsRequest(
                        5_000, 10_000, java.util.List.of(5_000, 20_000), 90, 300)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exceeds the maximum");
  }
}
