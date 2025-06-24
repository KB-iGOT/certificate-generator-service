package org.sunbird.message;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResponseCodeTest {

    @Test
    void getCodeReturnsCorrectValueForEachEnum() {
        assertEquals(200, ResponseCode.OK.getCode());
        assertEquals(400, ResponseCode.CLIENT_ERROR.getCode());
        assertEquals(500, ResponseCode.SERVER_ERROR.getCode());
        assertEquals(404, ResponseCode.RESOURCE_NOT_FOUND.getCode());
        assertEquals(401, ResponseCode.UNAUTHORIZED.getCode());
        assertEquals(403, ResponseCode.FORBIDDEN.getCode());
        assertEquals(302, ResponseCode.REDIRECTION_REQUIRED.getCode());
        assertEquals(429, ResponseCode.TOO_MANY_REQUESTS.getCode());
        assertEquals(503, ResponseCode.SERVICE_UNAVAILABLE.getCode());
        assertEquals(400, ResponseCode.BAD_REQUEST.getCode());
    }

    @Test
    void getResponseCodeReturnsCorrectEnumForKnownCodes() {
        assertEquals(ResponseCode.OK, ResponseCode.getResponseCode(200));
        assertEquals(ResponseCode.CLIENT_ERROR, ResponseCode.getResponseCode(400));
        assertEquals(ResponseCode.SERVER_ERROR, ResponseCode.getResponseCode(500));
        assertEquals(ResponseCode.RESOURCE_NOT_FOUND, ResponseCode.getResponseCode(404));
        assertEquals(ResponseCode.UNAUTHORIZED, ResponseCode.getResponseCode(401));
        assertEquals(ResponseCode.FORBIDDEN, ResponseCode.getResponseCode(403));
        assertEquals(ResponseCode.REDIRECTION_REQUIRED, ResponseCode.getResponseCode(302));
        assertEquals(ResponseCode.TOO_MANY_REQUESTS, ResponseCode.getResponseCode(429));
        assertEquals(ResponseCode.SERVICE_UNAVAILABLE, ResponseCode.getResponseCode(503));
    }

    @Test
    void getResponseCodeReturnsServerErrorForUnknownCode() {
        assertEquals(ResponseCode.SERVER_ERROR, ResponseCode.getResponseCode(999));
        assertEquals(ResponseCode.SERVER_ERROR, ResponseCode.getResponseCode(-1));
        assertEquals(ResponseCode.SERVER_ERROR, ResponseCode.getResponseCode(0));
    }
}