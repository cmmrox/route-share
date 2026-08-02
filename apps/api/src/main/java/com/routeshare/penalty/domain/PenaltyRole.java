package com.routeshare.penalty.domain;

/** Which side of a booking a penalty touches. {@code NONE} is a kind with no victim to pay. */
public enum PenaltyRole {
  PASSENGER,
  DRIVER,
  NONE
}
