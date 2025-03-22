package org.sunbird.cert.actor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.*;
import org.sunbird.auth.AccessTokenValidator;
import org.sunbird.cert.actor.operation.CertActorOperation;
import org.sunbird.cert.helper.CertRegistryHelper;
import org.sunbird.cert.helper.IssueCertificateContentHelper;
import org.sunbird.cert.helper.IssueCertificateEventHelper;
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
import org.sunbird.response.Response;
import scala.Option;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public void onReceive(Request request) throws Throwable {
        String operation = request.getOperation();
        logger.info("onReceive method call start for operation {}", operation);
        if (CertActorOperation.GENERATE_CERTIFICATE.getOperation().equalsIgnoreCase(operation)) {
            generateCertificate(request);
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
            List<String> userToken = (List<String>) request.getHeaders().get(JsonKeys.X_AUTHENTICATED_USER_TOKEN);
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
            Map<String, Object> contentInfo = issueCertificateContentHelper.getCourseInfo(courseId);
            boolean isUserEligibleForCertificate = true;
            boolean isEvent = false;
            Map<String, Object> certificateRegistryMap = new HashMap<>();
            List<Map<String, Object>> certificateList = new ArrayList<>();
            if (MapUtils.isNotEmpty(contentInfo)) {
                if (JsonKeys.EVENT.equalsIgnoreCase((String) contentInfo.get(JsonKeys.PRIMARY_CATEGORY))) {
                    isEvent = true;
                    Response userEventEnrolmentRecord = userEnrolmentHelper.getUserEventEnrollmentRecord(courseId, batchId, userId);
                    if (issueCertificateEventHelper.isUserEligibleForEventCertificate(userEventEnrolmentRecord)) {
                        List<Map<String, Object>> userCertificatesList = issueCertificateEventHelper.getUserCertificates(userEventEnrolmentRecord);
                        if (CollectionUtils.isNotEmpty(userCertificatesList)) {
                            certificateRegistryMap = getCertificateRegistryMap(userCertificatesList, certificateList);
                        }
                    } else {
                        isUserEligibleForCertificate = false;
                    }
                } else {
                    Response userEnrolmentRecord = userEnrolmentHelper.getUserEnrollmentRecord(courseId, batchId, userId);
                    if (issueCertificateContentHelper.isUserEligibleForContentCertificate(userEnrolmentRecord)) {
                        List<Map<String, Object>> userCertificatesList = issueCertificateEventHelper.getUserCertificates(userEnrolmentRecord);
                        if (CollectionUtils.isNotEmpty(userCertificatesList)) {
                            certificateRegistryMap = getCertificateRegistryMap(userCertificatesList, certificateList);
                        }
                    } else {
                        isUserEligibleForCertificate = false;
                    }
                }
                if (isUserEligibleForCertificate) {
                    String encodedSvg = generatePrintURIAndUpdateRecord(courseId, batchId, userId, request, isEvent, certificateRegistryMap, certificateList);
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
                    throw new BaseException(IResponseMessage.INTERNAL_ERROR, "user is not eligible for certificate", ResponseCode.SERVER_ERROR.getCode());
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

    private String generatePrintURIAndUpdateRecord(String courseId, String batchId, String userId, Request request, boolean isEvent, Map<String, Object> v2CertificateRegistryMap, List<Map<String, Object>> issuedCertificateList) throws BaseException {
        try {
            Response templateResponse = null;
            if (isEvent) {
                templateResponse = issueCertificateEventHelper.fetchEventTemplate(courseId, batchId);
            } else {
                templateResponse = issueCertificateContentHelper.fetchContentTemplate(courseId, batchId);
            }
            if (templateResponse != null) {
                Map<String, Object> certificateTemplate = getCertificateMetaData(request, templateResponse.getResult(), isEvent);
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
                        CertificateExtension certificateExtension = certificateGenerator.getCertificateExtension(certModel);
                        if (MapUtils.isNotEmpty(v2CertificateRegistryMap)) {
                            uuid = (String) v2CertificateRegistryMap.get(JsonKeys.ID);
                        } else {
                            uuid = certificateGenerator.getUUID(certificateExtension);
                        }
                        Map<String, Object> qrMap = new HashMap<>();
                        if (MapUtils.isNotEmpty(v2CertificateRegistryMap)) {
                            qrMap = certificateGenerator.generateQrCodeFromAccessCode((String) qrMap.get(JsonKeys.ACCESS_CODE));
                        } else {
                            qrMap = certificateGenerator.generateQrCode();
                        }
                        String encodedQrCode = encodeQrCode((File) qrMap.get(JsonKey.QR_CODE_FILE));
                        SvgGenerator svgGenerator = new SvgGenerator((String) ((Map) request.get(JsonKey.CERTIFICATE)).get(JsonKey.SVG_TEMPLATE), directory);
                        encodedSvg = svgGenerator.generate(certificateExtension, encodedQrCode, getStorageService());
                        if (MapUtils.isEmpty(v2CertificateRegistryMap)) {
                            certificateExtension.setPrintUri(encodedSvg);
                            Map<String, Object> courseRelatedInfo = new HashMap<>();
                            courseRelatedInfo.put(JsonKeys.COURSE_ID, courseId);
                            courseRelatedInfo.put(JsonKeys.BATCH_ID, batchId);
                            courseRelatedInfo.put(JsonKeys.TYPE, certificateTemplate.get(JsonKeys.NAME));
                            addCertificateToRegistry(uuid, certificateExtension, certModel, courseRelatedInfo, (String) qrMap.get(JsonKeys.ACCESS_CODE));
                            Map<String, Object> certificateMap = new HashMap<>();
                            certificateMap.put(JsonKeys.IDENTIFIER, uuid);
                            certificateMap.put(JsonKeys.LAST_ISSUED_ON, formatter.format(new Date()));
                            certificateMap.put(JsonKeys.TOKEN, qrMap.get(JsonKeys.ACCESS_CODE));
                            certificateMap.put(JsonKeys.NAME, certificateTemplate.get(JsonKeys.NAME));
                            certificateMap.put(JsonKeys.VERSION, JsonKeys.VERSION_2);
                            issuedCertificateList.add(certificateMap);
                            updateUserEnrolmentRecord(userId, courseId, batchId, issuedCertificateList, isEvent);
                        }
                        return encodedSvg;
                    } catch (Exception ex) {
                        logger.error("generateCertificateV2:Exception Occurred while generating certificate. : {}", ex.getStackTrace());
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

        logger.info("getProperties:properties got from Constant File ".concat(Collections.singleton(properties.toString()) + ""));
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

    public Map<String, Object> getCertName(Request request, Map<String, Object> template, boolean isEvent) {
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

    public void addCertificateToRegistry(String uuid, CertificateExtension certificateExtension, CertModel certModel, Map<String, Object> courseRelatedInfo, String accessCode) {
        try {
            Map<String, Object> requestMap = new HashMap<>();

            requestMap.put(JsonKeys.ID, uuid);
            requestMap.put(JsonKeys.JSON_DATA, certificateExtension);
            requestMap.put(JsonKeys.ACCESS_CODE, accessCode);
            requestMap.put(JsonKeys.RECIPIENT_NAME, certModel.getRecipientName());
            requestMap.put(JsonKeys.RECIPIENT_ID, certModel.getIdentifier());
            requestMap.put(JsonKeys.DYNAMIC_GENERATION, true);
            requestMap.put(JsonKeys.RELATED, courseRelatedInfo);

            Map<String, Object> addReq = new HashMap<>();
            addReq.put(JsonKeys.REQUEST, requestMap);

            certRegistryHelper.addCertToRegistry(addReq);
        } catch (Exception e) {
            logger.error("Issue while updating the registry");
        }
    }

    public Map<String, Object> getCertificateRegistryMap(List<Map<String, Object>> userCertificatesList, List<Map<String, Object>> certificationList) throws BaseException {
        Map<String, Object> userActiveEnrolment = userCertificatesList.stream().filter(m -> Boolean.TRUE.equals(m.get(JsonKeys.ACTIVE))).findFirst().orElse(null);
        if (MapUtils.isNotEmpty(userActiveEnrolment)) {
            List<Map<String, Object>> userCertificates = (List<Map<String, Object>>) userActiveEnrolment.get(JsonKeys.ISSUED_CERTIFICATES);
            if (CollectionUtils.isNotEmpty(userCertificates)) {
                certificationList.addAll(userCertificates);
                Map<String, Object> v2Certificates = userCertificates.stream().filter(m -> m.get(JsonKeys.VERSION) != null && m.get(JsonKeys.VERSION) == JsonKeys.VERSION_2).findFirst().orElse(null);
                if (MapUtils.isNotEmpty(v2Certificates)) {
                    String identifier = (String) v2Certificates.get(JsonKeys.IDENTIFIER);
                    Map<String, Object> v2CertificateRegistry = certRegistryHelper.getCertificateRegistryUsingIdentifier(identifier);
                    if (MapUtils.isNotEmpty(v2CertificateRegistry)) {
                        return v2CertificateRegistry;
                    }
                }
            }
        }
        return null;
    }

    public Response updateUserEnrolmentRecord(String userId, String courseId, String batchId, List<Map<String, Object>> issuedCertificates, boolean isEvent) throws BaseException {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(JsonKeys.ISSUED_CERTIFICATES, issuedCertificates);
        if (isEvent) {
            userEnrolmentHelper.updateUserEventEnrollmentRecord(courseId, batchId, userId, attributeMap);
        } else {
            userEnrolmentHelper.updateUserEnrollmentRecord(courseId, batchId, userId, attributeMap);
        }
        return null;
    }
}
