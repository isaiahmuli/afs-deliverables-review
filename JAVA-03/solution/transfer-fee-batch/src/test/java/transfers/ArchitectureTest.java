package transfers;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "transfers", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** No package cycles anywhere in the project. */
    @ArchTest
    static final ArchRule packagesAreFreeOfCycles =
            slices().matching("transfers.(*)..").should().beFreeOfCycles();

    /** The fee rules (core) depend on no other project package, so the website can reuse them. */
    @ArchTest
    static final ArchRule coreDependsOnNoOtherProjectPackage =
            classes().that().resideInAPackage("transfers.core..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage("transfers.core..", "java..");

    /** Application code logs through SLF4J, never System.out or System.err. */
    @ArchTest
    static final ArchRule noStandardStreams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
}
