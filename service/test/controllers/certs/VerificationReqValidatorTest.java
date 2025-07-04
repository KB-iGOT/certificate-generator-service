package controllers.certs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.BaseException;
import org.sunbird.request.Request;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerificationReqValidatorTest {

    private VerificationReqValidator validator;

    @BeforeEach
    void setUp() {
        validator = new VerificationReqValidator();
    }

    @Test
    void testValidateVerificationRequest_withValidData() {
        Request request = new Request();
        Map<String, Object> certMap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("something", "value");
        certMap.put("data", dataMap);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("certificate", certMap);
        request.setRequest(requestMap);

        assertDoesNotThrow(() -> validator.validateVerificationRequest(request));
    }

    @Test
    void testValidateVerificationRequest_withValidId() {
        Request request = new Request();
        Map<String, Object> certMap = new HashMap<>();
        certMap.put("id", "valid-uuid");
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("certificate", certMap);
        request.setRequest(requestMap);

        assertDoesNotThrow(() -> validator.validateVerificationRequest(request));
    }

    @Test
    void testValidateVerificationRequest_missingCertificateMap() {
        Request request = new Request();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("certificate", null);  // null certificate
        request.setRequest(requestMap);

        BaseException ex = assertThrows(BaseException.class,
                () -> validator.validateVerificationRequest(request));

        assertEquals("MANDATORY_PARAMETER_MISSING", ex.getCode());
        assertTrue(ex.getMessage().contains("certificate"));
    }

    @Test
    void testValidateVerificationRequest_emptyDataMap() {
        Request request = new Request();
        Map<String, Object> certMap = new HashMap<>();
        certMap.put("data", new HashMap<>()); // empty data
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("certificate", certMap);
        request.setRequest(requestMap);

        BaseException ex = assertThrows(BaseException.class,
                () -> validator.validateVerificationRequest(request));

        assertEquals("MANDATORY_PARAMETER_MISSING", ex.getCode());
        assertTrue(ex.getMessage().contains("certificate.data"));
    }

    @Test
    void testValidateVerificationRequest_blankId() {
        Request request = new Request();
        Map<String, Object> certMap = new HashMap<>();
        certMap.put("id", "");  // blank id
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("certificate", certMap);
        request.setRequest(requestMap);

        BaseException ex = assertThrows(BaseException.class,
                () -> validator.validateVerificationRequest(request));

        assertEquals("MANDATORY_PARAMETER_MISSING", ex.getCode());
        assertTrue(ex.getMessage().contains("certificate.uuid"));
    }

    @Test
    void testValidateVerificationRequest_missingDataAndId() {
        Request request = new Request();
        Map<String, Object> certMap = new HashMap<>(); // neither data nor id
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("certificate", certMap);
        request.setRequest(requestMap);

        BaseException ex = assertThrows(BaseException.class,
                () -> validator.validateVerificationRequest(request));

        assertEquals("MANDATORY_PARAMETER_MISSING", ex.getCode());
        assertTrue(ex.getMessage().contains("certificate"));
    }
}
