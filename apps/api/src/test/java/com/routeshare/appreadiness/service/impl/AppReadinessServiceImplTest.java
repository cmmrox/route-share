package com.routeshare.appreadiness.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.appreadiness.entity.WorkflowItemEntity;
import com.routeshare.appreadiness.repository.WorkflowItemRepository;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

class AppReadinessServiceImplTest {
  private final WorkflowItemRepository items = mock(WorkflowItemRepository.class);
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final AppReadinessServiceImpl service =
      new AppReadinessServiceImpl(items, current, identityFacade, new ObjectMapper());

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "user@example.test", null, "User", Set.of("PASSENGER"));
    var appUser =
        new AppUser(7L, UUID.randomUUID(), "sub", "user@example.test", null, "User", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(items.save(any(WorkflowItemEntity.class))).thenAnswer(assigningIds());
  }

  @Test
  void appConfigExposesEnabledFeatureFlagsForApps() {
    var config = service.appConfig();

    assertThat(config).containsEntry("currency", "LKR").containsEntry("country", "LK");
    assertThat((Map<String, Object>) config.get("features"))
        .containsEntry("liveTracking", true)
        .containsEntry("support", true)
        .containsEntry("recurringRoutes", true);
  }

  @Test
  void verificationStatusUsesLatestSubmittedKycStates() {
    when(items.findByItemTypeAndOwnerAppUserIdOrderByIdDesc("DRIVER_KYC_IDENTITY", 7L))
        .thenReturn(List.of(item(1L, "DRIVER_KYC_IDENTITY", "DRIVER", 7L, "APPROVED", "{}")));
    when(items.findByItemTypeAndOwnerAppUserIdOrderByIdDesc("DRIVER_KYC_LICENCE", 7L))
        .thenReturn(List.of(item(2L, "DRIVER_KYC_LICENCE", "DRIVER", 7L, "SUBMITTED", "{}")));

    var status = service.verificationStatus();

    assertThat(status)
        .containsEntry("appUserId", 7L)
        .containsEntry("identityStatus", "APPROVED")
        .containsEntry("licenceStatus", "SUBMITTED")
        .containsEntry("canCreateRoutes", true);
  }

  @Test
  void createPersistsWorkflowItemAndAuditRecordWithDefaultSupportStatus() {
    var created =
        service.create("SUPPORT_TICKET", "PASSENGER", "PASSENGER", "7", Map.of("title", "Help"));

    assertThat(created)
        .containsEntry("type", "SUPPORT_TICKET")
        .containsEntry("status", "OPEN")
        .containsEntry("title", "Help");
    verify(items, times(2)).save(any(WorkflowItemEntity.class));
  }

  @Test
  void createRejectsUnserializablePayload() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("self", payload);

    assertThatThrownBy(
            () -> service.create("SUPPORT_TICKET", "PASSENGER", "PASSENGER", "7", payload))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Payload is not serializable");
  }

  @Test
  void updateChangesStatusTitleAndPayload() {
    var existing = item(55L, "SUPPORT_TICKET", "PASSENGER", 7L, "OPEN", "{\"title\":\"Old\"}");
    when(items.findById(55L)).thenReturn(Optional.of(existing));

    var updated = service.update(55L, Map.of("status", "CLOSED", "title", "Done"));

    assertThat(updated).containsEntry("status", "CLOSED").containsEntry("title", "Done");
    verify(items).save(existing);
  }

  @Test
  void preferencesReturnsDefaultWhenNoPreferenceExists() {
    when(items.findByItemTypeAndOwnerAppUserIdOrderByIdDesc("NOTIFICATION_PREFERENCE", 7L))
        .thenReturn(List.of());

    var preferences = service.preferences("PASSENGER");

    assertThat((Map<String, Object>) preferences.get("channels"))
        .containsEntry("push", true)
        .containsEntry("sms", false);
  }

  @Test
  void markReadUpdatesNotificationStatus() {
    var notification = item(70L, "NOTIFICATION", "PASSENGER", 7L, "UNREAD", "{}");
    when(items.findById(70L)).thenReturn(Optional.of(notification));

    var read = service.markRead(70L);

    assertThat(read).containsEntry("status", "READ");
  }

  @Test
  void shareBookingAddsShareUrlBeforePersisting() {
    var response = service.shareBooking(99L, Map.of("title", "Share"));

    assertThat((Map<String, Object>) response.get("payload"))
        .containsEntry("shareUrl", "https://routeshare.local/share/booking/99");
  }

  @Test
  void payoutProfileReturnsNotConfiguredWhenDriverHasNoProfile() {
    when(items.findByItemTypeAndOwnerAppUserIdOrderByIdDesc("PAYOUT_PROFILE", 7L))
        .thenReturn(List.of());

    assertThat(service.payoutProfile()).containsEntry("status", "NOT_CONFIGURED");
  }

  @Test
  void dashboardSummarizesSupportSosAndAuditActions() {
    when(items.findTop50ByItemTypeOrderByIdDesc("SUPPORT_TICKET"))
        .thenReturn(List.of(item(1L, "SUPPORT_TICKET", "PASSENGER", 7L, "OPEN", "{}")));
    when(items.findTop50ByItemTypeOrderByIdDesc("SOS_EVENT"))
        .thenReturn(List.of(item(2L, "SOS_EVENT", "DRIVER", 8L, "OPEN", "{}")));
    when(items.findTop50ByItemTypeOrderByIdDesc("AUDIT_ACTION"))
        .thenReturn(List.of(item(3L, "AUDIT_ACTION", "SYSTEM", 7L, "RECORDED", "{}")));

    var dashboard = service.dashboard();

    assertThat(dashboard).containsEntry("openSupportTickets", 1).containsEntry("openSosEvents", 1);
    assertThat((List<?>) dashboard.get("recentAuditActions")).hasSize(1);
  }

  private static WorkflowItemEntity item(
      long id, String type, String ownerRole, long ownerId, String status, String payload) {
    var entity =
        WorkflowItemEntity.create(
            type, ownerRole, ownerId, "TARGET", "target-1", status, type, payload);
    entity.setId(id);
    return entity;
  }

  private static Answer<WorkflowItemEntity> assigningIds() {
    return invocation -> {
      WorkflowItemEntity entity = invocation.getArgument(0);
      if (entity.getId() == null) {
        entity.setId(Math.abs(Objects.hash(entity.getItemType(), entity.getTitle())) + 1L);
      }
      return entity;
    };
  }
}
