package org.sunbird.cert.actor;

import org.apache.commons.collections.MapUtils;
import org.sunbird.BaseActor;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.cert.helper.CertRegistryHelper;
import org.sunbird.cert.helper.IssueCertificateContentHelper;
import org.sunbird.cert.helper.IssueCertificateEventHelper;
import org.sunbird.cert.helper.UserEnrolmentHelper;
import org.sunbird.incredible.pojos.CertificateExtension;
import org.sunbird.incredible.processor.CertModel;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CertBackgroundActor extends BaseActor {
    private static final CertRegistryHelper certRegistryHelper = CertRegistryHelper.getInstance();
    private static final IssueCertificateContentHelper issueCertificateContentHelper = IssueCertificateContentHelper.getInstance();
    private static final IssueCertificateEventHelper issueCertificateEventHelper = IssueCertificateEventHelper.getInstance();
    private static final UserEnrolmentHelper userEnrolmentHelper = UserEnrolmentHelper.getInstance();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    public void onReceive(Request request) throws Throwable {
        logger.info("CertificationActor:onReceive:request arrived with operation" + request.getOperation());
        String operation = request.getOperation();
        switch (operation) {
            case "add_registry_req":
                addToRegistryAndUpdateCassandraRecord(request);
                break;
            default:
                onReceiveUnsupportedMessage("CertificationActor");
        }
    }

    private void addToRegistryAndUpdateCassandraRecord(Request request) throws BaseException {
        logger.debug("The request is for backgroundActor : " + request.getRequest());
        String courseId = (String) request.getRequest().get(JsonKeys.COURSE_ID);
        String batchId = (String) request.getRequest().get(JsonKeys.BATCH_ID);
        String userId = (String) request.getRequest().get(JsonKeys.USER_ID);
        Map<String, Object> certificateTemplate = (Map<String,Object>) request.getRequest().get(JsonKeys.CERTIFICATE);
        String uuid = (String) request.getRequest().get(JsonKeys.UUID);
        boolean isEvent = (boolean) request.getRequest().get(JsonKeys.IS_EVENT);
        CertificateExtension certificateExtension = (CertificateExtension) request.getRequest().get(JsonKeys.CERTIFICATE_EXTENSION);
        CertModel certModel = (CertModel) request.getRequest().get(JsonKeys.CERT_MODEL);
        String accessCode = (String) request.getRequest().get(JsonKeys.ACCESS_CODE);
        List<Map<String, Object>> issuedCertificateList = (List<Map<String, Object>>) request.getRequest().get(JsonKeys.USER_CERTICATE_LIST);
        Map<String, Object> courseRelatedInfo = new HashMap<>();
        courseRelatedInfo.put(JsonKeys.COURSE_ID, courseId);
        courseRelatedInfo.put(JsonKeys.BATCH_ID, batchId);
        courseRelatedInfo.put(JsonKeys.TYPE, certificateTemplate.get(JsonKeys.NAME));
        Map<String,Object> certificateRegistryResponse = addCertificateToRegistry(uuid, certificateExtension, certModel, courseRelatedInfo, accessCode);
        if (MapUtils.isNotEmpty(certificateRegistryResponse)) {
            Map<String, Object> certificateMap = new HashMap<>();
            certificateMap.put(JsonKeys.IDENTIFIER, uuid);
            certificateMap.put(JsonKeys.LAST_ISSUED_ON, formatter.format(new Date()));
            certificateMap.put(JsonKeys.TOKEN, accessCode);
            certificateMap.put(JsonKeys.NAME, certificateTemplate.get(JsonKeys.NAME));
            certificateMap.put(JsonKeys.VERSION, JsonKeys.VERSION_2);
            issuedCertificateList.add(certificateMap);
            updateUserEnrolmentRecord(userId, courseId, batchId, issuedCertificateList, isEvent);
        } else {
            logger.error("Issue while adding the registry for request: " + request);
        }

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

    public Map<String, Object> addCertificateToRegistry(String uuid, CertificateExtension certificateExtension, CertModel certModel, Map<String, Object> courseRelatedInfo, String accessCode) {
        try {
            Map<String, Object> requestMap = new HashMap<>();

            requestMap.put(JsonKeys.ID, uuid);
            requestMap.put(JsonKeys.JSON_DATA, certificateExtension);
            requestMap.put(JsonKeys.ACCESS_CODE, accessCode);
            requestMap.put(JsonKeys.RECIPIENT_NAME, certModel.getRecipientName());
            requestMap.put(JsonKeys.RECIPIENT_ID, certModel.getIdentifier());
            requestMap.put(JsonKeys.DYNAMIC_GENERATION, "true");
            requestMap.put(JsonKeys.RELATED, courseRelatedInfo);

            Map<String, Object> addReq = new HashMap<>();
            addReq.put(JsonKeys.REQUEST, requestMap);
            return certRegistryHelper.addCertToRegistry(addReq);
        } catch (Exception e) {
            logger.error("Issue while updating the registry: ", e.getMessage());
        }
        return null;
    }

}