package org.sunbird;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.incredible.processor.JsonKey;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QRStorageParamsTest {

    @BeforeEach
    void clearEnv() {
        System.clearProperty(JsonKey.PUBLIC_CONTAINER_NAME);
        System.clearProperty(JsonKey.PUBLIC_AZURE_STORAGE_KEY);
        System.clearProperty(JsonKey.PUBLIC_AZURE_STORAGE_SECRET);
        System.clearProperty(JsonKey.PUBLIC_AWS_STORAGE_KEY);
        System.clearProperty(JsonKey.PUBLIC_AWS_STORAGE_SECRET);
        System.clearProperty(JsonKey.PUBLIC_CEPHS3_STORAGE_KEY);
        System.clearProperty(JsonKey.PUBLIC_CEPHS3_STORAGE_SECRET);
        System.clearProperty(JsonKey.PUBLIC_CEPHS3_STORAGE_ENDPOINT);
        System.clearProperty(JsonKey.GCP_STORAGE_KEY);
        System.clearProperty(JsonKey.GCP_STORAGE_SECRET);
        System.clearProperty(JsonKey.GCP_STORAGE_ENDPOINT);
    }

    @Test
    void storeParamsContainsTypeForUnknownType() {
        QRStorageParams params = new QRStorageParams("unknown");
        assertEquals("unknown", params.storeParams.get(JsonKey.TYPE));
        assertEquals(1, params.storeParams.size());
    }

    @Test
    void storeParamsPopulatesAzureParamsWhenTypeIsAzure() {
        setEnv(JsonKey.PUBLIC_CONTAINER_NAME, "cont");
        setEnv(JsonKey.PUBLIC_AZURE_STORAGE_KEY, "acc");
        setEnv(JsonKey.PUBLIC_AZURE_STORAGE_SECRET, "key");
        QRStorageParams params = new QRStorageParams(JsonKey.AZURE);
        assertEquals(JsonKey.AZURE, params.storeParams.get(JsonKey.TYPE));
        Map<String, String> azure = (Map<String, String>) params.storeParams.get(JsonKey.AZURE);
        assertNotNull(azure);
    }

    @Test
    void storeParamsPopulatesAwsParamsWhenTypeIsAws() {
        setEnv(JsonKey.PUBLIC_CONTAINER_NAME, "cont");
        setEnv(JsonKey.PUBLIC_AWS_STORAGE_KEY, "acc");
        setEnv(JsonKey.PUBLIC_AWS_STORAGE_SECRET, "key");
        QRStorageParams params = new QRStorageParams(JsonKey.AWS);
        assertEquals(JsonKey.AWS, params.storeParams.get(JsonKey.TYPE));
        Map<String, String> aws = (Map<String, String>) params.storeParams.get(JsonKey.AWS);
        assertNotNull(aws);
    }

    @Test
    void storeParamsPopulatesCephs3ParamsWhenTypeIsCephs3() {
        setEnv(JsonKey.PUBLIC_CONTAINER_NAME, "cont");
        setEnv(JsonKey.PUBLIC_CEPHS3_STORAGE_KEY, "acc");
        setEnv(JsonKey.PUBLIC_CEPHS3_STORAGE_SECRET, "key");
        setEnv(JsonKey.PUBLIC_CEPHS3_STORAGE_ENDPOINT, "endpoint");
        QRStorageParams params = new QRStorageParams(JsonKey.CEPHS3);
        assertEquals(JsonKey.CEPHS3, params.storeParams.get(JsonKey.TYPE));
        Map<String, String> cephs3 = (Map<String, String>) params.storeParams.get(JsonKey.CEPHS3);
        assertNotNull(cephs3);
    }

//    @Test
//    void storeParamsPopulatesGcpParamsWhenTypeIsGcp() {
//        setEnv(JsonKey.PUBLIC_CONTAINER_NAME, "cont");
//        setEnv(JsonKey.GCP_STORAGE_KEY, "acc");
//        setEnv(JsonKey.GCP_STORAGE_SECRET, "line1\\nline2");
//        setEnv(JsonKey.GCP_STORAGE_ENDPOINT, "endpoint");
//        QRStorageParams params = new QRStorageParams(JsonKey.GCP);
//        assertEquals(JsonKey.GCP, params.storeParams.get(JsonKey.TYPE));
//        Map<String, String> gcp = (Map<String, String>) params.storeParams.get(JsonKey.GCP);
//        assertEquals("cont", gcp.get(JsonKey.containerName));
//        assertEquals("acc", gcp.get(JsonKey.ACCOUNT));
//        assertEquals("line1\nline2", gcp.get(JsonKey.KEY));
//        assertEquals("endpoint", gcp.get(JsonKey.ENDPOINT));
//    }

    @Test
    void storeParamsHandlesNullEnvVarsGracefully() {
        QRStorageParams params = new QRStorageParams(JsonKey.AZURE);
        Map<String, String> azure = (Map<String, String>) params.storeParams.get(JsonKey.AZURE);
        assertTrue(azure.containsKey(JsonKey.containerName));
        assertNull(azure.get(JsonKey.containerName));
    }

    private void setEnv(String key, String value) {
        Map<String, String> env = System.getenv();
        try {
            java.lang.reflect.Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            ((Map<String, String>) field.get(env)).put(key, value);
        } catch (Exception e) {
            // fallback for some JVMs
            System.setProperty(key, value);
        }
    }
}
