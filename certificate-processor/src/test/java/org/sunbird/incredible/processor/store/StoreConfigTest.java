package org.sunbird.incredible.processor.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.sunbird.incredible.processor.JsonKey;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StoreConfigTest {

    @Test
    public void testAzureConfigInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put(JsonKey.TYPE, JsonKey.AZURE);

        AzureStoreConfig azureConfig = new AzureStoreConfig();
        azureConfig.setContainerName("azure-container");
        azureConfig.setAccount("azure-account");
        azureConfig.setKey("azure-key");
        azureConfig.setPath("azure-path");

        params.put(JsonKey.AZURE, new ObjectMapper().convertValue(azureConfig, Map.class));

        StoreConfig config = new StoreConfig(params);
        assertEquals(JsonKey.AZURE, config.getType());
        assertEquals("azure-container", config.getAzureStoreConfig().getContainerName());
        assertTrue(config.isCloudStore());
        assertEquals("azure-container", config.getContainerName());
    }

    @Test
    public void testAwsConfigInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put(JsonKey.TYPE, JsonKey.AWS);

        AwsStoreConfig awsConfig = new AwsStoreConfig();
        awsConfig.setContainerName("aws-container");
        awsConfig.setAccount("aws-account");
        awsConfig.setKey("aws-key");
        awsConfig.setPath("aws-path");

        params.put(JsonKey.AWS, new ObjectMapper().convertValue(awsConfig, Map.class));

        StoreConfig config = new StoreConfig(params);
        assertEquals(JsonKey.AWS, config.getType());
        assertEquals("aws-container", config.getAwsStoreConfig().getContainerName());
        assertTrue(config.isCloudStore());
        assertEquals("aws-container", config.getContainerName());
    }

    @Test
    public void testCephConfigInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put(JsonKey.TYPE, JsonKey.CEPHS3);

        CephStoreConfig cephConfig = new CephStoreConfig();
        cephConfig.setContainerName("ceph-container");
        cephConfig.setAccount("ceph-account");
        cephConfig.setKey("ceph-key");
        cephConfig.setPath("ceph-path");

        params.put(JsonKey.CEPHS3, new ObjectMapper().convertValue(cephConfig, Map.class));

        StoreConfig config = new StoreConfig(params);
        assertEquals(JsonKey.CEPHS3, config.getType());
        assertEquals("ceph-container", config.getCephStoreConfig().getContainerName());
        assertFalse(config.isCloudStore()); // Because isCloudStore only checks Azure or AWS
        assertEquals("ceph-container", config.getContainerName());
    }

    @Test
    public void testGcpConfigInitialization() {
        Map<String, Object> params = new HashMap<>();
        params.put(JsonKey.TYPE, JsonKey.GCP);

        CloudStoreConfig cloudStoreConfig = new CloudStoreConfig();
        cloudStoreConfig.setContainerName("gcp-container");
        cloudStoreConfig.setAccount("gcp-account");
        cloudStoreConfig.setKey("gcp-key");
        cloudStoreConfig.setPath("gcp-path");

        params.put(JsonKey.GCP, new ObjectMapper().convertValue(cloudStoreConfig, Map.class));

        StoreConfig config = new StoreConfig(params);
        assertEquals(JsonKey.GCP, config.getType());
        assertEquals("gcp-container", config.getCloudStoreConfig().getContainerName());
        assertFalse(config.isCloudStore());
        assertEquals("gcp-container", config.getContainerName());
    }

    @Test
    public void testInvalidConfig() {
        Map<String, Object> params = new HashMap<>();
        params.put(JsonKey.TYPE, "unknown");

        StoreConfig config = new StoreConfig(params);
        assertEquals("unknown", config.getType());
        assertNull(config.getContainerName());
    }

}
