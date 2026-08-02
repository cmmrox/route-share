package com.routeshare.passenger.domain;

import java.util.List;

/**
 * P29a–d. The four captures, in the order the screens ask for them.
 *
 * <p>The label and hint travel with the step rather than living in the client, so the copy a rider
 * reads and the step a reviewer decides on can never drift apart.
 */
public enum VerificationStepKey {
  NIC_FRONT("NIC · front", "All four corners inside the frame, no glare on the hologram.", "CARD"),
  NIC_BACK("NIC · back", "Turn the card over. The address block has to be readable.", "CARD"),
  SELFIE_WITH_NIC(
      "Selfie holding your NIC",
      "Hold the front of the card beside your face. Both must be sharp.",
      "BOTH"),
  PROFILE_PHOTO(
      "Profile photo",
      "A plain selfie. This is the one other people may see — or may not.",
      "FACE");

  private final String label;
  private final String hint;
  private final String guideShape;

  VerificationStepKey(String label, String hint, String guideShape) {
    this.label = label;
    this.hint = hint;
    this.guideShape = guideShape;
  }

  public String label() {
    return label;
  }

  public String hint() {
    return hint;
  }

  public String guideShape() {
    return guideShape;
  }

  /** In capture order — P29a, b, c then d. */
  public static List<VerificationStepKey> inOrder() {
    return List.of(NIC_FRONT, NIC_BACK, SELFIE_WITH_NIC, PROFILE_PHOTO);
  }

  public static VerificationStepKey of(String value) {
    try {
      return VerificationStepKey.valueOf(value == null ? "" : value.trim().toUpperCase());
    } catch (IllegalArgumentException notAStep) {
      throw new IllegalArgumentException(
          "Unknown verification step. Expected one of " + inOrder(), notAStep);
    }
  }
}
