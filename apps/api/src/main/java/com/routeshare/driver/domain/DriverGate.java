package com.routeshare.driver.domain;

/**
 * One reason a driver cannot drive or cannot publish, in the shape the app renders directly.
 *
 * @param code one of {@code com.routeshare.common.errors.GateCodes}
 * @param message user-facing text — never a reviewer's note, an admin's name or a document id
 * @param actionPath the screen that clears the gate
 */
public record DriverGate(String code, String message, String actionPath) {}
