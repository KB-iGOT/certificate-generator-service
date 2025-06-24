package org.sunbird.incredible.processor.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.incredible.UrlManager;

import java.io.File;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CloudStorageTest {

    private BaseStorageService storageService;
    private CloudStorage cloudStorage;
    private File mockFile;

    @BeforeEach
    public void setUp() throws Exception {
        storageService = mock(BaseStorageService.class);
        cloudStorage = new CloudStorage(storageService);
        mockFile = mock(File.class);
        when(mockFile.getName()).thenReturn("test.pdf");
        when(mockFile.getAbsolutePath()).thenReturn("/tmp/test.pdf");
    }

    @Test
    public void testUpload() {
        try (MockedStatic<UrlManager> urlManagerMock = mockStatic(UrlManager.class)) {
            String signedUrl = "https://example.com/test.pdf?signature=xyz";
            String cleanUrl = "https://example.com/test.pdf";

            when(storageService.upload(eq("my-container"), eq("/tmp/test.pdf"), eq("certs/test.pdf"),
                    any(), any(), any(), any())).thenReturn(signedUrl);

            urlManagerMock.when(() -> UrlManager.removeQueryParams(signedUrl)).thenReturn(cleanUrl);

            String result = cloudStorage.upload("my-container", "certs/", mockFile, false, 3);

            assertEquals(cleanUrl, result);
            urlManagerMock.verify(() -> UrlManager.removeQueryParams(signedUrl), times(1));
        }
    }

    @Test
    public void testUploadFile() {
        try (MockedStatic<UrlManager> urlManagerMock = mockStatic(UrlManager.class)) {
            String signedUrl = "https://example.com/test.pdf?signature=abc";
            String sharableUrl = "https://example.com/public/test.pdf";

            when(storageService.upload(eq("my-container"), eq("/tmp/test.pdf"), eq("certs/test.pdf"),
                    any(), any(), any(), any())).thenReturn(signedUrl);

            urlManagerMock.when(() -> UrlManager.getSharableUrl(signedUrl, "my-container")).thenReturn(sharableUrl);

            String result = cloudStorage.uploadFile("my-container", "certs/", mockFile, false, 3);

            assertEquals(sharableUrl, result);
            urlManagerMock.verify(() -> UrlManager.getSharableUrl(signedUrl, "my-container"), times(1));
        }
    }

    @Test
    public void testDownloadFile() {
        cloudStorage.downloadFile("my-container", "test.pdf", "/tmp/", false);
        verify(storageService, times(1)).download(eq("my-container"), eq("test.pdf"), eq("/tmp/"), any());
    }

    @Test
    public void testCloseConnection() {
        cloudStorage.closeConnection();
        verify(storageService, times(1)).closeContext();
    }
}
