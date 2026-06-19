package com.routeshare.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.admin.dto.AdminDocReviewRequest;
import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.entity.DriverDocumentEntity;
import com.routeshare.driver.repository.DriverDocumentRepository;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.passenger.repository.PassengerDocumentRepository;
import com.routeshare.storage.config.ObjectStorageProperties;
import com.routeshare.storage.service.ObjectStoragePort;
import com.routeshare.vehicle.repository.VehicleDocumentRepository;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminDocumentServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final DriverDocumentRepository driverDocs = mock(DriverDocumentRepository.class);
  private final VehicleDocumentRepository vehicleDocs = mock(VehicleDocumentRepository.class);
  private final PassengerDocumentRepository passengerDocs = mock(PassengerDocumentRepository.class);
  private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
  private final ObjectStorageProperties props =
      new ObjectStorageProperties(
          true, "http://localhost:9000", "us-east-1", "bucket", "ak", "sk", true, 900);
  private final DomainEventPublisher events = mock(DomainEventPublisher.class);
  private final AdminAuditService audit = mock(AdminAuditService.class);
  private final AdminDocumentServiceImpl service =
      new AdminDocumentServiceImpl(
          current,
          identityFacade,
          driverDocs,
          vehicleDocs,
          passengerDocs,
          storage,
          props,
          events,
          audit);

  @BeforeEach
  void setUp() {
    var admin = new CurrentUser("a", "a@test", null, "Admin", Set.of("VERIFICATION_AGENT"));
    when(current.requireCurrentUser()).thenReturn(admin);
    when(identityFacade.upsertFromToken(admin))
        .thenReturn(new AppUser(99L, UUID.randomUUID(), "a", "a@test", null, "Admin", "ACTIVE"));
  }

  @Test
  void approveDriverDocumentSetsApprovedAuditsAndEmitsEvent() {
    var doc =
        DriverDocumentEntity.awaitingUpload(
            77L, "LICENCE", "driver/77/LICENCE/x.pdf", "application/pdf", 10L, "x.pdf");
    doc.setId(5L);
    when(driverDocs.findById(5L)).thenReturn(Optional.of(doc));

    var res = service.reviewDriverDocument(5L, new AdminDocReviewRequest("APPROVE", null));

    assertThat(res.status()).isEqualTo("APPROVED");
    assertThat(doc.getReviewedBy()).isEqualTo(99L);
    verify(audit).record(eq("DOCUMENT_APPROVED"), eq("driver_document"), eq("5"), any());
    verify(events).publish(any());
  }

  @Test
  void downloadUrlPresignsTheStoredObject() {
    var doc =
        DriverDocumentEntity.awaitingUpload(
            77L, "LICENCE", "driver/77/LICENCE/x.pdf", "application/pdf", 10L, "x.pdf");
    doc.setId(5L);
    when(driverDocs.findById(5L)).thenReturn(Optional.of(doc));
    when(storage.createDownloadUrl(eq("driver/77/LICENCE/x.pdf"), any(Duration.class)))
        .thenReturn(URI.create("http://localhost:9000/bucket/driver/77/LICENCE/x.pdf?sig=y"));

    var res = service.driverDocumentDownloadUrl(5L);

    assertThat(res.downloadUrl()).contains("sig=y");
    assertThat(res.expiresInSeconds()).isEqualTo(900);
  }
}
