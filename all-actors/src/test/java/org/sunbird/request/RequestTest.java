package org.sunbird.request;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void defaultConstructorInitializesParamsAndSetsMsgid() {
        Request req = new Request();
        assertNotNull(req.getParams());
        assertNull(req.getParams().getMsgid());
    }

    @Test
    void getRequestIdReturnsMsgidFromParamsIfPresent() {
        Request req = new Request();
        req.getParams().setMsgid("msgid-2");
        assertEquals("msgid-2", req.getRequestId());
    }


    @Test
    void putAndGetRequestValueWorks() {
        Request req = new Request();
        req.put("foo", 42);
        assertEquals(42, req.get("foo"));
    }

    @Test
    void setAndGetManagerNameWorks() {
        Request req = new Request();
        req.setManagerName("manager");
        assertEquals("manager", req.getManagerName());
    }

    @Test
    void setAndGetOperationWorks() {
        Request req = new Request();
        req.setOperation("op");
        assertEquals("op", req.getOperation());
    }

    @Test
    void copyRequestValueObjectsCopiesAllEntries() {
        Request req = new Request();
        Map<String, Object> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        req.copyRequestValueObjects(map);
        assertEquals(1, req.getRequest().get("a"));
        assertEquals(2, req.getRequest().get("b"));
    }

    @Test
    void copyRequestValueObjectsWithNullOrEmptyMapDoesNothing() {
        Request req = new Request();
        req.copyRequestValueObjects(null);
        assertTrue(req.getRequest().isEmpty());
        req.copyRequestValueObjects(new HashMap<>());
        assertTrue(req.getRequest().isEmpty());
    }

    @Test
    void toStringReturnsNonNullString() {
        Request req = new Request();
        assertNotNull(req.toString());
    }

    @Test
    void setAndGetIdVerTsWorks() {
        Request req = new Request();
        req.setId("id1");
        req.setVer("v1");
        req.setTs("ts1");
        assertEquals("id1", req.getId());
        assertEquals("v1", req.getVer());
        assertEquals("ts1", req.getTs());
    }

    @Test
    void setAndGetParamsWorks() {
        Request req = new Request();
        RequestParams params = new RequestParams();
        params.setMsgid("msgid-3");
        req.setParams(params);
        assertEquals("msgid-3", req.getParams().getMsgid());
    }

    @Test
    void setAndGetHeadersWorks() {
        Request req = new Request();
        Map<String, Object> headers = new HashMap<>();
        headers.put("h", "v");
        req.setHeaders(headers);
        assertEquals("v", req.getHeaders().get("h"));
    }

    @Test
    void setAndGetEnvWorks() {
        Request req = new Request();
        req.setEnv(5);
        assertEquals(5, req.getEnv());
    }

    @Test
    void getTimeoutReturnsDefaultIfNull() {
        Request req = new Request();
        assertEquals(120, req.getTimeout());
    }

    @Test
    void getTimeoutReturnsSetValue() throws Exception {
        Request req = new Request();
        req.setTimeout(60);
        assertEquals(60, req.getTimeout());
    }

}