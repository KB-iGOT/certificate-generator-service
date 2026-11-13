package utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import static org.junit.Assert.*;

public class SvgToPngConverterTest {
    @Test
    public void rendersTextEmbeddedLogoAndScannableQrAtOriginalDimensions() throws Exception {
        BufferedImage logo = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                logo.setRGB(x, y, Color.RED.getRGB());
            }
        }
        BitMatrix matrix = new QRCodeWriter().encode("KAR-1353 + certificate", BarcodeFormat.QR_CODE, 96, 96);
        BufferedImage qr = new BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 96; x++) {
                qr.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'"
                + " width='320' height='180' viewBox='0 0 320 180'>"
                + "<rect width='320' height='180' fill='white'/>"
                + "<image x='20' y='20' width='16' height='16' href='" + dataUri(logo) + "'/>"
                + "<image x='200' y='20' width='96' height='96' xlink:href='" + dataUri(qr) + "'/>"
                + "<text x='20' y='155' font-family='sans-serif' font-size='20'>A+B &amp; é नमस्ते</text></svg>";

        BufferedImage rendered = render(svg);
        assertEquals(320, rendered.getWidth());
        assertEquals(180, rendered.getHeight());
        assertEquals(Color.RED.getRGB(), rendered.getRGB(25, 25));
        BufferedImage renderedQr = rendered.getSubimage(200, 20, 96, 96);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(renderedQr)));
        assertEquals("KAR-1353 + certificate", new MultiFormatReader().decode(bitmap).getText());
        int textPixels = 0;
        for (int y = 132; y < 165; y++) {
            for (int x = 20; x < 190; x++) {
                if (rendered.getRGB(x, y) != Color.WHITE.getRGB()) {
                    textPixels++;
                }
            }
        }
        assertTrue("UTF-8 text must be rendered, not omitted", textPixels > 30);
    }

    @Test
    public void viewBoxOnlyUsesViewportWithoutTreatingCoordinatesAsPixels() throws Exception {
        BufferedImage image = render("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0, 0, 200, 100'>"
                + "<rect width='200' height='100' fill='blue'/></svg>");
        assertEquals(1920, image.getWidth());
        assertEquals(1080, image.getHeight());
        assertEquals(Color.BLUE.getRGB(), image.getRGB(960, 540));
        assertEquals("ViewBox content must preserve its aspect ratio", Color.WHITE.getRGB(), image.getRGB(960, 10));
        assertEquals(Color.WHITE.getRGB(), image.getRGB(960, 1070));
    }

    @Test
    public void keepsExplicitDimensionsWhenTheyDifferFromViewBox() throws Exception {
        BufferedImage image = render("<svg xmlns='http://www.w3.org/2000/svg' width='400' height='200'"
                + " viewBox='0 0 200 100'><rect width='200' height='100' fill='blue'/></svg>");
        assertEquals(400, image.getWidth());
        assertEquals(200, image.getHeight());
        assertEquals(Color.BLUE.getRGB(), image.getRGB(399, 199));
    }

    @Test
    public void percentageDimensionsUsePreviousPublicApiViewport() throws Exception {
        BufferedImage image = render("<svg xmlns='http://www.w3.org/2000/svg' width='100%' height='100%'"
                + " viewBox='0 0 200 100'><rect width='200' height='100' fill='blue'/></svg>");
        assertEquals(1920, image.getWidth());
        assertEquals(1080, image.getHeight());
        assertEquals(Color.BLUE.getRGB(), image.getRGB(960, 540));
    }

    @Test
    public void percentagesMixedAndMissingDimensionsResolveAgainstViewport() throws Exception {
        assertSize("width='50%' height='50%'", 960, 540);
        assertSize("width='400' height='50%'", 400, 540);
        assertSize("width='50%' height='300'", 960, 300);
        assertSize("width='400'", 400, 1080);
        assertSize("height='300'", 1920, 300);
        assertSize("width='2in' height='1in'", 192, 96);
        assertSize("", 1920, 1080);
    }

    @Test
    public void largeViewBoxCoordinatesDoNotCountAsOutputPixels() throws Exception {
        BufferedImage image = render("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100000 50000'>"
                + "<rect width='100000' height='50000' fill='blue'/></svg>");
        assertEquals(1920, image.getWidth());
        assertEquals(1080, image.getHeight());
        assertEquals(Color.BLUE.getRGB(), image.getRGB(960, 540));
    }

    @Test
    public void rejectsOversizedInputBeforeXmlParsing() {
        try {
            SvgToPngConverter.convert(new byte[SvgToPngConverter.MAX_SVG_BYTES + 1]);
            fail("Oversized input must not reach the XML parser");
        } catch (SvgToPngConverter.RenderLimitException ex) {
            assertEquals(500, ex.getStatus());
            assertTrue(ex.getMessage().contains("4 MiB"));
        }
    }

    @Test
    public void boundsEncodedDataUriAndDecodedSvgInput() {
        assertUriLimit("data:image/svg+xml," + " ".repeat(3 * SvgToPngConverter.MAX_SVG_BYTES + 65), "encoded input");
        assertUriLimit(
                "data:image/svg+xml," + "A".repeat(SvgToPngConverter.MAX_SVG_BYTES + 1),
                "4 MiB");
    }

    @Test
    public void rejectsResolvedEdgeAndPixelLimitsBeforeChildImageDecoding() {
        assertDimensionLimit("width='4097' height='1'", "4096");
        assertDimensionLimit("width='1' height='4097'", "4096");
        assertDimensionLimit("width='3000' height='3000'", "8000000");
        assertDimensionLimit("width='300%' height='1'", "4096");
        assertDimensionLimit("width='1100mm' height='1'", "4096");
    }

    @Test
    public void rejectsLargeEmbeddedRasterUsingHeaderBeforePixelDecode() throws Exception {
        BufferedImage tiny = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(tiny, "png", output);
        byte[] png = output.toByteArray();
        // Rewrite PNG IHDR dimensions and its CRC; retain tiny image data.
        // A correct preflight rejects from metadata without trying to inflate a 5000px raster.
        ByteBuffer.wrap(png).putInt(16, 5000).putInt(20, 1);
        CRC32 crc = new CRC32();
        crc.update(png, 12, 17);
        ByteBuffer.wrap(png).putInt(29, (int) crc.getValue());
        assertSvgLimit(imageSvg("data:image/png;base64," + Base64.getEncoder().encodeToString(png)), "4096");
    }

    @Test
    public void nestedSvgDataResourceIsRejected() {
        String nested = "<svg xmlns='http://www.w3.org/2000/svg' width='5000' height='20'/>";
        String uri = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(nested.getBytes(StandardCharsets.UTF_8));

        try {
            SvgToPngConverter.convert(
                    imageSvg(uri).getBytes(StandardCharsets.UTF_8));
            fail("Nested SVG data resource must not be rendered");
        } catch (IllegalArgumentException ex) {
            assertNotNull(ex);
        }
    }

    @Test
    public void boundsDocumentComplexityBeforeBuildingRenderTree() {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='10'>"
                + "<g/>".repeat(SvgToPngConverter.MAX_SVG_ELEMENTS) + "</svg>";
        assertSvgLimit(svg, "elements");
    }

    @Test
    public void boundsConcurrentConversionsWithoutQueuingAndReleasesPermits() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<byte[]> first = executor.submit(() -> holdPermit(entered, release));
            Future<byte[]> second = executor.submit(() -> holdPermit(entered, release));
            assertTrue("Both render slots should be usable", entered.await(5, TimeUnit.SECONDS));
            try {
                SvgToPngConverter.convertDataUri("data:image/svg+xml,%3Csvg/%3E");
                fail("A third conversion must be rejected without being queued");
            } catch (SvgToPngConverter.RenderLimitException ex) {
                assertEquals(503, ex.getStatus());
                assertTrue(ex.getMessage().contains("busy"));
            }
            release.countDown();
            assertArrayEquals(new byte[] {1}, first.get(5, TimeUnit.SECONDS));
            assertArrayEquals(new byte[] {1}, second.get(5, TimeUnit.SECONDS));
            assertSize("width='20' height='10'", 20, 10);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void failedConversionDoesNotLeakRenderPermits() {
        for (int i = 0; i < 2 * SvgToPngConverter.MAX_CONCURRENT_RENDERS; i++) {
            try {
                SvgToPngConverter.withRenderPermit(() -> { throw new IllegalStateException("fixture failure"); });
                fail("The conversion should fail");
            } catch (IllegalStateException expected) {
                assertEquals("fixture failure", expected.getMessage());
            }
        }
        assertArrayEquals(new byte[] {1}, SvgToPngConverter.withRenderPermit(() -> new byte[] {1}));
    }

    @Test
    public void usesWhitePageBackgroundLikePreviousPublicApi() throws Exception {
        BufferedImage image = render("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='10'>"
                + "<rect width='5' height='5' fill='blue'/></svg>");
        assertEquals(Color.WHITE.getRGB(), image.getRGB(19, 9));
        assertEquals(Color.BLUE.getRGB(), image.getRGB(2, 2));
    }

    @Test
    public void rejectsMalformedXmlNonSvgAndInvalidImageContent() {
        assertRejected(null);
        assertRejected("");
        assertRejected("<svg");
        assertRejected("<html/>");
        assertRejected("<svg xmlns='http://www.w3.org/2000/svg' width='0' height='0'/>");
        assertRejected(imageSvg("data:image/png;base64,invalid"));
    }

    @Test
    public void rejectsDoctypeAndExternalEntities() {
        assertRejected("<!DOCTYPE svg [<!ENTITY external SYSTEM 'file:///etc/passwd'>]>"
                + "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20'><text>&external;</text></svg>");
    }

    @Test
    public void rejectsExternalImagesInsteadOfReturningAnIncompleteCertificate() {
        assertRejected(imageSvg("file:///not-a-certificate-logo.png"));
        assertRejected(imageSvg("http://127.0.0.1:1/not-a-certificate-logo.png"));
        assertRejected(imageSvg("https://example.invalid/not-a-certificate-logo.png"));
    }

    @Test
    public void doesNotExecuteSvgScripts() throws Exception {
        BufferedImage image = render("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='10'"
                + " onload='throw new Error(&quot;must not execute&quot;)'>"
                + "<script type='text/ecmascript'>throw new Error('must not execute');</script>"
                + "<rect width='20' height='10' fill='blue'/></svg>");
        assertEquals(Color.BLUE.getRGB(), image.getRGB(5, 5));
    }

    private static byte[] holdPermit(CountDownLatch entered, CountDownLatch release) {
        return SvgToPngConverter.withRenderPermit(() -> {
            entered.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to release render fixture");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            }
            return new byte[] {1};
        });
    }

    private static void assertSize(String attributes, int width, int height) throws Exception {
        BufferedImage image = render("<svg xmlns='http://www.w3.org/2000/svg' " + attributes + "/>");
        assertEquals(width, image.getWidth());
        assertEquals(height, image.getHeight());
    }

    private static void assertDimensionLimit(String attributes, String message) {
        assertSvgLimit("<svg xmlns='http://www.w3.org/2000/svg' " + attributes + ">"
                + "<image href='data:image/png;base64,invalid' width='1' height='1'/></svg>", message);
    }

    private static void assertSvgLimit(String svg, String message) {
        try {
            SvgToPngConverter.convert(svg.getBytes(StandardCharsets.UTF_8));
            fail("Expected a rendering limit violation");
        } catch (SvgToPngConverter.RenderLimitException ex) {
            assertEquals(500, ex.getStatus());
            assertTrue(ex.getMessage(), ex.getMessage().contains(message));
        }
    }

    private static void assertUriLimit(String uri, String message) {
        try {
            SvgToPngConverter.convertDataUri(uri);
            fail("Expected an input limit violation");
        } catch (SvgToPngConverter.RenderLimitException ex) {
            assertEquals(500, ex.getStatus());
            assertTrue(ex.getMessage(), ex.getMessage().contains(message));
        }
    }

    private static BufferedImage render(String svg) throws Exception {
        byte[] png = SvgToPngConverter.convert(svg.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}, Arrays.copyOf(png, 8));
        assertFalse(new String(png, StandardCharsets.UTF_8).startsWith("<svg"));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull("PNG output must be readable", image);
        return image;
    }

    private static String dataUri(BufferedImage image) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }

    private static String imageSvg(String href) {
        return "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='10'>"
                + "<image width='20' height='10' href='" + href + "'/></svg>";
    }

    private static void assertRejected(String svg) {
        try {
            SvgToPngConverter.convert(svg == null ? null : svg.getBytes(StandardCharsets.UTF_8));
            fail("Expected malformed or unsupported SVG to fail conversion");
        } catch (IllegalArgumentException expected) {
            // Conversion errors are translated by the existing wrapper failure mapper.
        }
    }
}
