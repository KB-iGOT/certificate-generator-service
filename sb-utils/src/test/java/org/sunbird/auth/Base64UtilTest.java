package org.sunbird.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Base64UtilTest {

    @Test
    void encodeToStringReturnsCorrectBase64() {
        String result = Base64Util.encodeToString("hello".getBytes(), Base64Util.DEFAULT);
        Assertions.assertEquals("aGVsbG8=\n", result);
    }

    @Test
    void encodeToStringWithOffsetAndLengthReturnsCorrectBase64() {
        byte[] input = "hello world".getBytes();
        String result = Base64Util.encodeToString(input, 6, 5, Base64Util.DEFAULT);
        Assertions.assertEquals("d29ybGQ=\n", result);
    }

    @Test
    void encodeReturnsCorrectBase64Bytes() {
        byte[] encoded = Base64Util.encode("abc".getBytes(), Base64Util.DEFAULT);
        Assertions.assertArrayEquals("YWJj\n".getBytes(), encoded);
    }

    @Test
    void encodeWithOffsetAndLengthReturnsCorrectBase64Bytes() {
        byte[] input = "abcdef".getBytes();
        byte[] encoded = Base64Util.encode(input, 2, 4, Base64Util.DEFAULT);
        Assertions.assertArrayEquals("Y2RlZg==\n".getBytes(), encoded);
    }

    @Test
    void decodeStringReturnsOriginalBytes() {
        byte[] decoded = Base64Util.decode("aGVsbG8=", Base64Util.DEFAULT);
        Assertions.assertArrayEquals("hello".getBytes(), decoded);
    }

    @Test
    void decodeByteArrayReturnsOriginalBytes() {
        byte[] encoded = "aGVsbG8=".getBytes();
        byte[] decoded = Base64Util.decode(encoded, Base64Util.DEFAULT);
        Assertions.assertArrayEquals("hello".getBytes(), decoded);
    }

    @Test
    void decodeWithOffsetAndLengthReturnsOriginalBytes() {
        byte[] encoded = "aGVsbG8=".getBytes();
        byte[] decoded = Base64Util.decode(encoded, 0, encoded.length, Base64Util.DEFAULT);
        Assertions.assertArrayEquals("hello".getBytes(), decoded);
    }

    @Test
    void encodeToStringNoPaddingOmitsPadding() {
        String result = Base64Util.encodeToString("hi".getBytes(), Base64Util.NO_PADDING);
        Assertions.assertEquals("aGk\n", result);
    }

    @Test
    void encodeToStringNoWrapProducesSingleLine() {
        String longInput = "a".repeat(60);
        String result = Base64Util.encodeToString(longInput.getBytes(), Base64Util.NO_WRAP);
        Assertions.assertFalse(result.contains("\n"));
    }

    @Test
    void encodeToStringUrlSafeUsesUrlSafeAlphabet() {
        String result = Base64Util.encodeToString(new byte[] {(byte) 0xfb, (byte) 0xff}, Base64Util.URL_SAFE | Base64Util.NO_PADDING);
        Assertions.assertEquals("-_8\n", result);
    }

//    @Test
//    void decodeThrowsIllegalArgumentExceptionOnBadInput() {
//        Assertions.assertThrows(IllegalArgumentException.class, () -> Base64Util.decode("!!!", Base64Util.DEFAULT));
//    }

    @Test
    void decodeHandlesInputWithoutPadding() {
        byte[] decoded = Base64Util.decode("aGVsbG8", Base64Util.DEFAULT);
        Assertions.assertArrayEquals("hello".getBytes(), decoded);
    }

    @Test
    void encodeAndDecodeAreInverseOperations() {
        byte[] original = "unit test data".getBytes();
        String encoded = Base64Util.encodeToString(original, Base64Util.DEFAULT);
        byte[] decoded = Base64Util.decode(encoded, Base64Util.DEFAULT);
        Assertions.assertArrayEquals(original, decoded);
    }

    @Test
    void encodeToStringWithCrLfFlagAddsCarriageReturn() {
        String result = Base64Util.encodeToString("a".repeat(57).getBytes(), Base64Util.CRLF);
        Assertions.assertTrue(result.contains("\r\n"));
    }

    @Test
    void encodeToStringWithNoCloseFlagBehavesNormally() {
        String result = Base64Util.encodeToString("test".getBytes(), Base64Util.NO_CLOSE);
        Assertions.assertEquals("dGVzdA==\n", result);
    }

    @Test
    void encodeWithOneRemainingByteTriggersTailLogic() {
        byte[] input = new byte[]{(byte) 0xFC}; // binary 11111100
        byte[] encoded = Base64Util.encode(input, Base64Util.DEFAULT);
        Assertions.assertEquals("/A==\n", new String(encoded));
    }

    // ✅ Tail handling: 2 remaining bytes
    @Test
    void encodeWithTwoRemainingBytesTriggersTailLogic() {
        byte[] input = new byte[]{(byte) 0xFC, (byte) 0x0F};
        byte[] encoded = Base64Util.encode(input, Base64Util.DEFAULT);
        Assertions.assertEquals("/A8=\n", new String(encoded));
    }

    // ✅ tailLen = 1, process in next round with 2 bytes
    @Test
    void encodeMultipleCallsTriggersTailProcessingFromPreviousInput() {
        byte[] input = new byte[]{0x11};
        Base64Util.Encoder encoder = new Base64Util.Encoder(Base64Util.DEFAULT, null);
        byte[] out = new byte[20];
        encoder.process(input, 0, 1, false); // tailLen becomes 1

        byte[] moreInput = new byte[]{0x22, 0x33}; // triggers case 1 in switch
        encoder.output = out;
        encoder.process(moreInput, 0, 2, false); // uses tailLen = 1 logic

        Assertions.assertTrue(encoder.op > 0); // Output should be filled
    }

    // ✅ tailLen = 2, process in next round with 1 byte
    @Test
    void encodeMultipleCallsTriggersTail2Processing() {
        byte[] input = new byte[]{0x11, 0x22};
        Base64Util.Encoder encoder = new Base64Util.Encoder(Base64Util.DEFAULT, null);
        byte[] out = new byte[20];
        encoder.output = out;
        encoder.process(input, 0, 2, false); // tailLen becomes 2

        byte[] moreInput = new byte[]{0x33}; // triggers case 2 in switch
        encoder.output = out;
        encoder.process(moreInput, 0, 1, false);

        Assertions.assertTrue(encoder.op > 0); // Output should be filled
    }

    // ✅ Test final tail flush with finish = true (1 byte)
    @Test
    void finishTrueWith1TailByteShouldProducePadding() {
        byte[] input = new byte[]{0x41}; // 'A'
        byte[] encoded = Base64Util.encode(input, 0, 1, Base64Util.DEFAULT);
        Assertions.assertEquals("QQ==\n", new String(encoded));
    }

    // ✅ Test final tail flush with finish = true (2 bytes)
    @Test
    void finishTrueWith2TailBytesShouldProduceCorrectPadding() {
        byte[] input = new byte[]{0x41, 0x42}; // 'AB'
        byte[] encoded = Base64Util.encode(input, 0, 2, Base64Util.DEFAULT);
        Assertions.assertEquals("QUI=\n", new String(encoded));
    }
}