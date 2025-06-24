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

class CephStoreTest {

    private StoreConfig storeConfig;

    @BeforeEach
    void setUp() {
        // Deep stub mocks cephStoreConfig.getX().getY()
        storeConfig = mock(StoreConfig.class, RETURNS_DEEP_STUBS);
        when(storeConfig.getCloudRetryCount()).thenReturn("3");
        when(storeConfig.getType()).thenReturn("ceph");

        // Mocking CephConfig fields
        when(storeConfig.getCephStoreConfig().getContainerName()).thenReturn("test-container");
        when(storeConfig.getCephStoreConfig().getAccount()).thenReturn("test-account");
        when(storeConfig.getCephStoreConfig().getKey()).thenReturn("test-key");
        when(storeConfig.getCephStoreConfig().getPath()).thenReturn("subpath");
    }

    @Test
    void testUpload() {
        try (
                MockedStatic<StorageServiceFactory> staticMock = mockStatic(StorageServiceFactory.class);
                MockedConstruction<CloudStorage> cloudMock = mockConstruction(CloudStorage.class, (mock, context) ->
                        when(mock.uploadFile(any(), any(), any(), anyBoolean(), anyInt())).thenReturn("uploaded-file"))
        ) {
            staticMock.when(() -> StorageServiceFactory.getStorageService(any(StorageConfig.class)))
                    .thenReturn(mock(BaseStorageService.class));

            CephStore cephStore = new CephStore(storeConfig);
            String result = cephStore.upload(new File("test.txt"), "mypath/");
            assertEquals("uploaded-file", result);
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

            CephStore cephStore = new CephStore(storeConfig);
            cephStore.download("remote.txt", "local/path/");
            verify(cloudMock.constructed().get(0)).downloadFile("test-container", "remote.txt", "local/path/", false);
        }
    }

    @Test
    void testGetPublicLink() {
        try (
                MockedStatic<StorageServiceFactory> staticMock = mockStatic(StorageServiceFactory.class);
                MockedConstruction<CloudStorage> cloudMock = mockConstruction(CloudStorage.class, (mock, context) ->
                        when(mock.upload(any(), any(), any(), anyBoolean(), anyInt())).thenReturn("public-link"))
        ) {
            staticMock.when(() -> StorageServiceFactory.getStorageService(any(StorageConfig.class)))
                    .thenReturn(mock(BaseStorageService.class));

            CephStore cephStore = new CephStore(storeConfig);
            String result = cephStore.getPublicLink(new File("test.txt"), "path/");
            assertEquals("public-link", result);
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

            CephStore cephStore = new CephStore(storeConfig);
            cephStore.close();
            verify(cloudMock.constructed().get(0)).closeConnection();
        }
    }

    @Test
    void testInitWithEmptyType() {
        when(storeConfig.getType()).thenReturn("");
        // No exception should be thrown
        new CephStore(storeConfig);
    }
}
