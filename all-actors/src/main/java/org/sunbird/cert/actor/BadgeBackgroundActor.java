package org.sunbird.cert.actor;


import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseActor;
import org.sunbird.JsonKeys;
import org.sunbird.cert.helper.UserEnrolmentHelper;
import org.sunbird.request.Request;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class BadgeBackgroundActor extends BaseActor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BadgeBackgroundActor.class);
    private final UserEnrolmentHelper userEnrolmentHelper = UserEnrolmentHelper.getInstance();

    @Override
    public void onReceive(Request request) {

        if ("ADD_BADGE_REGISTRY".equalsIgnoreCase(request.getOperation())) {
            save(request);
        } else {
            unhandled(request);
        }
    }

    private void save(Request request) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> req = request.getRequest();

            Map<String, Object> db = new HashMap<>();

            String uuid = UUID.randomUUID().toString();

            db.put(JsonKeys.ID, uuid);
            db.put(JsonKeys.CREATED_AT, new Timestamp(System.currentTimeMillis()));
            db.put(JsonKeys.CREATED_BY, req.get(JsonKeys.USER_ID));
            db.put(JsonKeys.IS_REVOKED, false);
            db.put(JsonKeys.REASON, "badge-issued");

            db.put(JsonKeys.DATA, objectMapper.writeValueAsString(req.get("badge")));

            Map<String, Object> related = new HashMap<>();
            related.put(JsonKeys.COURSE_ID, req.get(JsonKeys.COURSE_ID));
            related.put(JsonKeys.BATCH_ID, req.get(JsonKeys.BATCH_ID));
            related.put(JsonKeys.FIELD_BADGE_ID, req.get(JsonKeys.FIELD_BADGE_ID));

            db.put(JsonKeys.RELATED, objectMapper.writeValueAsString(related));

            userEnrolmentHelper.insertMilestoneAchievementRegistry(db);

        } catch (Exception e) {
            LOGGER.error("Badge save failed", e);
        }
    }
}