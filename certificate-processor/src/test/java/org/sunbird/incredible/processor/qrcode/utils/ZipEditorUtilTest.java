package org.sunbird.incredible.processor.qrcode.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipEditorUtilTest {

    private final File tempFile1 = new File("test1.txt");
    private final File tempFile2 = new File("test2.txt");
    private final File zipFile = new File("output.zip");

    @AfterEach
    void cleanUp() {
        tempFile1.delete();
        tempFile2.delete();
        zipFile.delete();
    }

    @Test
    void testZipFiles_Success() throws IOException {
        // Create temporary test files
        writeToFile(tempFile1, "Hello World!");
        writeToFile(tempFile2, "Another File");

        List<File> filesToZip = Arrays.asList(tempFile1, tempFile2);
        File zipped = ZipEditorUtil.zipFiles(filesToZip, "output");

        assertNotNull(zipped);
        assertTrue(zipped.exists());
        assertTrue(zipped.length() > 0);

        // Validate zip contents
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipped))) {
            ZipEntry entry;
            int fileCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                assertTrue(entry.getName().equals("test1.txt") || entry.getName().equals("test2.txt"));
                fileCount++;
                zis.closeEntry();
            }
            assertEquals(2, fileCount, "Expected 2 files inside zip");
        }
    }

    private void writeToFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
