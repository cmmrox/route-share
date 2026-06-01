package com.routeshare.passenger.facade.impl;

import com.routeshare.passenger.facade.PassengerFacade;
import com.routeshare.passenger.repository.PassengerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PassengerFacadeImpl implements PassengerFacade {
  private final PassengerProfileRepository passengers;

  @Override
  public boolean existsPassengerProfileByAppUserId(long appUserId) {
    return passengers.existsByAppUserId(appUserId);
  }
}
