package org.sunbird.cert.actor;

import akka.actor.ActorRef;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.sunbird.*;
import org.sunbird.auth.AccessTokenValidator;
import org.sunbird.cache.platform.Platform;
import org.sunbird.cert.actor.operation.MilestoneAchievementActorOperation;
import org.sunbird.cert.helper.CertRegistryHelper;
import org.sunbird.cert.helper.IssueMilestoneAchievementContentHelper;
import org.sunbird.cert.helper.Recipient;
import org.sunbird.cert.helper.UserEnrolmentHelper;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import org.sunbird.incredible.CertificateGenerator;
import org.sunbird.incredible.pojos.CertificateExtension;
import org.sunbird.incredible.pojos.ob.BadgeClass;
import org.sunbird.incredible.processor.CertModel;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.incredible.processor.store.CertStoreFactory;
import org.sunbird.incredible.processor.store.ICertStore;
import org.sunbird.incredible.processor.store.StoreConfig;
import org.sunbird.incredible.processor.views.SvgGenerator;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;
import org.sunbird.request.RequestParams;
import org.sunbird.response.Response;
import scala.Option;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import static org.sunbird.cert.helper.IssueCertificateExternalContentHelper.getAPICall;

/**
 * This actor is responsible for milestone achievement generation.
 *
 * @author system
 */
