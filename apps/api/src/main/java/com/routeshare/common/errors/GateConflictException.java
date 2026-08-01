package com.routeshare.common.errors;

/**
 * The caller asked for a state they are not eligible for — switching into a mode that is gated, for
 * instance. A 409 rather than a 403: the request was understood and authorised, it just conflicts
 * with the account's current state, and the same {@link GateCodes} vocabulary explains why.
 */
public class GateConflictException extends RuntimeException {
  private final String code;
  private final String actionPath;

  public GateConflictException(String code, String message, String actionPath) {
    super(message);
    this.code = code;
    this.actionPath = actionPath;
  }

  public String code() {
    return code;
  }

  public String actionPath() {
    return actionPath;
  }
}
