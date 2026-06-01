package com.routeshare.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PersistenceArchitectureTest {
  private static final Path MAIN_JAVA = Path.of("src/main/java");

  @Test
  void jdbcTemplateIsNotUsedInMainSources() throws IOException {
    assertThat(filesContaining("JdbcTemplate", MAIN_JAVA)).isEmpty();
  }

  @Test
  void serviceImplementationsDoNotUseSqlOrLowLevelPersistenceApis() throws IOException {
    List<Path> offenders =
        javaFiles(MAIN_JAVA.resolve("com/routeshare"))
            .filter(path -> path.toString().contains("/service/impl/"))
            .filter(
                path -> {
                  String source = read(path);
                  return source.contains("EntityManager")
                      || source.contains("createNativeQuery")
                      || source.contains("JdbcTemplate")
                      || source.matches("(?s).*\\b(SELECT|INSERT|UPDATE|DELETE)\\b.*");
                })
            .toList();

    assertThat(offenders).isEmpty();
  }

  @Test
  void repositoryTypesLiveUnderRepositoryPackages() throws IOException {
    List<Path> offenders =
        javaFiles(MAIN_JAVA.resolve("com/routeshare"))
            .filter(path -> path.getFileName().toString().endsWith("Repository.java"))
            .filter(path -> !path.toString().contains("/repository/"))
            .toList();

    assertThat(offenders).isEmpty();
  }

  @Test
  void entityTypesLiveUnderEntityPackages() throws IOException {
    List<Path> offenders =
        javaFiles(MAIN_JAVA.resolve("com/routeshare"))
            .filter(path -> path.getFileName().toString().endsWith("Entity.java"))
            .filter(path -> !path.toString().contains("/entity/"))
            .toList();

    assertThat(offenders).isEmpty();
  }

  @Test
  void servicesHaveInterfacesAndImplementationsLiveUnderImpl() throws IOException {
    List<Path> offenders =
        javaFiles(MAIN_JAVA.resolve("com/routeshare"))
            .filter(path -> path.getFileName().toString().endsWith("Service.java"))
            .filter(path -> !read(path).contains("interface " + stripExtension(path)))
            .toList();

    assertThat(offenders).isEmpty();

    List<Path> implOffenders =
        javaFiles(MAIN_JAVA.resolve("com/routeshare"))
            .filter(path -> path.getFileName().toString().endsWith("ServiceImpl.java"))
            .filter(path -> !path.toString().contains("/service/impl/"))
            .toList();

    assertThat(implOffenders).isEmpty();
  }

  @Test
  void controllersDoNotImportRepositoriesOrEntities() throws IOException {
    List<Path> offenders =
        javaFiles(MAIN_JAVA.resolve("com/routeshare"))
            .filter(path -> path.toString().contains("/controller/"))
            .filter(
                path -> {
                  String source = read(path);
                  return source.contains(".repository.") || source.contains(".entity.");
                })
            .toList();

    assertThat(offenders).isEmpty();
  }

  @Test
  void mappersUseMapStructSharedConfig() throws IOException {
    List<Path> mapperFiles =
        javaFiles(MAIN_JAVA.resolve("com/routeshare"))
            .filter(path -> path.toString().contains("/mapper/"))
            .filter(path -> !path.getFileName().toString().endsWith("MapperConfig.java"))
            .toList();

    List<Path> offenders =
        mapperFiles.stream()
            .filter(path -> !read(path).contains("@Mapper(config = RouteShareMapperConfig.class)"))
            .toList();

    assertThat(offenders).isEmpty();
  }

  @Test
  void facadeTypesLiveUnderFacadePackages() throws IOException {
    List<Path> facadeOffenders =
        javaFiles(MAIN_JAVA.resolve("com/routeshare"))
            .filter(path -> path.getFileName().toString().endsWith("Facade.java"))
            .filter(path -> !path.toString().contains("/facade/"))
            .toList();

    assertThat(facadeOffenders).isEmpty();

    List<Path> implOffenders =
        javaFiles(MAIN_JAVA.resolve("com/routeshare"))
            .filter(path -> path.getFileName().toString().endsWith("FacadeImpl.java"))
            .filter(path -> !path.toString().contains("/facade/impl/"))
            .toList();

    assertThat(implOffenders).isEmpty();
  }

  @Test
  void noCrossModuleRepositoryEntityOrImplImports() throws IOException {
    Pattern modulePattern = Pattern.compile("com/routeshare/([^/]+)/");
    List<Path> offenders =
        javaFiles(MAIN_JAVA.resolve("com/routeshare"))
            .filter(
                path -> {
                  var matcher = modulePattern.matcher(path.toString().replace('\\', '/'));
                  if (!matcher.find()) {
                    return false;
                  }
                  String module = matcher.group(1);
                  if (module.equals("common") || module.equals("admin")) {
                    return false;
                  }
                  String source = read(path);
                  Pattern forbidden =
                      Pattern.compile(
                          "import com\\.routeshare\\.(?!"
                              + module
                              + "\\.)(?!common\\.)[^;]+\\.(repository|entity|impl)\\.[^;]+;");
                  // Facade implementations may access their own module repository/entity internals
                  // only;
                  // cross-module communication must go through facade/service interfaces or DTOs.
                  return forbidden.matcher(source).find();
                })
            .toList();

    assertThat(offenders).isEmpty();
  }

  private static List<Path> filesContaining(String needle, Path root) throws IOException {
    return javaFiles(root).filter(path -> read(path).contains(needle)).toList();
  }

  private static Stream<Path> javaFiles(Path root) throws IOException {
    return Files.walk(root).filter(path -> path.toString().endsWith(".java"));
  }

  private static String stripExtension(Path path) {
    String fileName = path.getFileName().toString();
    return fileName.substring(0, fileName.length() - ".java".length());
  }

  private static String read(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
