package org.sunbird.incredible.processor.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AwsStoreTest {

    private AwsStore awsStore;
    private StoreConfig storeConfig;
    private CloudStorage cloudStorage;
    private BaseStorageService baseStorageService;

    @BeforeEach
    void setUp() throws Exception {
        storeConfig = mock(StoreConfig.class, RETURNS_DEEP_STUBS);
        cloudStorage = mock(CloudStorage.class);
        baseStorageService = mock(BaseStorageService.class);

        when(storeConfig.getCloudRetryCount()).thenReturn("3");
        when(storeConfig.getType()).thenReturn("aws");
        when(storeConfig.getAwsStoreConfig().getAccount()).thenReturn("account");
        when(storeConfig.getAwsStoreConfig().getKey()).thenReturn("key");
        when(storeConfig.getAwsStoreConfig().getContainerName()).thenReturn("container");
        when(storeConfig.getAzureStoreConfig().getPath()).thenReturn("prefix");

        try (MockedStatic<StorageServiceFactory> factoryMock = Mockito.mockStatic(StorageServiceFactory.class)) {
            factoryMock.when(() -> StorageServiceFactory.getStorageService(any(StorageConfig.class)))
                    .thenReturn(baseStorageService);
            awsStore = new AwsStore(storeConfig);
        }

        // inject mock cloudStorage
        setPrivateField(awsStore, "cloudStorage", cloudStorage);
    }

    @Test
    void testUpload() {
        File dummyFile = new File("dummy.txt");
        when(cloudStorage.uploadFile(anyString(), anyString(), any(File.class), anyBoolean(), anyInt()))
                .thenReturn("url");

        String result = awsStore.upload(dummyFile, "test/");
        assertEquals("url", result);
    }

    @Test
    void testDownload() {
        doNothing().when(cloudStorage).downloadFile(anyString(), anyString(), anyString(), anyBoolean());
        awsStore.download("remoteFile.txt", "localPath/");
        verify(cloudStorage, times(1)).downloadFile(anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void testGetPublicLink() {
        File file = new File("dummy.txt");
        when(cloudStorage.upload(anyString(), anyString(), any(File.class), anyBoolean(), anyInt()))
                .thenReturn("public-url");

        String result = awsStore.getPublicLink(file, "public/path");
        assertEquals("public-url", result);
    }

    @Test
    void testClose() {
        doNothing().when(cloudStorage).closeConnection();
        awsStore.close();
        verify(cloudStorage, times(1)).closeConnection();
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
