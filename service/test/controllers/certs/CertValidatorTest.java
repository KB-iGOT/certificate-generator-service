package controllers.certs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.BaseException;
import org.sunbird.request.Request;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CertValidatorTest {

    private CertValidator certValidator;

    @BeforeEach
    void setUp() {
        certValidator = new CertValidator();
    }

    @Test
    void testValidateGenerateCertRequest_success() {
        Request request = new Request();
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("userId", "user123");
        innerMap.put("courseId", "course123");
        innerMap.put("batchId", "batch123");
        request.setRequest(innerMap);

        assertDoesNotThrow(() -> certValidator.validateGenerateCertRequest(request));
    }

    @Test
    void testValidateGenerateCertRequest_missingRequestMap() {
        Request request = new Request();
        request.setRequest(null);

        BaseException ex = assertThrows(BaseException.class,
                () -> certValidator.validateGenerateCertRequest(request));

        assertEquals("MANDATORY_PARAMETER_MISSING", ex.getCode());
        assertTrue(ex.getMessage().contains("request"));
    }

    @Test
    void testValidateGenerateCertRequest_missingMandatoryKey() {
        Request request = new Request();
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("userId", "user123");
        innerMap.put("courseId", "course123");
        // batchId missing
        request.setRequest(innerMap);

        BaseException ex = assertThrows(BaseException.class,
                () -> certValidator.validateGenerateCertRequest(request));

        assertEquals("MANDATORY_PARAMETER_MISSING", ex.getCode());
        assertTrue(ex.getMessage().contains("request.batchId"));
    }

    @Test
    void testValidateGenerateCertRequest_emptyStringMandatoryKey() {
        Request request = new Request();
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("userId", "user123");
        innerMap.put("courseId", "course123");
        innerMap.put("batchId", ""); // empty string
        request.setRequest(innerMap);

        BaseException ex = assertThrows(BaseException.class,
                () -> certValidator.validateGenerateCertRequest(request));

        assertEquals("MANDATORY_PARAMETER_MISSING", ex.getCode());
        assertTrue(ex.getMessage().contains("request.batchId"));
    }
}
