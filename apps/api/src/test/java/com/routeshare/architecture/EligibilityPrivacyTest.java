package com.routeshare.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * QA 08-21 and 08-22, asserted structurally rather than per endpoint.
 *
 * <p>Gender and the NIC are the two values in this slice that must never leave the server, and a
 * test that checks a handful of responses proves nothing about the next one somebody writes. So the
 * assertion is over the shape of the code: no response DTO declares either field, anywhere.
 *
 * <p>Gender exists to decide one thing — whether a rider may book a women-only trip — and every
 * other use of it is a disclosure the rider never agreed to. The NIC number is worse: it is the
 * single identifier that unlocks a person's records everywhere else in Sri Lanka.
 */
class EligibilityPrivacyTest {

  private static final Path MAIN_JAVA = Path.of("src/main/java/com/routeshare");

  @Test
  @DisplayName("08-21: no response DTO carries a gender field")
  void noResponseCarriesGender() throws IOException {
    // The driver's *policy* is a different thing and is allowed: "this trip carries women only" is
    // a fact about the trip, not about a person.
    List<Path> offenders =
        responseTypes()
            .filter(
                path -> {
                  String source = read(path);
                  return source.matches("(?s).*\\bString gender\\b.*")
                      || source.matches("(?s).*\\bGender gender\\b.*");
                })
            .toList();

    assertThat(offenders).isEmpty();
  }

  @Test
  @DisplayName("08-22: no response DTO carries an NIC number")
  void noResponseCarriesAnNicNumber() throws IOException {
    List<Path> offenders =
        responseTypes()
            .filter(
                path -> {
                  String source = read(path).toLowerCase(java.util.Locale.ROOT);
                  return source.contains("nicnumber")
                      || source.contains("nic_number")
                      || source.contains("string nic");
                })
            .toList();

    assertThat(offenders).isEmpty();
  }

  @Test
  @DisplayName(
      "a HIDDEN photo is resolved server-side, so no DTO takes a raw visibility + URL pair")
  void photoVisibilityIsNeverShippedAlongsideTheUrl() throws IOException {
    // Emitting both would let a client decide, and the first client that got it wrong would show a
    // face somebody had deliberately hidden.
    //
    // Two exemptions, both about the caller's own face rather than somebody else's:
    // PhotoVisibilityResponse is the settings screen and carries no URL at all, and
    // AppContextResponse returns the signed-in user their own photo alongside their own setting,
    // which is the one pairing that discloses nothing.
    java.util.Set<String> ownFaceOnly =
        java.util.Set.of("PhotoVisibilityResponse.java", "AppContextResponse.java");
    List<Path> offenders =
        responseTypes()
            .filter(path -> !ownFaceOnly.contains(path.getFileName().toString()))
            .filter(
                path -> {
                  String source = read(path);
                  return source.contains("photoVisibility") && source.contains("photoUrl");
                })
            .toList();

    assertThat(offenders).isEmpty();
  }

  private static Stream<Path> responseTypes() throws IOException {
    return Files.walk(MAIN_JAVA)
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".java"))
        .filter(path -> path.toString().contains("/dto/response/"));
  }

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read " + path, e);
    }
  }
}
