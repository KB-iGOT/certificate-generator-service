package org.sunbird.incredible.processor.qrcode;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccessCodeGeneratorTest {

    @Test
    void testGenerateCode_Success() {
        AccessCodeGenerator generator = new AccessCodeGenerator(6.0);
        String code = generator.generate();
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("[A-Z][0-9][A-Z][0-9][A-Z][0-9]"));
    }

    @Test
    void testBaseN_ZeroInput() throws Exception {
        AccessCodeGenerator generator = new AccessCodeGenerator(6.0);
        String result = invokeBaseN(generator, BigDecimal.ZERO, 10);
        assertEquals("0", result);
    }

    @Test
    void testBaseN_NonZeroInput() throws Exception {
        AccessCodeGenerator generator = new AccessCodeGenerator(6.0);
        BigDecimal input = new BigDecimal(12345);
        String result = invokeBaseN(generator, input, 10);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testIsValidCode_Valid() throws Exception {
        AccessCodeGenerator generator = new AccessCodeGenerator(6.0);
        boolean valid = invokeIsValidCode(generator, "A1B2C3");
        assertTrue(valid);
    }

    @Test
    void testIsValidCode_Invalid() throws Exception {
        AccessCodeGenerator generator = new AccessCodeGenerator(6.0);
        boolean valid = invokeIsValidCode(generator, "ABC123");
        assertFalse(valid);
    }

    // ===== Helpers to access private methods via reflection =====

    private String invokeBaseN(AccessCodeGenerator generator, BigDecimal num, int base) throws Exception {
        var method = AccessCodeGenerator.class.getDeclaredMethod("baseN", BigDecimal.class, int.class);
        method.setAccessible(true);
        return (String) method.invoke(generator, num, base);
    }

    private boolean invokeIsValidCode(AccessCodeGenerator generator, String code) throws Exception {
        var method = AccessCodeGenerator.class.getDeclaredMethod("isValidCode", String.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(generator, code);
    }
}
