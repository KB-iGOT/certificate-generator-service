package org.sunbird.cert.actor;

import akka.actor.ActorRef;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.sunbird.*;
import org.sunbird.auth.AccessTokenValidator;
import org.sunbird.cache.platform.Platform;
import org.sunbird.cert.actor.operation.CertActorOperation;
import org.sunbird.cert.helper.*;
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
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

import static org.sunbird.cert.helper.IssueCertificateEventHelper.getAPICall;

/**
 * This actor is responsible for certificate generation.
 *
 * @author manzarul
 */
public class CertificateGeneratorActor extends BaseActor {
    private static CertsConstant certVar = new CertsConstant();
    private static ObjectMapper mapper = new ObjectMapper();
    private BaseStorageService storageService = null;
    String directory = "conf/";
    private static final IssueCertificateContentHelper issueCertificateContentHelper = IssueCertificateContentHelper.getInstance();
    private static final IssueCertificateEventHelper issueCertificateEventHelper = IssueCertificateEventHelper.getInstance();
    private static final CertRegistryHelper certRegistryHelper = CertRegistryHelper.getInstance();
    private static final UserEnrolmentHelper userEnrolmentHelper = UserEnrolmentHelper.getInstance();
    private static final PropertiesCache propertiesCache = PropertiesCache.getInstance();
    private static final int eventCacheTTL = Platform.getInteger("special.event.cache.ttl", 24);
    private static Map<String, String> specialEventCertificateTemplateMap;
    private static final Cache<LocalDate, Map<String, String>> todayCertificateCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(eventCacheTTL)).build();
    private static final Logger log = LoggerFactory.getLogger(CertificateGenerator.class);
    private static final IssueCertificateExternalContentHelper issueCertificateExternalContentHelper = IssueCertificateExternalContentHelper.getInstance();

    @Inject
    @Named("certificate_background_actor")
    private ActorRef certBackgroundActorRef;

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        log.info("CertificateGeneratorActor initialized.");
        try {
            String json = Platform.getString(JsonKeys.SPECIAL_CERTIFICATE_TEMPLATE_MAP, "");
            if (StringUtils.isBlank(json)) {
                specialEventCertificateTemplateMap = Collections.emptyMap();
            } else {
                specialEventCertificateTemplateMap = mapper.readValue(
                        json,
                        new TypeReference<Map<String, String>>() {}
                );
            }
            getCertificateForToday();
        } catch (Exception e) {
            log.error("exception while getting the specialEventCertificateTemplateMap" , e.getMessage());
            specialEventCertificateTemplateMap = Collections.emptyMap();
        }
    }

    @Override
    public void onReceive(Request request) throws Throwable {
        String operation = request.getOperation();
        logger.info("onReceive method call start for operation {}", operation);
        if (CertActorOperation.GENERATE_CERTIFICATE.getOperation().equalsIgnoreCase(operation)) {
            generateCertificate(request);
        } else if (CertActorOperation.GENERATE_CERTIFICATES_LEGACY_APP.getOperation().equalsIgnoreCase(operation)) {
            generateCertificateLegacyApp(request);
        } else if (CertActorOperation.GENERATE_CERTIFICATE_ADMIN.getOperation().equalsIgnoreCase(operation)) {
            generateCertificateByAdmin(request);
        }
        logger.info("onReceive method call End");
    }

    private BaseStorageService getStorageService() {
        if(storageService == null) {
            StorageConfig storageConfig = null;
            if (certVar.getCloudStorageType().equalsIgnoreCase(certVar.getAzureStorage())) {
                storageConfig = new StorageConfig(certVar.getCloudStorageType(), certVar.getAzureStorageKey(), certVar.getAzureStorageSecret(), Option.apply(null), Option.empty());
            } else if (certVar.getCloudStorageType().equalsIgnoreCase(certVar.getAwsStorage())) {
                storageConfig = new StorageConfig(certVar.getCloudStorageType(), certVar.getAwsStorageKey(), certVar.getAwsStorageSecret(), Option.apply(null), Option.empty());
            } else if (certVar.getCloudStorageType().equalsIgnoreCase(certVar.getCephs3Storage())) {
                storageConfig = new StorageConfig(certVar.getCloudStorageType(), certVar.getCephs3StorageKey(), certVar.getCephs3StorageSecret(), Option.apply(certVar.getCephs3StorageEndPoint()), Option.empty());
            } else if (certVar.getCloudStorageType().equalsIgnoreCase(certVar.getGCPStorage())) {
                storageConfig = new StorageConfig(certVar.getCloudStorageType(), certVar.getGCPStorageKey(), certVar.getGCPStorageSecret(), Option.apply(certVar.getGCPStorageEndPoint()), Option.empty());
            } else
                try {
                    throw new BaseException(IResponseMessage.INTERNAL_ERROR, "Error while initialising cloud storage", ResponseCode.SERVER_ERROR.getCode());
                } catch (BaseException e) {
                    logger.error("Error while initialising cloud storage. : {}", e.getMessage());
                }
            logger.info("CertificateGeneratorActor:getStorageService:storage object formed: {}", storageConfig.toString());
            storageService = StorageServiceFactory.getStorageService(storageConfig);
        }
        return storageService;
    }

    private void generateCertificate(Request request) throws BaseException {
        try {
            logger.info("generateCertificate request received== {}", request.getRequest());
            String courseId = (String) request.getRequest().get(JsonKeys.COURSE_ID);
            String batchId = (String) request.getRequest().get(JsonKeys.BATCH_ID);
            String userId = (String) request.getRequest().get(JsonKeys.USER_ID);
            Boolean isAdmin = (Boolean) request.getRequest().get(JsonKeys.IS_ADMIN);
            if (isAdmin == null) {
                List<String> userToken = scala.collection.JavaConverters.seqAsJavaList(
                        (scala.collection.Seq<String>) request.getHeaders().get(JsonKeys.X_AUTHENTICATED_USER_TOKEN)
                );

                if (CollectionUtils.isEmpty(userToken)) {
                    userToken = scala.collection.JavaConverters.seqAsJavaList(
                            (scala.collection.Seq<String>) request.getHeaders().get(JsonKeys.X_AUTHENTICATED_USER_TOKEN_CAMEL_CASE)
                    );
                }

                if (CollectionUtils.isEmpty(userToken)) {
                    logger.error("generateCertificateV2: Exception occurred. User token is not valid. Headers: " + request.getHeaders());
                    throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, "Token is not proper", ResponseCode.BAD_REQUEST.getCode());
                }
                String userIdFromToken = AccessTokenValidator.verifyUserToken(userToken.get(0), true);
                logger.info("UserId from token:" + userIdFromToken);
                if (StringUtils.isEmpty(userIdFromToken)) {
                    logger.error("generateCertificateV2:Exception Occurred while generating certificate. User token is not valid" + userId);
                    throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, "Token is not proper", ResponseCode.BAD_REQUEST.getCode());
                }
                if (StringUtils.isNotEmpty(userIdFromToken) && !userId.equalsIgnoreCase(userIdFromToken)) {
                    logger.error("generateCertificateV2:Exception Occurred while generating certificate. User token is different from the request UserId" + userId);
                    throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, "You are not authorized to get the certificate for other user", ResponseCode.BAD_REQUEST.getCode());
                }
            }
            boolean isUserEligibleForCertificate = true;
            boolean isEvent = false;
            Map<String, Object> certificateRegistryMap = new HashMap<>();
            List<Map<String, Object>> certificateList = new ArrayList<>();
            Date userCompletedOn = null;
            Map<String, Object> contentInfo;
            boolean isExternalCourse = false;
            if (courseId.contains(JsonKey.EXT_PREFIX)) {
                contentInfo = issueCertificateExternalContentHelper.getExternalCourseInfo(courseId);
                isExternalCourse = true;
            } else {
                contentInfo = issueCertificateContentHelper.getCourseInfo(courseId);
            }
            if (MapUtils.isNotEmpty(contentInfo)) {
                if (isExternalCourse) {
                    Response userExternalContentEnrolmentRecord = userEnrolmentHelper.getUserEnrollmentRecordForExternalContent(courseId, userId);
                    if (issueCertificateExternalContentHelper.isUserEligibleForExternalContentCertificate(userExternalContentEnrolmentRecord)) {
                        certificateList = issueCertificateExternalContentHelper.getUserCertificates(userExternalContentEnrolmentRecord);
                        userCompletedOn = issueCertificateExternalContentHelper.getCompletedOnDate(userExternalContentEnrolmentRecord);
                        if (CollectionUtils.isNotEmpty(certificateList)) {
                            certificateRegistryMap = getCertificateRegistryMap(certificateList);
                        }
                    } else {
                        isUserEligibleForCertificate = false;
                    }
                } else if (JsonKeys.EVENT.equalsIgnoreCase((String) contentInfo.get(JsonKeys.CONTENT_TYPE))) {
                    isEvent = true;
                    Response userEventEnrolmentRecord = userEnrolmentHelper.getUserEventEnrollmentRecord(courseId, batchId, userId);
                    if (issueCertificateEventHelper.isUserEligibleForEventCertificate(userEventEnrolmentRecord)) {
                        certificateList = issueCertificateEventHelper.getUserCertificates(userEventEnrolmentRecord);
                        userCompletedOn = issueCertificateEventHelper.getCompletedOnDate(userEventEnrolmentRecord);
                        if (CollectionUtils.isNotEmpty(certificateList)) {
                            certificateRegistryMap = getCertificateRegistryMap(certificateList);
                        }
                    } else {
                        isUserEligibleForCertificate = false;
                    }
                } else {
                    Response userEnrolmentRecord = userEnrolmentHelper.getUserEnrollmentRecord(courseId, batchId, userId);
                    if (issueCertificateContentHelper.isUserEligibleForContentCertificate(userEnrolmentRecord)) {
                        certificateList = issueCertificateEventHelper.getUserCertificates(userEnrolmentRecord);
                        userCompletedOn = issueCertificateEventHelper.getCompletedOnDate(userEnrolmentRecord);
                        if (CollectionUtils.isNotEmpty(certificateList)) {
                            certificateRegistryMap = getCertificateRegistryMap(certificateList);
                            logger.debug("The certificationList is: " + mapper.writeValueAsString(certificateList));
                        }
                    } else {
                        isUserEligibleForCertificate = false;
                    }
                }
                if (isUserEligibleForCertificate) {
                    String encodedSvg = generatePrintURIAndUpdateRecord(courseId, batchId, request, isEvent, certificateRegistryMap, certificateList, userCompletedOn, isExternalCourse);
                    if (StringUtils.isNotBlank(encodedSvg)) {
                        Response response = new Response();
                        response.getResult().put(JsonKeys.PRINT_URI, encodedSvg);
                        sender().tell(response, getSelf());
                    } else {
                        logger.error("generateCertificateV2:Exception Occurred while generating certificate. svg content is empty");
                        throw new BaseException(IResponseMessage.INTERNAL_ERROR, "svg Content is Empty", ResponseCode.SERVER_ERROR.getCode());
                    }
                } else {
                    logger.error("generateCertificateV2:Exception Occurred while generating certificate. user is not eligible for certificate");
                    throw new BaseException(IResponseMessage.BAD_REQUEST, "user is not eligible for certificate", ResponseCode.BAD_REQUEST.getCode());
                }
            } else {
                logger.error("generateCertificateV2:Exception Occurred while generating certificate. Issue while fetching the content");
                throw new BaseException(IResponseMessage.INTERNAL_ERROR, "Issue while fetching the content", ResponseCode.SERVER_ERROR.getCode());
            }

        } catch (Exception ex) {
            logger.error("generateCertificateV2:Exception Occurred while generating certificate. : {}", ex.getStackTrace());
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, ex.getMessage(), ResponseCode.SERVER_ERROR.getCode());
        }
        logger.info("onReceive method call End");
    }

    private String generatePrintURIAndUpdateRecord(String courseId, String batchId, Request request, boolean isEvent, Map<String, Object> v2CertificateRegistryMap, List<Map<String, Object>> issuedCertificateList, Date userCompletedOn, Boolean isExternalCourse) throws BaseException {
        try {
            Response templateResponse = null;
            if (!isExternalCourse) {
                if (isEvent) {
                    templateResponse = issueCertificateEventHelper.fetchEventTemplate(courseId, batchId);
                } else {
                    templateResponse = issueCertificateContentHelper.fetchContentTemplate(courseId, batchId);
                }
            }

            if (templateResponse != null || isExternalCourse) {
                Map<String, Object> certificateTemplate ;
                if (isExternalCourse) {
                    certificateTemplate = getCertificateMetaDataForExternalContent(request);
                } else {
                    certificateTemplate  = getCertificateMetaData(request, templateResponse.getResult(), isEvent);
                }

                request.put(JsonKeys.CERTIFICATE, certificateTemplate);
                Map<String, String> properties = populatePropertiesMap(request);
                CertMapper certMapper = new CertMapper(properties);
                List<CertModel> certModelList = certMapper.toList(request.getRequest());
                CertificateGenerator certificateGenerator = new CertificateGenerator(properties, directory);
                CertStoreFactory certStoreFactory = new CertStoreFactory(properties);
                StoreConfig storeParams = new StoreConfig(getStorageParamsFromRequestOrEnv((Map<String, Object>) ((Map) request.get(JsonKey.CERTIFICATE)).get(JsonKey.STORE)));
                ICertStore certStore = certStoreFactory.getCertStore(storeParams, BooleanUtils.toBoolean(properties.get(JsonKey.PREVIEW)));
                String uuid = "";
                String encodedSvg = "";
                for (CertModel certModel : certModelList) {
                    try {
                        if (MapUtils.isNotEmpty(v2CertificateRegistryMap)) {
                            uuid = (String) v2CertificateRegistryMap.get(JsonKeys.ID);
                        }
                        CertificateExtension certificateExtension = certificateGenerator.getCertificateExtension(certModel, uuid);
                        if (StringUtils.isBlank(uuid)) {
                            uuid = certificateGenerator.getUUID(certificateExtension);
                        }
                        Map<String, Object> qrMap = new HashMap<>();
                        if (MapUtils.isNotEmpty(v2CertificateRegistryMap)) {
                            qrMap = certificateGenerator.generateQrCodeFromAccessCode((String) v2CertificateRegistryMap.get(JsonKeys.ACCESS_CODE));
                        } else {
                            String certificateAccessCodeV1 = issuedCertificateList.stream()
                                    .filter(cert -> !cert.containsKey(JsonKeys.VERSION)) // keep only those with the key
                                    .map(cert -> (String) cert.get(JsonKeys.TOKEN))
                                    .filter(Objects::nonNull)
                                    .findFirst()
                                    .orElse(null);
                            if (StringUtils.isNotBlank(certificateAccessCodeV1)) {
                                qrMap = certificateGenerator.generateQrCodeFromAccessCode(certificateAccessCodeV1);
                            } else {
                                qrMap = certificateGenerator.generateQrCode();
                            }
                        }
                        String encodedQrCode = encodeQrCodeBytes((byte[]) qrMap.get(JsonKey.QR_CODE_FILE));
                        String specialEventCertificateName = null;
                        if (CollectionUtils.isNotEmpty(issuedCertificateList)) {
                            specialEventCertificateName = issuedCertificateList.stream()
                                    .filter(cert -> cert.containsKey(JsonKeys.EVENT_ISSUE_NAME)) // keep only those with the key
                                    .map(cert -> (String) cert.get(JsonKeys.EVENT_ISSUE_NAME))
                                    .filter(Objects::nonNull)
                                    .findFirst()
                                    .orElse(null);

                        } else {
                            String userId = (String) request.getRequest().get(JsonKeys.USER_ID);
                            Map<String,String> specialEventCertificateDetails = getCertificateTemplate(userId);
                            if (MapUtils.isNotEmpty(specialEventCertificateDetails)) {
                                specialEventCertificateName = specialEventCertificateDetails.get(JsonKeys.SPECIAL_EVENT_NAME);
                            }
                        }

                        if (StringUtils.isNotBlank(specialEventCertificateName) && !isExternalCourse) {
                            if (MapUtils.isNotEmpty(specialEventCertificateTemplateMap)) {
                                logger.info("The size for specialEvent Certificate is: " + specialEventCertificateTemplateMap.size());
                                String svgTemplate = specialEventCertificateTemplateMap.get(specialEventCertificateName);
                                logger.info("The svg template is: " + svgTemplate);
                                ((Map) request.get(JsonKey.CERTIFICATE)).put(JsonKey.SVG_TEMPLATE, svgTemplate);
                            }
                        }

                        SvgGenerator svgGenerator = new SvgGenerator((String) ((Map) request.get(JsonKey.CERTIFICATE)).get(JsonKey.SVG_TEMPLATE), directory);
                        encodedSvg = svgGenerator.generate(certificateExtension, encodedQrCode, getStorageService());
                        if (MapUtils.isEmpty(v2CertificateRegistryMap)) {
                            certificateExtension.setPrintUri(encodedSvg);
                            Request req = new Request();
                            RequestParams params = new RequestParams();
                            params.setMsgid(MDC.get(JsonKeys.REQUEST_MESSAGE_ID));
                            req.setParams(params);
                            request.getRequest().put(JsonKeys.CERTIFICATE_EXTENSION, certificateExtension);
                            request.getRequest().put(JsonKeys.UUID, uuid);
                            request.getRequest().put(JsonKeys.ACCESS_CODE, qrMap.get(JsonKeys.ACCESS_CODE));
                            request.getRequest().put(JsonKeys.CERTIFICATE, certificateTemplate);
                            request.getRequest().put(JsonKeys.CERT_MODEL, certModel);
                            request.getRequest().put(JsonKeys.IS_EVENT, isEvent);
                            request.getRequest().put(JsonKeys.COMPLETED_ON, userCompletedOn);
                            if (StringUtils.isNotBlank(specialEventCertificateName)) {
                                request.getRequest().put(JsonKeys.EVENT_ISSUE_NAME, specialEventCertificateName);
                            }
                            request.getRequest().put(JsonKeys.USER_CERTICATE_LIST, issuedCertificateList);
                            request.getRequest().putAll(req.getRequest());
                            request.setOperation(JsonKeys.ADD_REGISTRY_REQUEST);
                            certBackgroundActorRef.tell(request, ActorRef.noSender());
                        }
                        return encodedSvg;
                    } catch (Exception ex) {
                        logger.error("generateCertificateV2:Exception Occurred while generating certificate. : {}", ex.getStackTrace());
                        throw new BaseException(IResponseMessage.INTERNAL_ERROR, ex.getMessage(), ResponseCode.SERVER_ERROR.getCode());
                    } finally {
                        certStore.close();
                        try {
                            cleanImageIOTempFiles();
                            certStoreFactory.cleanUp(uuid, directory);
                        } catch (Exception ex) {
                            logger.error("Exception occurred during resource clean");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("generateCertificateV2:Exception Occurred while generating certificate. : {}", ex.getStackTrace());
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, ex.getMessage(), ResponseCode.SERVER_ERROR.getCode());
        }
        return null;
    }

    private String encodeQrCode(File file) throws IOException {
        byte[] fileContent = FileUtils.readFileToByteArray(file);
        file.delete();
        return Base64.getEncoder().encodeToString(fileContent);
    }

    private String encodeQrCodeBytes(byte[] fileContent) throws IOException {
        return Base64.getEncoder().encodeToString(fileContent);
    }

    private HashMap<String, String> populatePropertiesMap(Request request) {
        HashMap<String, String> properties = new HashMap<>();
        String tag = (String) ((Map) request.get(JsonKey.CERTIFICATE)).get(JsonKey.TAG);
        String preview = (String) ((Map<String, Object>) request.getRequest().get(JsonKey.CERTIFICATE)).get(JsonKey.PREVIEW);
        Map<String, Object> keysObject = (Map<String, Object>) ((Map) request.get(JsonKey.CERTIFICATE)).get(JsonKey.KEYS);
        certVar.setBasePath((String) ((Map<String, Object>) request.getRequest().get(JsonKey.CERTIFICATE))
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

    private void cleanup(String path, String fileName) {
        try {
            File directory = new File(path);
            File[] files = directory.listFiles();
            for (File file : files) {
                if (file.getName().startsWith(fileName)) file.delete();
            }
            logger.info("CertificateGeneratorActor: cleanUp completed");
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public Map<String, Object> getCertificateMetaData(Request request, Map<String, Object> template, boolean isEvent) {
        List<Map<String, Object>> templateResponse = (List<Map<String, Object>>) template.get(JsonKey.RESPONSE);
        Map<String, Object> templateResponseKey = (Map<String, Object>) templateResponse.get(0).get(JsonKeys.CERT_TEMPLATES);
        String onlyKey = templateResponseKey.keySet().iterator().next();

        // Get the value associated with that key
        Map<String, Object> value = (Map<String, Object>) templateResponseKey.get(onlyKey);
        if (isEvent) {
            return issueCertificateEventHelper.generateCertificateMap(request.getRequest(), value);
        }
        return issueCertificateContentHelper.generateCertificateMap(request.getRequest(), value);
    }

    public Map<String, Object> getCertificateRegistryMap(List<Map<String, Object>> userCertificatesList) throws BaseException {
            if (CollectionUtils.isNotEmpty(userCertificatesList)) {
                Map<String, Object> v2Certificates = userCertificatesList.stream().filter(m -> m.get(JsonKeys.VERSION) != null && JsonKeys.VERSION_2.equalsIgnoreCase((String)m.get(JsonKeys.VERSION))).findFirst().orElse(null);
                if (MapUtils.isNotEmpty(v2Certificates)) {
                    String identifier = (String) v2Certificates.get(JsonKeys.IDENTIFIER);
                    Map<String, Object> v2CertificateRegistry = certRegistryHelper.getCertificateRegistryUsingIdentifier(identifier);
                    if (MapUtils.isNotEmpty(v2CertificateRegistry)) {
                        return v2CertificateRegistry;
                    }
                }
        }
        return null;
    }

    private static Map<String, String> getCertificateForToday() {
        LocalDate today = LocalDate.now();
        return todayCertificateCache.get(today, date -> {
            String json = propertiesCache.getProperty(JsonKeys.SPECIAL_EVENT_DETAILS_MAP);
            if (StringUtils.isBlank(json)) {
                return Collections.emptyMap();
            }
            Map<String, Object> eventMap = null;
            try {
                eventMap = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.error("exception while getting the certificateForToday" , e.getMessage());
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
                            log.error("getCertificateForToday Method: Not the proper type for value object object.");
                        }
                    }
                }
            }
            return Collections.emptyMap();
        });
    }

    private Map<String, String> getCertificateTemplate(String userId) {
        Map<String, String> certifiateTemplateMap = new HashMap<>();
        Map<String, String> certificateForToday = getCertificateForToday();
        if (MapUtils.isNotEmpty(certificateForToday)) {
            if (StringUtils.isNotBlank(certificateForToday.get(JsonKeys.L0_ORG_ID))) {
                logger.info("The event is for the L0OrgId: " + certificateForToday.get(JsonKeys.L0_ORG_ID));
                if (isUserValidForMdoSpecialEvent(userId, certificateForToday.get(JsonKeys.L0_ORG_ID))) {
                    certifiateTemplateMap.put(JsonKeys.SPECIAL_EVENT_NAME, certificateForToday.get(JsonKeys.SPECIAL_EVENT_NAME));
                    certifiateTemplateMap.put(JsonKeys.CERTIFICATE_TEMPLATE, specialEventCertificateTemplateMap.get(certificateForToday.get(JsonKeys.SPECIAL_EVENT_NAME)));
                }
            } else if (StringUtils.isNotBlank(certificateForToday.get(JsonKeys.SPECIAL_EVENT_NAME))) {
                certifiateTemplateMap.put(JsonKeys.SPECIAL_EVENT_NAME, certificateForToday.get(JsonKeys.SPECIAL_EVENT_NAME));
                certifiateTemplateMap.put(JsonKeys.CERTIFICATE_TEMPLATE, specialEventCertificateTemplateMap.get(certificateForToday.get(JsonKeys.SPECIAL_EVENT_NAME)));
            }
        }
        return certifiateTemplateMap;
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

    private void generateCertificateLegacyApp(Request request) throws BaseException {
        try {
            logger.info("generateCertificate request received== {}", request.getRequest());
            String identifier = (String) request.getRequest().get(JsonKeys.UID);
            if (StringUtils.isBlank(identifier)) {
                throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, JsonKeys.UID_BAD_REQUEST_ERROR_MSG ,ResponseCode.BAD_REQUEST.getCode());
            }
            Map<String, Object> certificateRegistry = certRegistryHelper.getCertificateRegistryUsingIdentifierV1(identifier);
            if (MapUtils.isEmpty(certificateRegistry)) {
                certificateRegistry = certRegistryHelper.getCertificateRegistryUsingIdentifier(identifier);
                if (MapUtils.isEmpty(certificateRegistry)) {
                    throw new BaseException(IResponseMessage.ERROR_GENERATING_CERTIFICATE, JsonKeys.UID_BAD_REQUEST_ERROR_MSG ,ResponseCode.BAD_REQUEST.getCode());
                }
            }
            Map<String, Object> userDetails = mapper.readValue((String)certificateRegistry.get(JsonKeys.RECIPIENT), Map.class);
            Map<String, Object> contentDetails = mapper.readValue((String)certificateRegistry.get(JsonKeys.RELATED), Map.class);
            if (MapUtils.isNotEmpty(userDetails) && MapUtils.isNotEmpty(contentDetails)) {
                String userId = (String) userDetails.get(JsonKeys.ID);
                String batchId = (String) contentDetails.get(JsonKeys.BATCH_ID);
                String courseId = StringUtils.isNotEmpty((String) contentDetails.get(JsonKeys.COURSE_ID)) ?  (String) contentDetails.get(JsonKeys.COURSE_ID) : (String) contentDetails.get(JsonKeys.EVENT_ID_CAMELCASE);
                request.getRequest().put(JsonKeys.COURSE_ID, courseId);
                request.getRequest().put(JsonKeys.BATCH_ID, batchId);
                request.getRequest().put(JsonKeys.USER_ID, userId);
                request.getRequest().put(JsonKeys.IS_ADMIN, true);
                generateCertificate(request);

            } else {
                logger.error("generateCertificateV2 Legacy App:Exception Occurred while generating certificate, facing issue to get the proper data.");
                throw new BaseException(IResponseMessage.INVALID_REQUESTED_DATA, JsonKeys.ISSUE_FETCHING_METADATA_ERROR_MSG, ResponseCode.SERVER_ERROR.getCode());
            }


        } catch (Exception ex) {
            logger.error("generateCertificateV2 Legacy App:Exception Occurred while generating certificate. : {}", ex.getStackTrace());
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, ex.getMessage(), ResponseCode.SERVER_ERROR.getCode());
        }
        logger.info("onReceive method call End");
    }

    private void generateCertificateByAdmin(Request request) throws BaseException {
        try {
            logger.info("generateCertificateByAdmin request received== {}", request.getRequest());
            request.getRequest().put(JsonKeys.IS_ADMIN, true);
            generateCertificate(request);
        } catch (Exception ex) {
            logger.error("generateCertificateByAdmin:Exception Occurred while generating certificate. : {}", ex.getStackTrace());
            throw new BaseException(IResponseMessage.INTERNAL_ERROR, ex.getMessage(), ResponseCode.SERVER_ERROR.getCode());
        }
        logger.info("onReceive method call End");
    }

    public Map<String, Object> getCertificateMetaDataForExternalContent(Request request) {
        return issueCertificateExternalContentHelper.generateCertificateMapForExternalContent(request.getRequest());
    }

    private void cleanImageIOTempFiles() {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File[] tempFiles = tmpDir.listFiles((dir, name) -> name.startsWith("+~JF") && name.endsWith(".tmp"));
        if (tempFiles != null) {
            for (File file : tempFiles) {
                try {
                    if (!file.delete()) {
                        logger.warn("Could not delete temp file (maybe in use): {}", file.getAbsolutePath());
                    }
                } catch (Exception ex) {
                }
            }
        }
    }
}
