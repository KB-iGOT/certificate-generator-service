package controllers.certs;

import org.junit.Before;
import org.junit.Test;
import org.sunbird.request.Request;
import org.sunbird.BaseException;
import org.sunbird.incredible.processor.JsonKey;
import java.util.HashMap;
import java.util.Map;

public class VerificationReqValidatorTest {

    private VerificationReqValidator validator;

    @Before
    public void setUp() {
        validator = new VerificationReqValidator();
    }

    @Test
    public void validateVerificationRequest_Succeeds_WhenCertificateDataPresent() throws Exception {
        Request request = new Request();
        Map<String, Object> certData = new HashMap<>();
        certData.put("someField", "someValue");
        Map<String, Object> certReq = new HashMap<>();
        certReq.put(JsonKey.DATA, certData);
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.CERTIFICATE, certReq);
        request.setRequest(reqMap);

        validator.validateVerificationRequest(request);
    }

    @Test
    public void validateVerificationRequest_Succeeds_WhenCertificateIdPresent() throws Exception {
        Request request = new Request();
        Map<String, Object> certReq = new HashMap<>();
        certReq.put(JsonKey.ID, "uuid-123");
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.CERTIFICATE, certReq);
        request.setRequest(reqMap);

        validator.validateVerificationRequest(request);
    }

    @Test(expected = BaseException.class)
    public void validateVerificationRequest_ThrowsException_WhenCertificateMissing() throws Exception {
        Request request = new Request();
        request.setRequest(new HashMap<>());

        validator.validateVerificationRequest(request);
    }

    @Test(expected = BaseException.class)
    public void validateVerificationRequest_ThrowsException_WhenCertificateDataIsEmpty() throws Exception {
        Request request = new Request();
        Map<String, Object> certReq = new HashMap<>();
        certReq.put(JsonKey.DATA, new HashMap<>());
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.CERTIFICATE, certReq);
        request.setRequest(reqMap);

        validator.validateVerificationRequest(request);
    }

    @Test(expected = BaseException.class)
    public void validateVerificationRequest_ThrowsException_WhenCertificateIdIsBlank() throws Exception {
        Request request = new Request();
        Map<String, Object> certReq = new HashMap<>();
        certReq.put(JsonKey.ID, "");
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.CERTIFICATE, certReq);
        request.setRequest(reqMap);

        validator.validateVerificationRequest(request);
    }

    @Test(expected = BaseException.class)
    public void validateVerificationRequest_ThrowsException_WhenCertificateHasNeitherDataNorId() throws Exception {
        Request request = new Request();
        Map<String, Object> certReq = new HashMap<>();
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put(JsonKey.CERTIFICATE, certReq);
        request.setRequest(reqMap);

        validator.validateVerificationRequest(request);
    }
}