package org.sunbird.cert.helper;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.PropertiesCache;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.response.Response;
import org.sunbird.HttpUtil;
import org.sunbird.cache.util.RedisCacheUtil;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IssueCertificateEventHelperTest {

    private static CassandraOperation cassandraOperation;
    private static RedisCacheUtil contentCache;
    private static PropertiesCache propertiesCache;
    private static MockedStatic<ServiceFactory> serviceFactoryMockedStatic;
    private static MockedStatic<PropertiesCache> propertiesCacheMockedStatic;
    private static MockedStatic<HttpUtil> httpUtilMockedStatic;
    private static IssueCertificateEventHelper helper;

    @BeforeAll
    static void setup() {
        cassandraOperation = mock(CassandraOperation.class);
        contentCache = mock(RedisCacheUtil.class);
        propertiesCache = mock(PropertiesCache.class);

        serviceFactoryMockedStatic = mockStatic(ServiceFactory.class);
        serviceFactoryMockedStatic.when(ServiceFactory::getInstance).thenReturn(cassandraOperation);

        propertiesCacheMockedStatic = mockStatic(PropertiesCache.class);
        propertiesCacheMockedStatic.when(PropertiesCache::getInstance).thenReturn(propertiesCache);

        httpUtilMockedStatic = mockStatic(HttpUtil.class);

        helper = IssueCertificateEventHelper.getInstance();
    }

    @AfterAll
    static void cleanup() {
        serviceFactoryMockedStatic.close();
        propertiesCacheMockedStatic.close();
        httpUtilMockedStatic.close();
    }

    @BeforeEach
    void resetMocksAndStaticContentCache() {
        reset(cassandraOperation, contentCache, propertiesCache);
    }


    public static void setFinalStatic(Class<?> clazz, String fieldName, Object newValue) {
        try {
            // Step 1: Get the field and make it accessible
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            // Step 2: Get Unsafe instance
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);

            // Step 3: Get offset and override static final field
            Object staticFieldBase = unsafe.staticFieldBase(field);
            long staticFieldOffset = unsafe.staticFieldOffset(field);

            unsafe.putObject(staticFieldBase, staticFieldOffset, newValue);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set final static field", e);
        }
    }

    @Test
    void testSingletonInstance() {
        IssueCertificateEventHelper instance1 = IssueCertificateEventHelper.getInstance();
        IssueCertificateEventHelper instance2 = IssueCertificateEventHelper.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testIsUserEligibleForEventCertificate_eligible() throws BaseException {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKeys.ACTIVE, true);
        map.put(JsonKeys.STATUS, 2);
        map.put(JsonKeys.COMPLETED_ON, new Date());
        List<Map<String, Object>> list = Collections.singletonList(map);
        Response response = new Response();
        response.put(JsonKeys.RESPONSE, list);

        assertTrue(helper.isUserEligibleForEventCertificate(response));
    }

    @Test
    void testIsUserEligibleForEventCertificate_notEligible() throws BaseException {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKeys.ACTIVE, false);
        List<Map<String, Object>> list = Collections.singletonList(map);
        Response response = new Response();
        response.put(JsonKeys.RESPONSE, list);

        assertFalse(helper.isUserEligibleForEventCertificate(response));
        assertFalse(helper.isUserEligibleForEventCertificate(null));
    }

    @Test
    void testGetUserCertificates_found() throws BaseException {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKeys.ACTIVE, true);
        List<Map<String, Object>> certs = new ArrayList<>();
        map.put(JsonKeys.ISSUED_CERTIFICATES, certs);
        List<Map<String, Object>> list = Collections.singletonList(map);
        Response response = new Response();
        response.put(JsonKeys.RESPONSE, list);

        assertNotNull(helper.getUserCertificates(response));
    }

    @Test
    void testGetUserCertificates_notFound() throws BaseException {
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKeys.ACTIVE, false);
        List<Map<String, Object>> list = Collections.singletonList(map);
        Response response = new Response();
        response.put(JsonKeys.RESPONSE, list);

        assertNull(helper.getUserCertificates(response));
        assertNull(helper.getUserCertificates(null));
    }

    @Test
    void testSanitizeString() {
        String input = "test\u0000string";
        String result = invokeSanitizeString(input);
        assertEquals("teststring", result);
    }

    @Test
    void testExtractProviderName() {
        List<Object> orgData = Collections.singletonList("provider");
        String result = invokeExtractProviderName(orgData);
        assertEquals("provider", result);

        assertEquals("", invokeExtractProviderName(Collections.emptyList()));
    }

    // Helper methods to invoke private static methods via reflection
    private String invokeSanitizeString(String input) {
        try {
            var method = IssueCertificateEventHelper.class.getDeclaredMethod("sanitizeString", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeExtractProviderName(List<Object> orgData) {
        try {
            var method = IssueCertificateEventHelper.class.getDeclaredMethod("extractProviderName", List.class);
            method.setAccessible(true);
            return (String) method.invoke(null, orgData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetAPICall_success() throws Exception {
        String url = "http://example.com";
        String json = "{\"key\":\"value\"}";
        httpUtilMockedStatic.when(() -> HttpUtil.sendGetRequest(anyString(), anyMap())).thenReturn(json);

        Map<String, Object> result = IssueCertificateEventHelper.getAPICall(url);
        assertEquals("value", result.get("key"));
    }

    @Test
    void testGetAPICall_failure() {
        String url = "http://example.com";
        httpUtilMockedStatic.when(() -> HttpUtil.sendGetRequest(anyString(), anyMap())).thenReturn("{}");
        assertThrows(RuntimeException.class, () -> IssueCertificateEventHelper.getAPICall(url));
    }

    @Test
    void testGetCourseInfo_withCache1() throws Exception {
        // Simulate cache hit
        String courseId = "course1";
        String courseMeta = "{\"name\":\"Course Name\",\"primaryCategory\":\"Category\",\"parentCollections\":[],\"posterImage\":\"img\",\"organisation\":[\"org\"]}";

        // Use setFinalStatic helper method instead of direct reflection
        setFinalStatic(IssueCertificateEventHelper.class, "contentCache", contentCache);

        when(contentCache.get(eq(courseId), any(), anyInt())).thenReturn(courseMeta);

        Map<String, Object> result = IssueCertificateEventHelper.getCourseInfo(courseId);
        assertEquals("Course Name", result.get("courseName"));
    }


    @Test
    void testGetCourseInfo_withoutCache() throws Exception {
        String courseId = "course2";
        when(contentCache.get(eq(courseId), any(), anyInt())).thenReturn(null);

        when(propertiesCache.getProperty("content_basePath")).thenReturn("http://content/");
        when(propertiesCache.getProperty("content_read_api")).thenReturn("read");

        String json = "{\"result\":{\"response\":{\"name\":\"Course Name\",\"primaryCategory\":\"Category\",\"parentCollections\":[],\"posterImage\":\"img\",\"organisation\":[\"org\"]}}}";
        httpUtilMockedStatic.when(() -> HttpUtil.sendGetRequest(anyString(), anyMap())).thenReturn(json);

        RedisCacheUtil mockedRedis = mock(RedisCacheUtil.class);

        // Inject mock
        setFinalStatic(IssueCertificateEventHelper.class, "contentCache", contentCache);

        Map<String, Object> result = IssueCertificateEventHelper.getCourseInfo(courseId);
        assertEquals("Course Name", result.get("courseName"));
    }

//    @Test
//    void generateCertificateMap_returnsCertificateMap_whenUserMatchesCriteria() throws Exception {
//        Map<String, Object> requestMap = new HashMap<>();
//        requestMap.put(JsonKeys.COURSE_ID, "courseId");
//        requestMap.put(JsonKeys.USER_ID, "userId");
//        requestMap.put(JsonKeys.BATCH_ID, "batchId");
//
//        Map<String, Object> template = new HashMap<>();
//        template.put("name", "CertName");
//        template.put("url", "templateUrl");
//        template.put("identifier", "templateId");
//        template.put("criteria", "{\"enrollment\":{\"status\":2},\"users\":{}}");
//        template.put("additionalProps", "{}");
//        template.put(JsonKeys.ISSUER, "{}");
//        template.put(JsonKeys.SIGNATORY_LIST, "signatoryList");
//        template.put("signatoryList", "[]");
//
//        Map<String, Object> enrolMap = new HashMap<>();
//        enrolMap.put("active", true);
//        enrolMap.put("issued_certificates", new ArrayList<>());
//        enrolMap.put("status", 2);
//        enrolMap.put("completedon", new Date());
//        Response cassandraResponse = new Response();
//        cassandraResponse.put("response", Collections.singletonList(enrolMap));
//        when(cassandraOperation.getRecordsByProperties(eq(JsonKeys.COURSE_KEY_SPACE_NAME), eq(JsonKeys.USER_ENTITY_ENROLMENTS), anyMap())).thenReturn(cassandraResponse);
//
//        when(propertiesCache.getProperty("learner_basePath")).thenReturn("http://learner/");
//        when(propertiesCache.getProperty("user_read_api")).thenReturn("user/v1/read");
//        when(propertiesCache.getProperty("cert_domain_url")).thenReturn("http://certs");
//
//        Map<String, Object> userDetails = new HashMap<>();
//        userDetails.put("firstName", "John");
//        userDetails.put("lastName", "Doe");
//        userDetails.put("rootOrgId", "orgId");
//        Map<String, Object> userResult = new HashMap<>();
//        userResult.put(JsonKeys.RESPONSE, userDetails);
////        when(cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_ENTITY_ENROLMENTS, anyMap())).thenReturn(cassandraResponse);
//
//        when(propertiesCache.getProperty("learner_basePath")).thenReturn("http://learner/");
//        when(propertiesCache.getProperty("user_read_api")).thenReturn("user/v1/read");
//        when(propertiesCache.getProperty("cert_domain_url")).thenReturn("http://certs");
//
//        Map<String, Object> userResponse = new HashMap<>();
//        userResponse.put(JsonKeys.RESULT, userResult);
//        httpUtilMockedStatic.when(() -> HttpUtil.sendGetRequest(contains("userId"), anyMap()))
//                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(userResponse));
//
//        when(contentCache.get(eq("courseId"), any(), anyInt()))
//                .thenReturn("{\"name\":\"Course Name\",\"primaryCategory\":\"Category\",\"parentCollections\":[],\"posterImage\":\"img\",\"organisation\":[\"org\"]}");
//
//        setFinalStatic(IssueCertificateEventHelper.class, "contentCache", contentCache);
//
//        Map<String, Object> result = IssueCertificateEventHelper.generateCertificateMap(requestMap, template);
//        assertNotNull(result);
//        assertEquals("CertName", result.get("name"));
//        assertEquals("John Doe", ((List<Map<String, Object>>) result.get("data")).get(0).get("recipientName"));
//    }

    @Test
    void generateCertificateMap_returnsNull_whenUserDoesNotMatchCriteria() throws Exception {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKeys.COURSE_ID, "courseId");
        requestMap.put(JsonKeys.USER_ID, "userId");
        requestMap.put(JsonKeys.BATCH_ID, "batchId");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "CertName");
        template.put("url", "templateUrl");
        template.put("identifier", "templateId");
        template.put("criteria", "{\"enrollment\":{\"status\":2},\"users\":{\"role\":\"admin\"}}");
        template.put("additionalProps", "{}");
        template.put(JsonKeys.ISSUER, "{}");
        template.put(JsonKeys.SIGNATORY_LIST, "signatoryList");
        template.put("signatoryList", "[]");

        Map<String, Object> enrolMap = new HashMap<>();
        enrolMap.put("active", true);
        enrolMap.put("issued_certificates", new ArrayList<>());
        enrolMap.put("status", 2);
        enrolMap.put("completedon", new Date());
        Response cassandraResponse = new Response();
        cassandraResponse.put("response", Collections.singletonList(enrolMap));
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap())).thenReturn(cassandraResponse);

        when(propertiesCache.getProperty("learner_basePath")).thenReturn("http://learner/");
        when(propertiesCache.getProperty("user_read_api")).thenReturn("user/v1/read");
        when(propertiesCache.getProperty("cert_domain_url")).thenReturn("http://certs");

        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("firstName", "John");
        userDetails.put("lastName", "Doe");
        userDetails.put("rootOrgId", "orgId");
        Map<String, Object> userResult = new HashMap<>();
        userResult.put(JsonKeys.RESPONSE, userDetails);
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put(JsonKeys.RESULT, userResult);
        httpUtilMockedStatic.when(() -> HttpUtil.sendGetRequest(contains("userId"), anyMap()))
                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(userResponse));

        when(contentCache.get(eq("courseId"), any(), anyInt()))
                .thenReturn("{\"name\":\"Course Name\",\"primaryCategory\":\"Category\",\"parentCollections\":[],\"posterImage\":\"img\",\"organisation\":[\"org\"]}");

        setFinalStatic(IssueCertificateEventHelper.class, "contentCache", contentCache);

        Map<String, Object> result = IssueCertificateEventHelper.generateCertificateMap(requestMap, template);
        assertNull(result);
    }

    @Test
    void generateCertificateMap_returnsNull_whenTemplateIsInvalid() {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKeys.COURSE_ID, "courseId");
        requestMap.put(JsonKeys.USER_ID, "userId");
        requestMap.put(JsonKeys.BATCH_ID, "batchId");

        Map<String, Object> template = new HashMap<>();
        template.put("criteria", "{}"); // Invalid, missing required keys

        Map<String, Object> result = IssueCertificateEventHelper.generateCertificateMap(requestMap, template);
        assertNull(result);
    }

    @Test
    void generateCertificateMap_returnsNull_whenExceptionIsThrown() throws Exception {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKeys.COURSE_ID, "courseId");
        requestMap.put(JsonKeys.USER_ID, "userId");
        requestMap.put(JsonKeys.BATCH_ID, "batchId");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "CertName");
        template.put("url", "templateUrl");
        template.put("identifier", "templateId");
        template.put("criteria", "{\"enrollment\":{\"status\":2},\"users\":{}}");
        template.put("additionalProps", "{}");
        template.put(JsonKeys.ISSUER, "{}");
        template.put(JsonKeys.SIGNATORY_LIST, "signatoryList");
        template.put("signatoryList", "[]");

        // Simulate exception in validateEnrolmentCriteria
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("DB error"));

        Map<String, Object> result = IssueCertificateEventHelper.generateCertificateMap(requestMap, template);
        assertNull(result);
    }
}