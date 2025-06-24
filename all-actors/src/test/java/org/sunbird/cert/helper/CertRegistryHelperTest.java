package org.sunbird.cert.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.PropertiesCache;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.response.Response;
import org.sunbird.HttpUtil;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CertRegistryHelperTest {

    private static CertRegistryHelper helper;
    private static CassandraOperation cassandraOperation;
    private static PropertiesCache propertiesCache;
    private static ObjectMapper objectMapper;

    private static MockedStatic<ServiceFactory> serviceFactoryMockedStatic;
    private static MockedStatic<PropertiesCache> propertiesCacheMockedStatic;
    private static MockedStatic<HttpUtil> httpUtilMockedStatic;

    @BeforeAll
    static void init() {
        cassandraOperation = mock(CassandraOperation.class);
        serviceFactoryMockedStatic = mockStatic(ServiceFactory.class);
        serviceFactoryMockedStatic.when(ServiceFactory::getInstance).thenReturn(cassandraOperation);

        propertiesCache = mock(PropertiesCache.class);
        propertiesCacheMockedStatic = mockStatic(PropertiesCache.class);
        propertiesCacheMockedStatic.when(PropertiesCache::getInstance).thenReturn(propertiesCache);

        httpUtilMockedStatic = mockStatic(HttpUtil.class);

        helper = CertRegistryHelper.getInstance();
    }

    @AfterAll
    static void cleanup() {
        serviceFactoryMockedStatic.close();
        propertiesCacheMockedStatic.close();
        httpUtilMockedStatic.close();
    }

    @Test
    void testAddCertToRegistry_success() throws Exception {
        Map<String, Object> addReq = new HashMap<>();
        addReq.put("recipient", "test");

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("certId", "12345");

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put(JsonKeys.RESPONSE_CODE, JsonKeys.OK);
        responseMap.put(JsonKeys.RESULT, resultMap);

        when(propertiesCache.getProperty(JsonKeys.CERT_REGISTRY_BASE_PATH)).thenReturn("http://example.com/");
        when(propertiesCache.getProperty(JsonKeys.ADD_CERT_REG_API)).thenReturn("add");

        httpUtilMockedStatic.when(() ->
                        HttpUtil.sendPostRequest(anyString(), anyString(), anyMap()))
                .thenReturn("{\"responseCode\":\"OK\", \"result\":{\"certId\":\"12345\"}}");

        Map<String, Object> result = helper.addCertToRegistry(addReq);

        assertNotNull(result);
        assertEquals("12345", result.get("certId"));
    }

    @Test
    void testAddCertToRegistry_failureDueToNonOKResponse() throws Exception {
        Map<String, Object> addReq = new HashMap<>();
        when(propertiesCache.getProperty(JsonKeys.CERT_REGISTRY_BASE_PATH)).thenReturn("http://example.com/");
        when(propertiesCache.getProperty(JsonKeys.ADD_CERT_REG_API)).thenReturn("add");

        httpUtilMockedStatic.when(() ->
                        HttpUtil.sendPostRequest(anyString(), anyString(), anyMap()))
                .thenReturn("{\"responseCode\":\"ERROR\"}");

        Map<String, Object> result = helper.addCertToRegistry(addReq);
        assertNull(result);
    }

    @Test
    void testPostApiCall_success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("data", "value");

        String jsonResponse = "{\"key\":\"value\"}";
        httpUtilMockedStatic.when(() ->
                        HttpUtil.sendPostRequest(anyString(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

        Map<String, Object> response = helper.postAPICall("http://example.com", request);

        assertNotNull(response);
        assertEquals("value", response.get("key"));
    }

    @Test
    void testPostApiCall_failure() {
        Map<String, Object> request = new HashMap<>();

        httpUtilMockedStatic.when(() ->
                        HttpUtil.sendPostRequest(anyString(), anyString(), anyMap()))
                .thenReturn("{}"); // empty map

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            helper.postAPICall("http://example.com", request);
        });
        assertTrue(ex.getMessage().contains("Error from get API"));
    }

    @Test
    void testGetCertificateRegistryUsingIdentifier_success() throws BaseException {
        Map<String, Object> record = new HashMap<>();
        record.put("certId", "abc");

        List<Map<String, Object>> recordList = Collections.singletonList(record);
        Response response = new Response();
        response.put(JsonKeys.RESPONSE, recordList);

        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), isNull()))
                .thenReturn(response);

        Map<String, Object> result = helper.getCertificateRegistryUsingIdentifier("cert123");
        assertNotNull(result);
        assertEquals("abc", result.get("certId"));
    }

    @Test
    void testGetCertificateRegistryUsingIdentifier_emptyRecord() throws BaseException {
        Response response = new Response();
        response.put(JsonKeys.RESPONSE, new ArrayList<>());

        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), isNull()))
                .thenReturn(response);

        Map<String, Object> result = helper.getCertificateRegistryUsingIdentifier("cert123");
        assertNull(result);
    }

    @Test
    void testSingletonInstance() {
        CertRegistryHelper instance1 = CertRegistryHelper.getInstance();
        CertRegistryHelper instance2 = CertRegistryHelper.getInstance();
        assertSame(instance1, instance2);
    }
}
