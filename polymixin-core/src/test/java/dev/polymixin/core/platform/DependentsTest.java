package dev.polymixin.core.platform;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependentsTest {

    private static final class StubSource implements ClasspathSource {

        private final Set<String> dependents;
        private final RuntimeException failure;

        StubSource(Set<String> dependents, RuntimeException failure) {
            this.dependents = dependents;
            this.failure = failure;
        }

        @Override
        public String platformName() {
            return "stub";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public List<Path> classpathRoots() {
            return List.of();
        }

        @Override
        public Set<String> dependentModIds(String modId) {
            if (this.failure != null) {
                throw this.failure;
            }
            return this.dependents;
        }
    }

    private void install(Set<String> dependents, RuntimeException failure) {
        Platform.override(new StubSource(dependents, failure));
        Dependents.reset();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(Dependents.PROP_DISCOVERY);
        Platform.override(null);
        Dependents.reset();
    }

    @Test
    void inspectsEverythingByDefaultEvenWhenNothingDeclaresADependency() {
        install(Set.of(), null);

        assertEquals(Dependents.Mode.ALL, Dependents.mode());
        assertTrue(Dependents.mayDeclareAnnotation("somemod"),
                "a mod that forgot to declare the dependency must still be inspected by default");
        assertTrue(Dependents.mayDeclareAnnotation(null));
    }

    @Test
    void doesNotNarrowByDefaultWhenSomeModsDeclareADependency() {
        install(Set.of("examplemod"), null);

        assertEquals(Dependents.Mode.ALL, Dependents.mode());
        assertTrue(Dependents.mayDeclareAnnotation("unrelatedmod"),
                "narrowing would silently skip a mod that forgot the dependency, so it is opt-in only");
    }

    @Test
    void narrowsToDependentModsWhenExplicitlyRequested() {
        System.setProperty(Dependents.PROP_DISCOVERY, "dependents");
        install(Set.of("examplemod"), null);

        assertEquals(Dependents.Mode.DEPENDENTS, Dependents.mode());
        assertTrue(Dependents.mayDeclareAnnotation("examplemod"));
        assertFalse(Dependents.mayDeclareAnnotation("unrelatedmod"));
        assertTrue(Dependents.mayDeclareAnnotation(null), "an unknown config owner is still inspected");
    }

    @Test
    void disablesDiscoveryOnlyWhenNarrowingIsRequestedAndNothingDepends() {
        System.setProperty(Dependents.PROP_DISCOVERY, "dependents");
        install(Set.of(), null);

        assertEquals(Dependents.Mode.PLUGINS_ONLY, Dependents.mode());
        assertFalse(Dependents.mayDeclareAnnotation("somemod"));
    }

    @Test
    void failsOpenWhenThePlatformCannotAnswer() {
        System.setProperty(Dependents.PROP_DISCOVERY, "dependents");
        install(null, null);

        assertEquals(Dependents.Mode.ALL, Dependents.mode());
        assertTrue(Dependents.mayDeclareAnnotation("anything"));
    }

    @Test
    void failsOpenWhenThePlatformThrows() {
        System.setProperty(Dependents.PROP_DISCOVERY, "dependents");
        install(null, new IllegalStateException("loader not ready"));

        assertEquals(Dependents.Mode.ALL, Dependents.mode());
        assertTrue(Dependents.mayDeclareAnnotation("anything"));
    }

    @Test
    void pluginsModeTurnsAnnotationDiscoveryOff() {
        System.setProperty(Dependents.PROP_DISCOVERY, "plugins");
        install(Set.of("examplemod"), null);

        assertEquals(Dependents.Mode.PLUGINS_ONLY, Dependents.mode());
        assertFalse(Dependents.mayDeclareAnnotation("examplemod"));
    }
}
