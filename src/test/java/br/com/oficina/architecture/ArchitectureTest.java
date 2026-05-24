package br.com.oficina.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

/**
 * Valida a arquitetura em camadas do monolito:
 *
 * <pre>
 *   controller -> service -> domain
 *   infrastructure -> domain
 *   domain é puro (sem Spring/JPA/Servlet)
 * </pre>
 */
class ArchitectureTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("br.com.oficina");

  @Test
  void camadasRespeitamDependencias() {
    Architectures.layeredArchitecture()
        .consideringAllDependencies()
        .layer("controller")
        .definedBy("br.com.oficina.controller..")
        .layer("service")
        .definedBy("br.com.oficina.service..")
        .layer("infrastructure")
        .definedBy("br.com.oficina.infrastructure..")
        .layer("domain")
        .definedBy("br.com.oficina.domain..")
        .layer("config")
        .definedBy("br.com.oficina.config..")
        .whereLayer("controller")
        .mayOnlyBeAccessedByLayers("config")
        .whereLayer("service")
        .mayOnlyBeAccessedByLayers("controller", "infrastructure", "config")
        .whereLayer("infrastructure")
        .mayOnlyBeAccessedByLayers("controller", "service", "config")
        .check(CLASSES);
  }

  @Test
  void dominioNaoDependeDeSpring() {
    noClasses()
        .that()
        .resideInAPackage("br.com.oficina.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.hibernate..",
            "jakarta.servlet..")
        .check(CLASSES);
  }

  @Test
  void dominioNaoUsaJpa() {
    noClasses()
        .that()
        .resideInAPackage("br.com.oficina.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
        .check(CLASSES);
  }
}
