package org.sunbird;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActorServiceExceptionTest {

    @Test
    void invalidOperationNameExceptionStoresValuesCorrectly() {
        ActorServiceException.InvalidOperationName ex = new ActorServiceException.InvalidOperationName("ERR_CODE", "Invalid operation", 400);
        assertEquals("ERR_CODE", ex.getCode());
        assertEquals("Invalid operation", ex.getMessage());
        assertEquals(400, ex.getResponseCode());
    }

    @Test
    void invalidRequestTimeoutExceptionStoresValuesCorrectly() {
        ActorServiceException.InvalidRequestTimeout ex = new ActorServiceException.InvalidRequestTimeout("TIMEOUT", "Request timed out", 408);
        assertEquals("TIMEOUT", ex.getCode());
        assertEquals("Request timed out", ex.getMessage());
        assertEquals(408, ex.getResponseCode());
    }

    @Test
    void invalidRequestDataExceptionStoresValuesCorrectly() {
        ActorServiceException.InvalidRequestData ex = new ActorServiceException.InvalidRequestData("BAD_DATA", "Invalid data", 422);
        assertEquals("BAD_DATA", ex.getCode());
        assertEquals("Invalid data", ex.getMessage());
        assertEquals(422, ex.getResponseCode());
    }
}
