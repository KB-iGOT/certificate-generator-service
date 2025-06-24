package org.sunbird.cert.helper;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.BaseException;
import org.sunbird.HttpUtil;
import org.sunbird.PropertiesCache;
import org.sunbird.cache.util.RedisCacheUtil;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cert.Models.AssessmentUserAttempt;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.response.Response;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IssueCertificateContentHelperTest {

    private static CassandraOperation cassandraOperation;
    private static RedisCacheUtil contentCache;
    private static PropertiesCache propertiesCache;
    private static MockedStatic<ServiceFactory> serviceFactoryMockedStatic;
    private static MockedStatic<PropertiesCache> propertiesCacheMockedStatic;
    private static MockedStatic<HttpUtil> httpUtilMockedStatic;
    private static IssueCertificateContentHelper helper;

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

        helper = IssueCertificateContentHelper.getInstance();
    }

    @BeforeEach
    void resetMocksAndStaticContentCache() {
        reset(cassandraOperation, contentCache, propertiesCache);
        setFinalStatic(IssueCertificateContentHelper.class, "contentCache", contentCache);
    }

    @AfterAll
    static void cleanup() {
        serviceFactoryMockedStatic.close();
        propertiesCacheMockedStatic.close();
        httpUtilMockedStatic.close();
    }



    @Test
    void getAPICall_returnsData_whenApiReturnsValidJson() throws Exception {
        String url = "http://test.com";
        String json = "{\"key\":\"value\"}";
        httpUtilMockedStatic.when(() -> HttpUtil.sendGetRequest(anyString(), anyMap())).thenReturn(json);

        Map<String, Object> result = helper.getAPICall(url);
        assertEquals("value", result.get("key"));
    }

    @Test
    void getAPICall_throwsException_whenApiReturnsEmptyJson() {
        String url = "http://test.com";
        httpUtilMockedStatic.when(() -> HttpUtil.sendGetRequest(anyString(), anyMap())).thenReturn("{}");
        assertThrows(RuntimeException.class, () -> helper.getAPICall(url));
    }

    @Test
    void getCourseInfo_returnsCourseInfo_whenCacheHit() throws Exception {
        String courseId = "cid";
        String courseMeta = "{\"name\":\"Course Name\",\"primaryCategory\":\"Category\",\"parentCollections\":[],\"posterImage\":\"img\",\"organisation\":[\"org\"]}";
        when(contentCache.get(eq(courseId), any(), anyInt())).thenReturn(courseMeta);

        Map<String, Object> result = helper.getCourseInfo(courseId);
        assertEquals("Course Name", result.get("courseName"));
    }

    @Test
    void getCourseInfo_returnsCourseInfo_whenCacheMiss() throws Exception {
        String courseId = "cid";
        when(contentCache.get(eq(courseId), any(), anyInt())).thenReturn(null);

        when(propertiesCache.getProperty("content_basePath")).thenReturn("http://content/");
        when(propertiesCache.getProperty("content_read_api")).thenReturn("read");

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> courseDetails = new HashMap<>();
        courseDetails.put("name", "Course Name");
        courseDetails.put("primaryCategory", "Category");
        courseDetails.put("parentCollections", new ArrayList<>());
        courseDetails.put("posterImage", "img");
        courseDetails.put("organisation", Arrays.asList("org"));
        result.put("content", courseDetails);
        response.put("result", result);

        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);
        httpUtilMockedStatic.when(() -> HttpUtil.sendGetRequest(anyString(), anyMap())).thenReturn(json);

        Map<String, Object> courseInfo = helper.getCourseInfo(courseId);
        assertNotNull(courseInfo);
        assertEquals("Course Name", courseInfo.get("courseName"));
    }


    @Test
    void isUserEligibleForContentCertificate_returnsTrue_whenEligible() throws BaseException {
        Map<String, Object> map = new HashMap<>();
        map.put("active", true);
        map.put("status", 2);
        map.put("completedon", new Date());
        List<Map<String, Object>> list = Collections.singletonList(map);
        Response response = new Response();
        response.put("response", list);

        assertTrue(helper.isUserEligibleForContentCertificate(response));
    }

    @Test
    void isUserEligibleForContentCertificate_returnsFalse_whenNotEligible() throws BaseException {
        Map<String, Object> map = new HashMap<>();
        map.put("active", false);
        List<Map<String, Object>> list = Collections.singletonList(map);
        Response response = new Response();
        response.put("response", list);

        assertFalse(helper.isUserEligibleForContentCertificate(response));
        assertFalse(helper.isUserEligibleForContentCertificate(null));
    }

    @Test
    void generateCertificateMap_returnsCertificateMap_whenUserMatchesCriteria() throws Exception {

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("courseId", "courseId");
        requestMap.put("userId", "userId");
        requestMap.put("batchId", "batchId");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "CertName");
        template.put("url", "templateUrl");
        template.put("identifier", "templateId");
        template.put("criteria", "{\"enrollment\":{\"status\":2},\"users\":{}}");
        template.put("additionalProps", "{}");
        template.put("issuer", "{}");
        template.put("signatoryList", "[]");
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
        userResult.put("response", userDetails);
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("result", userResult);
        httpUtilMockedStatic.when(() -> HttpUtil.sendGetRequest(contains("userId"), anyMap()))
                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(userResponse));

        when(contentCache.get(eq("courseId"), any(), anyInt()))
                .thenReturn("{\"name\":\"Course Name\",\"primaryCategory\":\"Category\",\"parentCollections\":[],\"posterImage\":\"img\",\"organisation\":[\"org\"]}");



        Map<String, Object> result = helper.generateCertificateMap(requestMap, template);
        assertNotNull(result);
        assertEquals("CertName", result.get("name"));
        assertEquals("John Doe", ((List<Map<String, Object>>) result.get("data")).get(0).get("recipientName"));
    }

    @Test
    void generateCertificateMap_returnsNull_whenUserDoesNotMatchCriteria() throws Exception {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("courseId", "courseId");
        requestMap.put("userId", "userId");
        requestMap.put("batchId", "batchId");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "CertName");
        template.put("url", "templateUrl");
        template.put("identifier", "templateId");
        template.put("criteria", "{\"enrollment\":{\"status\":2},\"users\":{\"role\":\"admin\"}}");
        template.put("additionalProps", "{}");
        template.put("issuer", "{}");
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
        userResult.put("response", userDetails);
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("result", userResult);
        httpUtilMockedStatic.when(() -> HttpUtil.sendGetRequest(contains("userId"), anyMap()))
                .thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(userResponse));

        when(contentCache.get(eq("courseId"), any(), anyInt()))
                .thenReturn("{\"name\":\"Course Name\",\"primaryCategory\":\"Category\",\"parentCollections\":[],\"posterImage\":\"img\",\"organisation\":[\"org\"]}");

        setFinalStatic(IssueCertificateContentHelper.class, "contentCache", contentCache);

        Map<String, Object> result = helper.generateCertificateMap(requestMap, template);
        assertNull(result);
    }

    @Test
    void generateCertificateMap_returnsNull_whenTemplateIsInvalid() {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("courseId", "courseId");
        requestMap.put("userId", "userId");
        requestMap.put("batchId", "batchId");

        Map<String, Object> template = new HashMap<>();
        template.put("criteria", "{}"); // Invalid, missing required keys

        Map<String, Object> result = helper.generateCertificateMap(requestMap, template);
        assertNull(result);
    }

    @Test
    void generateCertificateMap_returnsNull_whenExceptionIsThrown() throws Exception {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("courseId", "courseId");
        requestMap.put("userId", "userId");
        requestMap.put("batchId", "batchId");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "CertName");
        template.put("url", "templateUrl");
        template.put("identifier", "templateId");
        template.put("criteria", "{\"enrollment\":{\"status\":2},\"users\":{}}");
        template.put("additionalProps", "{}");
        template.put("issuer", "{}");
        template.put("signatoryList", "[]");

        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("DB error"));

        Map<String, Object> result = helper.generateCertificateMap(requestMap, template);
        assertNull(result);
    }

    private final Map<String, Object> requestMap = Map.of(
            "courseId", "course123",
            "userId", "user123",
            "batchId", "batch123"
    );

