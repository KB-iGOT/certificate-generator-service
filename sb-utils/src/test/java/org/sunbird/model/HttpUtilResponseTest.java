package org.sunbird.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpUtilResponseTest {

    @Test
    void testNoArgsConstructorAndSetters() {
        HttpUtilResponse response = new HttpUtilResponse();
        response.setBody("Test Body");
        response.setStatusCode(200);

        assertEquals("Test Body", response.getBody());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        HttpUtilResponse response = new HttpUtilResponse("Success", 201);

        assertEquals("Success", response.getBody());
        assertEquals(201, response.getStatusCode());
    }
}
