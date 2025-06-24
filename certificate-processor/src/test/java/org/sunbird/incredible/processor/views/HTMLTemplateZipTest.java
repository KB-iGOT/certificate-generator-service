package org.sunbird.incredible.processor.views;

import org.junit.jupiter.api.*;
import org.sunbird.incredible.processor.store.ICertStore;

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
}
