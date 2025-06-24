package org.sunbird.incredible.processor.store;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GenericCloudStoreTest {

    private MockedStatic<StorageServiceFactory> storageFactoryStatic;

    private StoreConfig mockStoreConfig;
    private CloudStoreConfig mockCloudConfig;
    private CephStoreConfig mockCephConfig;

    @TempDir
    Path tempDir;

    @BeforeAll
    void setupStaticMocks() {
        storageFactoryStatic = Mockito.mockStatic(StorageServiceFactory.class);
    }

    @AfterAll
    void closeStaticMocks() {
        storageFactoryStatic.close();
    }

    @BeforeEach
    void setup() {
        mockStoreConfig = mock(StoreConfig.class);
        mockCloudConfig = mock(CloudStoreConfig.class);
        mockCephConfig = mock(CephStoreConfig.class);

        when(mockStoreConfig.getCloudRetryCount()).thenReturn("1");
        when(mockStoreConfig.getType()).thenReturn("cephs3");
        when(mockStoreConfig.getCloudStoreConfig()).thenReturn(mockCloudConfig);
        when(mockStoreConfig.getCephStoreConfig()).thenReturn(mockCephConfig);
        when(mockCloudConfig.getAccount()).thenReturn("test-account");
        when(mockCloudConfig.getKey()).thenReturn("test-key");
        when(mockCephConfig.getContainerName()).thenReturn("test-container");
        when(mockCephConfig.getPath()).thenReturn("test-path");

        BaseStorageService storageService = mock(BaseStorageService.class);
        storageFactoryStatic.when(() -> StorageServiceFactory.getStorageService(any())).thenReturn(storageService);
    }

    @Test
    void testUpload() throws Exception {
        try (MockedConstruction<CloudStorage> cloudMock = mockConstruction(CloudStorage.class,
                (mock, context) -> {
                    when(mock.uploadFile(any(), any(), any(), anyBoolean(), anyInt())).thenReturn("upload-success");
                })) {

            File testFile = File.createTempFile("upload", ".txt", tempDir.toFile());
            GenericCloudStore store = new GenericCloudStore(mockStoreConfig);
            String result = store.upload(testFile, "certs/");
            assertEquals("upload-success", result);
        }
    }

    @Test
    void testDownload() {
        try (MockedConstruction<CloudStorage> cloudMock = mockConstruction(CloudStorage.class,
                (mock, context) -> {
                    doNothing().when(mock).downloadFile(any(), any(), any(), anyBoolean());
                })) {
            GenericCloudStore store = new GenericCloudStore(mockStoreConfig);
            store.download("myfile.pdf", tempDir.toString());
        }
    }

    @Test
    void testGetPublicLink() throws Exception {
        try (MockedConstruction<CloudStorage> cloudMock = mockConstruction(CloudStorage.class,
                (mock, context) -> {
                    when(mock.upload(any(), any(), any(), anyBoolean(), anyInt())).thenReturn("http://mocked.url");
                })) {

            File testFile = File.createTempFile("public", ".pdf", tempDir.toFile());
            GenericCloudStore store = new GenericCloudStore(mockStoreConfig);
            String link = store.getPublicLink(testFile, "certs/");
            assertEquals("http://mocked.url", link);
        }
    }

    @Test
    void testClose() {
        try (MockedConstruction<CloudStorage> cloudMock = mockConstruction(CloudStorage.class,
                (mock, context) -> {
                    doNothing().when(mock).closeConnection();
                })) {

            GenericCloudStore store = new GenericCloudStore(mockStoreConfig);
            store.close();
        }
    }

    @Test
    void testInit_invalidType() {
        when(mockStoreConfig.getType()).thenReturn("");
        GenericCloudStore store = new GenericCloudStore(mockStoreConfig);
        assertNotNull(store); // Should not crash
    }
}
