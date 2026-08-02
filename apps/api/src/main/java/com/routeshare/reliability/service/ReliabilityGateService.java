package com.routeshare.reliability.service;

import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.entity.MonthlyCounterEntity;

/**
 * What happens when a counter crosses a limit.
 *
 * <p>Kept separate from recording so that the rule fires wherever an event is recorded, and so the
 * two questions — "what happened?" and "what does that cost you?" — stay legible apart.
 */
public interface ReliabilityGateService {

  void onCounterChanged(
      long appUserId,
      ReliabilityRole role,
      ReliabilityEventType type,
      MonthlyCounterEntity counter);
}
