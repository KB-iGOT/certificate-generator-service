package org.sunbird.incredible.pojos;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class CertificateExtensionTest {

    @Test
    void testGettersAndSetters() {
        String context = "https://example.org/context";
        CertificateExtension certExt = new CertificateExtension(context);

        // Verify context and type
        assertEquals(context, certExt.getContext());
        assertArrayEquals(
                new String[]{"Assertion", "Extension", "extensions:CertificateExtension"},
                certExt.getType()
        );

        // awardedThrough
        String awardedThrough = "Test Program";
        certExt.setAwardedThrough(awardedThrough);
        assertEquals(awardedThrough, certExt.getAwardedThrough());

        // signatory
        SignatoryExtension signer1 = new SignatoryExtension("");
        SignatoryExtension signer2 = new SignatoryExtension("");
        SignatoryExtension[] signatories = new SignatoryExtension[]{signer1, signer2};
        certExt.setSignatory(signatories);
        assertArrayEquals(signatories, certExt.getSignatory());

        // printUri
        String printUri = "data:application/pdf;base64,XYZ123==";
        certExt.setPrintUri(printUri);
        assertEquals(printUri, certExt.getPrintUri());

        // validFrom
        String validFrom = "2023-01-01T00:00:00+00:00";
        certExt.setValidFrom(validFrom);
        assertEquals(validFrom, certExt.getValidFrom());

        // signature
        Signature signature = new Signature();
        certExt.setSignature(signature);
        assertEquals(signature, certExt.getSignature());

        // providerName
        String providerName = "ABC University";
        certExt.setProviderName(providerName);
        assertEquals(providerName, certExt.getProviderName());
    }
}
