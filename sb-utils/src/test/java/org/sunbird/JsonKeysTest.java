package org.sunbird;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonKeysTest {

    @Test
    void allStringConstantsHaveExpectedValues() {
        assertEquals("jsonData", JsonKeys.JSON_DATA);
        assertEquals("accessCode", JsonKeys.ACCESS_CODE);
        assertEquals("id", JsonKeys.ID);
        assertEquals("request", JsonKeys.REQUEST);
        assertEquals("response", JsonKeys.RESPONSE);
        assertEquals("result", JsonKeys.RESULT);
        assertEquals("certificate", JsonKeys.CERTIFICATE);
        assertEquals("recipientName", JsonKeys.RECIPIENT_NAME);
        assertEquals("courseName", JsonKeys.COURSE_NAME);
        assertEquals("name", JsonKeys.NAME);
        assertEquals("htmlTemplate", JsonKeys.HTML_TEMPLATE);
        assertEquals("issuer", JsonKeys.ISSUER);
        assertEquals("url", JsonKeys.URL);
        assertEquals("signatoryList", JsonKeys.SIGNATORY_LIST);
        assertEquals("type", JsonKeys.TYPE);
        assertEquals("account", JsonKeys.ACCOUNT);
        assertEquals("key", JsonKeys.key);
        assertEquals("recipientType", JsonKeys.RECIPIENT_TYPE);
        assertEquals("reason", JsonKeys.REASON);
        assertEquals("related", JsonKeys.RELATED);
        assertEquals("individual", JsonKeys.INDIVIDUAL);
        assertEquals("entity", JsonKeys.ENTITY);
        assertEquals("verify", JsonKeys.CERT_VERIFY);
        assertEquals("message", JsonKeys.MESSAGE);
        assertEquals("recipientId", JsonKeys.RECIPIENT_ID);
        assertEquals("sunbird_cassandra_host", JsonKeys.SUNBIRD_CASSANDRA_IP);
        assertEquals("sunbird_cassandra_password", JsonKeys.SUNBIRD_CASSANDRA_PASSWORD);
        assertEquals("sunbird_cassandra_port", JsonKeys.SUNBIRD_CASSANDRA_PORT);
        assertEquals("sunbird_cassandra_username", JsonKeys.SUNBIRD_CASSANDRA_USER_NAME);
        assertEquals("standalone", JsonKeys.STANDALONE_MODE);
        assertEquals("sunbird", JsonKeys.SUNBIRD);
        assertEquals("cert_registry", JsonKeys.CERT_REGISTRY);
        assertEquals("recipient", JsonKeys.RECIPIENT);
        assertEquals("oldId", JsonKeys.OLD_ID);
        assertEquals("issuedOn", JsonKeys.ISSUED_ON);
        assertEquals("qrCodeUrl", JsonKeys.QR_CODE_URL);
        assertEquals("identity", JsonKeys.IDENTITY);
        assertEquals("image", JsonKeys.IMAGE);
        assertEquals("v2", JsonKeys.VERSION_2);
        assertEquals("v1", JsonKeys.VERSION_1);
        assertEquals("version", JsonKeys.VERSION);
        assertEquals("printUri", JsonKeys.PRINT_URI);
        assertEquals("reqId", JsonKeys.REQ_ID);
        assertEquals("msgId", JsonKeys.REQUEST_MESSAGE_ID);
        assertEquals("courseId", JsonKeys.COURSE_ID);
        assertEquals("x-authenticated-user-token", JsonKeys.X_AUTHENTICATED_USER_TOKEN);
        assertEquals("user_enrolments", JsonKeys.USER_ENROLMENTS);
        assertEquals("userId", JsonKeys.USER_ID);
        assertEquals("batchId", JsonKeys.BATCH_ID);
        assertEquals("sunbird_courses", JsonKeys.COURSE_KEY_SPACE_NAME);
        assertEquals("course_batch", JsonKeys.COURSE_BATCH);
        assertEquals("criteria", JsonKeys.CRITERIA);
        assertEquals("enrollment", JsonKeys.ENROLLMENT);
        assertEquals("assessment", JsonKeys.ASSESSMENT);
        assertEquals("users", JsonKeys.USERS);
        assertEquals("user_activity_agg", JsonKeys.USER_ACTIVITY_AGG);
        assertEquals("AssessmentContentTypes", JsonKeys.ASSESSMENT_CONTENT_TYPE);
        assertEquals("dynamicGeneration", JsonKeys.DYNAMIC_GENERATION);
        assertEquals("responseCode", JsonKeys.RESPONSE_CODE);
        assertEquals("ok", JsonKeys.OK);
        assertEquals("user_entity_enrolments", JsonKeys.USER_ENTITY_ENROLMENTS);
        assertEquals("contentid", JsonKeys.CONTENT_ID);
        assertEquals("contextid", JsonKeys.CONTEXT_ID);
        assertEquals("event_batch", JsonKeys.EVENT_BATCH);
        assertEquals("eventid", JsonKeys.EVENT_ID);
        assertEquals("additionalProps", JsonKeys.ADDITIONAL_PROPS);
        assertEquals("active", JsonKeys.ACTIVE);
        assertEquals("issued_certificates", JsonKeys.ISSUED_CERTIFICATES);
        assertEquals("status", JsonKeys.STATUS);
        assertEquals("identifier", JsonKeys.IDENTIFIER);
        assertEquals("completedon", JsonKeys.COMPLETED_ON);
        assertEquals("user", JsonKeys.USER);
        assertEquals("cert_templates", JsonKeys.CERT_TEMPLATES);
        assertEquals("primaryCategory", JsonKeys.PRIMARY_CATEGORY);
        assertEquals("Event", JsonKeys.EVENT);
        assertEquals("certRegistry_basePath", JsonKeys.CERT_REGISTRY_BASE_PATH);
        assertEquals("addCertRegApi", JsonKeys.ADD_CERT_REG_API);
        assertEquals("cert_registry_v2", JsonKeys.CERT_REGISTRY_V2);
        assertEquals("lastIssuedOn", JsonKeys.LAST_ISSUED_ON);
        assertEquals("token", JsonKeys.TOKEN);
        assertEquals("healthy", JsonKeys.HEALTHY);
        assertEquals("checks", JsonKeys.CHECKS);
        assertEquals("system_settings", JsonKeys.TABLE_SYSTEM_SETTINGS);
        assertEquals("sunbird_sso_client_id", JsonKeys.SUNBIRD_SSO_CLIENT_ID);
        assertEquals("sunbird_sso_client_secret", JsonKeys.SUNBIRD_SSO_CLIENT_SECRET);
        assertEquals("sunbird_sso_password", JsonKeys.SUNBIRD_SSO_PASSWORD);
        assertEquals("sunbird_sso_realm", JsonKeys.SUNBIRD_SSO_RELAM);
        assertEquals("sunbird_sso_url", JsonKeys.SUNBIRD_SSO_URL);
        assertEquals("sunbird_sso_username", JsonKeys.SUNBIRD_SSO_USERNAME);
        assertEquals("SHA256withRSA", JsonKeys.SHA_256_WITH_RSA);
        assertEquals("sub", JsonKeys.SUB);
        assertEquals(".", JsonKeys.DOT_SEPARATOR);
        assertEquals("Unauthorized", JsonKeys.UNAUTHORIZED);
        assertEquals("parentId", JsonKeys.PARENT_ID);
        assertEquals("accesstoken.publickey.basepath", JsonKeys.ACCESS_TOKEN_PUBLICKEY_BASEPATH);
        assertEquals("content", JsonKeys.CONTENT);
        assertEquals("certificateExternsion", JsonKeys.CERTIFICATE_EXTENSION);
        assertEquals("uuid", JsonKeys.UUID);
        assertEquals("add_registry_req", JsonKeys.ADD_REGISTRY_REQUEST);
        assertEquals("certModel", JsonKeys.CERT_MODEL);
        assertEquals("isEvent", JsonKeys.IS_EVENT);
        assertEquals("user_certificate_list", JsonKeys.USER_CERTICATE_LIST);
        assertEquals("X-Authenticated-User-Token", JsonKeys.X_AUTHENTICATED_USER_TOKEN_CAMEL_CASE);
    }

    @Test
    void allObjectConstantsHaveExpectedValues() {
        assertEquals("cassandraDb", JsonKeys.CASSANDRA_DB);
        assertEquals("redisDb", JsonKeys.REDIS_CACHE);
    }
}
