package com.viaoa.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OAFileTest {

    @TempDir
    Path tempDir;

    @Test
    void constructorNormalizesPathAndCopyToStringCopiesFile() throws Exception {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("nested/target.txt");
        Files.writeString(source, "content", StandardCharsets.UTF_8);

        OAFile file = new OAFile(source.toString());

        assertTrue(file.copyTo(target.toString()));
        assertEquals("content", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void copyToOAFileHandlesNullAndCopiesFile() throws Exception {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        Files.writeString(source, "copy", StandardCharsets.UTF_8);
        OAFile file = new OAFile(source.toString());

        assertFalse(file.copyTo((OAFile) null));
        assertTrue(file.copyTo(new OAFile(target.toString())));
        assertEquals("copy", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void instanceMkdirsForFileCreatesParentDirectory() {
        OAFile file = new OAFile(tempDir.resolve("a/b/file.txt").toString());

        file.mkdirsForFile();

        assertTrue(tempDir.resolve("a/b").toFile().isDirectory());
        assertFalse(file.exists());
    }

    @Test
    void convertFileNameHandlesNullSeparatorsAndTrailingSeparator() {
        assertNull(OAFile.convertFileName(null));

        String converted = OAFile.convertFileName("a\\b/c", true);

        assertTrue(converted.endsWith(File.separator));
        assertFalse(converted.contains(File.separatorChar == '/' ? "\\" : "/"));
    }

    @Test
    void getFileNameExtractsLastPathSegment() {
        assertEquals("file.txt", OAFile.getFileName("a/b\\file.txt"));
    }

    @Test
    void getDirectoryNameExtractsDirectoryOrCurrentDirectory() {
        assertEquals(OAFile.convertFileName("a/b"), OAFile.getDirectoryName("a/b/file.txt"));
        assertEquals(".", OAFile.getDirectoryName("file.txt"));
    }

    @Test
    void getExtensionHandlesFileNullPathAndNoExtension() {
        assertNull(OAFile.getExtension((File) null));
        assertEquals("txt", OAFile.getExtension(new File("file.txt")));
        assertEquals("gz", OAFile.getExtension("archive.tar.gz"));
        assertEquals("", OAFile.getExtension("README"));
    }

    @Test
    void staticMkdirsForFileHandlesNullStringAndCreatesOnlyParents() {
        assertDoesNotThrow(() -> OAFile.mkdirsForFile((String) null));

        Path file = tempDir.resolve("x/y/file.txt");
        OAFile.mkdirsForFile(file.toString());

        assertTrue(tempDir.resolve("x/y").toFile().isDirectory());
        assertFalse(file.toFile().exists());
    }

    @Test
    void staticMkdirsForFileHandlesNullFileAndCreatesOnlyParents() {
        assertDoesNotThrow(() -> OAFile.mkdirsForFile((File) null));

        File file = tempDir.resolve("m/n/file.txt").toFile();
        OAFile.mkdirsForFile(file);

        assertTrue(tempDir.resolve("m/n").toFile().isDirectory());
        assertFalse(file.exists());
    }

    @Test
    void renameToCreatesDestinationDirectoriesWhenSourceExists() throws Exception {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("renamed/out.txt");
        Files.writeString(source, "rename", StandardCharsets.UTF_8);

        new OAFile(source.toString()).renameTo(target.toString());

        assertFalse(source.toFile().exists());
        assertEquals("rename", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void copyStringNoOpsForNullAndCopiesBytes() throws Exception {
        Path source = tempDir.resolve("source.bin");
        Path target = tempDir.resolve("dir/target.bin");
        byte[] data = new byte[] { 0, 1, 2, 3, 4 };
        Files.write(source, data);

        assertDoesNotThrow(() -> OAFile.copy((String) null, target.toString()));
        assertDoesNotThrow(() -> OAFile.copy(source.toString(), (String) null));
        OAFile.copy(source.toString(), target.toString());

        assertArrayEquals(data, Files.readAllBytes(target));
    }

    @Test
    void copyFileNoOpsForNullAndThrowsForMissingSource() throws Exception {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        Files.writeString(source, "data", StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> OAFile.copy((File) null, target.toFile()));
        assertDoesNotThrow(() -> OAFile.copy(source.toFile(), null));
        assertThrows(Exception.class, () -> OAFile.copy(tempDir.resolve("missing.txt").toFile(), target.toFile()));
    }

    @Test
    void copyResourceToFileReturnsFalseForMissingAndCopiesClassResource() throws Exception {
        Path target = tempDir.resolve("resource/OAFileTest.class");

        assertFalse(OAFile.copyResourceToFile(OAFileTest.class, "missing-resource.txt", target.toString()));
        assertFalse(OAFile.copyResourceToFile(OAFileTest.class, "/com/viaoa/io/OAFileTest.class", null));
        assertTrue(OAFile.copyResourceToFile(OAFileTest.class, "/com/viaoa/io/OAFileTest.class", target.toString()));
        assertTrue(Files.size(target) > 0);
    }

    @Test
    void readResourceTextFileReturnsNullForMissingAndReadsClassResourceLines() throws Exception {
        assertNull(OAFile.readResourceTextFile(OAFileTest.class, "missing-resource.txt"));

        String[] lines = OAFile.readResourceTextFile(OAFileTest.class, "/com/viaoa/io/OAFileTest.class");

        assertNotNull(lines);
        assertTrue(lines.length > 0);
    }

    @Test
    void readTextFileClassReturnsNullForNullAndMissingAndReadsResource() throws Exception {
        assertNull(OAFile.readTextFile(OAFileTest.class, null, 10));
        assertNull(OAFile.readTextFile(OAFileTest.class, "missing-resource.txt", 10));

        String text = OAFile.readTextFile(OAFileTest.class, "/com/viaoa/io/OAFileTest.class", 10);

        assertNotNull(text);
        assertFalse(text.isEmpty());
    }

    @Test
    void readTextFileFileAppendsConfiguredLineSeparator() throws Exception {
        Path file = tempDir.resolve("read.txt");
        Files.writeString(file, "a\nb", StandardCharsets.UTF_8);

        String text = OAFile.readTextFile(file.toFile(), 2);

        assertEquals("a" + OAFile.NL + "b" + OAFile.NL, text);
    }

    @Test
    void readTextFileStringHandlesNullAndSmallEstimate() throws Exception {
        Path file = tempDir.resolve("read-string.txt");
        Files.writeString(file, "a\nb", StandardCharsets.UTF_8);

        assertNull(OAFile.readTextFile((String) null, 10));
        assertEquals("a" + OAFile.NL + "b" + OAFile.NL, OAFile.readTextFile(file.toString(), 1));
    }

    @Test
    void readTextFileIntoListHandlesNullsAndReadsLines() throws Exception {
        Path file = tempDir.resolve("list.txt");
        Files.writeString(file, "a\nb", StandardCharsets.UTF_8);
        List<String> lines = new ArrayList<>();

        assertDoesNotThrow(() -> OAFile.readTextFile(null, lines));
        assertDoesNotThrow(() -> OAFile.readTextFile(file.toString(), null));
        OAFile.readTextFile(file.toString(), lines);

        assertEquals(List.of("a", "b"), lines);
    }

    @Test
    void writeTextFileStringHandlesNullNameAndWritesDataOrEmptyFile() throws Exception {
        Path file = tempDir.resolve("write/string.txt");
        Path empty = tempDir.resolve("write/empty.txt");

        assertFalse(OAFile.writeTextFile((String) null, "ignored"));
        assertTrue(OAFile.writeTextFile(file.toString(), "hello"));
        assertTrue(OAFile.writeTextFile(empty.toString(), null));

        assertEquals("hello", Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(0, Files.size(empty));
    }

    @Test
    void writeTextFileFileHandlesNullFileAndWritesDataOrEmptyFile() throws Exception {
        File file = tempDir.resolve("write-file/file.txt").toFile();
        File empty = tempDir.resolve("write-file/empty.txt").toFile();

        assertFalse(OAFile.writeTextFile((File) null, "ignored"));
        assertTrue(OAFile.writeTextFile(file, "hello"));
        assertTrue(OAFile.writeTextFile(empty, null));

        assertEquals("hello", Files.readString(file.toPath(), StandardCharsets.UTF_8));
        assertEquals(0, Files.size(empty.toPath()));
    }

    @Test
    void rmDirRemoveDirAndDelTreeDeleteTreesAndIgnoreNullMissing() throws Exception {
        Path one = tempDir.resolve("one/a.txt");
        Path two = tempDir.resolve("two/a.txt");
        Path three = tempDir.resolve("three/a.txt");
        Files.createDirectories(one.getParent());
        Files.createDirectories(two.getParent());
        Files.createDirectories(three.getParent());
        Files.writeString(one, "1", StandardCharsets.UTF_8);
        Files.writeString(two, "2", StandardCharsets.UTF_8);
        Files.writeString(three, "3", StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> OAFile.delTree(null));
        assertDoesNotThrow(() -> OAFile.delTree(tempDir.resolve("missing").toFile()));
        OAFile.rmDir(one.getParent().toFile());
        OAFile.removeDir(two.getParent().toFile());
        OAFile.delTree(three.getParent().toFile());

        assertFalse(one.getParent().toFile().exists());
        assertFalse(two.getParent().toFile().exists());
        assertFalse(three.getParent().toFile().exists());
    }
}
