package org.sunbird.incredible.builders;

import org.junit.jupiter.api.Test;
import org.sunbird.incredible.pojos.CertificateExtension;
import org.sunbird.incredible.pojos.CompositeIdentityObject;
import org.sunbird.incredible.pojos.SignatoryExtension;
import org.sunbird.incredible.pojos.Signature;
import org.sunbird.incredible.pojos.ob.BadgeClass;
import org.sunbird.incredible.pojos.ob.VerificationObject;
import org.sunbird.incredible.pojos.ob.exeptions.InvalidDateFormatException;

import static org.junit.jupiter.api.Assertions.*;

class CertificateExtensionBuilderTest {

    @Test
    void testCertificateExtensionBuilder_allFields() throws InvalidDateFormatException {
        String context = "https://example.org/context";
        String validDate = "2024-06-19"; // format accepted by validator (yyyy-MM-dd)

        SignatoryExtension[] signatories = new SignatoryExtension[]{new SignatoryExtension("")};
        Signature signature = new Signature();
        CompositeIdentityObject recipient = new CompositeIdentityObject();
        BadgeClass badge = new BadgeClass(context);
        VerificationObject verification = new VerificationObject();

        CertificateExtension extension = new CertificateExtensionBuilder(context)
                .setAwardedThrough("https://awarded.example.org")
                .setId("https://certificate.example.org/id")
                .setSignatory(signatories)
                .setPrintUri("https://certificate.example.org/print")
                .setIssuedOn(validDate)
                .setValidFrom(validDate)
                .setSignature(signature)
                .setRecipient(recipient)
                .setBadge(badge)
                .setVerification(verification)
                .setProviderName("Sunbird")
                .build();

        assertNotNull(extension);
        assertEquals("https://certificate.example.org/id", extension.getId());
        assertEquals("https://awarded.example.org", extension.getAwardedThrough());
        assertEquals("Sunbird", extension.getProviderName());
        assertEquals("https://certificate.example.org/print", extension.getPrintUri());
        assertEquals(validDate, extension.getValidFrom());
        assertEquals(signature, extension.getSignature());
        assertEquals(recipient, extension.getRecipient());
        assertEquals(badge, extension.getBadge());
        assertEquals(verification, extension.getVerification());
        assertEquals(signatories, extension.getSignatory());
    }
}
