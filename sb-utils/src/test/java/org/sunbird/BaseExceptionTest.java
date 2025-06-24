package org.sunbird;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseExceptionTest {

    @Test
    void constructorInitializesAllFieldsCorrectly() {
        BaseException ex = new BaseException("ERR_CODE", "Error occurred", 500);
        assertEquals("ERR_CODE", ex.getCode());
        assertEquals("Error occurred", ex.getMessage());
        assertEquals(500, ex.getResponseCode());
    }

    @Test
    void settersUpdateFieldsCorrectly() {
        BaseException ex = new BaseException("CODE1", "Msg1", 400);
        ex.setCode("CODE2");
        ex.setMessage("Msg2");
        ex.setResponseCode(404);
        assertEquals("CODE2", ex.getCode());
        assertEquals("Msg2", ex.getMessage());
        assertEquals(404, ex.getResponseCode());
    }
}