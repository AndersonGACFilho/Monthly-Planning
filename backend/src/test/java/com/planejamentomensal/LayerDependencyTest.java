package com.planejamentomensal;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Dependency rules between layers and modules (SOL-05, SOL-06). Breaking one fails the build.
 */
class LayerDependencyTest {

    private static final String ROOT = "com.planejamentomensal";
    private static final Pattern MODULE_LAYER = Pattern.compile("com\\.planejamentomensal\\.(\\w+)\\.(\\w+).*");
    private static final Set<String> LAYERS = Set.of("api", "application", "domain", "infra");

    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    @DisplayName("SOL-05: domain depends only on itself, shared domain, the JDK and Jakarta Persistence")
    void domainIsFrameworkFree() {
        classes().that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..domain..", "java..", "jakarta.persistence..")
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("SOL-05: application never depends on api or infra")
    void applicationDependsInward() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..api..", "..infra..")
                .allowEmptyShould(true) // no use case exists until M1
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("SOL-05: api never depends on infra, and infra never depends on api")
    void apiAndInfraAreSeparated() {
        noClasses().that().resideInAPackage(ROOT + ".(*).api..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".(*).infra..")
                .check(PRODUCTION);
        noClasses().that().resideInAPackage(ROOT + ".(*).infra..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".(*).api..")
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("SOL-06: a module reaches another module only through its application layer")
    void modulesTalkThroughApplication() {
        classes().that().resideInAPackage(ROOT + "..")
                .should(onlyUseOtherModulesThroughApplication())
                .check(PRODUCTION);
    }

    @Test
    @DisplayName("SOL-06: module code lives in one of the four layers")
    void moduleCodeLivesInALayer() {
        classes().that().resideInAPackage(ROOT + ".*..").and().areNotAnonymousClasses()
                .should(liveInALayer())
                .check(PRODUCTION);
    }

    private static ArchCondition<JavaClass> onlyUseOtherModulesThroughApplication() {
        return new ArchCondition<>("only use other modules through their application layer") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                Optional<String[]> from = moduleAndLayer(origin.getPackageName());
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    Optional<String[]> to = moduleAndLayer(dependency.getTargetClass().getPackageName());
                    if (from.isEmpty() || to.isEmpty() || from.get()[0].equals(to.get()[0])) {
                        continue;
                    }
                    String targetModule = to.get()[0];
                    String targetLayer = to.get()[1];
                    boolean sharedKernel = targetModule.equals("shared") && !targetLayer.equals("infra");
                    if (!sharedKernel && !targetLayer.equals("application")) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> liveInALayer() {
        return new ArchCondition<>("live in api, application, domain or infra") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean ok = moduleAndLayer(item.getPackageName()).isPresent()
                        || item.getSimpleName().equals("package-info");
                if (!ok) {
                    events.add(SimpleConditionEvent.violated(item, item.getName() + " is outside a layer"));
                }
            }
        };
    }

    private static Optional<String[]> moduleAndLayer(String packageName) {
        Matcher matcher = MODULE_LAYER.matcher(packageName);
        if (matcher.matches() && LAYERS.contains(matcher.group(2))) {
            return Optional.of(new String[] {matcher.group(1), matcher.group(2)});
        }
        return Optional.empty();
    }
}
