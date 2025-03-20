package org.sunbird.cert.helper;

import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.response.Response;

import java.util.HashMap;
import java.util.Map;

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
        return cassandraOperation.getRecordsByProperties(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_ENROLMENTS, primaryKey);
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
        return cassandraOperation.updateRecord(JsonKeys.COURSE_KEY_SPACE_NAME, JsonKeys.USER_ENROLMENTS, attributeMap, primaryKey);
    }
}
