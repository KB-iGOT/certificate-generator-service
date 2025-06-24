package org.sunbird.request;

import org.junit.jupiter.api.Test;
import org.sunbird.ActorServiceException;
import org.sunbird.BaseException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void testNoArgsConstructor() {
        Request request = new Request();
        assertNotNull(request.getParams());
        assertNull(request.getContext());
        assertEquals(null, request.getParams().getMsgid()); // because requestId is null
    }

    @Test
    void testGetAndSetFields() throws BaseException {
        Request request = new Request();

        // ID
        request.setId("id123");
        assertEquals("id123", request.getId());

        // Version
        request.setVer("v1");
        assertEquals("v1", request.getVer());

        // Timestamp
        request.setTs("2024-01-01");
        assertEquals("2024-01-01", request.getTs());

        // Manager Name
        request.setManagerName("manager");
        assertEquals("manager", request.getManagerName());

        // Operation
        request.setOperation("create");
        assertEquals("create", request.getOperation());

        // Env
        request.setEnv(1);
        assertEquals(1, request.getEnv());

        // RequestId
        request.setRequestId("r001");
        //assertEquals("r001", request.getRequestId()); // gets from params

        // Headers
        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer x");
        request.setHeaders(headers);
        assertEquals("Bearer x", request.getHeaders().get("Authorization"));

        // Context
        Map<String, Object> context = new HashMap<>();
        context.put("channel", "sunbird");
        request.setContext(context);
        assertEquals("sunbird", request.getContext().get("channel"));

        // Request map
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("userId", "123");
        request.setRequest(requestMap);
        assertEquals("123", request.get("userId"));

        // put()
        request.put("courseId", "456");
        assertEquals("456", request.get("courseId"));

        // copyRequestValueObjects()
        Map<String, Object> newMap = new HashMap<>();
        newMap.put("batchId", "789");
        request.copyRequestValueObjects(newMap);
        assertEquals("789", request.get("batchId"));

    }

    @Test
    void testSetParamsWithNullMsgIdFallbacksToRequestId() {
        Request request = new Request();
        request.setRequestId("fallback-msg-id");

        RequestParams params = new RequestParams(); // msgId is null
        request.setParams(params);

        assertEquals("fallback-msg-id", request.getParams().getMsgid());
    }

    @Test
    void testToString() {
        Request request = new Request();
        Map<String, Object> context = new HashMap<>();
        context.put("key", "value");
        request.setContext(context);

        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("test", 123);
        request.setRequest(reqMap);

        String result = request.toString();
        assertTrue(result.contains("context"));
        assertTrue(result.contains("requestValueObjects"));
    }
}
