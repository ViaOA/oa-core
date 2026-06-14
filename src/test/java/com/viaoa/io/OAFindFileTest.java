package com.viaoa.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OAFindFileTest {

    @TempDir
    Path tempDir;

    @Test
    void findAllStringDefaultsBlankRootAndReturnsEmptyForBlankFileName() throws Exception {
        OAFindFile finder = new OAFindFile();

        assertArrayEquals(new String[0], finder.findAll(tempDir.toString(), null));
        assertArrayEquals(new String[0], finder.findAll(tempDir.toString(), " "));
    }

    @Test
    void findAllFileFindsCaseInsensitiveMatchesAndClearsStateBetweenCalls() throws Exception {
        Path nested = tempDir.resolve("a/b");
        Files.createDirectories(nested);
        Path match = nested.resolve("Target.TXT");
        Files.writeString(match, "target", StandardCharsets.UTF_8);

        OAFindFile finder = new OAFindFile();
        String[] found = finder.findAll(tempDir.toFile(), "target.txt");
        String[] missing = finder.findAll(tempDir.toFile(), "missing.txt");

        assertArrayEquals(new String[] { match.toFile().getAbsolutePath() }, found);
        assertArrayEquals(new String[0], missing);
    }

    @Test
    void findFileScansDirectoriesAndZipCompatibleArchives() throws Exception {
        Path target = tempDir.resolve("plain.txt");
        Path zip = tempDir.resolve("archive.zip");
        Files.writeString(target, "plain", StandardCharsets.UTF_8);
        writeZip(zip, "nested/plain.txt", "zip");

        OAFindFile finder = new OAFindFile();
        String[] found = finder.findAll(tempDir.toFile(), "plain.txt");

        Arrays.sort(found);
        assertEquals(2, found.length);
        assertTrue(Arrays.stream(found).anyMatch(s -> s.equals(target.toFile().getAbsolutePath())));
        assertTrue(Arrays.stream(found).anyMatch(s -> s.endsWith("archive.zip!nested/plain.txt")));
    }

    @Test
    void protectedFindFileCanBeCalledBySubclass() throws Exception {
        Path target = tempDir.resolve("target.txt");
        Files.writeString(target, "target", StandardCharsets.UTF_8);
        ExposedFindFile finder = new ExposedFindFile();

        String[] found = finder.findAll(tempDir.toFile(), "target.txt");

        assertEquals(1, found.length);
        assertEquals(1, finder.exposeFindFile(tempDir.toFile(), "target.txt").length);
    }

    @Test
    void protectedFindZipCanBeCalledBySubclass() throws Exception {
        Path zip = tempDir.resolve("archive.jar");
        writeZip(zip, "inside/target.txt", "zip");
        ExposedFindFile finder = new ExposedFindFile();

        String[] found = finder.findAll(tempDir.toFile(), "target.txt");

        assertEquals(1, found.length);
        assertEquals(1, finder.exposeFindZip(zip, "target.txt").length);
    }

    private static void writeZip(Path file, String entryName, String text) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file))) {
            out.putNextEntry(new ZipEntry(entryName));
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    private static class ExposedFindFile extends OAFindFile {
        String[] exposeFindFile(java.io.File file, String name) throws IOException {
            return findAll(file, name);
        }

        String[] exposeFindZip(Path file, String name) throws IOException {
            java.io.File root = file.getParent().toFile();
            return findAll(root, name);
        }
    }
}
