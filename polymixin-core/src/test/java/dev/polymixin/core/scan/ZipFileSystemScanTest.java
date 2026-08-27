package dev.polymixin.core.scan;

import dev.polymixin.core.PolyMixin;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipFileSystemScanTest {

    private static byte[] simpleClass(String internalName, String superName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, superName, null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static Path buildJar(Path dir, String name, Map<String, byte[]> classes) throws IOException {
        Path jar = dir.resolve(name);
        try (OutputStream out = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey() + ".class"));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return jar;
    }

    @Test
    void scansSubclassesThroughAZipFileSystemRoot(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        classes.put("synth/Base", simpleClass("synth/Base", "java/lang/Object"));
        classes.put("synth/Middle", simpleClass("synth/Middle", "synth/Base"));
        classes.put("synth/Leaf", simpleClass("synth/Leaf", "synth/Middle"));
        classes.put("synth/Other", simpleClass("synth/Other", "java/lang/Object"));

        Path jar = buildJar(dir, "synthetic.jar", classes);

        URI uri = URI.create("jar:" + jar.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(uri, new HashMap<String, Object>())) {
            Path root = fs.getRootDirectories().iterator().next();

            ClassGraph graph = new ClassGraph()
                    .enableClassInfo()
                    .disableModuleScanning()
                    .disableNestedJarScanning();
            assertTrue(ClassGraphs.overrideClasspathPreservingFileSystems(graph, List.of(root)),
                    "PolyMixin must be able to hand ClassGraph raw Path objects");

            ExecutorService inline = new SameThreadExecutorService();
            try (ScanResult result = graph.scan(inline, 1)) {

                ClassInfo base = result.getClassInfo("synth.Base");
                assertTrue(base != null, "ClassGraph must resolve classes from a non-default FileSystem root");

                Set<String> subclasses = new LinkedHashSet<>();
                base.getSubclasses().forEach(info -> subclasses.add(info.getName()));

                assertEquals(Set.of("synth.Middle", "synth.Leaf"), subclasses);
            } finally {
                inline.shutdown();
            }
        }
    }

    @Test
    void stringifiedOverrideLosesTheFileSystem(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        classes.put("synth3/Base", simpleClass("synth3/Base", "java/lang/Object"));
        Path jar = buildJar(dir, "stringly.jar", classes);

        URI uri = URI.create("jar:" + jar.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(uri, new HashMap<String, Object>())) {
            Path root = fs.getRootDirectories().iterator().next();
            assertEquals("/", root.toString(),
                    "ClassGraph.overrideClasspath(Path) stringifies via ScanSpec.addClasspathOverride, so a"
                            + " zip/union filesystem root would be re-resolved against the default filesystem");
        }
    }

    @Test
    void scansAJarFileDirectly(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        classes.put("synth2/Base", simpleClass("synth2/Base", "java/lang/Object"));
        classes.put("synth2/Sub", simpleClass("synth2/Sub", "synth2/Base"));
        Path jar = buildJar(dir, "direct.jar", classes);

        ExecutorService inline = new SameThreadExecutorService();
        try (ScanResult result = new ClassGraph()
                .enableClassInfo()
                .disableModuleScanning()
                .overrideClasspath(List.of(jar))
                .scan(inline, 1)) {
            assertEquals(1, result.getClassInfo("synth2.Base").getSubclasses().size());
        } finally {
            inline.shutdown();
        }
    }
}
