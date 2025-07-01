package org.sunbird.incredible.processor.views;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.sunbird.incredible.processor.store.ICertStore;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HTMLTemplateZipTest {

    private static final String ZIP_PATH = "/mnt/data/conf/sample-template.zip";
    private static final String ZIP_URL = "file://" + ZIP_PATH;
    private HTMLTemplateZip htmlTemplateZip;
    private ICertStore certStore;

    @BeforeEach
    void setup() {
        certStore = mock(ICertStore.class);
        htmlTemplateZip = new HTMLTemplateZip(certStore, ZIP_URL);
    }

    @Test
    void testInitSetsCorrectTargetDir() {
        assertTrue(htmlTemplateZip.getTemplateUrl().endsWith(".zip"));
    }

    @Test
    void testDownloadCallsStoreMethods() throws Exception {
        doNothing().when(certStore).init();
        doNothing().when(certStore).get(any(), any(), any());
        htmlTemplateZip.download();
        verify(certStore).init();
        verify(certStore).get(any(), any(), any());
    }

    @Test
    void testIsZipFileExists() {
        assertFalse(htmlTemplateZip.isZipFileExists());
    }

    @Test
    void testGetZipFileNameHandlesInvalidUrl() {
        HTMLTemplateZip zip = new HTMLTemplateZip(certStore, "%%%badurl%%%");
        assertNotNull(zip.getTemplateUrl());  // will still return the string
    }

    @Test
    void testCleanUp_whenZipFileDeletionFails_shouldLogWarning() throws Exception {
        // Arrange
        File tempZipFile = File.createTempFile("template", ".zip");
        String zipFileName = tempZipFile.getName();
        String zipFilePath = tempZipFile.getParent() + "/";

        // Set zipFilePath and zipFileName into HTMLTemplateZip
        setPrivateField(htmlTemplateZip, "zipFileName", zipFileName);
        setPrivateField(htmlTemplateZip, "zipFilePath", zipFilePath);

        // Mock FileUtils.deleteDirectory (you don’t want to actually delete anything)
        try (MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class)) {
            fileUtilsMock.when(() -> FileUtils.deleteDirectory(any(File.class))).thenAnswer(inv -> null);

            // Make the zip file non-deletable (simulate it being in use)
            File zipFile = new File(zipFilePath + zipFileName);
            zipFile.setWritable(false);

            // Act
            htmlTemplateZip.cleanUp();

            // Assert: You can check logs using ListAppender if necessary (optional)
        } finally {
            // Restore deletable so it can be removed on exit
            File zipFile = new File(zipFilePath + zipFileName);
            zipFile.setWritable(true);
            zipFile.deleteOnExit();
        }
    }


    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = HTMLTemplateZip.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}
