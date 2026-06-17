package com.routeshare.driver.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.dto.response.DriverDocumentResponse;
import com.routeshare.driver.entity.DriverDocumentEntity;
import com.routeshare.driver.mapper.DriverMapper;
import com.routeshare.driver.repository.DriverDocumentRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.storage.config.ObjectStorageProperties;
import com.routeshare.storage.domain.DocumentUploadPolicy.InvalidUploadException;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.service.ObjectStoragePort;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DriverDocumentServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final com.routeshare.identity.facade.IdentityFacade identityFacade =
      mock(com.routeshare.identity.facade.IdentityFacade.class);
  private final DriverProfileRepository drivers = mock(DriverProfileRepository.class);
  private final DriverDocumentRepository documents = mock(DriverDocumentRepository.class);
  private final DriverMapper mapper = mock(DriverMapper.class);
  private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
  private final ObjectStorageProperties props =
      new ObjectStorageProperties(
          true, "http://localhost:9000", "us-east-1", "bucket", "ak", "sk", true, 900);
  private final DomainEventPublisher events = mock(DomainEventPublisher.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC);

  private final DriverDocumentServiceImpl service =
      new DriverDocumentServiceImpl(
          current, identityFacade, drivers, documents, mapper, storage, props, events, clock);

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "d@test", null, "Driver", Set.of("DRIVER"));
    var appUser = new AppUser(7L, UUID.randomUUID(), "sub", "d@test", null, "Driver", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(drivers.findIdByAppUserId(7L)).thenReturn(Optional.of(77L));
  }

  @Test
  void createUploadUrlValidatesPersistsAndPresigns() {
    var req = new UploadUrlRequest("LICENCE", "application/pdf", 2048, "licence.pdf");
    when(documents.save(any(DriverDocumentEntity.class)))
        .thenAnswer(
            inv -> {
              DriverDocumentEntity e = inv.getArgument(0);
              e.setId(101L);
              return e;
            });
    when(storage.createUploadUrl(anyString(), eq("application/pdf"), any(Duration.class)))
        .thenAnswer(
            inv ->
                new ObjectStoragePort.PresignedUpload(
                    inv.getArgument(0),
                    URI.create("http://localhost:9000/bucket/" + inv.getArgument(0) + "?sig=x"),
                    "PUT",
                    Map.of("Content-Type", "application/pdf")));

    var res = service.createUploadUrl(req);

    assertThat(res.documentId()).isEqualTo(101L);
    assertThat(res.storageKey()).startsWith("driver/77/LICENCE/");
    assertThat(res.uploadUrl()).contains("sig=x");
    assertThat(res.expiresInSeconds()).isEqualTo(900);
  }

  @Test
  void createUploadUrlRejectsUnsupportedType() {
    var req = new UploadUrlRequest("LICENCE", "image/gif", 2048, "x.gif");
    assertThatThrownBy(() -> service.createUploadUrl(req))
        .isInstanceOf(InvalidUploadException.class);
    verify(documents, never()).save(any());
  }

  @Test
  void submitFailsWhenObjectMissingFromStorage() {
    var doc =
        DriverDocumentEntity.awaitingUpload(
            77L, "LICENCE", "driver/77/LICENCE/x.pdf", "application/pdf", 10L, "x.pdf");
    when(documents.findByIdAndDriverProfileId(101L, 77L)).thenReturn(Optional.of(doc));
    when(storage.exists("driver/77/LICENCE/x.pdf")).thenReturn(false);

    assertThatThrownBy(() -> service.submit(101L)).isInstanceOf(ResponseStatusException.class);
    verify(events, never()).publish(any());
  }

  @Test
  void submitMarksSubmittedAndPublishesEvent() {
    var doc =
        DriverDocumentEntity.awaitingUpload(
            77L, "LICENCE", "driver/77/LICENCE/x.pdf", "application/pdf", 10L, "x.pdf");
    when(documents.findByIdAndDriverProfileId(101L, 77L)).thenReturn(Optional.of(doc));
    when(storage.exists("driver/77/LICENCE/x.pdf")).thenReturn(true);
    when(mapper.toDocumentResponse(doc))
        .thenReturn(
            new DriverDocumentResponse(
                101L,
                "LICENCE",
                "SUBMITTED",
                "application/pdf",
                10L,
                "x.pdf",
                null,
                Instant.now(clock),
                null,
                Instant.now(clock)));

    var res = service.submit(101L);

    assertThat(res.status()).isEqualTo("SUBMITTED");
    assertThat(doc.getStatus()).isEqualTo(DriverDocumentEntity.STATUS_SUBMITTED);
    assertThat(doc.getSubmittedAt()).isEqualTo(Instant.now(clock));
    verify(events).publish(any());
  }
}
