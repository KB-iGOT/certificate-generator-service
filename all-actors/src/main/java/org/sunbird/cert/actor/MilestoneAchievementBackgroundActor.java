package org.sunbird.cert.actor;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.sunbird.BaseActor;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.PropertiesCache;
import org.sunbird.cert.helper.*;
import org.sunbird.incredible.pojos.CertificateExtension;
import org.sunbird.incredible.processor.CertModel;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MilestoneAchievementBackgroundActor extends BaseActor {
    private static final UserEnrolmentHelper userEnrolmentHelper = UserEnrolmentHelper.getInstance();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    public void onReceive(Request request) throws Throwable {
        logger.info("MilestoneAchievementActor:onReceive:request arrived with operation" + request.getOperation());
        String operation = request.getOperation();
        switch (operation) {
            case "add_milestone_achievement_registry_req":
                addToRegistryAndUpdateCassandraRecord(request);
                break;
            default:
                onReceiveUnsupportedMessage("MilestoneAchievementActor");
        }
    }

    private void addToRegistryAndUpdateCassandraRecord(Request request) {
        String courseId = (String) request.getRequest().get(JsonKeys.COURSE_ID);
        String batchId = (String) request.getRequest().get(JsonKeys.BATCH_ID);
        String userId = (String) request.getRequest().get(JsonKeys.USER_ID);
        String milestoneId = (String) request.getRequest().get(JsonKeys.MILESTONE_ID);
        try {
            logger.debug("The request is for backgroundActor : " + request.getRequest());
            Map<String, Object> milestoneAchievementTemplate = (Map<String,Object>) request.getRequest().get(JsonKeys.MILESTONE_ACHIEVEMENT);
            String uuid = (String) request.getRequest().get(JsonKeys.UUID);
            boolean isEvent = (boolean) request.getRequest().get(JsonKeys.IS_EVENT);
            CertificateExtension certificateExtension = (CertificateExtension) request.getRequest().get(JsonKeys.MILESTONE_ACHIEVEMENT_EXTENSION);
            CertModel certModel = (CertModel) request.getRequest().get(JsonKeys.CERT_MODEL);
            String accessCode = (String) request.getRequest().get(JsonKeys.ACCESS_CODE);
            Date userCompletedOn = (Date)request.getRequest().get(JsonKeys.COMPLETED_ON);
            Object rawList =
                    request.getRequest().get(JsonKeys.USER_MILESTONE_ACHIEVEMENT_LIST);

            List<Map<String, Object>> issuedMilestoneAchievementList;

            if (rawList instanceof List) {
                issuedMilestoneAchievementList =
                        new ArrayList<>((List<Map<String, Object>>) rawList);
            } else {
                issuedMilestoneAchievementList = new ArrayList<>();
            }
            Map<String, Object> courseRelatedInfo = new HashMap<>();
            courseRelatedInfo.put(JsonKeys.COURSE_ID, courseId);
            courseRelatedInfo.put(JsonKeys.BATCH_ID, batchId);
            courseRelatedInfo.put(JsonKeys.TYPE, milestoneAchievementTemplate.get(JsonKeys.NAME));
            Map<String,Object> milestoneAchievementRegistryResponse = addMilestoneAchievementToRegistry(uuid, certificateExtension, certModel, courseRelatedInfo, accessCode);
            if (MapUtils.isNotEmpty(milestoneAchievementRegistryResponse)) {
                Map<String, Object> milestoneAchievementMap = new HashMap<>();
                milestoneAchievementMap.put(JsonKeys.IDENTIFIER, uuid);
                if (CollectionUtils.isNotEmpty(issuedMilestoneAchievementList)) {
                    Map<String, String> milestoneAchievementValues = issuedMilestoneAchievementList.stream()
                            .filter(milestoneAchievement -> !milestoneAchievement.containsKey(JsonKeys.VERSION))
                            .findFirst()
                            .map(milestoneAchievement -> {
                                Map<String, String> values = new HashMap<>();
                                values.put(JsonKeys.LAST_ISSUED_ON,
                                        (String) milestoneAchievement.getOrDefault(JsonKeys.LAST_ISSUED_ON, formatter.format(userCompletedOn)));
                                values.put(JsonKeys.EVENT_ISSUE_NAME, (String) milestoneAchievement.get(JsonKeys.EVENT_ISSUE_NAME));
                                return values;
                            })
                            .orElseGet(() -> {
                                Map<String, String> values = new HashMap<>();
                                values.put(JsonKeys.LAST_ISSUED_ON, formatter.format(userCompletedOn));
                                return values;
                            });

                    milestoneAchievementMap.put(JsonKeys.LAST_ISSUED_ON, milestoneAchievementValues.get(JsonKeys.LAST_ISSUED_ON));
                    milestoneAchievementMap.put(JsonKeys.DOWNLOADED_ON, formatter.format(new Date()));
                    Object eventIssueName = request.getRequest().get(JsonKeys.EVENT_ISSUE_NAME);
                    if (eventIssueName instanceof String && StringUtils.isNotBlank((String) eventIssueName)) {
                        logger.info("The Special Event MilestoneAchievement is present and value is::: " + milestoneAchievementValues.get(JsonKeys.EVENT_ISSUE_NAME) + " for old MilestoneAchievements.");
                        milestoneAchievementMap.put(JsonKeys.EVENT_ISSUE_NAME, request.getRequest().get(JsonKeys.EVENT_ISSUE_NAME));
                    }

                } else {
                    milestoneAchievementMap.put(JsonKeys.LAST_ISSUED_ON, formatter.format(new Date()));
                }

                milestoneAchievementMap.put(JsonKeys.TOKEN, accessCode);
                milestoneAchievementMap.put(JsonKeys.NAME, milestoneAchievementTemplate.get(JsonKeys.NAME));
                milestoneAchievementMap.put(JsonKeys.VERSION, JsonKeys.VERSION_2);
                issuedMilestoneAchievementList.add(milestoneAchievementMap);
                insertUserMilestoneAchievementRecord(userId, courseId, batchId, milestoneId, issuedMilestoneAchievementList, isEvent, userCompletedOn);
            } else {
                logger.error("Issue while adding the registry for request for userId: " + userId + " courseId: " + courseId + " batchId: " + batchId);
            }
        } catch (Exception ex) {
            logger.error("Issue while adding the registry for request for userId: " + userId + " courseId: " + courseId + " batchId: " + batchId , ex);
        }


    }

    public Response insertUserMilestoneAchievementRecord(String userId, String courseId, String batchId, String milestoneId, List<Map<String, Object>> issuedMilestoneAchievements, boolean isEvent, Date userCompletedOn) throws BaseException {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(JsonKeys.ISSUED_MILESTONE_ACHIEVEMENTS, issuedMilestoneAchievements);
        if (isEvent) {
            userEnrolmentHelper.updateUserEventEnrollmentRecord(courseId, batchId, userId, attributeMap);
        } else {
            userEnrolmentHelper.insertUserMilestoneAchievements(courseId, batchId, userId, milestoneId, userCompletedOn ,attributeMap);
        }
        return null;
    }

    public Map<String, Object> addMilestoneAchievementToRegistry(
            String uuid,
            CertificateExtension certificateExtension,
            CertModel certModel,
            Map<String, Object> courseRelatedInfo,
            String accessCode) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> certificateData =
                    objectMapper.convertValue(certificateExtension, Map.class);

            CertificateV2 certificate =
                    buildCertificateV2(
                            uuid,
                            certificateData,
                            certModel,
                            courseRelatedInfo,
                            accessCode
                    );

            Response response = persistMilestoneDirectly(certificate);

            return response.getResult();

        } catch (Exception e) {
            logger.error("Issue while persisting milestone achievement", e);
        }

        return null;
    }

    public Response persistMilestoneDirectly(CertificateV2 certificate) throws BaseException {

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> certMap =
                objectMapper.convertValue(certificate, Map.class);

        long now = System.currentTimeMillis();

        try {
            String id = String.valueOf(certMap.get(JsonKeys.ID));
            String accessCode = String.valueOf(certMap.get(JsonKeys.ACCESS_CODE));
            String reason = String.valueOf(certMap.get(JsonKeys.REASON));
            Boolean isRevoked = (Boolean) certMap.getOrDefault("revoked", false);

            certMap.clear();

            certMap.put("id", id);
            certMap.put("accesscode", accessCode);
            certMap.put("reason", reason);
            certMap.put("isrevoked", isRevoked);
            certMap.put("createdat", new Timestamp(now));
            certMap.put("updatedat", null);
            certMap.put("createdby", null);
            certMap.put("updatedby", null);

            Map<String, Object> data =
                    objectMapper.convertValue(certificate.getData(), Map.class);

            if (data != null) {
                data.remove(JsonKeys.PRINT_URI);
            }

            certMap.put("data", objectMapper.writeValueAsString(data));
            certMap.put("recipient",
                    objectMapper.writeValueAsString(certificate.getRecipient()));
            certMap.put("related",
                    objectMapper.writeValueAsString(certificate.getRelated()));

        } catch (Exception e) {
            throw new BaseException(
                    "INVALID_REQUESTED_DATA",
                    "Invalid certificate data for milestone registry",
                    ResponseCode.CLIENT_ERROR.getCode()
            );
        }

        return userEnrolmentHelper.insertMilestoneAchievementRegistry(certMap);
    }



    private CertificateV2 buildCertificateV2(
            String uuid,
            Map<String, Object> certificateData,
            CertModel certModel,
            Map<String, Object> related,
            String accessCode) {

        Recipient recipient = new Recipient();
        Recipient.Builder recipientBuilder = new Recipient.Builder();
        recipientBuilder.setName(certModel.getRecipientName());
        recipientBuilder.setId(certModel.getIdentifier());

        return new CertificateV2.Builder()
                .setId(uuid)
                .setData(certificateData)
                .setRevoked(false)
                .setAccessCode(accessCode)
                .setRecipient(recipient)
                .setRelated(related)
                .setReason(JsonKeys.MILESTONE_COMPLETION)
                .build();
    }
}

