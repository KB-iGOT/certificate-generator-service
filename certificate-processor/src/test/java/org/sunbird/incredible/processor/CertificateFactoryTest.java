package org.sunbird.incredible.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sunbird.incredible.pojos.ob.Issuer;
import org.sunbird.incredible.processor.signature.SignatureHelper;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CertificateFactoryTest {

    @InjectMocks
    private CertificateFactory certificateFactory;

    @Mock
    private SignatureHelper signatureHelper;

    private CertModel certModel;
    private Map<String, String> props;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        certModel = new CertModel();
        certModel.setRecipientName("John Doe");
        certModel.setIdentifier("did:1234");
        certModel.setCertificateDescription("Test Certificate");
        certModel.setCertificateLogo("https://example.com/logo.png");
        certModel.setCourseName("Test Course");
        certModel.setCertificateName("Completion Cert");
        certModel.setIssuedDate("2024-06-18T00:00:00Z");
        certModel.setExpiry("2025-06-18T00:00:00Z");
        certModel.setValidFrom("2024-06-18T00:00:00Z");
        certModel.setProviderName("MyOrg");

        Issuer issuer = new Issuer("");
        issuer.setName("IssuerName");
        issuer.setUrl("https://issuer.com");
        issuer.setPublicKey(new String[]{"publicKey"});
        certModel.setIssuer(issuer);
        //certModel.setSignatoryList(new Signatory[]{});

        props = new HashMap<>();
        props.put(JsonKey.CONTEXT, "https://example.com/context");
        props.put(JsonKey.ISSUER_URL, "https://issuer.com");
        props.put(JsonKey.BADGE_URL, "https://badge.com/badge");
        props.put(JsonKey.EVIDENCE_URL, "https://badge.com/evidence");
        props.put(JsonKey.BASE_PATH, "https://example.com/certificates");
        props.put(JsonKey.SIGN_CREATOR, "https://signer.com/123_abc");
        props.put(JsonKey.ENC_SERVICE_URL, "https://encryptor.com");
        props.put(JsonKey.PUBLIC_KEY_URL, "https://signer.com/123_abc");
        props.put(JsonKey.KEY_ID, "123");
        props.put(JsonKey.TAG, "mytag");
    }

    @Test
    public void testGetKeyIdInvalidCreatorUrl() {
        String creator = "invalid url";
        assertThrows(NumberFormatException.class, () -> {
            new CertificateFactory().verifySignature(new ObjectMapper().createObjectNode(), "sigVal", "https://url", creator);
        });
    }
}
