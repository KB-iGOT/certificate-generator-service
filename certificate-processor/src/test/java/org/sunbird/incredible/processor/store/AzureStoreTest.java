package org.sunbird.incredible.processor.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AzureStoreTest {

    private StoreConfig storeConfig;

    @BeforeEach
    void setUp() {
        storeConfig = mock(StoreConfig.class, RETURNS_DEEP_STUBS);
        when(storeConfig.getCloudRetryCount()).thenReturn("3");
        when(storeConfig.getType()).thenReturn("azure");

        // Mock AzureConfig values
        when(storeConfig.getAzureStoreConfig().getContainerName()).thenReturn("test-container");
        when(storeConfig.getAzureStoreConfig().getAccount()).thenReturn("test-account");
        when(storeConfig.getAzureStoreConfig().getKey()).thenReturn("test-key");
        when(storeConfig.getAzureStoreConfig().getPath()).thenReturn("folder");
    }

    @Test
    void testUpload() {
        try (
                MockedStatic<StorageServiceFactory> staticMock = mockStatic(StorageServiceFactory.class);
                MockedConstruction<CloudStorage> cloudMock = mockConstruction(CloudStorage.class, (mock, context) ->
                        when(mock.uploadFile(any(), any(), any(), anyBoolean(), anyInt())).thenReturn("upload-success"))
        ) {
            staticMock.when(() -> StorageServiceFactory.getStorageService(any(StorageConfig.class)))
                    .thenReturn(mock(BaseStorageService.class));

            AzureStore azureStore = new AzureStore(storeConfig);
            String result = azureStore.upload(new File("dummy.txt"), "basepath/");
            assertEquals("upload-success", result);
        }
    }

    @Test
    void testDownload() {
        try (
                MockedStatic<StorageServiceFactory> staticMock = mockStatic(StorageServiceFactory.class);
                MockedConstruction<CloudStorage> cloudMock = mockConstruction(CloudStorage.class, (mock, context) ->
                        doNothing().when(mock).downloadFile(any(), any(), any(), anyBoolean()))
        ) {
            staticMock.when(() -> StorageServiceFactory.getStorageService(any(StorageConfig.class)))
                    .thenReturn(mock(BaseStorageService.class));

            AzureStore azureStore = new AzureStore(storeConfig);
            azureStore.download("file.pdf", "local/dir/");
            verify(cloudMock.constructed().get(0)).downloadFile("test-container", "file.pdf", "local/dir/", false);
        }
    }

    @Test
    void testGetPublicLink() {
        try (
                MockedStatic<StorageServiceFactory> staticMock = mockStatic(StorageServiceFactory.class);
                MockedConstruction<CloudStorage> cloudMock = mockConstruction(CloudStorage.class, (mock, context) ->
                        when(mock.upload(any(), any(), any(), anyBoolean(), anyInt())).thenReturn("http://public.link"))
        ) {
            staticMock.when(() -> StorageServiceFactory.getStorageService(any(StorageConfig.class)))
                    .thenReturn(mock(BaseStorageService.class));

            AzureStore azureStore = new AzureStore(storeConfig);
            String result = azureStore.getPublicLink(new File("doc.pdf"), "upload/path/");
            assertEquals("http://public.link", result);
        }
    }

    @Test
    void testClose() {
        try (
                MockedStatic<StorageServiceFactory> staticMock = mockStatic(StorageServiceFactory.class);
                MockedConstruction<CloudStorage> cloudMock = mockConstruction(CloudStorage.class, (mock, context) ->
                        doNothing().when(mock).closeConnection())
        ) {
            staticMock.when(() -> StorageServiceFactory.getStorageService(any(StorageConfig.class)))
                    .thenReturn(mock(BaseStorageService.class));

            AzureStore azureStore = new AzureStore(storeConfig);
            azureStore.close();
            verify(cloudMock.constructed().get(0)).closeConnection();
        }
    }

    @Test
    void testInitWithEmptyType() {
        when(storeConfig.getType()).thenReturn("");
        // Should log error but not crash
        new AzureStore(storeConfig);
    }
}