//    @Test
//    void test_getMaxScore_validCase() throws Exception {
//        Map<String, Object> dbRow = new HashMap<>();
//        dbRow.put("aggregates", Map.of("score:Q1", 9.0, "max_score:Q1", 10.0));
//
//        List<Map<String, Object>> responseList = List.of(dbRow);
//        Map<String, Integer> aggMap = Map.of("score:Q1", 9);
//
//        Response response = new Response();
//        response.put("response", responseList);
//        response.put("agg", aggMap);
//
//        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList())).thenReturn(response);
//        when(contentCache.get(eq("Q1"), isNull(), anyInt())).thenReturn("{\"contenttype\":\"SelfAssess\"}");
//
//        Map<String, List<AssessmentUserAttempt>> result = IssueCertificateContentHelper.getMaxScore(requestMap);
//
//        assertNotNull(result);
//    }

//    @Test
//    void test_getMaxScore_nullResponse() throws Exception {
//        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList())).thenReturn(null);
//
//        Map<String, List<AssessmentUserAttempt>> result = IssueCertificateContentHelper.getMaxScore(requestMap);
//        assertTrue(result.isEmpty());
//    }

//    @Test
//    void test_getMaxScore_nullCacheValue() throws Exception {
//        Map<String, Object> dbRow = new HashMap<>();
//        dbRow.put("aggregates", Map.of("score:Q1", 9.0, "max_score:Q1", 10.0));
//        List<Map<String, Object>> responseList = List.of(dbRow);
//
//        Response response = new Response();
//        response.put("response", responseList);
//        response.put("agg", Map.of("score:Q1", 9));
//
//        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList())).thenReturn(response);
//        when(contentCache.get(eq("Q1"), isNull(), anyInt())).thenReturn(null);
//
//        Map<String, List<AssessmentUserAttempt>> result = IssueCertificateContentHelper.getMaxScore(requestMap);
//        assertTrue(result.isEmpty());
//    }

    @Test
    void test_getMaxScore_invalidJsonInCache() throws Exception {
        Map<String, Object> dbRow = new HashMap<>();
        dbRow.put("aggregates", Map.of("score:Q1", 9.0, "max_score:Q1", 10.0));
        List<Map<String, Object>> responseList = List.of(dbRow);

        Response response = new Response();
        response.put("response", responseList);
        response.put("agg", Map.of("score:Q1", 9));

        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList())).thenReturn(response);
        when(contentCache.get(eq("Q1"), isNull(), anyInt())).thenReturn("invalid-json");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            IssueCertificateContentHelper.getMaxScore(requestMap);
        });

        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void test_getMaxScore_emptyAggregates() throws Exception {
        Map<String, Object> dbRow = new HashMap<>();
        dbRow.put("aggregates", new HashMap<>());
        List<Map<String, Object>> responseList = List.of(dbRow);

        Response response = new Response();
        response.put("response", responseList);
        response.put("agg", new HashMap<>());

        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList())).thenReturn(response);

        Map<String, List<AssessmentUserAttempt>> result = IssueCertificateContentHelper.getMaxScore(requestMap);
        assertTrue(result.isEmpty());
    }


    @Test
    void test_isValidAssessCriteria_equalMatch_directScore() {
        Map<String, Object> criteria = Map.of("score", 85);
        boolean result = IssueCertificateContentHelper.isValidAssessCriteria(criteria, 85.0);
        assertTrue(result);
    }

    @Test
    void test_isValidAssessCriteria_EQ_operation() {
        Map<String, Object> scoreMap = Map.of("EQ", 90);
        Map<String, Object> criteria = Map.of("score", scoreMap);
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 90.0));
    }

    @Test
    void test_isValidAssessCriteria_eq_operation_lowercase() {
        Map<String, Object> scoreMap = Map.of("eq", 70);
        Map<String, Object> criteria = Map.of("score", scoreMap);
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 70.0));
    }

    @Test
    void test_isValidAssessCriteria_equal_operation_with_equalSign() {
        Map<String, Object> scoreMap = Map.of("=", 50);
        Map<String, Object> criteria = Map.of("score", scoreMap);
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 50.0));
    }

    @Test
    void test_isValidAssessCriteria_greaterThan_operation() {
        Map<String, Object> scoreMap = Map.of(">", 60);
        Map<String, Object> criteria = Map.of("score", scoreMap);
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 75.0));
    }

    @Test
    void test_isValidAssessCriteria_lessThan_operation() {
        Map<String, Object> scoreMap = Map.of("<", 80);
        Map<String, Object> criteria = Map.of("score", scoreMap);
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 60.0));
    }

    @Test
    void test_isValidAssessCriteria_greaterThanOrEqual_operation() {
        Map<String, Object> scoreMap = Map.of(">=", 85);
        Map<String, Object> criteria = Map.of("score", scoreMap);
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 85.0));
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 90.0));
    }

    @Test
    void test_isValidAssessCriteria_lessThanOrEqual_operation() {
        Map<String, Object> scoreMap = Map.of("<=", 75);
        Map<String, Object> criteria = Map.of("score", scoreMap);
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 75.0));
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 70.0));
    }

    @Test
    void test_isValidAssessCriteria_notEqual_operation_ne() {
        Map<String, Object> scoreMap = Map.of("ne", 45);
        Map<String, Object> criteria = Map.of("score", scoreMap);
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 60.0));
    }

    @Test
    void test_isValidAssessCriteria_notEqual_operation_exclamationMark() {
        Map<String, Object> scoreMap = Map.of("!=", 55);
        Map<String, Object> criteria = Map.of("score", scoreMap);
        assertTrue(IssueCertificateContentHelper.isValidAssessCriteria(criteria, 70.0));
    }


    // Helper for static field injection
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

}