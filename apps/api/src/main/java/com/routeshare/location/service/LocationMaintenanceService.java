package com.routeshare.location.service;

public interface LocationMaintenanceService {
  int sweepStaleness(int batchSize);

  int retainSamples();
}
