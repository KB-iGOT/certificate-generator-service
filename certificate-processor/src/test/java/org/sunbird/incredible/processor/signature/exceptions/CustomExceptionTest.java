package org.sunbird.incredible.processor.signature.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {

    @Test
    void testCustomExceptionMessage() {
        String message = "This is a custom exception message";
        CustomException exception = new CustomException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }
}
