package com.planejamentomensal;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * Conventions every test follows (TST-01, TST-02).
 */
class TestConventionsTest {

    /** "RN-13: projected installment becomes MATCHED", several ids allowed: "RF-46, RF-48: ...". */
    private static final Pattern RULE_DISPLAY_NAME =
            Pattern.compile("^[A-Z0-9]+-\\d+[a-z]?(, [A-Z0-9]+-\\d+[a-z]?)*: \\S.*");

    private static final JavaClasses TESTS = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
            .importPackages("com.planejamentomensal");

    @Test
    @DisplayName("TST-01: test classes mirror a production module package")
    void testsMirrorModules() {
        classes().that().haveSimpleNameEndingWith("Test").or().haveSimpleNameEndingWith("IT")
                .should().resideInAnyPackage("com.planejamentomensal", "com.planejamentomensal.*..")
                .check(TESTS);
    }

    @Test
    @DisplayName("TST-02: every test has an English display name that starts with the rule id")
    void testsNameTheirRule() {
        methods().that().areAnnotatedWith(Test.class).or().areAnnotatedWith(ParameterizedTest.class)
                .should(haveRuleDisplayName())
                .check(TESTS);
    }

    private static ArchCondition<JavaMethod> haveRuleDisplayName() {
        return new ArchCondition<>("have @DisplayName(\"<RULE-ID>: behaviour\")") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean ok = method.tryGetAnnotationOfType(DisplayName.class)
                        .map(name -> RULE_DISPLAY_NAME.matcher(name.value()).matches())
                        .orElse(false);
                if (!ok) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName() + " lacks a rule display name"));
                }
            }
        };
    }
}
