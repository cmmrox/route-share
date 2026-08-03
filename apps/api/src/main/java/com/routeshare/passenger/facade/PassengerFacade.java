package com.routeshare.passenger.facade;

import java.util.List;

public interface PassengerFacade {
  boolean existsPassengerProfileByAppUserId(long appUserId);

  /** Trusted contacts (name + phone) for a passenger, used for safety/share notifications. */
  List<TrustedContact> findTrustedContacts(long appUserId);

  record TrustedContact(String name, String phone, boolean autoShareSos) {}

  /**
   * The two facts eligibility turns on, and nothing else.
   *
   * <p>Deliberately narrow. Gender is an eligibility input only — widening this record into "the
   * rider's profile" is how it would end up on a response by accident.
   */
  RiderEligibilityProfile riderEligibilityProfile(long appUserId);

  /** How a rider's photo is shown, for the app shell. */
  String photoVisibilityOf(long appUserId);

  record RiderEligibilityProfile(String verificationLevel, String gender) {
    public boolean isVerified() {
      return "VERIFIED".equals(verificationLevel);
    }

    public boolean isFemale() {
      return "FEMALE".equals(gender);
    }

    /** A rider with no profile row yet: unverified, and no gender on record. */
    public static RiderEligibilityProfile unknown() {
      return new RiderEligibilityProfile("NONE", "UNSPECIFIED");
    }
  }
}
