package org.sunbird.cert.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseException;
import org.sunbird.HttpUtil;
import org.sunbird.JsonKeys;
import org.sunbird.PropertiesCache;
import org.sunbird.cache.platform.Platform;
import org.sunbird.cache.util.RedisCacheUtil;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.response.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class IssueCertificateExternalContentHelper {

    private static final Logger logger = LoggerFactory.getLogger(IssueCertificateExternalContentHelper.class);
    private static final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private static final RedisCacheUtil contentCache = new RedisCacheUtil();
    private static ObjectMapper mapper = new ObjectMapper();
    private static final int extnernalCourseNameMaximumLength = Platform.getInteger("external_course_max_length", 100);

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private static IssueCertificateExternalContentHelper instance = null;

    public static synchronized IssueCertificateExternalContentHelper getInstance() {
        if (instance == null) {
            instance = new IssueCertificateExternalContentHelper();
        }
        return instance;
    }

    private IssueCertificateExternalContentHelper() {}

    public Map<String, Object> generateCertificateMapForExternalContent(Map<String, Object> requestMap) {

        try {
            logger.info("issueCertificate i/p event for externalContent=>" + requestMap);

            String courseId = (String) requestMap.get(JsonKeys.COURSE_ID);
            String userId = (String) requestMap.get(JsonKeys.USER_ID);

            Map<String, Object> enrolledUser = validateEnrolmentCriteria(requestMap);
            logger.debug("enrolledUser: " + enrolledUser);

            Map<String, Object> userDetails = validateUser(userId, new HashMap<>(), null);
            logger.debug("userDetails: " + userDetails);


            if (!userDetails.isEmpty()) {
                return generateCertificateForExternalContent(
                        requestMap,
                        userDetails,
                        enrolledUser
                );
            } else {
                logger.error(String.format("User :: %s did not match the criteria for course external Content :: %s",
                        userId, courseId));
                return null;
            }
        } catch (Exception e) {
            logger.error("Issue while validating the user Enrollment.", e);
        }
        return null;
    }

    private static Map<String, Object> validateEnrolmentCriteria(Map<String, Object> requestMap) throws BaseException {
        if (MapUtils.isNotEmpty(requestMap)) {
            Map<String, Object> primaryKey = new HashMap<>();
            String courseId = (String) requestMap.get(JsonKeys.COURSE_ID);
            String userId = (String) requestMap.get(JsonKeys.USER_ID);
            primaryKey.put(JsonKeys.USER_ID, userId);
            primaryKey.put(JsonKeys.COURSE_ID, courseId);
            Response row = cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.TABLE_USER_EXTERNAL_ENROLMENTS, primaryKey);
            if (row != null) {
                List<Map<String, Object>> mapList = (List<Map<String, Object>>) row.get("response");
                if (CollectionUtils.isNotEmpty(mapList)) {
                    Map<String, Object> map = mapList.get(0);

                    Date issuedOn = (Date) map.get("completedon");

                    Map<String, Object> enrolledUserMap = new HashMap<>();
                    enrolledUserMap.put("user", userId);
                    enrolledUserMap.put("issuedOn", issuedOn);
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

    private static Map<String, Object> generateCertificateForExternalContent(Map<String, Object> requestMap, Map<String, Object> userDetails, Map<String, Object> enrolledUser) throws UnirestException, IOException {
        String firstName = (String) userDetails.getOrDefault("firstName", "");
        String lastName = (String) userDetails.getOrDefault("lastName", "");
        String recipientName = StringUtils.isNotBlank(lastName) ? firstName + " " + lastName : firstName;

        recipientName = recipientName.trim();

        Map<String, Object> courseInfo = getExternalCourseInfo((String)requestMap.get(JsonKeys.COURSE_ID));
        if (MapUtils.isNotEmpty(courseInfo)) {
            Map<String, Object> template = getExternalCoursePartnerInfo((String)courseInfo.get("contentPartnerId"));
            String courseName = (String) courseInfo.getOrDefault("courseName", "");

            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

            Map<String, Object> eData = new HashMap<>();
            eData.put("issuedDate", dateFormatter.format(enrolledUser.get(JsonKeys.ISSUED_ON)));
            String finalRecipientName = recipientName;
            eData.put("data", Collections.singletonList(
                    new HashMap<String, Object>() {{
                        put("recipientName", finalRecipientName);
                        put("recipientId", requestMap.get(JsonKeys.USER_ID));
                    }}
            ));
            String certName = (String)template.get("certificateName");
            eData.put("reIssueDate", "");
            eData.put("criteria", Collections.singletonMap("narrative", certName));
            eData.put("svgTemplate", template.getOrDefault("url", ""));
            eData.put("oldId", enrolledUser.get("oldId"));
            eData.put("userId",  requestMap.get(JsonKeys.USER_ID));
            eData.put("orgId", userDetails.getOrDefault("rootOrgId", ""));
            eData.put("issuer", mapper.readValue((String)template.getOrDefault(JsonKeys.ISSUER, "{}"), Map.class));
            eData.put("signatoryList", mapper.readValue((String)template.getOrDefault(template.get(JsonKeys.SIGNATORY_LIST), "[]"), List.class));
            if (StringUtils.isNotBlank(courseName) && courseName.length() > extnernalCourseNameMaximumLength) {
                String wrappedCourseName = WordUtils.wrap(courseName, extnernalCourseNameMaximumLength, "\n", false);
                String[] lines = wrappedCourseName.split("\n", 2);
                String courseNameLine = lines[0].trim();
                String courseNameExtended = lines.length > 1 ? lines[1].trim() : "";
                eData.put("courseName", courseNameLine);
                eData.put("courseNameExtended", courseNameExtended);
            } else {
                eData.put("courseName", courseName);
                eData.put("courseNameExtended", "\u200B");
            }
            logger.info("The edata is updated : " + eData.get("courseName") + " : testing" +eData.get("courseNameExtended"));
            eData.put("basePath", PropertiesCache.getInstance().getProperty("cert_domain_url") + "/certs");
            eData.put("name", certName);
            eData.put("providerName", courseInfo.getOrDefault("providerName", ""));
            eData.put("coursePosterImage", courseInfo.getOrDefault("coursePosterImage", ""));
            return eData;
        }
        return null;
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

    public static Map<String, Object> getExternalCourseInfo(
            String courseId) throws UnirestException, IOException {

        String courseMetadataString = contentCache.get(courseId, null, 0);
        if (StringUtils.isBlank(courseMetadataString)) {
            String url = PropertiesCache.getInstance().getProperty("cb_pores_basePath") + PropertiesCache.getInstance().getProperty("cios_content_read_api") + "/" + courseId;

            Map<String, Object> responseObject = getAPICall(url);
            Map<String, Object> resultObject = (Map<String,Object>)responseObject.get(JsonKeys.CONTENT);
            if (MapUtils.isNotEmpty(resultObject)) {
                String courseName = sanitizeString((String) resultObject.getOrDefault("name", ""));
                String posterImage = sanitizeString((String) resultObject.getOrDefault("appIcon",""));
                String contentPartnerName = "";
                String contentPartnerId = "";
                Map<String, Object> contentPartner = (Map<String, Object>) resultObject.getOrDefault("contentPartner", new HashMap<>());
                if (MapUtils.isNotEmpty(contentPartner)) {
                    contentPartnerName = (String)contentPartner.get("contentPartnerName");
                    contentPartnerId = (String)contentPartner.get("id");
                }

                Map<String, Object> courseInfoMap = new HashMap<>();
                courseInfoMap.put("courseId", courseId);
                courseInfoMap.put("courseName", courseName);
                courseInfoMap.put("coursePosterImage", posterImage);
                courseInfoMap.put("providerName", contentPartnerName);
                courseInfoMap.put("contentPartnerId", contentPartnerId);

                return courseInfoMap;
            } else {
                return new HashMap<>();
            }
        } else {
            Map<String, Object> resultObject = mapper.readValue(courseMetadataString, Map.class);
            Map<String, Object> courseMetadata = (Map<String,Object>)resultObject.get(JsonKeys.CONTENT);
            String courseName = sanitizeString((String) courseMetadata.getOrDefault("name", ""));
            String posterImage = sanitizeString((String) courseMetadata.getOrDefault("appIcon",""));
            String contentPartnerName = "";
            String contentPartnerId = "";
            Map<String, Object> contentPartner = (Map<String, Object>) courseMetadata.getOrDefault("contentPartner", new HashMap<>());
            if (MapUtils.isNotEmpty(contentPartner)) {
                contentPartnerName = (String)contentPartner.get("contentPartnerName");
                contentPartnerId = (String)contentPartner.get("id");
            }

            Map<String, Object> courseInfoMap = new HashMap<>();
            courseInfoMap.put("courseId", courseId);
            courseInfoMap.put("courseName", courseName);
            courseInfoMap.put("coursePosterImage", posterImage);
            courseInfoMap.put("providerName", contentPartnerName);
            courseInfoMap.put("contentPartnerId", contentPartnerId);

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

    public boolean isUserEligibleForExternalContentCertificate(Response row) throws BaseException {
        if (row != null) {
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) row.get(JsonKeys.RESPONSE);
            if (CollectionUtils.isNotEmpty(mapList)) {
                Map<String, Object> map = mapList.get(0);
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
                Map<String, Object> map = mapList.get(0);
                if (MapUtils.isNotEmpty(map) && map.get(JsonKeys.ISSUED_CERTIFICATES) != null) {
                    return (List<Map<String, Object>>)map.get(JsonKeys.ISSUED_CERTIFICATES);
                }
            }
        }
        return null;
    }

    public Date getCompletedOnDate(Response row) {
        if (row != null) {
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) row.get(JsonKeys.RESPONSE);
            if (CollectionUtils.isNotEmpty(mapList)) {
                Map<String, Object> map = mapList.get(0);
                if (MapUtils.isNotEmpty(map) && map.get(JsonKeys.COMPLETED_ON) != null) {
                    return (Date)map.get(JsonKeys.COMPLETED_ON);
                }
            }
        }
        return null;
    }

    public static Map<String, Object> getExternalCoursePartnerInfo(
            String partnerId) throws UnirestException, IOException {

        String url = PropertiesCache.getInstance().getProperty("cb_pores_basePath") + PropertiesCache.getInstance().getProperty("cios_content_partner_read_api") + "/" + partnerId;

        Map<String, Object> responseObject = getAPICall(url);
        Map<String, Object> resultObject = (Map<String, Object>) responseObject.get(JsonKeys.RESULT);
        if (MapUtils.isNotEmpty(resultObject)) {
            String certificateTemplate = sanitizeString((String) resultObject.getOrDefault("certificateTemplateUrl", ""));
            Map<String, Object> templateMap = new HashMap<>();
            templateMap.put("url", certificateTemplate);
            templateMap.put("certificateName", PropertiesCache.getInstance().getProperty("external_course_cert_name"));
            return templateMap;
        } else {
            return new HashMap<>();
        }
    }

}
