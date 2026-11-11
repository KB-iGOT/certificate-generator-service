package utils;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

public class SvgDataUriDecoderTest {
    @Test
    public void decodesPercentEscapesExactlyOnceAndPreservesPlus() {
        assertDecoded("<svg>A+B+C D#25% %20 %2B</svg>",
                "%3Csvg%3EA+B%2BC%20D%2325%25%20%2520%20%252B%3C/svg%3E");
    }

    @Test
    public void preservesEmbeddedBase64AndAllPayloadCommas() {
        String svg = "<svg><image href='data:image/png;base64,ab+c/def=='/>"
                + "<image href='data:image/jpeg;base64,xy+z/12='/></svg>";
        assertDecoded(svg, svg.replace("<", "%3C").replace(">", "%3E"));
    }

    @Test
    public void decodesUtf8AndAcceptsLiteralUnicodeAndXmlDeclaration() {
        assertDecoded("<?xml version='1.0'?><svg>नमस्ते é</svg>",
                "%3C?xml version='1.0'?%3E%3Csvg%3Eनमस्ते%20%C3%A9%3C/svg%3E");
    }

    @Test
    public void acceptsLowercaseHexAndLiteralPercentAfterOneDecode() {
        assertDecoded("<svg>100% %ZZ</svg>", "%3csvg%3e100%25%20%25ZZ%3c/svg%3e");
    }

    @Test
    public void rejectsMissingBlankOrUnsupportedDataUris() {
        for (String uri : new String[] {null, "", " ", "<svg/>", "data:image/png,%3Csvg/%3E",
                "data:image/svg+xml;base64,PHN2Zy8+", "data:image/svg+xml,",
                "data:image/svg+xml, %20%09%0A"}) {
            assertRejected(uri);
        }
    }

    @Test
    public void rejectsMalformedPercentEscapesAndInvalidUtf8() {
        for (String payload : new String[] {"%", "%2", "%GG", "%FF", "%C3", "%C0%AF"}) {
            assertRejected("data:image/svg+xml," + payload);
        }
    }

    private static void assertDecoded(String expected, String payload) {
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8),
                SvgDataUriDecoder.decode("data:image/svg+xml," + payload));
    }

    private static void assertRejected(String uri) {
        try {
            SvgDataUriDecoder.decode(uri);
            fail("Expected an invalid SVG data URI to be rejected: " + uri);
        } catch (IllegalArgumentException expected) {
            // Expected invalid input.
        }
    }
}
