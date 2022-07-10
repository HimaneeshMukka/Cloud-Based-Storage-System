package core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileSystemTest {
    FileSystem fileSystem;
    static String directoryPath = "src/test/resources/filesystem";

    @BeforeAll
    public static void beforeAll() throws IOException {
        Files.createDirectories(Paths.get(directoryPath));
    }

    @BeforeEach
    public void beforeEach() {
        fileSystem = new FileSystem(directoryPath);
        for(File file: Objects.requireNonNull(new File(directoryPath).listFiles())) {
            file.delete();
        }
    }

    @Test
    void testNewFiles() throws IOException {
        fileSystem.reloadCachedFiles();
        File file = new File(directoryPath + "/test.txt");
        assertTrue(file.createNewFile());
        assertEquals(1, fileSystem.getAddedFiles().size());
        assertEquals("test.txt", fileSystem.getAddedFiles().get(0).name);
    }

    @Test
    void testDeleteFiles() throws IOException {
        File file = new File(directoryPath + "/test.txt");
        assertTrue(file.createNewFile());
        fileSystem.reloadCachedFiles();

        assertEquals(Objects.requireNonNull(new File(directoryPath).listFiles()).length, fileSystem.getCachedFiles().size());

        file.delete();

        assertEquals(1, fileSystem.getDeletedFiles().size());
        assertEquals("test.txt", fileSystem.getDeletedFiles().get(0).name);
    }

    @Test
    void testModifiedFiles() throws IOException {
        fileSystem.reloadCachedFiles();
        File file = new File(directoryPath + "/test.txt");
        assertTrue(file.createNewFile());
        assertEquals(1, fileSystem.getAddedFiles().size());
        assertEquals("test.txt", fileSystem.getAddedFiles().get(0).name);
        fileSystem.reloadCachedFiles();

        String message = "Hello World";
        Files.writeString(Paths.get(directoryPath + "/test.txt"), message);

        assertEquals(1, fileSystem.getModifiedFiles().size());
        assertEquals("test.txt", fileSystem.getModifiedFiles().get(0).name);
        assertEquals(message.length(), fileSystem.getModifiedFiles().get(0).length);
    }

    @Test
    void testDeleteFile() throws IOException {
        File file = new File(directoryPath + "/test.txt");
        assertTrue(file.createNewFile());

        assertEquals(1, Objects.requireNonNull(new File(directoryPath).listFiles()).length);

        fileSystem.deleteFromDisk(new FileMeta("test.txt", file.length(), file.lastModified()));

        assertEquals(0, Objects.requireNonNull(new File(directoryPath).listFiles()).length);
    }

    @Test
    void testReadFile() throws IOException {
        File file = new File(directoryPath + "/test.txt");
        assertTrue(file.createNewFile());
        String message = "Hello World";
        Files.writeString(Paths.get(directoryPath + "/test.txt"), message);

        assertEquals(message, new String(fileSystem.readFromDisk("test.txt")));
    }

    @Test
    void testWriteFile() throws IOException {
        String message = "Hello World";
        fileSystem.writeToDisk(message.getBytes(), new FileMeta("test.txt", message.length(), System.currentTimeMillis()));
        assertEquals(message, new String(fileSystem.readFromDisk("test.txt")));
    }

    @Test
    void testModifyCachedFile() {
        assertEquals(0, fileSystem.getCachedFiles().size());
        FileMeta fileMeta = new FileMeta("test.txt", 0, System.currentTimeMillis());
        fileSystem.addFileToCache(fileMeta);
        assertEquals(1, fileSystem.getCachedFiles().size());
        assertTrue(fileSystem.getCachedFiles().contains(fileMeta));
        fileSystem.deleteFileFromCache(fileMeta);
        assertEquals(0, fileSystem.getCachedFiles().size());
    }


    @AfterAll
    public static void afterAll() throws IOException {
        // Clean up
        for(File file: Objects.requireNonNull(new File(directoryPath).listFiles())) {
            file.delete();
        }
        Files.delete(Paths.get(directoryPath));

    }
}
