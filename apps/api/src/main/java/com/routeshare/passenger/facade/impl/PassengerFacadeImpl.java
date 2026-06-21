package com.routeshare.passenger.facade.impl;

import com.routeshare.passenger.facade.PassengerFacade;
import com.routeshare.passenger.repository.PassengerProfileRepository;
import com.routeshare.passenger.repository.TrustedContactRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PassengerFacadeImpl implements PassengerFacade {
  private final PassengerProfileRepository passengers;
  private final TrustedContactRepository trustedContacts;

  @Override
  public boolean existsPassengerProfileByAppUserId(long appUserId) {
    return passengers.existsByAppUserId(appUserId);
  }

  @Override
  public List<TrustedContact> findTrustedContacts(long appUserId) {
    return trustedContacts.findByAppUserIdOrderByIdDesc(appUserId).stream()
        .map(c -> new TrustedContact(c.getName(), c.getPhone()))
        .toList();
  }
}
