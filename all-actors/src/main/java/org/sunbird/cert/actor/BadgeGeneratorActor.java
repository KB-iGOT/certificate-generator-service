package org.sunbird.cert.actor;

import akka.actor.ActorRef;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.sunbird.*;
import org.sunbird.cert.helper.CertRegistryHelper;
import org.sunbird.cert.helper.IssueCertificateContentHelper;
import org.sunbird.cert.helper.IssueMilestoneAchievementContentHelper;
import org.sunbird.cert.helper.UserEnrolmentHelper;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import org.sunbird.incredible.CertificateGenerator;
import org.sunbird.incredible.pojos.CertificateExtension;
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
import java.util.*;

public class BadgeGeneratorActor extends BaseActor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BadgeGeneratorActor.class);

    private static final String OPERATION = "GENERATE_BADGE";
    private static final String BG_OPERATION = "ADD_BADGE_REGISTRY";
    private static final String PRINT_URI = "printUri";
    private static final String directory = "conf/";

    private static CertsConstant certVar = new CertsConstant();
    private final CertRegistryHelper certRegistryHelper = CertRegistryHelper.getInstance();
    private final UserEnrolmentHelper enrolmentHelper = UserEnrolmentHelper.getInstance();
    private final PropertiesCache propertiesCache = PropertiesCache.getInstance();
    private BaseStorageService storageService = null;
    private static final IssueCertificateContentHelper issueCertificateContentHelper = IssueCertificateContentHelper.getInstance();
    private static final IssueMilestoneAchievementContentHelper issueMilestoneAchievementContentHelper = IssueMilestoneAchievementContentHelper.getInstance();

    @Inject
    @Named("badge_background_actor")
    private ActorRef badgeBackgroundActorRef;

    @Override
    public void onReceive(Request request) throws Throwable {

        if (OPERATION.equalsIgnoreCase(request.getOperation())) {
            handleRequest(request);
            return;
        }

        throw new BaseException(JsonKeys.INVALID_OPERATION, JsonKeys.UNSUPPORTED_OPERATION, 400);
    }

    private void handleRequest(Request request) throws Exception {

        Map<String, Object> req = request.getRequest();

        String userId = get(req, JsonKeys.USER_ID);
        String courseId = get(req, JsonKeys.COURSE_ID);
        String badgeId = get(req, JsonKeys.BADGE_ID);

        validate(userId, courseId, badgeId);

        if (!isEligible(userId, courseId, badgeId)) {
            throw new BaseException(JsonKeys.NOT_ELIGIBLE, JsonKeys.USER_NOT_ELIGIBLE, 400);
        }

        Map<String, Object> contentInfo =
                issueCertificateContentHelper.getCourseInfo(courseId);

        Map<String, Object> badgeData =
                getBadgeFromContent(contentInfo, badgeId);

        injectBadgeIntoRequest(request, badgeData);

        Map<String, Object> existing =
                certRegistryHelper.getMilestoneAchievementRegistryUsingIdentifier(badgeId);

        if (MapUtils.isNotEmpty(existing)) {
            sendResponse(existing.get(PRINT_URI));
            return;
        }

        String svg = generateBadgePrintURIAndUpdateRecord(request, badgeData);

        sendResponse(svg);
    }

    private Map<String, Object> getBadgeFromContent(
            Map<String, Object> content,
            String badgeId) {

        List<Map<String, Object>> badgeList =
                (List<Map<String, Object>>) content.get(JsonKeys.BADGE_DETAILS_V1);

        if (CollectionUtils.isEmpty(badgeList)) {
            throw new RuntimeException("No badgeDetailsV1 found in content");
        }

        return badgeList.stream()
                .filter(b -> badgeId.equalsIgnoreCase((String) b.get(JsonKeys.BADGE_ID)))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Badge not found for id: " + badgeId));
    }

    private void injectBadgeIntoRequest(Request request, Map<String, Object> badgeData) {

        Map<String, Object> badge = new HashMap<>();

        badge.put(JsonKey.NAME, badgeData.get(JsonKey.BADGETITLE));
        badge.put(JsonKey.SIGNATORY_IMAGE, badgeData.get(JsonKey.BADGETEMPLATE));

        request.getRequest().put(JsonKey.BADGE, badge);
    }

    private boolean isEligible(String userId, String courseId, String badgeId) throws BaseException {

        Response userEnrolmentRecord;
        boolean isExternal = courseId != null && courseId.startsWith("ext_");

        if (isExternal) {
            userEnrolmentRecord =
                    enrolmentHelper.getUserExternalEnrollmentRecord(courseId, userId);
        } else {
            userEnrolmentRecord =
                    enrolmentHelper.getUserEnrollmentRecord(courseId, null, userId);
        }

        if (userEnrolmentRecord != null) {

            List<Map<String, Object>> mapList =
                    (List<Map<String, Object>>) userEnrolmentRecord.get(JsonKeys.RESPONSE);

            if (CollectionUtils.isNotEmpty(mapList)) {

                Map<String, Object> map = mapList.get(0);

                if (!isExternal) {
                    boolean active = (boolean) map.getOrDefault(JsonKeys.ACTIVE, false);
                    if (!active) {
                        return false;
                    }
                }

                List<Map<String, String>> issuedBadges =
                        (List<Map<String, String>>) map.getOrDefault(
                                JsonKeys.ISSUED_BADGES,
                                new ArrayList<>());

                return !issuedBadges.isEmpty() && issuedBadges.stream().anyMatch(badge -> badgeId.equalsIgnoreCase(badge.getOrDefault(JsonKeys.FIELD_BADGE_ID, "")));
            }
        }
        return false;
    }

    private void sendResponse(Object svg) {
        Response res = new Response();
        res.getResult().put(PRINT_URI, svg);
        sender().tell(res, getSelf());
    }

    private String get(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) throw new IllegalArgumentException("Missing: " + key);
        return val.toString();
    }

    private void validate(String... values) {
        for (String v : values) {
            if (StringUtils.isBlank(v)) {
                throw new IllegalArgumentException("Invalid input");
            }
        }
    }

    private String generateBadgePrintURIAndUpdateRecord(Request request, Map<String, Object> badgeData) throws BaseException {

        String uuid = "";
        String encodedSvg = "";

        try {
            String badgeTemplateId = (String) request.getRequest().get(JsonKeys.FIELD_BADGE_ID);
            Response response = enrolmentHelper.fetchTemplate(JsonKeys.BADGE_CERT_TEMPLATE);
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) response.get(JsonKeys.RESPONSE);
            String svgTemplate =  null;
            Map<String, Object> valueMap = null;
            if (CollectionUtils.isNotEmpty(mapList)) {
                java.util.Map<String, Object> map = mapList.get(0);
                String value = (String) map.get(JsonKeys.VALUE);
                ObjectMapper objectMapper = new ObjectMapper();
                 valueMap = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
                svgTemplate = (String) valueMap.get(JsonKeys.PREVIEW_URL);
            }

            if (StringUtils.isBlank(svgTemplate)) {
                throw new BaseException(JsonKeys.TEMPLATE_NOT_FOUND,
                        "Badge template not found for id: " + badgeTemplateId,
                        ResponseCode.CLIENT_ERROR.getCode());
            }
            request.getRequest().put(JsonKeys.IS_BADGE, true);
            Map<String, Object> metadata = issueMilestoneAchievementContentHelper.generateMilestoneAchievementMap(request.getRequest(), valueMap);
            metadata.put(JsonKey.SVG_TEMPLATE, svgTemplate);
            request.put(JsonKeys.MILESTONE_ACHIEVEMENT, metadata);
            Map<String, String> properties = populatePropertiesMap(request);

            CertMapper certMapper = new CertMapper(properties);
            List<CertModel> certModelList =
                    certMapper.toMilestoneAchievementList(request.getRequest());

            CertificateGenerator certificateGenerator =
                    new CertificateGenerator(properties, directory);

            CertStoreFactory certStoreFactory = new CertStoreFactory(properties);

            StoreConfig storeParams = new StoreConfig(
                    getStorageParamsFromRequestOrEnv(
                            (Map<String, Object>) ((Map) request.get(JsonKeys.MILESTONE_ACHIEVEMENT))
                                    .get(JsonKey.STORE)));

            ICertStore certStore = certStoreFactory.getCertStore(
                    storeParams,
                    BooleanUtils.toBoolean(properties.get(JsonKey.PREVIEW))
            );

            for (CertModel certModel : certModelList) {
                try {

                    CertificateExtension certificateExtension =
                            certificateGenerator.getCertificateExtension(certModel, uuid);

                    if (StringUtils.isBlank(uuid)) {
                        uuid = certificateGenerator.getUUID(certificateExtension);
                    }

                    SvgGenerator svgGenerator = new SvgGenerator(
                            (String) ((Map) request.get(JsonKeys.MILESTONE_ACHIEVEMENT))
                                    .get(JsonKey.SVG_TEMPLATE),
                            directory
                    );

                    encodedSvg = svgGenerator.generate(
                            certificateExtension,
                            null,
                            getStorageService()
                    );

                    certificateExtension.setPrintUri(encodedSvg);

                    Request bgRequest = new Request();
                    RequestParams params = new RequestParams();
                    params.setMsgid(MDC.get(JsonKeys.REQUEST_MESSAGE_ID));

                    bgRequest.setParams(params);

                    request.getRequest().put(JsonKeys.MILESTONE_ACHIEVEMENT_EXTENSION, certificateExtension);
                    request.getRequest().put(JsonKeys.UUID, uuid);
                    request.getRequest().put(JsonKeys.PRINT_URI, encodedSvg);
                    request.getRequest().put(JsonKeys.CERT_MODEL, certModel);

                    bgRequest.setRequest(request.getRequest());
                    bgRequest.setOperation(JsonKeys.ADD_BADGE_REGISTRY);

                    badgeBackgroundActorRef.tell(bgRequest, ActorRef.noSender());

                    return encodedSvg;

                } catch (Exception ex) {
                    logger.error("Badge generation error", ex);
                    throw new BaseException(
                            IResponseMessage.INTERNAL_ERROR,
                            ex.getMessage(),
                            ResponseCode.SERVER_ERROR.getCode()
                    );
                } finally {
                    certStore.close();
                    try {
                        certStoreFactory.cleanUp(uuid, directory);
                    } catch (Exception ex) {
                        logger.error("Cleanup error", ex);
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("generateBadgePrintURIAndUpdateRecord failed", ex);
            throw new BaseException(
                    IResponseMessage.INTERNAL_ERROR,
                    ex.getMessage(),
                    ResponseCode.SERVER_ERROR.getCode()
            );
        }

        return null;
    }

    private BaseStorageService getStorageService() {
        if(storageService == null) {
            StorageConfig storageConfig = null;
            String cloudStorageType = "gcloud";
            CertsConstant certVar = new CertsConstant();
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

    private HashMap<String, String> populatePropertiesMap(Request request) {
        HashMap<String, String> properties = new HashMap<>();
        String tag = (String) ((Map) request.get(JsonKeys.MILESTONE_ACHIEVEMENT)).get(JsonKey.TAG);
        String preview = (String) ((Map<String, Object>) request.getRequest().get(JsonKeys.MILESTONE_ACHIEVEMENT)).get(JsonKey.PREVIEW);
        Map<String, Object> keysObject = (Map<String, Object>) ((Map) request.get(JsonKeys.MILESTONE_ACHIEVEMENT)).get(JsonKey.KEYS);
        certVar.setBasePath((String) ((Map<String, Object>) request.getRequest().get(JsonKeys.MILESTONE_ACHIEVEMENT))
                .get(JsonKey.BASE_PATH));
        Map<String, Object> milestone =
                (Map<String, Object>) request.get(JsonKeys.MILESTONE_ACHIEVEMENT);

        String badgeName = (String) milestone.get(JsonKey.badgeName);
        String badgeImage = (String) milestone.get(JsonKey.badgeImage);
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
        properties.put(JsonKey.badgeName, badgeName);
        properties.put(JsonKey.badgeImage, badgeImage);

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
}