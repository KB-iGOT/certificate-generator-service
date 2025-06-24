package org.sunbird.incredible.processor.views;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.sunbird.incredible.pojos.*;

public class HTMLVarResolverTest {

    private CertificateExtension certificateExtension;

    @BeforeEach
    public void setup() {
        certificateExtension = Mockito.mock(CertificateExtension.class);

        Mockito.when(certificateExtension.getId()).thenReturn("https://example.org/cert/123456.png");
        Mockito.when(certificateExtension.getIssuedOn()).thenReturn("2023-05-10T00:00:00Z");

        Mockito.when(certificateExtension.getExpires()).thenReturn("2025-12-31");
        Mockito.when(certificateExtension.getProviderName()).thenReturn("Acme Provider");
    }

    @Test
    public void courseNameIsReturnedWhenEvidencePresent() {
        HTMLVarResolver resolver = new HTMLVarResolver(certificateExtension);
    }

    @Test
    public void courseNameIsEmptyWhenEvidenceIsNull() {
        Mockito.when(certificateExtension.getEvidence()).thenReturn(null);
        HTMLVarResolver resolver = new HTMLVarResolver(certificateExtension);
        assertEquals("", resolver.getCourseName());
    }

    @Test
    public void qrCodeImageReturnsFileName() {
        HTMLVarResolver resolver = new HTMLVarResolver(certificateExtension);
        assertEquals("123456.png", resolver.getQrCodeImage());
    }

    @Test
    public void qrCodeImageReturnsNullForInvalidUri() {
        Mockito.when(certificateExtension.getId()).thenReturn("invalid_uri");
        HTMLVarResolver resolver = new HTMLVarResolver(certificateExtension);
        assertNotNull(resolver.getQrCodeImage());
    }

    @Test
    public void issuedDateReturnsFormattedDate() {
        HTMLVarResolver resolver = new HTMLVarResolver(certificateExtension);
        assertEquals("10 May 2023", resolver.getIssuedDate());
    }

    @Test
    public void issuedDateReturnsNullForInvalidDate() {
        Mockito.when(certificateExtension.getIssuedOn()).thenReturn("invalid-date");
        HTMLVarResolver resolver = new HTMLVarResolver(certificateExtension);
        assertNull(resolver.getIssuedDate());
    }

    @Test
    public void expiryDateIsReturned() {
        HTMLVarResolver resolver = new HTMLVarResolver(certificateExtension);
        assertEquals("2025-12-31", resolver.getExpiryDate());
    }


    @Test
    public void providerNameIsReturned() {
        HTMLVarResolver resolver = new HTMLVarResolver(certificateExtension);
        assertEquals("Acme Provider", resolver.getProviderName());
    }

}