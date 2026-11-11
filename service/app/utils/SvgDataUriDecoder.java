package utils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Decodes the non-base64 SVG data URI produced by the existing certificate generator. */
public final class SvgDataUriDecoder {
    private static final String PREFIX = "data:image/svg+xml,";

    private SvgDataUriDecoder() {
    }

    public static byte[] decode(String printUri) {
        if (printUri == null || printUri.isBlank() || !printUri.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Expected a nonblank SVG data URI");
        }
        byte[] payload = printUri.substring(PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream(payload.length);
        for (int i = 0; i < payload.length; i++) {
            if (payload[i] == '%') {
                if (i + 2 >= payload.length) {
                    throw new IllegalArgumentException("Incomplete percent escape in SVG data URI");
                }
                int high = Character.digit((char) payload[++i], 16);
                int low = Character.digit((char) payload[++i], 16);
                if (high < 0 || low < 0) {
                    throw new IllegalArgumentException("Invalid percent escape in SVG data URI");
                }
                output.write((high << 4) | low);
            } else {
                // Copy literal '+' unchanged, including in embedded QR/image base64.
                // Decode each percent escape only once; decoded bytes are never reprocessed.
                output.write(payload[i]);
            }
        }
        byte[] svg = output.toByteArray();
        try {
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(svg)).toString();
            if (decoded.isBlank()) {
                throw new IllegalArgumentException("SVG data URI payload must not be blank");
            }
        } catch (CharacterCodingException ex) {
            throw new IllegalArgumentException("SVG data URI must contain valid UTF-8", ex);
        }
        return svg;
    }
}
