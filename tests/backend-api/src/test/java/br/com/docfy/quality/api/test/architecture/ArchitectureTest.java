package br.com.docfy.quality.api.test.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("architecture")
class ArchitectureTest {

  private static final JavaClasses PROJECT_CLASSES =
      new ClassFileImporter().importPackages("br.com.docfy.quality.api");

  @Test
  void shouldKeepFrameworkLayersFreeOfCycles() {
    slices()
        .matching("br.com.docfy.quality.api.(*)..")
        .should()
        .beFreeOfCycles()
        .check(PROJECT_CLASSES);
  }

  @Test
  void clientsShouldNotDependOnTestsOrAssertions() {
    noClasses()
        .that()
        .resideInAPackage("..client..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..test..", "..assertion..")
        .check(PROJECT_CLASSES);
  }

  @Test
  void specificationsShouldNotDependOnHigherLevelLayers() {
    noClasses()
        .that()
        .resideInAPackage("..specification..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..client..", "..assertion..", "..test..")
        .check(PROJECT_CLASSES);
  }
}
