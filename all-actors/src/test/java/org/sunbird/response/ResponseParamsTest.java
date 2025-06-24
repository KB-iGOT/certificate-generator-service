package org.sunbird.response;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResponseParamsTest {

    @Test
    void setAndGetResmsgidWorks() {
        ResponseParams params = new ResponseParams();
        params.setResmsgid("res-123");
        assertEquals("res-123", params.getResmsgid());
    }

    @Test
    void setAndGetMsgidWorks() {
        ResponseParams params = new ResponseParams();
        params.setMsgid("msg-456");
        assertEquals("msg-456", params.getMsgid());
    }

    @Test
    void setAndGetErrWorks() {
        ResponseParams params = new ResponseParams();
        params.setErr("error-789");
        assertEquals("error-789", params.getErr());
    }

    @Test
    void setAndGetStatusWorks() {
        ResponseParams params = new ResponseParams();
        params.setStatus("SUCCESSFUL");
        assertEquals("SUCCESSFUL", params.getStatus());
    }

    @Test
    void setAndGetErrmsgWorks() {
        ResponseParams params = new ResponseParams();
        params.setErrmsg("Some error message");
        assertEquals("Some error message", params.getErrmsg());
    }

    @Test
    void defaultValuesAreNull() {
        ResponseParams params = new ResponseParams();
        assertNull(params.getResmsgid());
        assertNull(params.getMsgid());
        assertNull(params.getErr());
        assertNull(params.getStatus());
        assertNull(params.getErrmsg());
    }

    @Test
    void statusTypeEnumValuesAreCorrect() {
        assertEquals(ResponseParams.StatusType.SUCCESSFUL, ResponseParams.StatusType.valueOf("SUCCESSFUL"));
        assertEquals(ResponseParams.StatusType.WARNING, ResponseParams.StatusType.valueOf("WARNING"));
        assertEquals(ResponseParams.StatusType.FAILED, ResponseParams.StatusType.valueOf("FAILED"));
        assertEquals(3, ResponseParams.StatusType.values().length);
    }
}