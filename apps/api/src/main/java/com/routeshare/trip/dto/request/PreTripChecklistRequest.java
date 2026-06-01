package com.routeshare.trip.dto.request;

public record PreTripChecklistRequest(
    boolean vehicleChecked, boolean documentsReady, boolean routeReviewed, String notes) {}
