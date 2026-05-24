package br.com.oficina.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

/**
 * Valida a arquitetura hexagonal do monolito:
 *
 * <pre>
 *   adapter.in.web  → application (ports + services)  → domain
 *   adapter.out.*   → application (ports)              → domain
 *   config pode acessar tudo (composition root)
 *   domain é puro (sem Spring/JPA/Servlet)
 * </pre>
 */
class ArchitectureTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("br.com.oficina");

  @Test
  void hexagonalRespeitaDependencias() {
    Architectures.layeredArchitecture()
        .consideringAllDependencies()
        .layer("adapter-in")
        .definedBy("br.com.oficina.adapter.in..")
        .layer("adapter-out")
        .definedBy("br.com.oficina.adapter.out..")
        .layer("application")
        .definedBy("br.com.oficina.application..")
        .layer("domain")
        .definedBy("br.com.oficina.domain..")
        .layer("config")
        .definedBy("br.com.oficina.config..")
        .whereLayer("adapter-in")
        .mayOnlyBeAccessedByLayers("config")
        .whereLayer("adapter-out")
        .mayOnlyBeAccessedByLayers("config", "adapter-in")
        .whereLayer("application")
        .mayOnlyBeAccessedByLayers("adapter-in", "adapter-out", "config")
        .whereLayer("domain")
        .mayOnlyBeAccessedByLayers("application", "adapter-in", "adapter-out", "config")
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

  @Test
  void applicationNaoDependeDeAdapters() {
    noClasses()
        .that()
        .resideInAPackage("br.com.oficina.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("br.com.oficina.adapter..")
        .check(CLASSES);
  }
}
