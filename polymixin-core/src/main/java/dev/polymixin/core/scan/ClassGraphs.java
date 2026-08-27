package dev.polymixin.core.scan;

import dev.polymixin.core.diagnostics.Log;
import io.github.classgraph.ClassGraph;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ClassGraphs {

    private ClassGraphs() {
    }

    public static boolean canPreserveFileSystems() {
        return overrideClasspathPreservingFileSystems(new ClassGraph(), List.of());
    }

    public static boolean overrideClasspathPreservingFileSystems(ClassGraph graph, List<Path> roots) {
        try {
            Field scanSpecField = ClassGraph.class.getDeclaredField("scanSpec");
            scanSpecField.setAccessible(true);
            Object scanSpec = scanSpecField.get(graph);

            Field overrideField = scanSpec.getClass().getField("overrideClasspath");
            overrideField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<Object> override = (List<Object>) overrideField.get(scanSpec);
            if (override == null) {
                override = new ArrayList<>();
                overrideField.set(scanSpec, override);
            }
            override.addAll(roots);
            return true;
        } catch (Throwable th) {
            Log.warn("could not install classpath roots as Path objects ({}); falling back to string"
                    + " classpath override, which loses non-default FileSystems", th);
            return false;
        }
    }
}
