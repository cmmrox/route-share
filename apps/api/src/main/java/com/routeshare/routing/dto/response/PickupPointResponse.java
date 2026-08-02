package com.routeshare.routing.dto.response;

/**
 * Where to actually stand, and how the server worked it out.
 *
 * <p>{@code source} is returned because it is operationally load-bearing: the whole cost model of
 * this feature is that {@code CURATED} and {@code DERIVED} dominate and Places is a last resort,
 * and a hit rate you cannot see is a hit rate you cannot defend.
 */
public record PickupPointResponse(
    Long pickupPointId,
    String label,
    String description,
    String sideHint,
    double latitude,
    double longitude,
    String source) {}
