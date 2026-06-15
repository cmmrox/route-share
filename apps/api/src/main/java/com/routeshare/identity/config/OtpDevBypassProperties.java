package com.routeshare.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routeshare.otp")
public record OtpDevBypassProperties(boolean devBypassEnabled, String devBypassCode) {
  public OtpDevBypassProperties {
    devBypassCode = devBypassCode == null ? "" : devBypassCode.trim();
  }

  public boolean isEnabled() {
    return devBypassEnabled;
  }

  public boolean accepts(String code) {
    return devBypassEnabled && !devBypassCode.isBlank() && devBypassCode.equals(code);
  }
}
