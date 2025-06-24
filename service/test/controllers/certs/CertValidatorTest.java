package controllers.certs;

import org.junit.Before;
import org.junit.Test;
import org.sunbird.request.Request;
import org.sunbird.BaseException;
import java.util.HashMap;
import java.util.Map;

public class CertValidatorTest {

    private CertValidator certValidator;

    @Before
    public void setUp() {
        certValidator = new CertValidator();
    }

    @Test
    public void validateGenerateCertRequest_Succeeds_WhenAllMandatoryParamsPresent() throws Exception {
        Request request = new Request();
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("userId", "user-1");
        reqMap.put("courseId", "course-1");
        reqMap.put("batchId", "batch-1");
        request.setRequest(reqMap);

        certValidator.validateGenerateCertRequest(request);
    }

    @Test(expected = BaseException.class)
    public void validateGenerateCertRequest_ThrowsException_WhenRequestMapIsEmpty() throws Exception {
        Request request = new Request();
        request.setRequest(new HashMap<>());

        certValidator.validateGenerateCertRequest(request);
    }

    @Test(expected = BaseException.class)
    public void validateGenerateCertRequest_ThrowsException_WhenRequestMapIsNull() throws Exception {
        Request request = new Request();
        request.setRequest(null);

        certValidator.validateGenerateCertRequest(request);
    }

    @Test(expected = BaseException.class)
    public void validateGenerateCertRequest_ThrowsException_WhenUserIdIsMissing() throws Exception {
        Request request = new Request();
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("courseId", "course-1");
        reqMap.put("batchId", "batch-1");
        request.setRequest(reqMap);

        certValidator.validateGenerateCertRequest(request);
    }

    @Test(expected = BaseException.class)
    public void validateGenerateCertRequest_ThrowsException_WhenCourseIdIsBlank() throws Exception {
        Request request = new Request();
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("userId", "user-1");
        reqMap.put("courseId", "");
        reqMap.put("batchId", "batch-1");
        request.setRequest(reqMap);

        certValidator.validateGenerateCertRequest(request);
    }

    @Test(expected = BaseException.class)
    public void validateGenerateCertRequest_ThrowsException_WhenBatchIdIsNull() throws Exception {
        Request request = new Request();
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("userId", "user-1");
        reqMap.put("courseId", "course-1");
        reqMap.put("batchId", null);
        request.setRequest(reqMap);

        certValidator.validateGenerateCertRequest(request);
    }
}
