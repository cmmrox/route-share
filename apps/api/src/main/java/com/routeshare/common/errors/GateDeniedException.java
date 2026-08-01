package com.routeshare.common.errors;

import org.springframework.security.access.AccessDeniedException;

/**
 * A refusal that explains itself: the app gets a {@link GateCodes} code, user-safe text and the
 * screen to send the user to, instead of a bare 403.
 *
 * <p>Extends {@link AccessDeniedException} on purpose, so every existing security rule that reacts
 * to an access denial keeps working; only the rendered body is richer.
 *
 * <p>The message is user-facing. It must never carry a reviewer's name, an internal note or a
 * document id.
 */
public class GateDeniedException extends AccessDeniedException {
  private final String code;
  private final String actionPath;

  public GateDeniedException(String code, String message, String actionPath) {
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

  public static GateDeniedException accountSuspended() {
    return new GateDeniedException(
        GateCodes.ACCOUNT_SUSPENDED,
        "Your account is on hold. You can't book or publish trips while this is open.",
        "/support");
  }
}
