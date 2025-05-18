package org.akj.test.tracker.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class CredentialEncryptorTest {

  @Test
  public void generateKey() {
    SecureRandom secureRandom = new SecureRandom();
    for (int i = 0; i < 20; i++) {
      byte[] key = new byte[32]; // 256-bit key
      secureRandom.nextBytes(key);
      String encodedKey = Base64.getEncoder().encodeToString(key);
      System.out.println("Key " + (i + 1) + ": " + encodedKey);
    }
  }
}
