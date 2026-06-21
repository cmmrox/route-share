package com.routeshare.appreadiness;

import static org.assertj.core.api.Assertions.assertThat;

import com.routeshare.driver.controller.DriverAppReadinessController;
import com.routeshare.passenger.controller.PassengerAppReadinessController;
import com.routeshare.platform.controller.AppConfigController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

class Phase065AppBackendReadinessContractTest {
  @Test
  void passengerDriverAdminReadinessControllersExist() {
    assertThat(AppConfigController.class).isNotNull();
    assertThat(PassengerAppReadinessController.class).isNotNull();
    assertThat(DriverAppReadinessController.class).isNotNull();
    // Admin driver-application review consolidated onto the real AdminDriverReviewController
    // (Phase 06.6-K); the workflow_item AdminAppReadinessController was removed.
    assertThat(com.routeshare.admin.controller.AdminDriverReviewController.class).isNotNull();
  }

  @Test
  void criticalPhase065ContractMethodsAreMapped() throws Exception {
    assertThat(AppConfigController.class.getMethod("config").isAnnotationPresent(GetMapping.class))
        .isTrue();
    // Early drop-off moved to the real PassengerBookingController in Phase 06.6-K.
    assertThat(
            com.routeshare.booking.controller.PassengerBookingController.class
                .getDeclaredMethod(
                    "earlyDropOff",
                    long.class,
                    com.routeshare.booking.dto.request.EarlyDropOffRequest.class)
                .isAnnotationPresent(PostMapping.class))
        .isTrue();
    // SOS moved to the real safety module in Phase 06.6-E.
    assertThat(
            com.routeshare.safety.controller.PassengerSosController.class
                .getDeclaredMethod("raise", com.routeshare.safety.dto.RaiseSosRequest.class)
                .isAnnotationPresent(PostMapping.class))
        .isTrue();
    assertThat(
            DriverAppReadinessController.class
                .getMethod("verificationStatus")
                .isAnnotationPresent(GetMapping.class))
        .isTrue();
    // Payout profile moved to the real DriverPayoutController in Phase 06.6-F.
    assertThat(
            com.routeshare.driver.controller.DriverPayoutController.class
                .getDeclaredMethod(
                    "update", com.routeshare.driver.dto.request.PayoutProfileRequest.class)
                .isAnnotationPresent(PutMapping.class))
        .isTrue();
    // Dashboard moved to the real AdminDashboardController in Phase 06.6-G3.
    assertThat(
            com.routeshare.admin.controller.AdminDashboardController.class
                .getDeclaredMethod("dashboard")
                .isAnnotationPresent(GetMapping.class))
        .isTrue();
    // Audit actions moved to the real AdminAuditController in Phase 06.6-G.
    assertThat(
            com.routeshare.admin.controller.AdminAuditController.class
                .getDeclaredMethod("actions", int.class)
                .isAnnotationPresent(GetMapping.class))
        .isTrue();
  }
}
