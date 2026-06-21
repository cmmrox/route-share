package com.routeshare.passenger.facade;

import java.util.List;

public interface PassengerFacade {
  boolean existsPassengerProfileByAppUserId(long appUserId);

  /** Trusted contacts (name + phone) for a passenger, used for safety/share notifications. */
  List<TrustedContact> findTrustedContacts(long appUserId);

  record TrustedContact(String name, String phone) {}
}
