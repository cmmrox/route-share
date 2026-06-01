package com.routeshare.identity.service;

import com.routeshare.identity.dto.response.AuthMeResponse;

public interface AuthMeService {
  AuthMeResponse current();
}
