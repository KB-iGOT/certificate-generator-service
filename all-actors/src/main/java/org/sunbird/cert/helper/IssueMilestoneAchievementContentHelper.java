package org.sunbird.cert.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseException;
import org.sunbird.HttpUtil;
import org.sunbird.JsonKeys;
import org.sunbird.PropertiesCache;
import org.sunbird.cache.util.RedisCacheUtil;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cert.Models.AssessmentUserAttempt;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.response.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class IssueMilestoneAchievementContentHelper {
    private static final Logger logger = LoggerFactory.getLogger(IssueMilestoneAchievementContentHelper.class);

    private static final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private static final RedisCacheUtil contentCache = new RedisCacheUtil();
    private static ObjectMapper mapper = new ObjectMapper();
    private static PropertiesCache propertiesCache = PropertiesCache.getInstance();
    private static final IssueCertificateExternalContentHelper issueCertificateExternalContentHelper = IssueCertificateExternalContentHelper.getInstance();
    private static final UserEnrolmentHelper userEnrolmentHelper = UserEnrolmentHelper.getInstance();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private static IssueMilestoneAchievementContentHelper instance = null;

    public static synchronized IssueMilestoneAchievementContentHelper getInstance() {
        if (instance == null) {
            instance = new IssueMilestoneAchievementContentHelper();
        }
        return instance;
    }

    private IssueMilestoneAchievementContentHelper() {}

    public Map<String, Object> generateMilestoneAchievementMap(Map<String, Object> requestMap, Map<String, Object> template) {

        try {
            logger.info("issueMilestoneAchievement i/p event =>" + requestMap);

            String courseId = (String) requestMap.get(JsonKeys.COURSE_ID);
            String userId = (String) requestMap.get(JsonKeys.USER_ID);
            String batchId = (String) requestMap.get(JsonKeys.BATCH_ID);

            Map<String, Object> criteria = validateTemplate(template, batchId);
            String milestoneAchievementName = (String) template.getOrDefault(JsonKeys.NAME, "");
            logger.info("MilestoneAchievementName: " + milestoneAchievementName);

            Map<String, List<String>> additionalProps = mapper.readValue((String) template.getOrDefault(JsonKeys.ADDITIONAL_PROPS, "{}"), Map.class);

            Map<String, Object> enrolledUser = validateEnrolmentCriteria(requestMap, (Map<String, Object>) criteria.getOrDefault(JsonKeys.ENROLLMENT, new HashMap<>()), milestoneAchievementName, additionalProps);
            logger.debug("enrolledUser: " + enrolledUser);

            Map<String, Object> assessedUser = validateAssessmentCriteria(requestMap, (Map<String, Object>) criteria.getOrDefault(JsonKeys.ASSESSMENT, new HashMap<>()), userId, additionalProps);
            logger.debug("assessedUser: " + assessedUser);

            Map<String, Object> userDetails = validateUser(userId, (Map<String, Object>) criteria.getOrDefault(JsonKeys.USERS, new HashMap<>()), additionalProps);
            logger.debug("userDetails: " + userDetails);

            if (!userDetails.isEmpty()) {
                return generateMilestoneAchievementEvent(requestMap, template, userDetails, milestoneAchievementName, enrolledUser);
            } else {
                logger.error(String.format("User :: %s did not match the criteria for batch :: %s and course :: %s", userId, batchId, courseId));
                return null;
            }
        } catch (Exception e) {
            logger.error("Issue while validating the user Enrollment.");
        }
        return null;
    }

    private static Map<String, Object> validateTemplate(Map<String, Object> template, String batchId) throws Exception {
        Map<String, Object> criteria =
                (Map<String, Object>) template.get(JsonKeys.CRITERIA);

        if (StringUtils.isNotBlank((String) template.getOrDefault("previewUrl", "")) && !criteria.isEmpty() && !Collections.disjoint(criteria.keySet(), Arrays.asList(JsonKeys.ENROLLMENT, JsonKeys.ASSESSMENT, JsonKeys.USERS))) {
            return criteria;
        } else {
            throw new Exception("Invalid template for batch: " + batchId);
        }
    }

    private static Map<String, Object> validateEnrolmentCriteria(
            Map<String, Object> requestMap,
            Map<String, Object> enrollmentCriteria,
            String milestoneAchievementName,
            Map<String, List<String>> additionalProps) throws BaseException {

        Map<String, Object> enrolledUserMap = new HashMap<>();
        boolean reIssue = false;

        if (MapUtils.isEmpty(enrollmentCriteria)) {
            return enrolledUserMap;
        }

        String userId = (String) requestMap.get(JsonKeys.USER_ID);
        String courseId = (String) requestMap.get(JsonKeys.COURSE_ID);
        String batchId = (String) requestMap.get(JsonKeys.BATCH_ID);
        String milestoneId = (String) requestMap.get(JsonKeys.MILESTONE_ID);

        Map<String, Object> primaryKeys = new HashMap<>();
        primaryKeys.put(JsonKeys.USER_ID_KEY, userId);
        primaryKeys.put(JsonKeys.COURSE_ID_KEY, courseId);

        if (StringUtils.isNotBlank(batchId)) {
            primaryKeys.put(JsonKeys.BATCH_ID_KEY, batchId);
        }

        Response lpEnrolRecord = cassandraOperation.getRecordsByProperties(
                JsonKeys.COURSE_KEY_SPACE_NAME,
                JsonKeys.USER_ENROLMENTS_V2,
                primaryKeys,
                null
        );

        if (lpEnrolRecord == null) {
            return enrolledUserMap;
        }

        List<Map<String, Object>> lpMapList =
                (List<Map<String, Object>>) lpEnrolRecord.get(JsonKeys.RESPONSE);

        if (CollectionUtils.isEmpty(lpMapList)) {
            return enrolledUserMap;
        }

        Map<String, Object> lpMap = userEnrolmentHelper.getActiveEnrollment(lpMapList);
        if (MapUtils.isEmpty(lpMap)) {
            return enrolledUserMap;
        }
        boolean active = Boolean.TRUE.equals(lpMap.get(JsonKeys.ACTIVE));

        if (!active) {
            return enrolledUserMap;
        }
        Boolean isBadge = (Boolean) requestMap.getOrDefault("isBadge", false);
        boolean isMilestoneAchievementIssued = false;
        Response milestoneRow = null;
        String oldId = "";
        Date issuedOn = null;
        if (!isBadge) {
            Map<String, Object> primaryKey = new HashMap<>();
            primaryKey.put(JsonKeys.USER_ID_KEY, userId);
            primaryKey.put(JsonKeys.COURSE_ID_KEY, courseId);
            primaryKey.put(JsonKeys.BATCH_ID_KEY, batchId);
            primaryKey.put(JsonKeys.CONTEXT_ID_KEY, milestoneId);

            milestoneRow = cassandraOperation.getRecordsByProperties(
                    JsonKeys.COURSE_KEY_SPACE_NAME,
                    JsonKeys.USER_MILESTONE_ACHIEVEMENTS_TABLE,
                    primaryKey
            );

        }
        if (milestoneRow != null) {
            List<Map<String, Object>> mapList =
                    (List<Map<String, Object>>) milestoneRow.get(JsonKeys.RESPONSE);

            if (CollectionUtils.isNotEmpty(mapList)) {
                Map<String, Object> milestoneMap = mapList.get(0);

                List<Map<String, String>> issuedAchievements =
                        (List<Map<String, String>>) milestoneMap.getOrDefault(
                                JsonKeys.ISSUED_MILESTONE_ACHIEVEMENTS,
                                new ArrayList<>()
                        );

                if (CollectionUtils.isNotEmpty(issuedAchievements)) {
                    isMilestoneAchievementIssued =
                            issuedAchievements.stream()
                                    .anyMatch(a ->
                                            milestoneAchievementName.equalsIgnoreCase(
                                                    a.getOrDefault(JsonKeys.NAME, "")
                                            )
                                    );

                    if (isMilestoneAchievementIssued && reIssue) {
                        oldId = issuedAchievements.stream()
                                .filter(a ->
                                        milestoneAchievementName.equalsIgnoreCase(
                                                a.getOrDefault(JsonKeys.NAME, "")
                                        )
                                )
                                .map(a -> a.getOrDefault(JsonKeys.IDENTIFIER, ""))
                                .findFirst()
                                .orElse("");
                    }
                }

                issuedOn = (Date) milestoneMap.get(JsonKeys.COMPLETED_ON);
            }
        }

        enrolledUserMap.put(JsonKeys.USER, userId);
        enrolledUserMap.put(JsonKeys.OLD_ID, oldId);
        enrolledUserMap.put(JsonKeys.ISSUED_ON, issuedOn);

        if (MapUtils.isNotEmpty(additionalProps)) {
            enrolledUserMap.put(JsonKeys.ENROLLMENT, additionalProps);
        }

        return enrolledUserMap;
    }

    private static Map<String, Object> validateAssessmentCriteria(Map<String, Object> requestMap, Map<String, Object> assessmentCriteria, String enrolledUser, Map<String, List<String>> additionalProps) throws BaseException {
        if (!assessmentCriteria.isEmpty() && !enrolledUser.isEmpty()) {
            Map<String, List<AssessmentUserAttempt>> filteredUserAssessments = getMaxScore(requestMap);

            Map<String, Double> scoreMap = filteredUserAssessments.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (e.getValue().get(0).getScore() * 100.0) / e.getValue().get(0).getTotalScore()));

            double score = scoreMap.isEmpty() ? 0d : Collections.max(scoreMap.values());

            List<String> assessmentAdditionProps = additionalProps.getOrDefault("assessment", new ArrayList<>());
            Map<String, Object> addProps = new HashMap<>();
            if (!assessmentAdditionProps.isEmpty() && assessmentAdditionProps.contains("score")) {
                addProps.put("score", scoreMap);
            }

            if (isValidAssessCriteria(assessmentCriteria, score)) {
                Map<String, Object> assessedUserMap = new HashMap<>();
                assessedUserMap.put("user", enrolledUser);
                if (!addProps.isEmpty()) {
                    assessedUserMap.put("assessment", addProps);
                }
                return assessedUserMap;
            } else {
                return Collections.singletonMap("user", "");
            }
        } else {
            return Collections.singletonMap("user", enrolledUser);
        }
    }

    public static Map<String, List<AssessmentUserAttempt>> getMaxScore(Map<String, Object> requestMap) throws BaseException {

        Map<String, Object> primaryKey = new HashMap<>();
        String courseId = (String) requestMap.get(JsonKeys.COURSE_ID);
        String userId = (String) requestMap.get(JsonKeys.USER_ID);
        String batchId = (String) requestMap.get(JsonKeys.BATCH_ID);

        String contextId = "cb:" + batchId;

        primaryKey.put("user_id", userId);
        primaryKey.put("activity_type", courseId);
        primaryKey.put("context_id", contextId);

        Response row = cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_ACTIVITY_AGG, primaryKey, Arrays.asList("aggregates", "agg"));
        if (row != null) {
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) row.get("response");
            Map<String, Object> map = mapList.get(0);
            Map<String, Double> aggregatesRaw = (Map<String, Double>) map.get("aggregates");
            Map<String, Integer> aggRaw = (Map<String, Integer>) row.get("agg");
            Map<String, Double> agg = new HashMap<>();
            if (aggRaw != null) {
                for (Map.Entry<String, Integer> entry : aggRaw.entrySet()) {
                    agg.put(entry.getKey(), entry.getValue() != null ? entry.getValue().doubleValue() : null);
                }
            }
            // Combine maps
            Map<String, Double> aggs = new HashMap<>(agg);
            aggs.putAll(aggregatesRaw);
            Map<String, List<AssessmentUserAttempt>> userAssessments = aggs.keySet().stream().filter(key -> key.startsWith("score:")).map(key -> {
                String id = key.replaceAll("score:", "");
                double score = aggs.getOrDefault("score:" + id, 0.0);
                double totalScore = aggs.getOrDefault("max_score:" + id, 1.0);
                return new AssessmentUserAttempt(id, score, totalScore);
            }).collect(Collectors.groupingBy(AssessmentUserAttempt::getContentId, HashMap::new, Collectors.toList()));

            if (!userAssessments.isEmpty()) {
                Map<String, List<AssessmentUserAttempt>> filteredUserAssessments = userAssessments.entrySet().stream().filter(entry -> {
                    String key = entry.getKey();
                    try {
                        String metadataString = contentCache.get(key, null, 60);

                        if (metadataString != null && !metadataString.isEmpty()) {
                            Map<String, Object> metadata = mapper.readValue(metadataString, new TypeReference<Map<String, Object>>() {
                            });
                            String contentType = (String) metadata.getOrDefault("contenttype", "");
                            List<String> assessmentContentType = Arrays.asList(JsonKeys.ASSESSMENT_CONTENT_TYPE.split(","));
                            return assessmentContentType.contains(contentType);
                        } else {
                            logger.error("Suppressed exception: Metadata cache not available for: " + key);
                            return false;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1,  // Merge function (just keeps first value if duplicate keys)
                        HashMap::new));

                // Return filtered map if not empty, otherwise empty map
                return !filteredUserAssessments.isEmpty() ? filteredUserAssessments : new HashMap<>();
            }

        }
        return new HashMap<>();
    }

    public static boolean isValidAssessCriteria(Map<String, Object> assessmentCriteria, double score) {
        if (assessmentCriteria.containsKey("score") && assessmentCriteria.get("score") instanceof Number) {
            return score == ((Number) assessmentCriteria.get("score")).doubleValue();
        } else {
            Map<String, Integer> scoreCriteria = (Map<String, Integer>) assessmentCriteria.getOrDefault("score", new HashMap<>());
            if (scoreCriteria.isEmpty()) {
                return false;
            } else {
                String operation = scoreCriteria.keySet().iterator().next();
                double criteriaScore = scoreCriteria.get(operation).doubleValue();

                switch (operation) {
                    case "EQ":
                    case "eq":
                    case "=":
                        return score == criteriaScore;
                    case ">":
                        return score > criteriaScore;
                    case "<":
                        return score < criteriaScore;
                    case ">=":
                        return score >= criteriaScore;
                    case "<=":
                        return score <= criteriaScore;
                    case "ne":
                    case "!=":
                        return score != criteriaScore;
                    default:
                        return false;
                }
            }
        }
    }

    private Map<String, Object> validateUser(String userId, Map<String, Object> userCriteria, Map<String, List<String>> additionalProps) throws UnirestException, IOException {
        if (userId != null && !userId.isEmpty()) {
            String url = propertiesCache.getProperty("learner_basePath") + propertiesCache.getProperty("user_read_api") + "/" + userId + "?organisations,roles,locations,declarations,externalIds";

            Map<String, Object> result = getAPICall(url);

            if (userCriteria.isEmpty() || userCriteria.entrySet().stream().allMatch(entry -> entry.getValue().equals(result.getOrDefault(entry.getKey(), null)))) {
                Map<String, Object> resultObject = (Map<String, Object>) result.get(JsonKeys.RESULT);
                Map<String, Object> responseObject = (Map<String, Object>) resultObject.get(JsonKeys.RESPONSE);
                return responseObject;
            } else {
                return Collections.emptyMap();
            }
        } else {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> generateMilestoneAchievementEvent(Map<String, Object> requestMap, Map<String, Object> template, Map<String, Object> userDetails, String milestoneAchievementName, Map<String, Object> enrolledUser) throws UnirestException, IOException {
        String firstName = (String) userDetails.getOrDefault("firstName", "");
        String lastName = (String) userDetails.getOrDefault("lastName", "");

        String recipientName = StringUtils.isNotBlank(lastName) ? firstName + " " + lastName : firstName;
        recipientName = recipientName.trim();
        Boolean isExternal = (Boolean) requestMap.getOrDefault(JsonKeys.IS_EXTERNAL, false);
        Map<String, Object> courseInfo;
        if (isExternal) {
            courseInfo = issueCertificateExternalContentHelper.getExternalCourseInfo((String) requestMap.get(JsonKeys.COURSE_ID));
        } else {
            courseInfo = getCourseInfo((String) requestMap.get(JsonKeys.COURSE_ID));
        }
        Boolean isBadge = (Boolean) requestMap.getOrDefault("isBadge", false);
        String badgeName = "";
        String courseName = "";
        String badgeImage = "";
        if (isBadge) {
                String badgeId = (String) requestMap.get("badgeId");
                List<Map<String, Object>> badgeDetailsV1 =
                        (List<Map<String, Object>>) courseInfo.getOrDefault("badgeDetails_v1", Collections.emptyList());

                if (CollectionUtils.isNotEmpty(badgeDetailsV1)) {
                    Map<String, Object> badgeDetails = badgeDetailsV1.stream()
                            .filter(m -> badgeId.equalsIgnoreCase((String) m.get("badgeId")))
                            .findFirst()
                            .orElse(null);

                    if (badgeDetails != null) {
                        badgeName = (String) badgeDetails.getOrDefault("badgeTitle", "");
                        badgeImage = (String) badgeDetails.getOrDefault("badgeTemplate", "");
                    }
                }
                courseName = (String) courseInfo.getOrDefault("courseName", "");
        } else {
            String incomingMilestoneId = (String) requestMap.get(JsonKeys.MILESTONE_ID);
            List<Map<String, Object>> milestones =
                    (List<Map<String, Object>>) courseInfo.getOrDefault("milestones_v1", Collections.emptyList());
            Map<String, Object> milestone = milestones.stream()
                    .filter(m -> incomingMilestoneId.equalsIgnoreCase((String) m.get("id")))
                    .findFirst()
                    .orElse(null);
             courseName = (String) milestone.getOrDefault("name", "");
        }

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        List<String> parentCollections = Optional.ofNullable((List<String>) courseInfo.get("parentCollections")).orElse(Collections.emptyList());
        Date issuedOn = (Date) enrolledUser.get(JsonKeys.ISSUED_ON);
        Map<String, Object> eData = new HashMap<>();
        eData.put("issuedDate", dateFormatter.format(issuedOn != null ? issuedOn : new Date()));
        String finalRecipientName = recipientName;
        eData.put("data", Collections.singletonList(new HashMap<String, Object>() {{
            put("recipientName", finalRecipientName);
            put("recipientId", requestMap.get(JsonKeys.USER_ID));
        }}));
        eData.put("reIssueDate", "");
        eData.put("criteria", Collections.singletonMap("narrative", milestoneAchievementName));
        eData.put("svgTemplate", template.getOrDefault("previewUrl", ""));
        eData.put("oldId", enrolledUser.get("oldId"));
        eData.put("templateId", template.getOrDefault("identifier", ""));
        eData.put("userId", requestMap.get(JsonKeys.USER_ID));
        eData.put("orgId", userDetails.getOrDefault("rootOrgId", ""));
        Object issuerObj = template.get(JsonKeys.ISSUER);
        eData.put("issuer", issuerObj instanceof Map ? issuerObj : new HashMap<>());

        //eData.put("issuer", mapper.readValue((String) template.getOrDefault(JsonKeys.ISSUER, "{}"), Map.class));
        eData.put("signatoryList", template.getOrDefault(JsonKeys.SIGNATORY_LIST, new ArrayList<>()));
        eData.put("courseName", courseName);
        eData.put("basePath", propertiesCache.getProperty("cert_domain_url") + "/milestoneAchievements");
        eData.put("name", milestoneAchievementName);
        eData.put("providerName", courseInfo.getOrDefault("providerName", ""));
        eData.put("tag", requestMap.get(JsonKeys.BATCH_ID));
        eData.put("primaryCategory", courseInfo.getOrDefault("primaryCategory", ""));
        eData.put("parentCollections", parentCollections);
        eData.put("coursePosterImage", courseInfo.getOrDefault("coursePosterImage", ""));
        eData.put("badgeName", badgeName);
        eData.put("badgeImage", badgeImage);
        return eData;
    }

    public Map<String, Object> getAPICall(String url) throws UnirestException, IOException {
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

    public Map<String, Object> getCourseInfo(String courseId) throws UnirestException, IOException {

        String courseMetadataString = contentCache.get(courseId, null, 0);
        if (StringUtils.isBlank(courseMetadataString)) {
            String url = PropertiesCache.getInstance().getProperty("content_basePath") + PropertiesCache.getInstance().getProperty("content_read_api") + "/" + courseId + "?fields=name,parentCollections,primaryCategory,posterImage,organisation,milestones_v1,preliminaryAssessment,batches,language,badgeDetails_v1";

            Map<String, Object> responseObject = getAPICall(url);
            Map<String, Object> resultObject = (Map<String, Object>) responseObject.get(JsonKeys.RESULT);
            if (MapUtils.isNotEmpty(resultObject)) {
                Map<String, Object> response = (Map<String, Object>) resultObject.get(JsonKeys.CONTENT);
                String courseName = sanitizeString((String) response.getOrDefault("name", ""));
                String primaryCategory = sanitizeString((String) response.getOrDefault("primaryCategory", ""));
                String posterImage = sanitizeString((String) response.getOrDefault("posterImage", ""));

                List<String> parentCollections = (List<String>) response.getOrDefault("parentCollections", new ArrayList<>());

                List<Object> orgData = (List<Object>) response.getOrDefault("organisation", Collections.emptyList());
                String providerName = extractProviderName(orgData);
                List<String> language = (List<String>) response.getOrDefault("language", new ArrayList<>());
                Map<String, Object> courseInfoMap = new HashMap<>();
                courseInfoMap.put("courseId", courseId);
                courseInfoMap.put("courseName", courseName);
                courseInfoMap.put("parentCollections", parentCollections);
                courseInfoMap.put("primaryCategory", primaryCategory);
                courseInfoMap.put("coursePosterImage", posterImage);
                courseInfoMap.put("providerName", providerName);
                courseInfoMap.put("milestones_v1", response.getOrDefault("milestonesv1", new ArrayList<>()));
                courseInfoMap.put("preliminaryAssessment", response.getOrDefault("preliminaryassessment", ""));
                List<Map<String, Object>> batches =
                        (List<Map<String, Object>>) response.getOrDefault("batches", Collections.emptyList());
                courseInfoMap.put("batches", batches);
                courseInfoMap.put("language", language);
                courseInfoMap.put("badgeDetails_v1", response.getOrDefault("badgeDetails_v1", new ArrayList<>()));
                return courseInfoMap;
            } else {
                return new HashMap<>();
            }

        } else {
            Map<String, Object> courseMetadata = mapper.readValue(courseMetadataString, Map.class);
            String courseName = sanitizeString((String) courseMetadata.getOrDefault("name", ""));
            String primaryCategory = sanitizeString((String) courseMetadata.getOrDefault("primaryCategory", ""));
            List<String> parentCollections = (List<String>) courseMetadata.getOrDefault("parentCollections", new ArrayList<>());
            String posterImage = sanitizeString((String) courseMetadata.getOrDefault("posterImage", ""));

            List<Object> orgData = (List<Object>) courseMetadata.getOrDefault("organisation", Collections.emptyList());
            String providerName = extractProviderName(orgData);
            List<String> language = (List<String>) courseMetadata.getOrDefault("language", new ArrayList<>());
            Map<String, Object> courseInfoMap = new HashMap<>();
            courseInfoMap.put("courseId", courseId);
            courseInfoMap.put("courseName", courseName);
            courseInfoMap.put("parentCollections", parentCollections);
            courseInfoMap.put("primaryCategory", primaryCategory);
            courseInfoMap.put("coursePosterImage", posterImage);
            courseInfoMap.put("providerName", providerName);
            courseInfoMap.put("milestones_v1", courseMetadata.getOrDefault("milestones_v1", new ArrayList<>()));
            courseInfoMap.put("preliminaryAssessment", courseMetadata.getOrDefault("preliminaryAssessment", ""));
            List<Map<String, Object>> batches =
                    (List<Map<String, Object>>) courseMetadata.getOrDefault("batches", Collections.emptyList());
            courseInfoMap.put("batches", batches);
            courseInfoMap.put("language", language);
            courseInfoMap.put("badgeDetails_v1", courseMetadata.getOrDefault("badgeDetails_v1", new ArrayList<>()));
            return courseInfoMap;
        }
    }

    private static String sanitizeString(String input) {
        return Optional.ofNullable(input).map(str -> str.chars().filter(c -> c >= ' ').mapToObj(c -> String.valueOf((char) c)).collect(Collectors.joining())).orElse("");
    }

    private static String extractProviderName(List<Object> orgData) {
        if (!orgData.isEmpty()) {
            String pm = orgData.get(0).toString();
            return pm;
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public boolean isUserEligibleForContentMilestoneAchievement(
            Response lpRow,
            String userId,
            Map<String, Object> contentInfo,
            String incomingMilestoneId) throws BaseException, UnirestException, IOException {

        if (lpRow == null || contentInfo == null) {
            return false;
        }


        List<Map<String, Object>> milestones =
                (List<Map<String, Object>>) contentInfo.getOrDefault("milestones_v1", Collections.emptyList());

        List<String> languages =
                (List<String>) contentInfo.getOrDefault("language", Collections.emptyList());

        if (milestones.isEmpty() || languages.isEmpty()) {
            return false;
        }

        Map<String, Object> milestone = milestones.stream()
                .filter(m -> incomingMilestoneId.equalsIgnoreCase((String) m.get("id")))
                .findFirst()
                .orElse(null);

        if (milestone == null) {
            return false;
        }

        List<Map<String, Object>> courses =
                (List<Map<String, Object>>) milestone.getOrDefault("courses", Collections.emptyList());

        List<String> mandatoryCourseIds = courses.stream()
                .filter(c -> Boolean.TRUE.equals(c.get("isMandatory")))
                .map(c -> String.valueOf(c.get(JsonKeys.IDENTIFIER)))
                .collect(Collectors.toList());

        if (mandatoryCourseIds.isEmpty()) {
            return false;
        }

        String assessmentId = Optional.ofNullable(
                        (Map<String, Object>) milestone.get("assessmentDetail"))
                .map(ad -> ad.get("identifier"))
                .map(Object::toString)
                .orElse(null);

        if (assessmentId == null) {
            return false;
        }

        for (String mandatoryCourseId : mandatoryCourseIds) {
            Map<String, Object> mandatoryCourseContent = getCourseInfo(mandatoryCourseId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mandatoryCourseBatches =
                    (List<Map<String, Object>>) mandatoryCourseContent.getOrDefault("batches", Collections.emptyList());
            String mandatoryCourseBatchId = mandatoryCourseBatches.stream()
                    .map(b -> (String) b.get("batchId"))
                    .findFirst()
                    .orElse(null);

            Response courseEnrollment =
                    getUserEnrollmentRecord(mandatoryCourseId, mandatoryCourseBatchId, userId);

            if (courseEnrollment == null ||
                    CollectionUtils.isEmpty((List<?>) courseEnrollment.get(JsonKeys.RESPONSE))) {
                return false;
            }

            Map<String, Object> enrolment =
                    ((List<Map<String, Object>>) courseEnrollment.get(JsonKeys.RESPONSE))
                            .stream()
                            .filter(r -> Boolean.TRUE.equals(r.get(JsonKeys.ACTIVE)))
                            .findFirst()
                            .orElse(null);

            if (enrolment == null) {
                return false;
            }

            Integer status = (Integer) enrolment.getOrDefault("status", 0);
            List<Map<String, String>> issuedCertificates =
                    (List<Map<String, String>>) enrolment.get("issued_certificates");

            boolean courseCompleted =
                    status == 2 ||
                            (CollectionUtils.isNotEmpty(issuedCertificates));

            if (!courseCompleted) {
                return false;
            }
        }

        List<Map<String, Object>> lpRows =
                (List<Map<String, Object>>) lpRow.get(JsonKeys.RESPONSE);

        if (CollectionUtils.isEmpty(lpRows)) {
            return false;
        }

        Map<String, Object> activeLpRow = lpRows.stream()
                .filter(r -> Boolean.TRUE.equals(r.get(JsonKeys.ACTIVE)))
                .findFirst()
                .orElse(null);

        if (activeLpRow == null) {
            return false;
        }

        Map<String, Map<String, Integer>> langContentStatus =
                (Map<String, Map<String, Integer>>) activeLpRow.get("lang_contentstatus");

        String lang = languages.get(0);
        Map<String, Integer> statusByLang =
                langContentStatus != null ? langContentStatus.get(lang.toLowerCase()) : null;

        if (statusByLang == null) {
            return false;
        }

        return statusByLang.getOrDefault(assessmentId, 0) == 2;
    }

    public Response getUserEnrollmentRecord(String courseId, String batchId, String userId) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.USER_ID, userId);
        primaryKey.put(JsonKeys.COURSE_ID, courseId);
        primaryKey.put(JsonKeys.BATCH_ID, batchId);
        return cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_ENROLMENTS_V2, primaryKey);
    }

    @SuppressWarnings("unchecked")
    public Response fetchContentTemplate(
            Map<String, Object> contentInfo,
            String incomingMilestoneId
    ) throws Exception {

        List<Map<String, Object>> milestones =
                (List<Map<String, Object>>) contentInfo.get(JsonKeys.MILESTONES_V1);

        if (CollectionUtils.isEmpty(milestones)) {
            throw new Exception("Milestones not found in contentInfo");
        }

        Map<String, Object> lastMilestone =
                milestones.get(milestones.size() - 1);

        String lastMilestoneId =
                (String) lastMilestone.get(JsonKeys.ID);

        boolean isFinalMilestone =
                incomingMilestoneId != null
                        && incomingMilestoneId.equalsIgnoreCase(lastMilestoneId);

        String templateId = isFinalMilestone
                ? propertiesCache.getProperty(
                JsonKeys.FINAL_MILESTONE_ACHIEVEMENT_TEMPLATE_ID)
                : propertiesCache.getProperty(
                JsonKeys.INTERMEDIATE_MILESTONE_ACHIEVEMENT_TEMPLATE_ID);

        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.ID, templateId);

        return cassandraOperation.getRecordsByProperties(
                JsonKeys.SUNBIRD,
                JsonKeys.TABLE_SYSTEM_SETTINGS,
                primaryKey,
                null
        );
    }
}

