package com.routeshare.location.domain;

public enum LocationRejectionReason {
  ACCURACY_TOO_LOW,
  IMPLAUSIBLE_SPEED,
  OFF_ROUTE,
  BACKWARD_PROGRESS,
  DUPLICATE,
  OUT_OF_ORDER
}
