package com.routeshare.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards against the ambiguous-mapping startup failures that unit tests miss (no Spring context is
 * loaded in this suite). Scans controller sources and asserts that no two handler methods map the
 * same HTTP verb + path, with path variables normalised ({@code {vehicleId}} and {@code {id}} are
 * the same route to Spring). This catches duplicates introduced when a real controller supersedes a
 * facade endpoint but the facade mapping is left behind.
 */
class RequestMappingUniquenessTest {
  private static final Path MAIN = Path.of("src/main/java/com/routeshare");
  private static final Pattern CLASS_RM = Pattern.compile("@RequestMapping\\(\\s*\"([^\"]*)\"");
  private static final Pattern METHOD_MAPPING =
      Pattern.compile(
          "@(Get|Post|Put|Delete|Patch)Mapping(?:\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\")?");

  @Test
  void noDuplicateHttpVerbAndPathAcrossControllers() throws IOException {
    Map<String, String> seen = new HashMap<>();
    List<String> duplicates = new ArrayList<>();

    for (Path file : controllerFiles()) {
      String src = Files.readString(file);
      int classIdx = src.indexOf("class ");
      String head = classIdx > 0 ? src.substring(0, classIdx) : "";
      Matcher cm = CLASS_RM.matcher(head);
      String prefix = cm.find() ? cm.group(1) : "";

      String body = classIdx > 0 ? src.substring(classIdx) : src;
      Matcher mm = METHOD_MAPPING.matcher(body);
      while (mm.find()) {
        String verb = mm.group(1).toUpperCase();
        String path = mm.group(2) == null ? "" : mm.group(2);
        String full = normalize(prefix + path);
        String key = verb + " " + full;
        String owner = file.getFileName().toString();
        if (seen.containsKey(key)) {
          duplicates.add(key + " in " + owner + " and " + seen.get(key));
        } else {
          seen.put(key, owner);
        }
      }
    }

    assertThat(duplicates).as("ambiguous request mappings").isEmpty();
  }

  private static String normalize(String path) {
    String p = path.replaceAll("\\{[^}]+}", "{}");
    if (p.length() > 1 && p.endsWith("/")) {
      p = p.substring(0, p.length() - 1);
    }
    return p;
  }

  private static List<Path> controllerFiles() throws IOException {
    try (Stream<Path> walk = Files.walk(MAIN)) {
      return walk.filter(p -> p.toString().endsWith("Controller.java")).toList();
    }
  }
}
