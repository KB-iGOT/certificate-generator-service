package org.sunbird.cert.helper;

import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.response.Response;

import java.sql.Timestamp;
import java.util.*;

public class UserEnrolmentHelper {

    private static final CassandraOperation cassandraOperation = ServiceFactory.getInstance();

    private static UserEnrolmentHelper instance = null;

    public static synchronized UserEnrolmentHelper getInstance() {
        if (instance == null) {
            instance = new UserEnrolmentHelper();
        }
        return instance;
    }

    private UserEnrolmentHelper() {
    }

    public Response getUserEnrollmentRecord(String courseId, String batchId, String userId) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.USER_ID, userId);
        primaryKey.put(JsonKeys.COURSE_ID, courseId);
        primaryKey.put(JsonKeys.BATCH_ID, batchId);
        return cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_ENROLMENTS_V2, primaryKey);
    }

    public Response getUserEventEnrollmentRecord(String courseId, String batchId, String userId) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.USER_ID, userId);
        primaryKey.put(JsonKeys.CONTENT_ID, courseId);
        primaryKey.put(JsonKeys.CONTEXT_ID, courseId);
        primaryKey.put(JsonKeys.BATCH_ID, batchId);
        return cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_ENTITY_ENROLMENTS, primaryKey);
    }

    public Response updateUserEventEnrollmentRecord(String courseId, String batchId, String userId, Map<String, Object> attributeMap) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.USER_ID, userId);
        primaryKey.put(JsonKeys.CONTENT_ID, courseId);
        primaryKey.put(JsonKeys.CONTEXT_ID, courseId);
        primaryKey.put(JsonKeys.BATCH_ID, batchId);
        return cassandraOperation.updateRecord(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_ENTITY_ENROLMENTS, attributeMap, primaryKey);
    }

    public Response updateUserEnrollmentRecord(String courseId, String batchId, String userId, Map<String, Object> attributeMap) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.USER_ID, userId);
        primaryKey.put(JsonKeys.COURSE_ID, courseId);
        primaryKey.put(JsonKeys.BATCH_ID, batchId);
        return cassandraOperation.updateRecord(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_ENROLMENTS_V2, attributeMap, primaryKey);
    }

    public Response getUserEnrollmentRecordForExternalContent(String courseId, String userId) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.USER_ID, userId);
        primaryKey.put(JsonKeys.COURSE_ID, courseId);
        return cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.TABLE_USER_EXTERNAL_ENROLMENTS, primaryKey);
    }

    public Response updateExternalEnrollmentRecord(String courseId, String userId, Map<String, Object> attributeMap) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.USER_ID, userId);
        primaryKey.put(JsonKeys.COURSE_ID, courseId);
        return cassandraOperation.updateRecord(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.TABLE_USER_EXTERNAL_ENROLMENTS, attributeMap, primaryKey);
    }

    public Response getUserMilestoneAchievements(String courseId, String batchId, String userId, String contextId) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.USER_ID_KEY, userId);
        primaryKey.put(JsonKeys.COURSE_ID_KEY, courseId);
        primaryKey.put(JsonKeys.BATCH_ID_KEY, batchId);
        primaryKey.put(JsonKeys.CONTEXT_ID, contextId);
        return cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_MILESTONE_ACHIEVEMENTS_TABLE, primaryKey);
    }

    public Response insertUserMilestoneAchievements(String courseId, String batchId, String userId, String contextId, Date userCompletedOn, Map<String, Object> attributeMap) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.USER_ID_KEY, userId);
        primaryKey.put(JsonKeys.COURSE_ID_KEY, courseId);
        primaryKey.put(JsonKeys.BATCH_ID_KEY, batchId);
        primaryKey.put(JsonKeys.CONTEXT_ID, contextId);
        List<Map<String, String>> issuedAchievements =
                normalizeIssuedAchievements(
                        attributeMap.get(JsonKeys.ISSUED_MILESTONE_ACHIEVEMENTS)
                );
        primaryKey.put(JsonKeys.ISSUED_MILESTONE_ACHIEVEMENTS, issuedAchievements);
        primaryKey.put(JsonKeys.COMPLETED_ON, userCompletedOn != null
                ? new Timestamp(userCompletedOn.getTime())
                : null);
        return cassandraOperation.insertRecord(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_MILESTONE_ACHIEVEMENTS_TABLE, primaryKey);
    }

    public Response insertMilestoneAchievementRegistry(Map<String, Object> attributeMap) throws BaseException {
        return cassandraOperation.insertRecord(JsonKeys.SUNBIRD, JsonKeys.MILESTONEACHIEVEMENT_REGISTRY_TABLE, attributeMap);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> normalizeIssuedAchievements(Object rawIssued) {

        List<Map<String, String>> normalized = new ArrayList<>();

        if (rawIssued instanceof List) {
            for (Map<String, String> item : (List<Map<String, String>>) rawIssued) {
                Map<String, String> mutableMap = new HashMap<>();
                item.forEach((k, v) -> {
                    if (k != null && v != null) {
                        mutableMap.put(k, v);
                    }
                });
                normalized.add(mutableMap);
            }
        }

        return normalized;
    }

}
