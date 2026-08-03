package com.routeshare.rewards.domain;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ReferralCodeGenerator {
  private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
  private final SecureRandom random = new SecureRandom();

  public String generate(String displayName) {
    String cleaned =
        displayName == null
            ? ""
            : displayName.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-HJ-NP-Z]", "");
    String stem = cleaned.substring(0, Math.min(5, cleaned.length()));
    if (stem.length() < 2) {
      stem = "CMG";
    }
    StringBuilder code = new StringBuilder(stem);
    while (code.length() < 10) {
      code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
    }
    return code.toString();
  }
}
