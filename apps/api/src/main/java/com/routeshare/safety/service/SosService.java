package com.routeshare.safety.service;

import com.routeshare.safety.dto.RaiseSosRequest;
import com.routeshare.safety.dto.SosEventResponse;
import java.util.List;

public interface SosService {
  SosEventResponse raise(String ownerRole, RaiseSosRequest req);

  SosEventResponse raiseCurrent(RaiseSosRequest req);

  List<SosEventResponse> listMine();

  SosEventResponse getMine(long id);
}
