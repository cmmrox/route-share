package com.routeshare.platform.service;

import com.routeshare.platform.dto.request.UserSettingsRequest;
import com.routeshare.platform.dto.response.AccountRequestResponse;
import com.routeshare.platform.dto.response.UserSettingsResponse;
import java.util.List;

public interface UserSettingsService {
  UserSettingsResponse mine();

  UserSettingsResponse forAppUser(long appUserId);

  UserSettingsResponse update(UserSettingsRequest request);

  AccountRequestResponse requestDataExport();

  AccountRequestResponse requestDeletion();

  List<AccountRequestResponse> listAccountRequests();
}
