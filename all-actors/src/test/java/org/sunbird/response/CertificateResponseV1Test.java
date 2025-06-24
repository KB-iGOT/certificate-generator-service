package org.sunbird.response;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CertificateResponseV1Test {

    @Test
    void constructorInitializesAllFieldsIncludingPdfUrl() {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("foo", "bar");
        CertificateResponseV1 response = new CertificateResponseV1("id1", "access1", "recipient1", jsonData, "http://pdf.url/cert.pdf");
        assertEquals("id1", response.getId());
        assertEquals("access1", response.getAccessCode());
        assertEquals("recipient1", response.getRecipientId());
        assertEquals(jsonData, response.getJsonData());
        assertEquals("http://pdf.url/cert.pdf", response.getPdfUrl());
    }

    @Test
    void settersAndGettersWorkForPdfUrl() {
        CertificateResponseV1 response = new CertificateResponseV1(null, null, null, null, null);
        response.setPdfUrl("https://example.com/cert.pdf");
        assertEquals("https://example.com/cert.pdf", response.getPdfUrl());
        response.setPdfUrl(null);
        assertNull(response.getPdfUrl());
    }

    @Test
    void pdfUrlCanBeNullInConstructor() {
        CertificateResponseV1 response = new CertificateResponseV1("id2", "access2", "recipient2", null, null);
        assertNull(response.getPdfUrl());
    }
}