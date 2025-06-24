package org.sunbird.incredible.processor.store;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sunbird.incredible.processor.JsonKey;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CertStoreFactoryTest {

    private Map<String, String> properties;
    private CertStoreFactory factory;

    @BeforeEach
    public void setUp() {
        properties = new HashMap<>();
        properties.put(JsonKey.DOMAIN_URL, "https://certs.org");
        properties.put(JsonKey.BASE_PATH, "https://certs.org/path");
        properties.put(JsonKey.TAG, "sunbird");
        factory = new CertStoreFactory(properties);
    }

    @Test
    public void testGetHtmlTemplateStore_HttpAzure() {
        StoreConfig config = mock(StoreConfig.class);
        AzureStoreConfig azureConfig = mock(AzureStoreConfig.class);

        when(config.getContainerName()).thenReturn("azure-container");
        when(config.isCloudStore()).thenReturn(true);
        when(config.getType()).thenReturn(JsonKey.AZURE);
        when(config.getCloudRetryCount()).thenReturn("3");
        when(config.getAzureStoreConfig()).thenReturn(azureConfig);

        when(azureConfig.getContainerName()).thenReturn("azure-container");
        when(azureConfig.getAccount()).thenReturn("account");
        when(azureConfig.getKey()).thenReturn("key");
        when(azureConfig.getPath()).thenReturn("path");

        ICertStore store = factory.getHtmlTemplateStore("http://azure-container.blob.core.windows.net", config);
        assertTrue(store instanceof AzureStore);
    }


    @Test
    public void testGetHtmlTemplateStore_HttpLocal() {
        StoreConfig config = mock(StoreConfig.class);
        when(config.getContainerName()).thenReturn("wrong-container");
        when(config.isCloudStore()).thenReturn(true);

        ICertStore store = factory.getHtmlTemplateStore("http://otherdomain.com/file.html", config);
        assertTrue(store instanceof LocalStore);
    }

    @Test
    public void testGetHtmlTemplateStore_NonHttpCloud() {
        StoreConfig config = mock(StoreConfig.class);
        AwsStoreConfig awsStoreConfig = mock(AwsStoreConfig.class);

        when(config.isCloudStore()).thenReturn(true);
        when(config.getType()).thenReturn(JsonKey.AWS);
        when(config.getCloudRetryCount()).thenReturn("3"); // ✅ FIX HERE
        when(config.getAwsStoreConfig()).thenReturn(awsStoreConfig);
        when(awsStoreConfig.getContainerName()).thenReturn("bucket");
        when(awsStoreConfig.getAccount()).thenReturn("account");
        when(awsStoreConfig.getKey()).thenReturn("key");
        when(awsStoreConfig.getPath()).thenReturn("path");

        ICertStore store = factory.getHtmlTemplateStore("file/path/template.html", config);
        assertTrue(store instanceof AwsStore);
    }


    @Test
    public void testGetCertStore_PreviewTrue() {
        StoreConfig config = mock(StoreConfig.class);
        ICertStore store = factory.getCertStore(config, true);
        assertTrue(store instanceof LocalStore);
    }

    @Test
    public void testGetCertStore_CloudStore() {
        StoreConfig config = mock(StoreConfig.class);
        CephStoreConfig cephConfig = mock(CephStoreConfig.class);

        when(config.isCloudStore()).thenReturn(true);
        when(config.getType()).thenReturn(JsonKey.CEPHS3);
        when(config.getCephStoreConfig()).thenReturn(cephConfig);
        when(config.getCloudRetryCount()).thenReturn("3");

        when(cephConfig.getContainerName()).thenReturn("ceph-container");
        when(cephConfig.getAccount()).thenReturn("ceph-account");
        when(cephConfig.getKey()).thenReturn("ceph-key");
        when(cephConfig.getPath()).thenReturn("ceph-path");

        ICertStore store = factory.getCertStore(config, false);
        assertTrue(store instanceof CephStore);
    }


    @Test
    public void testGetCertStore_NonCloudStore() {
        StoreConfig config = mock(StoreConfig.class);
        when(config.isCloudStore()).thenReturn(false);

        ICertStore store = factory.getCertStore(config, false);
        assertTrue(store instanceof LocalStore);
    }

    @Test
    public void testCleanUp() throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File file = new File(tempDir, "test123.txt");
        file.createNewFile();

        try (MockedStatic<FileUtils> utilities = Mockito.mockStatic(FileUtils.class)) {
            Collection<File> files = Collections.singletonList(file);
            utilities.when(() -> FileUtils.listFiles(any(), any(), isNull())).thenReturn(files);
            factory.cleanUp("test123", tempDir);
            assertFalse(file.exists() && file.delete());
        }
    }

    @Test
    public void testGetDirectoryName_WithTag() {
        String dir = factory.getDirectoryName("sample.zip");
        assertEquals("conf/sunbird_sample.zip/", dir);
    }

    @Test
    public void testGetCloudStore_Azure() {
        StoreConfig mockStoreConfig = mock(StoreConfig.class);
        AzureStoreConfig mockAzureStoreConfig = mock(AzureStoreConfig.class);

        when(mockStoreConfig.getType()).thenReturn(JsonKey.AZURE);
        when(mockStoreConfig.isCloudStore()).thenReturn(true);
        when(mockStoreConfig.getCloudRetryCount()).thenReturn("3"); // ✅ Fix here
        when(mockStoreConfig.getAzureStoreConfig()).thenReturn(mockAzureStoreConfig);

        when(mockAzureStoreConfig.getContainerName()).thenReturn("container");
        when(mockAzureStoreConfig.getAccount()).thenReturn("account");
        when(mockAzureStoreConfig.getKey()).thenReturn("key");
        when(mockAzureStoreConfig.getPath()).thenReturn("path");

        CertStoreFactory factory = new CertStoreFactory(Map.of(JsonKey.DOMAIN_URL, "http://example.com"));
        CloudStore cloudStore = factory.getCloudStore(mockStoreConfig);
        assertTrue(cloudStore instanceof AzureStore);
    }

    @Test
    public void testGetCloudStore_Aws() {
        StoreConfig config = mock(StoreConfig.class);
        AwsStoreConfig awsConfig = mock(AwsStoreConfig.class);

        when(config.getType()).thenReturn(JsonKey.AWS);
        when(config.getCloudRetryCount()).thenReturn("3"); // important
        when(config.getAwsStoreConfig()).thenReturn(awsConfig);

        when(awsConfig.getContainerName()).thenReturn("bucket");
        when(awsConfig.getAccount()).thenReturn("account");
        when(awsConfig.getKey()).thenReturn("secret");
        when(awsConfig.getPath()).thenReturn("path");

        CloudStore store = factory.getCloudStore(config);
        assertTrue(store instanceof AwsStore);
    }

    @Test
    public void testGetCloudStore_Ceph() {
        StoreConfig config = mock(StoreConfig.class);
        CephStoreConfig cephConfig = mock(CephStoreConfig.class);

        when(config.getType()).thenReturn(JsonKey.CEPHS3);
        when(config.getCloudRetryCount()).thenReturn("3"); // important
        when(config.getCephStoreConfig()).thenReturn(cephConfig);

        when(cephConfig.getContainerName()).thenReturn("ceph-container");
        when(cephConfig.getAccount()).thenReturn("account");
        when(cephConfig.getKey()).thenReturn("secret");
        when(cephConfig.getPath()).thenReturn("path");

        CloudStore store = factory.getCloudStore(config);
        assertTrue(store instanceof CephStore);
    }


    @Test
    public void testGetCloudStore_Invalid() {
        StoreConfig config = mock(StoreConfig.class);
        when(config.getType()).thenReturn("invalid");
        CloudStore store = factory.getCloudStore(config);
        assertNull(store);
    }

    @Test
    public void testSetCloudPath_Cloud() {
        StoreConfig config = mock(StoreConfig.class);
        when(config.isCloudStore()).thenReturn(true);
        String path = factory.setCloudPath(config);
        assertEquals("sunbird/", path);
    }

    @Test
    public void testSetCloudPath_Local() {
        StoreConfig config = mock(StoreConfig.class);
        when(config.isCloudStore()).thenReturn(false);
        String path = factory.setCloudPath(config);
        assertEquals("public/", path);
    }
}
