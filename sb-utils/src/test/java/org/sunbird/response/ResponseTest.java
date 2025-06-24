package org.sunbird.response;

import org.junit.jupiter.api.Test;
import org.sunbird.message.ResponseCode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    @Test
    void setAndGetIdWorks() {
        Response response = new Response();
        response.setId("id-123");
        assertEquals("id-123", response.getId());
    }

    @Test
    void setAndGetVerWorks() {
        Response response = new Response();
        response.setVer("v1.0");
        assertEquals("v1.0", response.getVer());
    }

    @Test
    void setAndGetTsWorks() {
        Response response = new Response();
        response.setTs("2024-06-01T12:00:00Z");
        assertEquals("2024-06-01T12:00:00Z", response.getTs());
    }

    @Test
    void getResultReturnsMutableMap() {
        Response response = new Response();
        response.getResult().put("key", "value");
        assertEquals("value", response.getResult().get("key"));
    }

    @Test
    void putAndGetValueWorks() {
        Response response = new Response();
        response.put("foo", 42);
        assertEquals(42, response.get("foo"));
    }

    @Test
    void putAllAddsAllEntries() {
        Response response = new Response();
        Map<String, Object> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        response.putAll(map);
        assertEquals(1, response.get("a"));
        assertEquals(2, response.get("b"));
    }

    @Test
    void containsKeyReturnsTrueIfKeyExists() {
        Response response = new Response();
        response.put("exists", "yes");
        assertTrue(response.containsKey("exists"));
        assertFalse(response.containsKey("missing"));
    }

    @Test
    void setAndGetParamsWorks() {
        Response response = new Response();
        ResponseParams params = new ResponseParams();
        response.setParams(params);
        assertEquals(params, response.getParams());
    }

    @Test
    void setAndGetResponseCodeWorks() {
        Response response = new Response();
        response.setResponseCode(ResponseCode.CLIENT_ERROR);
        assertEquals(ResponseCode.CLIENT_ERROR, response.getResponseCode());
    }

    @Test
    void getResponseCodeReturnsDefaultOK() {
        Response response = new Response();
        assertEquals(ResponseCode.OK, response.getResponseCode());
    }

}