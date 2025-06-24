package org.sunbird.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RequestParamsTest {

    @Test
    void setAndGetAuthTokenWorks() {
        RequestParams params = new RequestParams();
        params.setAuthToken("token123");
        assertEquals("token123", params.getAuthToken());
    }

    @Test
    void setAndGetUidWorks() {
        RequestParams params = new RequestParams();
        params.setUid("user-1");
        assertEquals("user-1", params.getUid());
    }

    @Test
    void setAndGetDidWorks() {
        RequestParams params = new RequestParams();
        params.setDid("device-1");
        assertEquals("device-1", params.getDid());
    }

    @Test
    void setAndGetKeyWorks() {
        RequestParams params = new RequestParams();
        params.setKey("key-1");
        assertEquals("key-1", params.getKey());
    }

    @Test
    void setAndGetMsgidWorks() {
        RequestParams params = new RequestParams();
        params.setMsgid("msg-1");
        assertEquals("msg-1", params.getMsgid());
    }

    @Test
    void setAndGetCidWorks() {
        RequestParams params = new RequestParams();
        params.setCid("cid-1");
        assertEquals("cid-1", params.getCid());
    }

    @Test
    void setAndGetSidWorks() {
        RequestParams params = new RequestParams();
        params.setSid("sid-1");
        assertEquals("sid-1", params.getSid());
    }

    @Test
    void defaultValuesAreNull() {
        RequestParams params = new RequestParams();
        assertNull(params.getAuthToken());
        assertNull(params.getUid());
        assertNull(params.getDid());
        assertNull(params.getKey());
        assertNull(params.getMsgid());
        assertNull(params.getCid());
        assertNull(params.getSid());
    }
}