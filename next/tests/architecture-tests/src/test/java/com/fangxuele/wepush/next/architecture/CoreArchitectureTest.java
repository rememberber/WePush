package com.fangxuele.wepush.next.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CoreArchitectureTest {
    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.fangxuele.wepush.next");

    @Test
    void coreApiDependsOnlyOnJdkAndItsOwnPackage() {
        noClasses()
                .that().resideInAPackage("..core.api..")
                .should().dependOnClassesThat().resideOutsideOfPackages(
                        "..core.api..", "java..", "javax..", "jdk..")
                .check(classes);
    }

    @Test
    void providerSpiDoesNotDependOnEngineAgentServiceSdkOrUi() {
        noClasses()
                .that().resideInAPackage("..provider.spi..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..core.engine..", "..agent..", "..service..", "..sdk..", "..ui..")
                .check(classes);
    }

    @Test
    void coreDoesNotDependOnFrameworksPersistenceOrDesktopUi() {
        noClasses()
                .that().resideInAnyPackage("..core.api..", "..core.engine..", "..provider.spi..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "org.mybatis..",
                        "jakarta.persistence..",
                        "java.sql..",
                        "javax.swing..",
                        "java.awt..")
                .check(classes);
    }

    @Test
    void providersDoNotReachIntoAgentOrService() {
        noClasses()
                .that().resideInAPackage("..provider.http..")
                .should().dependOnClassesThat().resideInAnyPackage("..agent..", "..service..")
                .check(classes);
    }

    @Test
    void serviceApiDomainAndApplicationStayFrameworkFree() {
        noClasses()
                .that().resideInAnyPackage("..service.api..", "..service.domain..", "..service.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "org.mybatis..",
                        "jakarta.persistence..",
                        "java.sql..",
                        "..service.infrastructure..",
                        "..service.app..")
                .check(classes);
    }

    @Test
    void serviceInfrastructureDoesNotDependOnWebComposition() {
        noClasses()
                .that().resideInAPackage("..service.infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("..service.app..")
                .check(classes);
    }

    @Test
    void remoteJavaSdkDependsOnlyOnPublicServiceContract() {
        noClasses()
                .that().resideInAPackage("..next.sdk..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..wepush.next.core..",
                        "..wepush.next.provider..",
                        "..service.domain..",
                        "..service.application..",
                        "..service.infrastructure..",
                        "..service.app..",
                        "org.springframework..")
                .check(classes);
    }

    @Test
    void embeddedJavaSdkDependsOnCoreAndSpiButNotServiceAgentOrConcreteProviders() {
        noClasses()
                .that().resideInAPackage("..next.embedded..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..agent..",
                        "..service..",
                        "..next.sdk..",
                        "..provider.http..",
                        "org.springframework..",
                        "org.mybatis..",
                        "jakarta.persistence..")
                .check(classes);
    }

    @Test
    void agentProtocolAndRuntimeRemainIndependentFromServiceAndFrameworks() {
        noClasses()
                .that().resideInAnyPackage("..agent.protocol..", "..agent.runtime..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..service..",
                        "..sdk..",
                        "org.springframework..",
                        "org.mybatis..",
                        "jakarta.persistence..")
                .check(classes);
    }
}
