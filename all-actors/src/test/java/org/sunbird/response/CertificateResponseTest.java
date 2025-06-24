package org.sunbird.response;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CertificateResponseTest {

    @Test
    void constructorInitializesAllFieldsCorrectly() {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("key", "value");
        CertificateResponse response = new CertificateResponse("id1", "access123", "recipient42", jsonData);
        assertEquals("id1", response.getId());
        assertEquals("access123", response.getAccessCode());
        assertEquals("recipient42", response.getRecipientId());
        assertEquals(jsonData, response.getJsonData());
        assertNull(response.getJsonUrl());
    }

    @Test
    void settersAndGettersWorkForAllFields() {
        CertificateResponse response = new CertificateResponse(null, null, null, null);
        response.setId("id2");
        response.setAccessCode("access456");
        response.setRecipientId("recipient99");
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("foo", 123);
        response.setJsonData(jsonData);
        response.setJsonUrl("http://example.com/cert.json");

        assertEquals("id2", response.getId());
        assertEquals("access456", response.getAccessCode());
        assertEquals("recipient99", response.getRecipientId());
        assertEquals(jsonData, response.getJsonData());
        assertEquals("http://example.com/cert.json", response.getJsonUrl());
    }

    @Test
    void jsonDataCanBeSetToNull() {
        CertificateResponse response = new CertificateResponse("id3", "access789", "recipient77", null);
        assertNull(response.getJsonData());
        response.setJsonData(null);
        assertNull(response.getJsonData());
    }

    @Test
    void jsonUrlCanBeSetAndRetrieved() {
        CertificateResponse response = new CertificateResponse("id4", "access000", "recipient88", null);
        response.setJsonUrl("https://certs.org/cert.json");
        assertEquals("https://certs.org/cert.json", response.getJsonUrl());
    }
}