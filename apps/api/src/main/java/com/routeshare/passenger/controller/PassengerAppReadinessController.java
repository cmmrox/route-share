package com.routeshare.passenger.controller;

import com.routeshare.appreadiness.service.AppReadinessService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PassengerAppReadinessController {
  private final AppReadinessService service;

  @GetMapping("/api/v1/passenger/ride-searches/{searchId}/results")
  public List<Map<String, Object>> rideSearchResults(@PathVariable long searchId) {
    return service.mine("RIDE_SEARCH_RESULT", "PASSENGER");
  }

  @GetMapping("/api/v1/passenger/ride-searches/{searchId}/results/{resultId}")
  public Map<String, Object> rideSearchResult(
      @PathVariable long searchId, @PathVariable long resultId) {
    return Map.of(
        "searchId",
        searchId,
        "resultId",
        resultId,
        "status",
        "DETAIL_AVAILABLE_FROM_SEARCH_RESPONSE");
  }

  @PostMapping("/api/v1/passenger/bookings/{bookingId}/early-drop-off")
  public Map<String, Object> earlyDropOff(
      @PathVariable long bookingId, @RequestBody(required = false) Map<String, Object> body) {
    return service.earlyDropOff(bookingId, body);
  }

  @PostMapping("/api/v1/passenger/bookings/{bookingId}/share")
  public Map<String, Object> share(
      @PathVariable long bookingId, @RequestBody(required = false) Map<String, Object> body) {
    return service.shareBooking(bookingId, body);
  }

  @PostMapping("/api/v1/passenger/bookings/{bookingId}/share-link")
  public Map<String, Object> shareLink(
      @PathVariable long bookingId, @RequestBody(required = false) Map<String, Object> body) {
    return service.shareBooking(bookingId, body);
  }

  @PostMapping("/api/v1/passenger/bookings/{bookingId}/rating")
  public Map<String, Object> rateBooking(
      @PathVariable long bookingId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create("RATING", "PASSENGER", "BOOKING", String.valueOf(bookingId), body);
  }

  @PostMapping("/api/v1/passenger/sos-events")
  public Map<String, Object> createSos(@RequestBody(required = false) Map<String, Object> body) {
    return service.create("SOS_EVENT", "PASSENGER", "PASSENGER", null, body);
  }

  // Notifications, preferences, and push registrations are served by the real notification module
  // (PassengerNotificationController); the workflow_item-backed versions were removed in Phase
  // 06.6-D.

  @PostMapping("/api/v1/passenger/support/tickets")
  public Map<String, Object> createSupportTicket(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.create("SUPPORT_TICKET", "PASSENGER", "PASSENGER", null, body);
  }

  @GetMapping("/api/v1/passenger/support/tickets")
  public List<Map<String, Object>> supportTickets() {
    return service.mine("SUPPORT_TICKET", "PASSENGER");
  }

  @GetMapping("/api/v1/passenger/support/tickets/{ticketId}")
  public Map<String, Object> supportTicket(@PathVariable long ticketId) {
    return service.get(ticketId);
  }

  @PostMapping("/api/v1/passenger/support/tickets/{ticketId}/messages")
  public Map<String, Object> supportTicketMessage(
      @PathVariable long ticketId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "SUPPORT_MESSAGE", "PASSENGER", "SUPPORT_TICKET", String.valueOf(ticketId), body);
  }

  // Payment methods are served by the real tokenized PaymentMethodController (Phase 06.6-C);
  // the workflow_item-backed versions were removed here to avoid duplicate request mappings.

  @PostMapping("/api/v1/passenger/profile/avatar-upload")
  public Map<String, Object> avatarUpload(@RequestBody(required = false) Map<String, Object> body) {
    return service.create("AVATAR_UPLOAD", "PASSENGER", "PASSENGER", null, body);
  }

  @GetMapping("/api/v1/passenger/verification/status")
  public Map<String, Object> verificationStatus() {
    return Map.of("status", "OPTIONAL", "required", false);
  }

  @PostMapping("/api/v1/passenger/verification/documents")
  public Map<String, Object> verificationDocument(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.create("PASSENGER_DOCUMENT", "PASSENGER", "PASSENGER", null, body);
  }
}
