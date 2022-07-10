package core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileMetaTest {

    @Test
    void testFileMetaEquals() {
        FileMeta fileMeta1 = new FileMeta("file1", 10, 10);
        FileMeta fileMeta2 = new FileMeta("file1", 10, 10);
        assertTrue(fileMeta1.equals(fileMeta2));
    }

    @Test
    void testFileMetaCompareTo() {
        FileMeta fileMeta1 = new FileMeta("file1", 10, 10);
        FileMeta fileMeta2 = new FileMeta("file2", 10, 10);
        assertEquals(-1, fileMeta1.compareTo(fileMeta2));
    }
}