public class MilestoneAchievementGeneratorActor extends BaseActor {
    private static CertsConstant certVar = new CertsConstant();
    private static ObjectMapper mapper = new ObjectMapper();
    private BaseStorageService storageService = null;
    String directory = "conf/";
    private static final IssueMilestoneAchievementContentHelper issueMilestoneAchievementContentHelper = IssueMilestoneAchievementContentHelper.getInstance();
    private static final CertRegistryHelper certRegistryHelper = CertRegistryHelper.getInstance();
    private static final UserEnrolmentHelper userEnrolmentHelper = UserEnrolmentHelper.getInstance();
    private static final PropertiesCache propertiesCache = PropertiesCache.getInstance();
    private static final int eventCacheTTL = Platform.getInteger("special.event.milestone.achievement.cache.ttl", 24);
    private static Map<String, String> specialEventMilestoneAchievementTemplateMap;
    private static final Cache<LocalDate, Map<String, String>> todayMilestoneAchievementCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(eventCacheTTL)).build();
    private static final Logger log = LoggerFactory.getLogger(CertificateGenerator.class);

    @Inject
    @Named("milestone_achievement_background_actor")
    private ActorRef milestoneAchievementBackgroundActorRef;

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        log.info("MilestoneAchievementGeneratorActor initialized.");
        try {
            String json = Platform.getString(JsonKeys.SPECIAL_MILESTONE_ACHIEVEMENT_TEMPLATE_MAP, "");
            if (StringUtils.isBlank(json)) {
                specialEventMilestoneAchievementTemplateMap = Collections.emptyMap();
            } else {
                specialEventMilestoneAchievementTemplateMap = mapper.readValue(
                        json,
                        new TypeReference<Map<String, String>>() {}
                );
            }
            getMilestoneAchievementForToday();
        } catch (Exception e) {
            log.error("exception while getting the specialEventMilestoneAchievementTemplateMap" , e.getMessage());
            specialEventMilestoneAchievementTemplateMap = Collections.emptyMap();
        }
    }

    @Override
    public void onReceive(Request request) throws Throwable {
        String operation = request.getOperation();
        logger.info("onReceive method call start for operation {}", operation);
        if (MilestoneAchievementActorOperation.GENERATE_MILESTONE_ACHIEVEMENT.getOperation().equalsIgnoreCase(operation)) {
            generateMilestoneAchievement(request);
        } else if (MilestoneAchievementActorOperation.MILESTONE_ACHIEVEMENT_DOWNLOAD.getOperation().equals(operation)) {
            handlePublicMilestoneDownload(request);
        } else {
            logger.error("MilestoneAchievementGeneratorActor:onReceive: Invalid operation request: {}", operation);
            throw new BaseException(IResponseMessage.INVALID_OPERATION_NAME, "Invalid operation request: " + operation, ResponseCode.CLIENT_ERROR.getCode());
        }
    }


    private BaseStorageService getStorageService() {
        if(storageService == null) {
            StorageConfig storageConfig = null;
            java.lang.String cloudStorageType = "gcloud";
            if (cloudStorageType.equalsIgnoreCase(certVar.getAzureStorage())) {
                storageConfig = new StorageConfig(certVar.getCloudStorageType(), certVar.getAzureStorageKey(), certVar.getAzureStorageSecret(), Option.apply(null), Option.empty());
            } else if (cloudStorageType.equalsIgnoreCase(certVar.getAwsStorage())) {
                storageConfig = new StorageConfig(certVar.getCloudStorageType(), certVar.getAwsStorageKey(), certVar.getAwsStorageSecret(), Option.apply(null), Option.empty());
            } else if (cloudStorageType.equalsIgnoreCase(certVar.getCephs3Storage())) {
                storageConfig = new StorageConfig(certVar.getCloudStorageType(), certVar.getCephs3StorageKey(), certVar.getCephs3StorageSecret(), Option.apply(certVar.getCephs3StorageEndPoint()), Option.empty());
            } else if (cloudStorageType.equalsIgnoreCase(certVar.getGCPStorage())) {
                storageConfig = new StorageConfig(certVar.getCloudStorageType(), certVar.getGCPStorageKey(), certVar.getGCPStorageSecret(), Option.apply(certVar.getGCPStorageEndPoint()), Option.empty());
            } else
                try {
                    throw new BaseException(IResponseMessage.INTERNAL_ERROR, "Error while initialising cloud storage", ResponseCode.SERVER_ERROR.getCode());
                } catch (BaseException e) {
                    logger.error("Error while initialising cloud storage. : {}", e.getMessage());
                }
            logger.info("MilestoneAchievementGeneratorActor:getStorageService:storage object formed: {}", storageConfig.toString());
            storageService = StorageServiceFactory.getStorageService(storageConfig);
        }
        return storageService;
    }

    private void generateMilestoneAchievement(Request request) throws BaseException {
        try {
            logger.info("generateMilestoneAchievement request received== {}", request.getRequest());
            String courseId = (String) request.getRequest().get(JsonKeys.COURSE_ID);
            String batchId = (String) request.getRequest().get(JsonKeys.BATCH_ID);
            String userId = (String) request.getRequest().get(JsonKeys.USER_ID);
            Boolean isAdmin = (Boolean) request.getRequest().get(JsonKeys.IS_ADMIN);
            String incomingMilestoneId = (String) request.getRequest().get(JsonKeys.MILESTONE_ID);
            if (isAdmin == null) {
                validateUserToken(request, userId);
            }
            Map<String, Object> contentInfo = issueMilestoneAchievementContentHelper.getCourseInfo(courseId);
            boolean isUserEligibleForMilestoneAchievement = true;
            Map<String, Object> milestoneAchievementRegistryMap = new HashMap<>();
            List<Map<String, Object>> milestoneAchievementList = new ArrayList<>();
            Date userCompletedOn = null;
            if (MapUtils.isEmpty(contentInfo)) {
                logger.error("generateMilestoneAchievementV2:Exception Occurred while generating milestone achievement. Issue while fetching the content");
                throw new BaseException(IResponseMessage.INTERNAL_ERROR, "Issue while fetching the content", ResponseCode.SERVER_ERROR.getCode());
            } else {
                org.sunbird.response.Response lpEnrollmentRecord = userEnrolmentHelper.getUserEnrollmentRecord(courseId, batchId, userId);
                Response userMilestoneAchievementsRecord = userEnrolmentHelper.getUserMilestoneAchievements(courseId, batchId, userId, incomingMilestoneId);
                if (issueMilestoneAchievementContentHelper.isUserEligibleForContentMilestoneAchievement(lpEnrollmentRecord, userId, contentInfo, incomingMilestoneId)) {
                    milestoneAchievementList = userEnrolmentHelper.getUserMilestoneAchievements(userMilestoneAchievementsRecord);
                    userCompletedOn = userEnrolmentHelper.getCompletionTimeIfPassed(userId, getAssessmentIdFromMilestone((List<Map<String, Object>>) contentInfo.getOrDefault("milestones_v1", Collections.emptyList()), incomingMilestoneId));
                    if (userCompletedOn == null)
                        isUserEligibleForMilestoneAchievement = false;
                    if (CollectionUtils.isNotEmpty(milestoneAchievementList)) {
                        milestoneAchievementRegistryMap = getMilestoneAchievementRegistryMap(milestoneAchievementList);
                        logger.debug("The milestoneAchievementList is: " + mapper.writeValueAsString(milestoneAchievementList));
                    }
                } else {
                    isUserEligibleForMilestoneAchievement = false;
                }
                if (isUserEligibleForMilestoneAchievement) {
                    String encodedSvg = generatePrintURIAndUpdateRecord(request, milestoneAchievementRegistryMap, milestoneAchievementList, userCompletedOn, contentInfo);
                    if (StringUtils.isNotBlank(encodedSvg)) {
                        Response response = new Response();
                        response.getResult().put(JsonKeys.PRINT_URI, encodedSvg);
                        sender().tell(response, getSelf());
                    } else {
                        logger.error("generateMilestoneAchievementV2:Exception Occurred while generating milestone achievement. svg content is empty");
                        throw new BaseException(IResponseMessage.INTERNAL_ERROR, "svg Content is Empty", ResponseCode.SERVER_ERROR.getCode());
                    }
                } else {
                    logger.error("generateMilestoneAchievementV2:Exception Occurred while generating milestone achievement. user is not eligible for milestone achievement");
                    throw new BaseException(IResponseMessage.INTERNAL_ERROR, "user is not eligible for milestone achievement", ResponseCode.SERVER_ERROR.getCode());
                }
            }

        } catch (Exception ex) {
            logger.error("generateMilestoneAchievementV2:Exception Occurred while generating milestone achievement. : {}", ex.getStackTrace());
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, ex.getMessage(), ResponseCode.SERVER_ERROR.getCode());
        }
        logger.info("onReceive method call End");
    }

    private String generatePrintURIAndUpdateRecord(Request request, Map<String, Object> v2MilestoneAchievementRegistryMap, List<Map<String, Object>> issuedMilestoneAchievementList, Date userCompletedOn, Map<String, Object> contentInfo) throws BaseException {
        try {
            Response templateResponse = null;
            templateResponse = issueMilestoneAchievementContentHelper.fetchContentTemplate(contentInfo, (String) request.getRequest().get(JsonKeys.MILESTONE_ID));

            if (templateResponse != null) {
                Map<String, Object> milestoneAchievementTemplate = getMilestoneAchievementMetaData(request, templateResponse.getResult());
                request.put(JsonKeys.MILESTONE_ACHIEVEMENT, milestoneAchievementTemplate);
                Map<String, String> properties = populatePropertiesMap(request);
                CertMapper certMapper = new CertMapper(properties);
                List<CertModel> certModelList = certMapper.toMilestoneAchievementList(request.getRequest());
                CertificateGenerator certificateGenerator = new CertificateGenerator(properties, directory);
                CertStoreFactory certStoreFactory = new CertStoreFactory(properties);
                StoreConfig storeParams = new StoreConfig(getStorageParamsFromRequestOrEnv((Map<String, Object>) ((Map) request.get(JsonKeys.MILESTONE_ACHIEVEMENT)).get(JsonKey.STORE)));
                ICertStore certStore = certStoreFactory.getCertStore(storeParams, BooleanUtils.toBoolean(properties.get(JsonKey.PREVIEW)));
                String uuid = "";
                String encodedSvg = "";
                for (CertModel certModel : certModelList) {
                    try {
                        if (MapUtils.isNotEmpty(v2MilestoneAchievementRegistryMap)) {
                            uuid = (String) v2MilestoneAchievementRegistryMap.get(JsonKeys.ID);
                        }
                        CertificateExtension certificateExtension = certificateGenerator.getCertificateExtension(certModel, uuid);
                        if (StringUtils.isBlank(uuid)) {
                            uuid = certificateGenerator.getUUID(certificateExtension);
                        }
                        Map<String, Object> qrMap = new HashMap<>();
                        if (MapUtils.isNotEmpty(v2MilestoneAchievementRegistryMap)) {
                            qrMap = certificateGenerator.generateQrCodeFromAccessCode((String) v2MilestoneAchievementRegistryMap.get(JsonKeys.ACCESS_CODE));
                        } else {
                            String milestoneAchievementAccessCodeV1 = issuedMilestoneAchievementList.stream()
                                    .filter(milestoneAchievement -> !milestoneAchievement.containsKey(JsonKeys.VERSION)) // keep only those with the key
                                    .map(milestoneAchievement -> (String) milestoneAchievement.get(JsonKeys.TOKEN))
                                    .filter(Objects::nonNull)
                                    .findFirst()
                                    .orElse(null);
                            if (StringUtils.isNotBlank(milestoneAchievementAccessCodeV1)) {
                                qrMap = certificateGenerator.generateQrCodeFromAccessCode(milestoneAchievementAccessCodeV1);
                            } else {
                                qrMap = certificateGenerator.generateQrCode();
                            }
                        }
                        String encodedQrCode = encodeQrCodeBytes((byte[]) qrMap.get(JsonKey.QR_CODE_FILE));
                        String specialEventMilestoneAchievementName = null;
                        if (CollectionUtils.isNotEmpty(issuedMilestoneAchievementList)) {
                            specialEventMilestoneAchievementName = issuedMilestoneAchievementList.stream()
                                    .filter(milestoneAchievement -> milestoneAchievement.containsKey(JsonKeys.EVENT_ISSUE_NAME)) // keep only those with the key
                                    .map(milestoneAchievement -> (String) milestoneAchievement.get(JsonKeys.EVENT_ISSUE_NAME))
                                    .filter(Objects::nonNull)
                                    .findFirst()
                                    .orElse(null);

                        } else {
                            String userId = (String) request.getRequest().get(JsonKeys.USER_ID);
                            Map<String,String> specialEventMilestoneAchievementDetails = getMilestoneAchievementTemplate(userId);
                            if (MapUtils.isNotEmpty(specialEventMilestoneAchievementDetails)) {
                                specialEventMilestoneAchievementName = specialEventMilestoneAchievementDetails.get(JsonKeys.SPECIAL_EVENT_NAME);
                            }
                        }

                        if (StringUtils.isNotBlank(specialEventMilestoneAchievementName)) {
                            if (MapUtils.isNotEmpty(specialEventMilestoneAchievementTemplateMap)) {
                                logger.info("The size for specialEvent MilestoneAchievement is: " + specialEventMilestoneAchievementTemplateMap.size());
                                String svgTemplate = specialEventMilestoneAchievementTemplateMap.get(specialEventMilestoneAchievementName);
                                logger.info("The svg template is: " + svgTemplate);
                                ((Map) request.get(JsonKey.CERTIFICATE)).put(JsonKey.SVG_TEMPLATE, svgTemplate);
                            }
                        }

                        SvgGenerator svgGenerator = new SvgGenerator((String) ((Map) request.get(JsonKeys.MILESTONE_ACHIEVEMENT)).get(JsonKey.SVG_TEMPLATE), directory);
                        encodedSvg = svgGenerator.generate(certificateExtension, encodedQrCode, getStorageService());
                        if (MapUtils.isEmpty(v2MilestoneAchievementRegistryMap)) {
                            certificateExtension.setPrintUri(encodedSvg);
                            Request req = new Request();
                            RequestParams params = new RequestParams();
                            params.setMsgid(MDC.get(JsonKeys.REQUEST_MESSAGE_ID));
                            req.setParams(params);
                            request.getRequest().put(JsonKeys.MILESTONE_ACHIEVEMENT_EXTENSION, certificateExtension);
                            request.getRequest().put(JsonKeys.UUID, uuid);
                            request.getRequest().put(JsonKeys.ACCESS_CODE, qrMap.get(JsonKeys.ACCESS_CODE));
                            request.getRequest().put(JsonKeys.MILESTONE_ACHIEVEMENT, milestoneAchievementTemplate);
                            request.getRequest().put(JsonKeys.CERT_MODEL, certModel);
                            request.getRequest().put(JsonKeys.COMPLETED_ON, userCompletedOn);
                            if (StringUtils.isNotBlank(specialEventMilestoneAchievementName)) {
                                request.getRequest().put(JsonKeys.EVENT_ISSUE_NAME, specialEventMilestoneAchievementName);
                            }
                            request.getRequest().put(JsonKeys.USER_MILESTONE_ACHIEVEMENT_LIST, issuedMilestoneAchievementList);
                            request.getRequest().putAll(req.getRequest());
                            request.setOperation(JsonKeys.ADD_MILESTONE_ACHIEVEMENT_REGISTRY_REQUEST);
                            milestoneAchievementBackgroundActorRef.tell(request, ActorRef.noSender());
                        }
                        return encodedSvg;
                    } catch (Exception ex) {
                        logger.error("generateMilestoneAchievementV2:Exception Occurred while generating milestone achievement. : {}", ex.getStackTrace());
                        throw new BaseException(IResponseMessage.INTERNAL_ERROR, ex.getMessage(), ResponseCode.SERVER_ERROR.getCode());
                    } finally {
                        certStore.close();
                        try {
                            certStoreFactory.cleanUp(uuid, directory);
                        } catch (Exception ex) {
                            logger.error("Exception occurred during resource clean");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("generateMilestoneAchievementV2:Exception Occurred while generating milestone achievement. : {}", ex.getStackTrace());
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, ex.getMessage(), ResponseCode.SERVER_ERROR.getCode());
        }
        return null;
    }

    private String encodeQrCodeBytes(byte[] fileContent) throws IOException {
        return Base64.getEncoder().encodeToString(fileContent);
    }

    private HashMap<String, String> populatePropertiesMap(Request request) {
        HashMap<String, String> properties = new HashMap<>();
        String tag = (String) ((Map) request.get(JsonKeys.MILESTONE_ACHIEVEMENT)).get(JsonKey.TAG);
        String preview = (String) ((Map<String, Object>) request.getRequest().get(JsonKeys.MILESTONE_ACHIEVEMENT)).get(JsonKey.PREVIEW);
        Map<String, Object> keysObject = (Map<String, Object>) ((Map) request.get(JsonKeys.MILESTONE_ACHIEVEMENT)).get(JsonKey.KEYS);
        certVar.setBasePath((String) ((Map<String, Object>) request.getRequest().get(JsonKeys.MILESTONE_ACHIEVEMENT))
                .get(JsonKey.BASE_PATH));
        if (MapUtils.isNotEmpty(keysObject)) {
            String keyId = (String) keysObject.get(JsonKey.ID);
            properties.put(JsonKey.KEY_ID, keyId);
            properties.put(JsonKey.SIGN_CREATOR, certVar.getSignCreator(keyId));
            properties.put(JsonKey.PUBLIC_KEY_URL, certVar.getPUBLIC_KEY_URL(keyId));
            logger.info("populatePropertiesMap: keys after {}", keyId);
        }
        properties.put(JsonKey.TAG, tag);
        properties.put(JsonKey.CONTAINER_NAME, certVar.getCONTAINER_NAME());
        properties.put(JsonKey.BADGE_URL, certVar.getBADGE_URL(tag));
        properties.put(JsonKey.ISSUER_URL, certVar.getISSUER_URL());
        properties.put(JsonKey.EVIDENCE_URL, certVar.getEVIDENCE_URL());
        properties.put(JsonKey.CONTEXT, certVar.getCONTEXT());
        properties.put(JsonKey.VERIFICATION_TYPE, certVar.getVERIFICATION_TYPE());
        properties.put(JsonKey.ACCESS_CODE_LENGTH, certVar.getACCESS_CODE_LENGTH());
        properties.put(JsonKey.SIGN_URL, certVar.getEncSignUrl());
        properties.put(JsonKey.SIGN_VERIFY_URL, certVar.getEncSignVerifyUrl());
        properties.put(JsonKey.ENC_SERVICE_URL, certVar.getEncryptionServiceUrl());
        properties.put(JsonKey.SIGNATORY_EXTENSION, certVar.getSignatoryExtensionUrl());
        properties.put(JsonKey.SLUG, certVar.getSlug());
        properties.put(JsonKey.PREVIEW, certVar.getPreview(preview));
        properties.put(JsonKey.BASE_PATH, certVar.getBasePath());

        logger.debug("getProperties:properties got from Constant File ".concat(Collections.singleton(properties.toString()) + ""));
        return properties;
    }

    private Map<String, Object> getStorageParamsFromRequestOrEnv(Map<String, Object> storeParams) {
        if (MapUtils.isNotEmpty(storeParams)) {
            return storeParams;
        } else {
            return certVar.getStorageParamsFromEvn();
        }
    }

    public Map<String, Object> getMilestoneAchievementMetaData(Request request, Map<String, Object> template) throws JsonProcessingException {
        List<Map<String, Object>> templateResponse = (List<Map<String, Object>>) template.get(JsonKey.RESPONSE);
        String valueJson = (String) templateResponse.get(0).get("value");
        Map<String, Object> templateResponseKey =
                mapper.readValue(valueJson, new TypeReference<Map<String, Object>>() {});
        return issueMilestoneAchievementContentHelper.generateMilestoneAchievementMap(request.getRequest(), templateResponseKey);
    }

    public Map<String, Object> getMilestoneAchievementRegistryMap(List<Map<String, Object>> userMilestoneAchievementsList) throws BaseException {
            if (CollectionUtils.isNotEmpty(userMilestoneAchievementsList)) {
                Map<String, Object> v2MilestoneAchievements = userMilestoneAchievementsList.stream().filter(m -> m.get(JsonKeys.VERSION) != null && JsonKeys.VERSION_2.equalsIgnoreCase((String)m.get(JsonKeys.VERSION))).findFirst().orElse(null);
                if (MapUtils.isNotEmpty(v2MilestoneAchievements)) {
                    String identifier = (String) v2MilestoneAchievements.get(JsonKeys.IDENTIFIER);
                    Map<String, Object> v2MilestoneAchievementRegistry = certRegistryHelper.getMilestoneAchievementRegistryUsingIdentifier(identifier);
                    if (MapUtils.isNotEmpty(v2MilestoneAchievementRegistry)) {
                        return v2MilestoneAchievementRegistry;
                    }
                }
        }
        return null;
    }

    private static Map<String, String> getMilestoneAchievementForToday() {
        LocalDate today = LocalDate.now();
        return todayMilestoneAchievementCache.get(today, date -> {
            String json = propertiesCache.getProperty(JsonKeys.SPECIAL_EVENT_DETAILS_MAP);
            if (StringUtils.isBlank(json)) {
                return Collections.emptyMap();
            }
            Map<String, Object> eventMap = null;
            try {
                eventMap = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.error("exception while getting the milestoneAchievementForToday" , e.getMessage());
            }
            for (Map.Entry<String, Object> entry : eventMap.entrySet()) {
                String key = entry.getKey();
                if (key.contains("#")) {
                    String[] parts = key.split("#");
                    LocalDate startDate = LocalDate.parse(parts[0]);
                    LocalDate endDate = LocalDate.parse(parts[1]);

                    if ((date.isEqual(startDate) || date.isAfter(startDate)) &&
                            (date.isEqual(endDate) || date.isBefore(endDate))) {

                        Object value = entry.getValue();
                        if (value instanceof Map) {
                            return (Map<String, String>) value;
                        } else {
                            log.error("getMilestoneAchievementForToday Method: Not the proper type for value object object.");
                        }
                    }
                }
            }
            return Collections.emptyMap();
        });
    }

    private Map<String, String> getMilestoneAchievementTemplate(String userId) {
        Map<String, String> milestoneAchievementTemplateMap = new HashMap<>();
        Map<String, String> milestoneAchievementForToday = getMilestoneAchievementForToday();
        if (MapUtils.isNotEmpty(milestoneAchievementForToday)) {
            if (StringUtils.isNotBlank(milestoneAchievementForToday.get(JsonKeys.L0_ORG_ID))) {
                logger.info("The event is for the L0OrgId: " + milestoneAchievementForToday.get(JsonKeys.L0_ORG_ID));
                if (isUserValidForMdoSpecialEvent(userId, milestoneAchievementForToday.get(JsonKeys.L0_ORG_ID))) {
                    milestoneAchievementTemplateMap.put(JsonKeys.SPECIAL_EVENT_NAME, milestoneAchievementForToday.get(JsonKeys.SPECIAL_EVENT_NAME));
                    milestoneAchievementTemplateMap.put(JsonKeys.MILESTONE_ACHIEVEMENT_TEMPLATE, specialEventMilestoneAchievementTemplateMap.get(milestoneAchievementForToday.get(JsonKeys.SPECIAL_EVENT_NAME)));
                }
            } else if (StringUtils.isNotBlank(milestoneAchievementForToday.get(JsonKeys.SPECIAL_EVENT_NAME))) {
                milestoneAchievementTemplateMap.put(JsonKeys.SPECIAL_EVENT_NAME, milestoneAchievementForToday.get(JsonKeys.SPECIAL_EVENT_NAME));
                milestoneAchievementTemplateMap.put(JsonKeys.MILESTONE_ACHIEVEMENT_TEMPLATE, specialEventMilestoneAchievementTemplateMap.get(milestoneAchievementForToday.get(JsonKeys.SPECIAL_EVENT_NAME)));
            }
        }
        return milestoneAchievementTemplateMap;
    }

    private boolean isUserValidForMdoSpecialEvent(String userId, String L0OrgId) {
        try {
            String userReadUrl = propertiesCache.getProperty("learner_basePath") + propertiesCache.getProperty("user_read_api") + "/" + userId + "?organisations,roles,locations,declarations,externalIds,rootOrgId";

            Map<String, Object> result = getAPICall(userReadUrl);

            if (MapUtils.isNotEmpty(result)) {
                Map<String, Object> resultObject = (Map<String, Object>) result.get(JsonKeys.RESULT);
                Map<String, Object> responseObject = (Map<String, Object>) resultObject.get(JsonKeys.RESPONSE);
                if (MapUtils.isNotEmpty(responseObject)) {
                    String userOrgId = (String)responseObject.get("rootOrgId");
                    if (L0OrgId.equalsIgnoreCase(userOrgId)) {
                        return true;
                    } else {
                        String ministryOrStateId = getMinistryOrStateId(userOrgId);
                        if (StringUtils.isNotBlank(ministryOrStateId) && L0OrgId.equalsIgnoreCase(ministryOrStateId)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Issue while validating the isUserValid for Mdo Special Event.");
        }
        return false;
    }

    private String getMinistryOrStateId(String userOrgId) {
        try {
            String orgReadUrl = propertiesCache.getProperty("learner_basePath") + propertiesCache.getProperty("org_read_api");

            Map<String, Object> result = getPostAPICall(orgReadUrl, userOrgId);

            if (MapUtils.isNotEmpty(result)) {
                Map<String, Object> resultObject = (Map<String, Object>) result.get(JsonKeys.RESULT);
                Map<String, Object> responseObject = (Map<String, Object>) resultObject.get(JsonKeys.RESPONSE);
                if (MapUtils.isNotEmpty(responseObject)) {
                    Object ministryOrStateId = responseObject.get(JsonKeys.MINISTRY_OR_STATE_ID);
                    if (ObjectUtils.isNotEmpty(ministryOrStateId)) {
                        log.info("The ministry or state Id for which special Event occurring is : " + ministryOrStateId);
                        return (String) ministryOrStateId;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Issue while validating the isUserValid for Mdo Special Event.");
        }
        return null;
    }

    public static Map<String, Object> getPostAPICall(
            String url, String userOrgId) throws Exception {
        Map<String, String> defaultHeader = new HashMap<>();
        defaultHeader.put("Content-Type", "application/json");
        Map<String, Object> requestData = new HashMap<>();
        Map<String, String> organisationData = new HashMap<>();
        organisationData.put(JsonKeys.ORGANISATION, userOrgId);
        requestData.put(JsonKey.REQUEST, organisationData);
        String response = HttpUtil.sendPostRequest(url, mapper.writeValueAsString(requestData), defaultHeader);
        Map<String, Object> data = mapper.readValue(response, Map.class);
        if (MapUtils.isNotEmpty(data)) {
           return data;
        } else {
            throw new RuntimeException("Error from get API: " + url + ", with response: " + response);
        }
    }

    private String getAssessmentIdFromMilestone(
            List<Map<String, Object>> milestonesList, String incomingMilestoneId) {

        if (CollectionUtils.isEmpty(milestonesList)) {
            return null;
        }

        Map<String, Object> milestone = milestonesList.stream()
                .filter(m -> incomingMilestoneId.equalsIgnoreCase((String) m.get("id")))
                .findFirst()
                .orElse(null);
        return Optional.ofNullable(
                        (Map<String, Object>) milestone.get("assessmentDetail"))
                .map(ad -> ad.get("identifier"))
                .map(Object::toString)
                .orElse(null);


    }

    private void validateUserToken(Request request, String userId) throws BaseException {
        List<String> userToken = scala.collection.JavaConverters.seqAsJavaList(
                (scala.collection.Seq<String>) request.getHeaders().get(JsonKeys.X_AUTHENTICATED_USER_TOKEN)
        );

        if (CollectionUtils.isEmpty(userToken)) {
            userToken = scala.collection.JavaConverters.seqAsJavaList(
                    (scala.collection.Seq<String>) request.getHeaders().get(JsonKeys.X_AUTHENTICATED_USER_TOKEN_CAMEL_CASE)
            );
        }

        if (CollectionUtils.isEmpty(userToken)) {
            logger.error("generateMilestoneAchievementV2: Exception occurred. User token is not valid. Headers: " + request.getHeaders());
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, "Token is not proper", ResponseCode.BAD_REQUEST.getCode());
        }
        String userIdFromToken = AccessTokenValidator.verifyUserToken(userToken.get(0), true);
        logger.info("UserId from token:" + userIdFromToken);
        if (StringUtils.isEmpty(userIdFromToken)) {
            logger.error("generateMilestoneAchievementV2:Exception Occurred while generating milestone achievement. User token is not valid" + userId);
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, "Token is not proper", ResponseCode.BAD_REQUEST.getCode());
        }
        if (StringUtils.isNotEmpty(userIdFromToken) && !userId.equalsIgnoreCase(userIdFromToken)) {
            logger.error("generateMilestoneAchievementV2:Exception Occurred while generating milestone achievement. User token is different from the request UserId" + userId);
            throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, "You are not authorized to get the milestone achievement for other user", ResponseCode.BAD_REQUEST.getCode());
        }
    }

    private void handlePublicMilestoneDownload(Request request) {

        Response response = new Response();

        try {

            Map<String, Object> req = request.getRequest();
            String uuid = (String) req.get(JsonKey.UUID);

            if (StringUtils.isBlank(uuid)) {
                sendFailure(response, IResponseMessage.MANDATORY_PARAMETER_MISSING,
                        "uuid is required");
                return;
            }

            Map<String, Object> milestoneRecord =
                    certRegistryHelper.getMilestoneAchievementRegistryUsingIdentifier(uuid);

            if (MapUtils.isEmpty(milestoneRecord)) {
                sendFailure(response, IResponseMessage.INVALID_REQUESTED_DATA,
                        "Milestone record not found for uuid: " + uuid);
                return;
            }

            Map<String, Object> related;
            try {
                String relatedJson = (String) milestoneRecord.get(JsonKey.RELATED);
                related = mapper.readValue(
                        relatedJson,
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                sendFailure(response, IResponseMessage.INVALID_REQUESTED_DATA,
                        "Unable to parse related context");
                return;
            }

            String courseId = (String) related.get(JsonKey.COURSE_ID);
            String milestoneId = (String) related.get(JsonKeys.MILESTONE_ID);

            if (StringUtils.isBlank(courseId) || StringUtils.isBlank(milestoneId)) {
                sendFailure(response, IResponseMessage.INVALID_REQUESTED_DATA,
                        "courseId or milestoneId missing in related context");
                return;
            }

            Map<String, Object> contentInfo =
                    issueMilestoneAchievementContentHelper.getCourseInfo(courseId);

            if (MapUtils.isEmpty(contentInfo)) {
                sendFailure(response, IResponseMessage.INVALID_REQUESTED_DATA,
                        "Course content not found for courseId: " + courseId);
                return;
            }

            Response templateResponse =
                    issueMilestoneAchievementContentHelper.fetchContentTemplate(contentInfo, milestoneId);

            if (templateResponse == null || templateResponse.getResult() == null) {
                sendFailure(response, IResponseMessage.INVALID_REQUESTED_DATA,
                        "Template not found for milestoneId: " + milestoneId);
                return;
            }
            enrichRequestForTemplate(request, milestoneRecord, related);

            Map<String, Object> requestMap = buildRequestFromRegistry(milestoneRecord);

            Map<String, Object> milestoneAchievementTemplate =
                    getMilestoneAchievementMetaData(request, templateResponse.getResult());

            request.put(JsonKeys.MILESTONE_ACHIEVEMENT, milestoneAchievementTemplate);
            requestMap.put(JsonKeys.MILESTONE_ACHIEVEMENT, milestoneAchievementTemplate);

            Map<String, String> properties = populatePropertiesMap(request);

            CertMapper certMapper = new CertMapper(properties);
            List<CertModel> certModelList =
                    certMapper.toMilestoneAchievementList(requestMap);

            CertificateGenerator certificateGenerator =
                    new CertificateGenerator(properties, directory);

            CertModel certModel = certModelList.get(0);

            CertificateExtension certificateExtension =
                    certificateGenerator.getCertificateExtension(certModel, uuid);

            String accessCode = (String) milestoneRecord.get(JsonKeys.ACCESS_CODE);

            Map<String, Object> qrMap =
                    certificateGenerator.generateQrCodeFromAccessCode(accessCode);

            String encodedQrCode =
                    Base64.getEncoder().encodeToString(
                            (byte[]) qrMap.get(JsonKey.QR_CODE_FILE)
                    );

            SvgGenerator svgGenerator = new SvgGenerator(
                    (String) milestoneAchievementTemplate.get(JsonKey.SVG_TEMPLATE),
                    directory
            );

            String encodedSvg =
                    svgGenerator.generate(certificateExtension, encodedQrCode, getStorageService());

            response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
            response.put(JsonKeys.PRINT_URI, encodedSvg);

            sender().tell(response, self());

        } catch (Exception e) {
            logger.error("PublicMilestoneDownload | Unexpected error", e);
            sendFailure(response, IResponseMessage.INTERNAL_ERROR,
                    "Error while generating milestone certificate");
        }
    }


    private void sendFailure(Response response, String errorMessage, String logMessage) {
        logger.error(logMessage);
        response.put(JsonKey.RESPONSE, JsonKey.FAILED);
        response.put(JsonKey.ERROR, errorMessage);
        sender().tell(response, self());
    }

    private Map<String, Object> buildRequestFromRegistry(Map<String, Object> milestoneRecord) throws Exception {

        Map<String, Object> requestMap = new HashMap<>();

        String certJson = (String) milestoneRecord.get(JsonKey.DATA);

        Map<String, Object> certData = mapper.readValue(
                certJson,
                new TypeReference<Map<String, Object>>() {}
        );

        requestMap.putAll(certData);
        requestMap.put(JsonKeys.UUID, milestoneRecord.get(JsonKeys.ID));
        requestMap.put(JsonKeys.ACCESS_CODE, milestoneRecord.get("accesscode"));

        return requestMap;
    }


    private void enrichRequestForTemplate(
            Request request,
            Map<String, Object> milestoneRecord,
            Map<String, Object> related
    ) throws Exception {

        String courseId = (String) related.get(JsonKey.COURSE_ID);
        String batchId  = (String) related.get(JsonKeys.BATCH_ID);
        String milestoneId = (String) related.get(JsonKeys.MILESTONE_ID);

        String certJson = (String) milestoneRecord.get(JsonKey.DATA);

        Map<String, Object> certData = mapper.readValue(
                certJson,
                new TypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> recipient =
                (Map<String, Object>) certData.get(JsonKeys.RECIPIENT);

        String userId = (String) recipient.get(JsonKeys.IDENTITY);

        request.getRequest().put(JsonKeys.COURSE_ID, courseId);
        request.getRequest().put(JsonKeys.BATCH_ID, batchId);
        request.getRequest().put(JsonKeys.USER_ID, userId);
        request.getRequest().put(JsonKeys.MILESTONE_ID, milestoneId);
    }

}

