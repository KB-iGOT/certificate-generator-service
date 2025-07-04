package utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.BaseException;
import org.sunbird.ActorServiceException;
import play.libs.Json;
import play.mvc.Http;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestMapperTest {

    Http.Request mockRequest;
    Http.RequestBody mockBody;
    Http.Headers mockHeaders;

    @BeforeEach
    void setUp() {
        mockRequest = mock(Http.Request.class);
        mockBody = mock(Http.RequestBody.class);
        mockHeaders = mock(Http.Headers.class);

        when(mockRequest.body()).thenReturn(mockBody);
        when(mockRequest.getHeaders()).thenReturn(mockHeaders);
    }

    @Test
    void testMapRequest_Success() throws BaseException {
        // given
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("field", "value");
        Map<String, String[]> headersMap = Collections.singletonMap("Authorization", new String[]{"Bearer token"});

        when(mockBody.asJson()).thenReturn(jsonNode);
       // when(mockHeaders.toMap()).thenReturn(headersMap);

        // when
        DummyClass result = (DummyClass) RequestMapper.mapRequest(mockRequest, DummyClass.class);

        // then
        assertNotNull(result);
        assertEquals("value", result.field);
    }

    @Test
    void testMapRequest_NullRequest_ThrowsException() {
        ActorServiceException.InvalidRequestData ex =
                assertThrows(ActorServiceException.InvalidRequestData.class,
                        () -> RequestMapper.mapRequest(null, DummyClass.class));

        assertEquals("INVALID_REQUESTED_DATA", ex.getCode());
    }

    @Test
    void testMapRequest_BodyIsNull_ThrowsException() {
        when(mockBody.asJson()).thenReturn(null);

        ActorServiceException.InvalidRequestData ex =
                assertThrows(ActorServiceException.InvalidRequestData.class,
                        () -> RequestMapper.mapRequest(mockRequest, DummyClass.class));

        assertEquals("INVALID_REQUESTED_DATA", ex.getCode());
    }

    @Test
    void testMapRequest_InvalidJson_ThrowsException() {
        // simulate exception while casting or setting headers
        when(mockBody.asJson()).thenReturn(Json.newObject());
        when(mockHeaders.toMap()).thenThrow(new RuntimeException("bad headers"));

        ActorServiceException.InvalidRequestData ex =
                assertThrows(ActorServiceException.InvalidRequestData.class,
                        () -> RequestMapper.mapRequest(mockRequest, DummyClass.class));

        assertEquals("INVALID_REQUESTED_DATA", ex.getCode());
    }

    static class DummyClass {
        public String field;
    }
}
