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

  @Test
  void getReturnsFallbackDefaultsWhenRowMissing() {
    when(repo.current()).thenReturn(Optional.empty());

    var res = service.get();

    assertThat(res.defaultSearchRadiusMeters()).isEqualTo(1_000);
    assertThat(res.maxDepartureWindowMinutes()).isEqualTo(720);
  }

  @Test
  void updatePersistsAndReturnsValues() {
    when(repo.current()).thenReturn(Optional.empty());
    when(repo.save(any(MatchingSettingsEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    var res = service.update(new MatchingSettingsRequest(1500, 6000, 90, 300));

    assertThat(res.defaultSearchRadiusMeters()).isEqualTo(1500);
    assertThat(res.maxSearchRadiusMeters()).isEqualTo(6000);
    assertThat(res.defaultDepartureWindowMinutes()).isEqualTo(90);
  }

  @Test
  void updateRejectsDefaultExceedingMax() {
    assertThatThrownBy(() -> service.update(new MatchingSettingsRequest(6000, 1500, 90, 300)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
