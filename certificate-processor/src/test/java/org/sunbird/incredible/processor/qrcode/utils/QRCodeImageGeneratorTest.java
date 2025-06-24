package org.sunbird.incredible.processor.qrcode.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.incredible.processor.qrcode.QRCodeGenerationModel;

import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeImageGeneratorTest {

    private QRCodeImageGenerator generator;
    private QRCodeGenerationModel model;
    private static final String OUTPUT_DIR = "build";

    @BeforeEach
    void setUp() {
        generator = new QRCodeImageGenerator();

        model = new QRCodeGenerationModel();
        model.setData("https://example.com/cert123.json");
        model.setText("CERT123");
        model.setFileName("build/test-cert");
        model.setErrorCorrectionLevel("M");
        model.setPixelsPerBlock(4);
        model.setQrCodeMargin(2);
        model.setTextFontName("Verdana"); // must exist in resources folder as Verdana.ttf
        model.setTextFontSize(12);
        model.setTextCharacterSpacing(0.1);
        model.setFileFormat("png");
        model.setColorModel("grayscale");
        model.setImageBorderSize(1);
        model.setQrCodeMarginBottom(2);
        model.setImageMargin(2);
    }

    @Test
    void testCreateQRImages_Success() throws Exception {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) {
            assertTrue(dir.mkdirs(), "Failed to create build/ directory for tests");
        }
        QRCodeGenerationModel model = new QRCodeGenerationModel();
        model.setData("https://example.org");
        model.setText("ExampleText");
        model.setFileName(OUTPUT_DIR + "/test-cert");
        model.setErrorCorrectionLevel("H");
        model.setPixelsPerBlock(10);
        model.setQrCodeMargin(2);
        model.setTextFontName("Verdana"); // make sure Verdana.ttf exists or it falls back gracefully
        model.setTextFontSize(14);
        model.setTextCharacterSpacing(0.1);
        model.setFileFormat("png");
        model.setColorModel("grayscale");
        model.setImageBorderSize(1);
        model.setQrCodeMarginBottom(10);
        model.setImageMargin(10);

        File file = generator.createQRImages(model);
        assertNotNull(file);
        assertTrue(file.exists(), "QR image file should be created");
        assertTrue(file.length() > 0, "QR image file should not be empty");
    }

    @Test
    void testCreateQRImages_WithoutText() throws Exception {
        model.setText("");
        File file = generator.createQRImages(model);
        assertNotNull(file);
        assertTrue(file.exists());
        file.delete();
    }

    @Test
    void testGetImageTypeRGB() {
        int type = invokeGetImageType("RGB");
        assertEquals(BufferedImage.TYPE_INT_RGB, type);
    }

    @Test
    void testGetImageTypeGray() {
        int type = invokeGetImageType("grayscale");
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, type);
    }

    private int invokeGetImageType(String model) {
        try {
            var method = QRCodeImageGenerator.class.getDeclaredMethod("getImageType", String.class);
            method.setAccessible(true);
            return (int) method.invoke(null, model);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
            return -1;
        }
    }

    @Test
    void testCreateQRImages_WithoutText_ShouldSucceed() throws Exception {
        model.setText(null); // Skip font logic
        File file = generator.createQRImages(model);
        assertNotNull(file);
        assertTrue(file.exists());
        file.delete();
    }


}
