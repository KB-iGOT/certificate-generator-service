package org.sunbird.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestParamsTest {

    @Test
    void testAllFieldsGettersAndSetters() {
        RequestParams params = new RequestParams();

        params.setDid("device123");
        params.setKey("keyABC");
        params.setMsgid("msg-001");
        params.setUid("user-xyz");
        params.setCid("channel001");
        params.setSid("session-001");
        params.setAuthToken("auth-token-123");

        assertEquals("device123", params.getDid());
        assertEquals("keyABC", params.getKey());
        assertEquals("msg-001", params.getMsgid());
        assertEquals("user-xyz", params.getUid());
        assertEquals("channel001", params.getCid());
        assertEquals("session-001", params.getSid());
        assertEquals("auth-token-123", params.getAuthToken());
    }
}
