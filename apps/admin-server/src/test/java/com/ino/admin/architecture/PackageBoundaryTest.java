package com.ino.admin.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("architecture")
class PackageBoundaryTest {
    private final com.tngtech.archunit.core.domain.JavaClasses classes =
            new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.ino.admin");

    @Test
    void businessFeaturesDoNotDirectlyDependOnEachOther() {
        noClasses().that().resideInAPackage("com.ino.admin.identity..")
                .should().dependOnClassesThat().resideInAnyPackage("com.ino.admin.menu..", "com.ino.admin.file..")
                .check(classes);
        noClasses().that().resideInAPackage("com.ino.admin.menu..")
                .should().dependOnClassesThat().resideInAnyPackage("com.ino.admin.identity..", "com.ino.admin.file..")
                .check(classes);
        noClasses().that().resideInAPackage("com.ino.admin.file..")
                .should().dependOnClassesThat().resideInAnyPackage("com.ino.admin.identity..", "com.ino.admin.menu..")
                .check(classes);
    }

    @Test
    void domainDoesNotDependOnOuterApplicationLayers() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..application..", "..infrastructure..", "..web..", "..auth..", "..config..")
                .check(classes);
    }
}
