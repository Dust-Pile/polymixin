package dev.polymixin.core.scan;

import dev.polymixin.core.diagnostics.Log;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class ScanCache {

    public static final String PROP_THREADS = "polymixin.scan.threads";
    public static final String PROP_REJECT = "polymixin.scan.rejectPackages";
    public static final String PROP_VERBOSE = "polymixin.scan.verbose";

    private static ScanResult result;
    private static int scanCount;
    private static long elapsedMillis;
    private static int rootCount;
    private static int classCount;

    private ScanCache() {
    }

    public static synchronized ScanResult scan(List<Path> roots) {
        if (result != null) {
            return result;
        }

        warnIfThreadsRequested();
        ClassGraph graph = new ClassGraph()
                .enableClassInfo()
                .disableModuleScanning()
                .disableNestedJarScanning();
        if (!ClassGraphs.overrideClasspathPreservingFileSystems(graph, roots)) {
            graph.overrideClasspath(roots);
        }

        String reject = System.getProperty(PROP_REJECT);
        if (reject != null && !reject.isEmpty()) {
            graph.rejectPackages(reject.split(","));
        }
        if (Boolean.getBoolean(PROP_VERBOSE)) {
            graph.verbose();
        }

        scanCount++;
        long start = System.nanoTime();
        ExecutorService inline = new SameThreadExecutorService();
        try {
            result = graph.scan(inline, 1);
        } finally {
            inline.shutdown();
        }
        elapsedMillis = (System.nanoTime() - start) / 1_000_000L;
        rootCount = roots.size();
        classCount = result.getAllClasses().size();

        String message = "scanned {} roots / {} classes in {} ms (classInfo-only, 1 thread)";
        if (elapsedMillis > 2000L) {
            Log.warn(message + " - exceeds 2s", rootCount, classCount, elapsedMillis);
        } else {
            Log.info(message, rootCount, classCount, elapsedMillis);
        }
        return result;
    }

    public static synchronized long elapsedMillis() {
        return elapsedMillis;
    }

    public static synchronized int rootCount() {
        return rootCount;
    }

    public static synchronized int classCount() {
        return classCount;
    }

    public static synchronized int scanCount() {
        return scanCount;
    }

    private static void warnIfThreadsRequested() {
        String raw = System.getProperty(PROP_THREADS);
        if (raw == null || raw.trim().equals("1")) {
            return;
        }
        Log.warn("-D{}={} is ignored. ClassGraph runs the scan on a worker thread, which deadlocks"
                + " against the class loader while the mixin lock is held"
                + " (https://github.com/classgraph/classgraph/issues/933)", PROP_THREADS, raw.trim());
    }
}
