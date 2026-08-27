package dev.polymixin.core.scan;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanTimingTest {

    private static final int JARS = 20;
    private static final int CLASSES_PER_JAR = 500;

    private static byte[] clazz(String internalName, String superName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, superName, null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    @Test
    void reportsScanCostForASyntheticModpack(@TempDir Path dir) throws Exception {
        List<Path> roots = new ArrayList<>();
        for (int j = 0; j < JARS; j++) {
            Path jar = dir.resolve("mod" + j + ".jar");
            try (OutputStream out = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry("pack" + j + "/Base.class"));
                zip.write(clazz("pack" + j + "/Base", "java/lang/Object"));
                zip.closeEntry();
                for (int c = 0; c < CLASSES_PER_JAR; c++) {
                    String name = "pack" + j + "/C" + c;
                    zip.putNextEntry(new ZipEntry(name + ".class"));
                    zip.write(clazz(name, "pack" + j + "/Base"));
                    zip.closeEntry();
                }
            }
            roots.add(jar);
        }

        classInfoScan(roots, 1);
        classInfoScan(roots, 4);
        allInfoScan(roots, 4);

        int classCount;
        try (ScanResult result = build(roots, true).scan(4)) {
            classCount = result.getAllClasses().size();
            assertTrue(result.getClassInfo("pack0.Base").getSubclasses().size() == CLASSES_PER_JAR);
        }

        long single = classInfoScan(roots, 1);
        long parallel = classInfoScan(roots, 4);
        long allInfo = allInfoScan(roots, 4);

        System.out.printf("[polymixin-timing] %d jars / %d classes: classInfo 1 thread = %d ms, "
                        + "classInfo %d threads = %d ms, allInfo %d threads = %d ms%n",
                JARS, classCount, single, 4, parallel, 4, allInfo);
    }

    private static ClassGraph build(List<Path> roots, boolean classInfoOnly) {
        ClassGraph graph = classInfoOnly
                ? new ClassGraph().enableClassInfo()
                : new ClassGraph().enableAllInfo();
        graph.disableModuleScanning().disableNestedJarScanning();
        ClassGraphs.overrideClasspathPreservingFileSystems(graph, roots);
        return graph;
    }

    private static long classInfoScan(List<Path> roots, int threads) {
        long start = System.nanoTime();
        if (threads == 1) {
            ExecutorService inline = new SameThreadExecutorService();
            try (ScanResult result = build(roots, true).scan(inline, 1)) {
                result.getAllClasses().size();
            } finally {
                inline.shutdown();
            }
        } else {
            try (ScanResult result = build(roots, true).scan(threads)) {
                result.getAllClasses().size();
            }
        }
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private static long allInfoScan(List<Path> roots, int threads) {
        long start = System.nanoTime();
        try (ScanResult result = build(roots, false).scan(threads)) {
            result.getAllClasses().size();
        }
        return (System.nanoTime() - start) / 1_000_000L;
    }
}
