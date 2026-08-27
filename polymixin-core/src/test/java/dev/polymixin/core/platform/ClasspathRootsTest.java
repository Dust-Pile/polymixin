package dev.polymixin.core.platform;

import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathRootsTest {

    private static Path buildJar(Path dir, String name) throws Exception {
        Path jar = dir.resolve(name);
        try (OutputStream out = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("pkg/A.class"));
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
        }
        return jar;
    }

    @Test
    void keepsDefaultFileSystemPaths(@TempDir Path dir) throws Exception {
        Path jar = buildJar(dir, "plain.jar");
        assertEquals(List.of(dir, jar), ClasspathRoots.normalize(List.of(dir, jar)));
    }

    @Test
    void unwrapsAZipFileSystemRootToItsBackingJar(@TempDir Path dir) throws Exception {
        Path jar = buildJar(dir, "zipped.jar");
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jar.toUri()), new HashMap<String, Object>())) {
            Path root = fs.getRootDirectories().iterator().next();
            List<Path> normalized = ClasspathRoots.normalize(List.of(root));
            assertEquals(1, normalized.size());
            assertEquals(jar.toAbsolutePath(), normalized.get(0).toAbsolutePath());
        }
    }

    @Test
    void extractsANestedJarThatCannotBeUnwrapped(@TempDir Path dir) throws Exception {
        Path inner = buildJar(dir, "inner.jar");
        Path outer = dir.resolve("outer.jar");
        try (OutputStream out = Files.newOutputStream(outer); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("META-INF/jarjar/inner.jar"));
            zip.write(Files.readAllBytes(inner));
            zip.closeEntry();
        }

        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + outer.toUri()), new HashMap<String, Object>())) {
            Path nested = fs.getPath("/META-INF/jarjar/inner.jar");
            List<Path> normalized = ClasspathRoots.normalize(List.of(nested));
            assertEquals(1, normalized.size());
            Path extracted = normalized.get(0);
            assertTrue(ClasspathRoots.isDefaultFileSystem(extracted), extracted.toString());
            assertEquals(Files.size(inner), Files.size(extracted));
        }
    }
}
