package com.routeshare.passenger.service;

import com.routeshare.passenger.dto.request.TrustedContactRequest;
import com.routeshare.passenger.dto.response.TrustedContactResponse;
import java.util.List;

public interface TrustedContactService {
  TrustedContactResponse create(TrustedContactRequest req);

  List<TrustedContactResponse> listMine();

  TrustedContactResponse get(long trustedContactId);

  TrustedContactResponse getMine(long trustedContactId);

  TrustedContactResponse update(long trustedContactId, TrustedContactRequest req);

  void delete(long trustedContactId);
}
