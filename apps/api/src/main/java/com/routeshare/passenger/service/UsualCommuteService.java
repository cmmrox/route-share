package com.routeshare.passenger.service;

import com.routeshare.passenger.dto.request.UsualCommuteRequest;
import com.routeshare.passenger.dto.response.UsualCommuteResponse;

/**
 * P02 — the trip a rider makes over and over, and how many drivers are on it right now.
 *
 * <p>A saved search rather than a new domain. The match count runs the ordinary search over the
 * stored pair with a small window; duplicating the query here would give the dashboard a number the
 * list it opens could contradict.
 */
public interface UsualCommuteService {

  /** The saved commute with its live match count, or an empty answer if she has not saved one. */
  UsualCommuteResponse mine();

  UsualCommuteResponse save(UsualCommuteRequest request);

  void clear();
}
