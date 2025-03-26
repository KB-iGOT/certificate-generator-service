package org.sunbird.cert.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.PropertiesCache;
import org.sunbird.cache.util.RedisCacheUtil;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.HttpUtil;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.response.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class IssueCertificateEventHelper {
    private static final Logger logger = LoggerFactory.getLogger(IssueCertificateEventHelper.class);
    private static final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private static final RedisCacheUtil contentCache = new RedisCacheUtil();
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private static IssueCertificateEventHelper instance = null;

    public static synchronized IssueCertificateEventHelper getInstance() {
        if (instance == null) {
            instance = new IssueCertificateEventHelper();
        }
        return instance;
    }

    private IssueCertificateEventHelper() {}

    public static Map<String, Object> generateCertificateMap(Map<String, Object> requestMap, Map<String, Object> template) {

        try {
            logger.info("issueCertificate i/p event =>" + requestMap);

            String courseId = (String) requestMap.get(JsonKeys.COURSE_ID);
            String userId = (String) requestMap.get(JsonKeys.USER_ID);
            String batchId = (String) requestMap.get(JsonKeys.BATCH_ID);

            Map<String, Object> criteria = validateTemplate(template, batchId);
            String certName = (String)template.getOrDefault("name", "");
            logger.info("CertName: " + certName);

            Map<String, List<String>> additionalProps = mapper.readValue(
                    (String)template.getOrDefault("additionalProps", "{}"), Map.class);

            Map<String, Object> enrolledUser = validateEnrolmentCriteria(
                    requestMap,
                    (Map<String, Object>) criteria.getOrDefault(JsonKeys.ENROLLMENT, new HashMap<>()),
                    certName,
                    additionalProps
            );
            logger.debug("enrolledUser: " + enrolledUser);

            Map<String, Object> userDetails = validateUser(
                    userId,
                    (Map<String, Object>) criteria.getOrDefault(JsonKeys.USERS, new HashMap<>()),
                    additionalProps
            );
            logger.debug("userDetails: " + userDetails);

            if (!userDetails.isEmpty()) {
                return generateCertificateEvent(
                        requestMap,
                        template,
                        userDetails,
                        certName,
                        enrolledUser
                );
            } else {
                logger.error(String.format("User :: %s did not match the criteria for batch :: %s and course :: %s",
                        userId, batchId, courseId));
                return null;
            }
        } catch (Exception e) {
            logger.error("Issue while validating the user Enrollment.");
        }
        return null;
    }

    // Placeholder methods for dependent logic
    private static Map<String, Object> validateTemplate(Map<String, Object> template, String batchId) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> criteria = objectMapper.readValue((String)template.getOrDefault("criteria", "{}"), Map.class);

        if (StringUtils.isNotBlank((String)template.getOrDefault("url", "")) && !criteria.isEmpty() &&
                !Collections.disjoint(criteria.keySet(), Arrays.asList(JsonKeys.ENROLLMENT, JsonKeys.ASSESSMENT, JsonKeys.USERS))) {
            return criteria;
        } else {
            throw new Exception("Invalid template for batch: " + batchId);
        }
    }

    private static Map<String, Object> validateEnrolmentCriteria(Map<String, Object> requestMap, Map<String, Object> enrollmentCriteria, String certName, Map<String, List<String>> additionalProps) throws BaseException {
        Boolean reIssue = false;
        if (MapUtils.isNotEmpty(enrollmentCriteria)) {
            Map<String, Object> primaryKey = new HashMap<>();
            String courseId = (String) requestMap.get(JsonKeys.COURSE_ID);
            String userId = (String) requestMap.get(JsonKeys.USER_ID);
            String batchId = (String) requestMap.get(JsonKeys.BATCH_ID);
            primaryKey.put(JsonKeys.USER_ID, userId);
            primaryKey.put(JsonKeys.CONTENT_ID, courseId);
            primaryKey.put(JsonKeys.CONTEXT_ID, courseId);
            primaryKey.put(JsonKeys.BATCH_ID, batchId);
            Response row = cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_ENTITY_ENROLMENTS, primaryKey);
            if (row != null) {
                List<Map<String, Object>> mapList = (List<Map<String, Object>>) row.get("response");
                if (CollectionUtils.isNotEmpty(mapList)) {
                    Map<String, Object> map = mapList.get(0);

                    boolean active = (boolean) map.getOrDefault("active", false);
                    List<Map<String, String>> issuedCertificates = (List<Map<String, String>>) map.getOrDefault("issued_certificates", new ArrayList<>());

                    boolean isCertIssued = !issuedCertificates.isEmpty() &&
                            issuedCertificates.stream()
                                    .anyMatch(cert -> certName.equalsIgnoreCase(cert.getOrDefault("name", "")));

                    int status = (int) map.getOrDefault("status", 0);
                    int criteriaStatus = (int) enrollmentCriteria.getOrDefault("status", 2);

                    String oldId = (isCertIssued && reIssue) ?
                            issuedCertificates.stream()
                                    .filter(cert -> certName.equalsIgnoreCase(cert.getOrDefault("name", "")))
                                    .map(cert -> cert.getOrDefault("identifier", ""))
                                    .findFirst()
                                    .orElse("") : "";

                    Date issuedOn = (Date) map.get("completedon");

                    Map<String, Object> enrolledUserMap = new HashMap<>();
                    enrolledUserMap.put("user", userId);
                    enrolledUserMap.put("oldId", oldId);
                    enrolledUserMap.put("issuedOn", issuedOn);
                    if (!additionalProps.isEmpty()) {
                        enrolledUserMap.put("enrollment", additionalProps);
                    }
                    return enrolledUserMap;
                }
            }
        }
        return new HashMap<>();
    }

    private static Map<String, Object> validateUser(String userId, Map<String, Object> userCriteria, Map<String, List<String>> additionalProps) throws UnirestException, IOException {
        if (userId != null && !userId.isEmpty()) {
            String url = PropertiesCache.getInstance().getProperty("learner_basePath") + PropertiesCache.getInstance().getProperty("user_read_api") + "/" + userId
                    + "?organisations,roles,locations,declarations,externalIds";

            Map<String, Object> result = getAPICall(url);

            if (userCriteria.isEmpty() || userCriteria.entrySet().stream()
                    .allMatch(entry -> entry.getValue().equals(result.getOrDefault(entry.getKey(), null)))) {
                Map<String, Object> resultObject = (Map<String,Object>)result.get(JsonKeys.RESULT);
                Map<String, Object> responseObject = (Map<String,Object>)resultObject.get(JsonKeys.RESPONSE);
                return responseObject;
            } else {
                return Collections.emptyMap();
            }
        } else {
            return Collections.emptyMap();
        }
    }

    private static Map<String, Object> generateCertificateEvent(Map<String, Object> requestMap, Map<String, Object> template, Map<String, Object> userDetails, String certName, Map<String, Object> enrolledUser) throws UnirestException, IOException {
        String firstName = (String) userDetails.getOrDefault("firstName", "");
        String lastName = (String) userDetails.getOrDefault("lastName", "");
        String recipientName = StringUtils.isNotBlank(lastName) ? firstName + " " + lastName : firstName;;

        recipientName = recipientName.trim();

        Map<String, Object> courseInfo = getCourseInfo((String)requestMap.get(JsonKeys.COURSE_ID));
        String courseName = (String) courseInfo.getOrDefault("courseName", "");

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        List<String> parentCollections = Optional.ofNullable((List<String>) courseInfo.get("parentCollections"))
                .orElse(Collections.emptyList());

        Map<String, Object> eData = new HashMap<>();
        eData.put("issuedDate", dateFormatter.format(enrolledUser.get(JsonKeys.ISSUED_ON)));
        String finalRecipientName = recipientName;
        eData.put("data", Collections.singletonList(
                new HashMap<String, Object>() {{
                    put("recipientName", finalRecipientName);
                    put("recipientId", requestMap.get(JsonKeys.USER_ID));
                }}
        ));
        eData.put("reIssueDate", "");
        eData.put("criteria", Collections.singletonMap("narrative", certName));
        eData.put("svgTemplate", template.getOrDefault("url", ""));
        eData.put("oldId", enrolledUser.get("oldId"));
        eData.put("templateId", template.getOrDefault("identifier", ""));
        eData.put("userId",  requestMap.get(JsonKeys.USER_ID));
        eData.put("orgId", userDetails.getOrDefault("rootOrgId", ""));
        eData.put("issuer", mapper.readValue((String)template.getOrDefault(JsonKeys.ISSUER, "{}"), Map.class));
        eData.put("signatoryList", mapper.readValue((String)template.getOrDefault(template.get(JsonKeys.SIGNATORY_LIST), "[]"), List.class));
        eData.put("courseName", courseName);
        eData.put("basePath", PropertiesCache.getInstance().getProperty("cert_domain_url") + "/certs");
        eData.put("name", certName);
        eData.put("providerName", courseInfo.getOrDefault("providerName", ""));
        eData.put("tag", requestMap.get(JsonKeys.BATCH_ID));
        eData.put("primaryCategory", courseInfo.getOrDefault("primaryCategory", ""));
        eData.put("parentCollections", parentCollections);
        eData.put("coursePosterImage", courseInfo.getOrDefault("coursePosterImage", ""));
        return eData;
    }

    public static Map<String, Object> getAPICall(
            String url) throws UnirestException, IOException {
        Map<String, String> defaultHeader = new HashMap<>();
        defaultHeader.put("Content-Type", "application/json");
        String response = HttpUtil.sendGetRequest(url, defaultHeader);
        Map<String, Object> data = mapper.readValue(response, Map.class);
        if (MapUtils.isNotEmpty(data)) {
            return data;
        } else {
            throw new RuntimeException("Error from get API: " + url + ", with response: " + response);
        }
    }

    public static Map<String, Object> getCourseInfo(
            String courseId) throws UnirestException, IOException {

        String courseMetadataString = contentCache.get(courseId, null, 0);
        if (StringUtils.isBlank(courseMetadataString)) {
            String url = PropertiesCache.getInstance().getProperty("content_basePath") + PropertiesCache.getInstance().getProperty("content_read_api") + "/" + courseId
                    + "?fields=name,parentCollections,primaryCategory,posterImage,organisation";

            Map<String, Object> responseObject = getAPICall(url);
            Map<String, Object> resultObject = (Map<String,Object>)responseObject.get(JsonKeys.RESULT);
            Map<String, Object> response = (Map<String,Object>)resultObject.get(JsonKeys.RESPONSE);
            String courseName = sanitizeString((String) response.getOrDefault("name", ""));
            String primaryCategory = sanitizeString((String) response.getOrDefault("primaryCategory", ""));
            String posterImage = sanitizeString((String) response.getOrDefault("posterImage",""));

            List<String> parentCollections = (List<String>) response.getOrDefault("parentCollections", new ArrayList<>());

            List<Object> orgData = (List<Object>) response.getOrDefault("organisation", Collections.emptyList());
            String providerName = extractProviderName(orgData);

            Map<String, Object> courseInfoMap = new HashMap<>();
            courseInfoMap.put("courseId", courseId);
            courseInfoMap.put("courseName", courseName);
            courseInfoMap.put("parentCollections", parentCollections);
            courseInfoMap.put("primaryCategory", primaryCategory);
            courseInfoMap.put("coursePosterImage", posterImage);
            courseInfoMap.put("providerName", providerName);

            return courseInfoMap;

        } else {
            Map<String, Object> courseMetadata = mapper.readValue(courseMetadataString, Map.class);
            String courseName = sanitizeString((String) courseMetadata.getOrDefault("name", ""));
            String primaryCategory = sanitizeString((String) courseMetadata.getOrDefault("primaryCategory", ""));
            List<String> parentCollections = (List<String>) courseMetadata.getOrDefault("parentCollections", new ArrayList<>());
            String posterImage = sanitizeString((String) courseMetadata.getOrDefault("posterImage", ""));

            List<Object> orgData = (List<Object>) courseMetadata.getOrDefault("organisation", Collections.emptyList());
            String providerName = extractProviderName(orgData);

            Map<String, Object> courseInfoMap = new HashMap<>();
            courseInfoMap.put("courseId", courseId);
            courseInfoMap.put("courseName", courseName);
            courseInfoMap.put("parentCollections", parentCollections);
            courseInfoMap.put("primaryCategory", primaryCategory);
            courseInfoMap.put("coursePosterImage", posterImage);
            courseInfoMap.put("providerName", providerName);

            return courseInfoMap;
        }
    }

    private static String sanitizeString(String input) {
        return Optional.ofNullable(input)
                .map(str -> str.chars()
                        .filter(c -> c >= ' ')
                        .mapToObj(c -> String.valueOf((char) c))
                        .collect(Collectors.joining()))
                .orElse("");
    }

    private static String extractProviderName(List<Object> orgData) {
        if (!orgData.isEmpty()) {
            String pm = orgData.get(0).toString();
            return pm;
        }
        return "";
    }

    public Response fetchEventTemplate(String eventId, String batchId) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.EVENT_ID, eventId);
        primaryKey.put(JsonKeys.BATCH_ID, batchId);
        return cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.EVENT_BATCH, primaryKey, Arrays.asList(JsonKeys.CERT_TEMPLATES));
    }

    public boolean isUserEligibleForEventCertificate(Response row) throws BaseException {
        if (row != null) {
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) row.get(JsonKeys.RESPONSE);
            if (CollectionUtils.isNotEmpty(mapList)) {
                Map<String, Object> map = mapList.stream().filter(m -> Boolean.TRUE.equals(m.get(JsonKeys.ACTIVE))).findFirst().orElse(null);
                if (MapUtils.isNotEmpty(map)) {
                    int status = (int) map.getOrDefault(JsonKeys.STATUS, 0);
                    Date competedOn = (Date) map.get(JsonKeys.COMPLETED_ON);
                    if (status == 2 && competedOn != null) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    public List<Map<String, Object>> getUserCertificates(Response row) throws BaseException {
        if (row != null) {
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) row.get(JsonKeys.RESPONSE);
            if (CollectionUtils.isNotEmpty(mapList)) {
                Map<String, Object> map = mapList.stream().filter(m -> Boolean.TRUE.equals(m.get(JsonKeys.ACTIVE))).findFirst().orElse(null);
                if (MapUtils.isNotEmpty(map) && map.get(JsonKeys.ISSUED_CERTIFICATES) != null) {
                   return (List<Map<String, Object>>)map.get(JsonKeys.ISSUED_CERTIFICATES);
                }
            }
        }
        return null;
    }
}

