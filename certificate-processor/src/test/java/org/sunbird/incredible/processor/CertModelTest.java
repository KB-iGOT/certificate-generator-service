package org.sunbird.incredible.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.SignatoryExtension;
import org.sunbird.incredible.pojos.ob.Criteria;
import org.sunbird.incredible.pojos.ob.Issuer;

import static org.junit.jupiter.api.Assertions.*;

class CertModelTest {

    @Test
    void testGettersAndSetters() {
        CertModel certModel = new CertModel();

        Issuer issuer = new Issuer("");
        Criteria criteria = new Criteria();
        SignatoryExtension[] signatoryList = new SignatoryExtension[]{new SignatoryExtension(""), new SignatoryExtension("")};

        certModel.setCourseName("Java Course")
                .setRecipientName("Ajay")
                .setRecipientEmail("ajay@example.com")
                .setRecipientPhone("1234567890")
                .setCertificateName("Java Certificate")
                .setCertificateDescription("Certificate for Java Course")
                .setCertificateLogo("logo.png")
                .setIssuer(issuer)
                .setValidFrom("2024-01-01");

        certModel.setSignatoryList(signatoryList);
        certModel.setIssuedDate("2024-03-01");
        certModel.setCriteria(criteria);

        assertEquals("Java Course", certModel.getCourseName());
        assertEquals("Ajay", certModel.getRecipientName());
        assertEquals("ajay@example.com", certModel.getRecipientEmail());
        assertEquals("1234567890", certModel.getRecipientPhone());
        assertEquals("Java Certificate", certModel.getCertificateName());
        assertEquals("Certificate for Java Course", certModel.getCertificateDescription());
        assertEquals("logo.png", certModel.getCertificateLogo());
        assertEquals(issuer, certModel.getIssuer());
        assertEquals("2024-01-01", certModel.getValidFrom());
        assertEquals(criteria, certModel.getCriteria());
        assertArrayEquals(signatoryList, certModel.getSignatoryList());
    }

    @Test
    void testToStringWithJsonProcessingException() throws Exception {
        CertModel certModel = new CertModel();

        // Use reflection to inject a faulty ObjectMapper that throws exception
        var mapperField = CertModel.class.getDeclaredField("mapper");
        mapperField.setAccessible(true);

        ObjectMapper mockMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("Mocked Exception") {};
            }
        };

        mapperField.set(null, mockMapper);

        // should return null stringRep due to exception
        assertNull(certModel.toString());
    }
}
