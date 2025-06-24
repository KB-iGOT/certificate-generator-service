package org.sunbird.incredible;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.sunbird.incredible.pojos.CertificateExtension;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.incredible.processor.qrcode.AccessCodeGenerator;
import org.sunbird.incredible.processor.qrcode.utils.QRCodeImageGenerator;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CertificateGeneratorTest {

    private Map<String, String> properties;
    private CertificateGenerator certificateGenerator;
    private String directory = "tempTestDir/";

    @BeforeEach
    void setUp() {
        properties = new HashMap<>();
        properties.put(JsonKey.ACCESS_CODE_LENGTH, "6");
        properties.put(JsonKey.BASE_PATH, "https://certs.example.org");
        certificateGenerator = new CertificateGenerator(properties, directory);
    }

    @Test
    void testGetUUID_validURI() {
        CertificateExtension certExt = new CertificateExtension("");
        certExt.setId("https://domain.org/cert/abc123.json");
        String uuid = certificateGenerator.getUUID(certExt);
        assertEquals("abc123", uuid);
    }

    @Test
    void testGetUUID_invalidURI() {
        CertificateExtension certExt = new CertificateExtension("");
        certExt.setId("invalid uri");
        assertNull(certificateGenerator.getUUID(certExt));
    }

    @Test
    void testGenerateCertificateJson_createsFileAndReturnsJson() throws IOException {
        CertificateExtension certExt = new CertificateExtension("");
        certExt.setId("https://example.org/cert/test123.json");
        certExt.setContext("https://example.org/context");

        String json = certificateGenerator.generateCertificateJson(certExt);
        assertTrue(json.contains("https://example.org/context"));
        File file = new File(directory + "test123.json");
        assertTrue(file.exists());
        file.delete();
    }

    @Test
    void testGenerateQrCode() throws Exception {
        try (MockedConstruction<AccessCodeGenerator> mockAccessCode =
                     Mockito.mockConstruction(AccessCodeGenerator.class,
                             (mock, context) -> when(mock.generate()).thenReturn("123456"));
             MockedConstruction<QRCodeImageGenerator> mockQrGen =
                     Mockito.mockConstruction(QRCodeImageGenerator.class,
                             (mock, context) -> when(mock.createQRImages(any())).thenReturn(new File("testQr.png")))) {

            CertificateExtension ext = new CertificateExtension("");
            ext.setId("https://example.org/cert/qrTest123.json");

            // ✅ Set private field via reflection
            java.lang.reflect.Field field = CertificateGenerator.class.getDeclaredField("certificateExtension");
            field.setAccessible(true);
            field.set(certificateGenerator, ext);

            Map<String, Object> result = certificateGenerator.generateQrCode();

            assertEquals("123456", result.get(JsonKey.ACCESS_CODE));
            assertNotNull(result.get(JsonKey.QR_CODE_FILE));
        }
    }

    @Test
    void testGenerateQrCodeFromAccessCode() throws Exception {
        try (MockedConstruction<QRCodeImageGenerator> mockQrGen =
                     Mockito.mockConstruction(QRCodeImageGenerator.class,
                             (mock, context) -> when(mock.createQRImages(any())).thenReturn(new File("testQr.png")))) {

            CertificateExtension ext = new CertificateExtension("");
            ext.setId("https://example.org/cert/fromAccessCode.json");

            // Set private field via reflection
            java.lang.reflect.Field field = CertificateGenerator.class.getDeclaredField("certificateExtension");
            field.setAccessible(true);
            field.set(certificateGenerator, ext);

            Map<String, Object> result = certificateGenerator.generateQrCodeFromAccessCode("654321");
            assertEquals("654321", result.get(JsonKey.ACCESS_CODE));
            assertNotNull(result.get(JsonKey.QR_CODE_FILE));
        }
    }
}
