package com.routeshare.storage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.routeshare.storage.domain.DocumentUploadPolicy.InvalidUploadException;
import org.junit.jupiter.api.Test;

class DocumentUploadPolicyTest {

  @Test
  void acceptsAllowedTypeAndSize() {
    // does not throw
    DocumentUploadPolicy.validate("image/jpeg", 1024);
    DocumentUploadPolicy.validate("application/pdf", DocumentUploadPolicy.MAX_FILE_SIZE_BYTES);
  }

  @Test
  void rejectsUnsupportedContentType() {
    assertThatThrownBy(() -> DocumentUploadPolicy.validate("image/gif", 1024))
        .isInstanceOf(InvalidUploadException.class)
        .hasMessageContaining("Unsupported content type");
  }

  @Test
  void rejectsNonPositiveSize() {
    assertThatThrownBy(() -> DocumentUploadPolicy.validate("image/png", 0))
        .isInstanceOf(InvalidUploadException.class);
  }

  @Test
  void rejectsOversizeFile() {
    assertThatThrownBy(
            () ->
                DocumentUploadPolicy.validate(
                    "image/png", DocumentUploadPolicy.MAX_FILE_SIZE_BYTES + 1))
        .isInstanceOf(InvalidUploadException.class)
        .hasMessageContaining("10 MB");
  }

  @Test
  void storageKeyIsNamespacedAndExtensionMatchesContentType() {
    String key = DocumentUploadPolicy.storageKey("driver", 42, "LICENCE", "application/pdf");
    assertThat(key).startsWith("driver/42/LICENCE/").endsWith(".pdf");
  }

  @Test
  void storageKeySanitizesDocumentType() {
    String key = DocumentUploadPolicy.storageKey("vehicle", 7, "reg number!", "image/jpeg");
    assertThat(key).startsWith("vehicle/7/reg_number_/").endsWith(".jpg");
  }
}
