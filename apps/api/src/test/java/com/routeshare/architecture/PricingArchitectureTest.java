package com.routeshare.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the two properties that make the fare engine trustworthy.
 *
 * <p>First, <b>no policy figure is inlined</b>. A commission rate written into a Java constant is a
 * rate that cannot be corrected without a deploy, and — worse — one that will eventually disagree
 * with the database. Decision D1 put every such number in {@code platform.policy_setting}; this
 * test is what stops the next one drifting back into code.
 *
 * <p>Second, <b>price is server-authoritative</b>. No pricing input may be read from a request
 * body: a client-supplied distance or rate is a free-money bug, and the removed {@code POST
 * /pricing/estimate} was exactly that.
 */
class PricingArchitectureTest {
  private static final Path MAIN_JAVA = Path.of("src/main/java/com/routeshare");

  private static final Pattern SUSPICIOUS_LITERAL =
      Pattern.compile(
          "\\bnew BigDecimal\\(\"(?<value>[0-9.]+)\"\\)|\\bBigDecimal\\.valueOf\\((?<v2>[0-9.]+)\\)");

  /** Percentages and money figures that are policy, expressed however Java might express them. */
  private static final List<String> FORBIDDEN_VALUES =
      List.of("10", "8", "5", "2.5", "95", "75", "45", "20", "25", "50", "1000", "150");

  @Test
  void policyFiguresAreNotInlinedInPricingSources() throws IOException {
    List<String> offenders = new ArrayList<>();
    for (Path file : pricingSources()) {
      String source = read(file);
      Matcher matcher = SUSPICIOUS_LITERAL.matcher(source);
      while (matcher.find()) {
        String value =
            matcher.group("value") != null ? matcher.group("value") : matcher.group("v2");
        if (FORBIDDEN_VALUES.contains(value) && !isAllowed(source, matcher.start())) {
          offenders.add(file + " -> " + matcher.group());
        }
      }
    }

    assertThat(offenders)
        .as("policy figures must be read from platform.policy_setting, not inlined")
        .isEmpty();
  }

  @Test
  void theRetiredCalculatorIsGone() throws IOException {
    assertThat(filesContaining("FareCalculator")).isEmpty();
    assertThat(filesContaining("FareBreakdown")).isEmpty();
  }

  @Test
  void noPricingInputIsReadFromARequestBody() throws IOException {
    // A distance or a rate arriving in a request body is a client naming its own fare. The engine
    // takes only fractions along a server-held line and the vehicle's own assessed rate.
    List<Path> offenders =
        javaFiles(MAIN_JAVA.resolve("pricing"))
            .filter(path -> path.toString().contains("/dto/request/"))
            .filter(path -> declaresPricingInput(read(path)))
            .toList();

    assertThat(offenders).isEmpty();
  }

  /**
   * Looks at declared record components, not at prose: a javadoc that explains why a fare may not
   * be sent is not itself a fare being sent.
   */
  private boolean declaresPricingInput(String source) {
    return Pattern.compile(
            "\\b(BigDecimal|Long|long|Double|double|Integer|int)\\s+"
                + "(distanceMeters|onRouteMeters|ratePerKm|fare|grossFare|passengerPays)\\b")
        .matcher(source)
        .find();
  }

  /**
   * A literal inside a comment, a scale argument or a rounding divisor is not a policy value. The
   * check is deliberately narrow: it looks for money-shaped constants, not every number.
   */
  private boolean isAllowed(String source, int position) {
    int lineStart = source.lastIndexOf('\n', position) + 1;
    String line = source.substring(lineStart, source.indexOf('\n', position) + 1).trim();
    return line.startsWith("//") || line.startsWith("*") || line.contains("METERS_PER_KM");
  }

  private List<Path> pricingSources() throws IOException {
    List<Path> files = new ArrayList<>();
    for (String module : List.of("pricing", "penalty", "reliability")) {
      Path root = MAIN_JAVA.resolve(module);
      if (Files.exists(root)) {
        files.addAll(javaFiles(root).toList());
      }
    }
    return files;
  }

  private List<Path> filesContaining(String token) throws IOException {
    return javaFiles(MAIN_JAVA).filter(path -> read(path).contains(token)).toList();
  }

  private Stream<Path> javaFiles(Path root) throws IOException {
    return Files.walk(root).filter(path -> path.toString().endsWith(".java"));
  }

  private String read(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
