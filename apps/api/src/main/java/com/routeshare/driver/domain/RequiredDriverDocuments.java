package com.routeshare.driver.domain;

import java.util.List;

/**
 * The KYC documents a driver must hold, approved and unexpired, before they can publish a route.
 *
 * <p>Shared so the verification screen and the publish gate can never disagree about what is
 * required — a driver told they are ready and then refused at publish is the worst of both.
 */
public final class RequiredDriverDocuments {
  private RequiredDriverDocuments() {}

  public static final List<String> TYPES = List.of("IDENTITY", "LICENCE");
}
