package org.sunbird.incredible.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BaseLoggerTest {

    private final BaseLogger baseLogger = new BaseLogger();

    @AfterEach
    public void clearMDC() {
        MDC.clear();
    }

    @Test
    public void testSetReqIdWithMissingRequestId() {
        Map<String, Object> trace = new HashMap<>();

        baseLogger.setReqId(trace);

        assertNull(baseLogger.getReqId());
    }

    @Test
    public void testSetReqIdWithNullValue() {
        Map<String, Object> trace = new HashMap<>();
        trace.put(JsonKey.REQUEST_MESSAGE_ID, null);

        baseLogger.setReqId(trace);

        assertNull(baseLogger.getReqId());
    }

    @Test
    public void testGetReqIdWhenNotSet() {
        assertNull(baseLogger.getReqId());
    }
}
