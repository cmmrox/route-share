package com.routeshare.passenger.dto.response;

import java.util.List;

/**
 * P30/P30b/P30c — the three options with the copy that explains them, and which one is in force.
 *
 * <p>The options travel with the setting so the description a rider reads and the rule the server
 * applies are written once.
 */
public record PhotoVisibilityResponse(String visibility, List<Option> options) {

  public record Option(String value, String label, String description) {}

  public static List<Option> allOptions() {
    return List.of(
        new Option(
            "PUBLIC", "Show to everyone", "Any driver browsing your request sees your photo."),
        new Option(
            "MATCHED",
            "Only my confirmed driver",
            "Hidden until a booking is confirmed. He needs it to find you at the kerb."),
        new Option(
            "HIDDEN",
            "Hide it completely",
            "Nobody sees it — not even your driver. Your initials show instead."));
  }
}
