package utils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.junit.Test;
import play.mvc.Http;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;
import org.sunbird.ActorServiceException;
import java.util.HashMap;
import java.util.Map;

public class RequestMapperTest {

    public static class DummyClass {
        public String name;
        public Map<String, Object> headers;
    }

    @Test
    public void mapRequest_MapsValidJsonRequestToObject() throws Exception {
        Http.Request request = mock(Http.Request.class);
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("name", "testName");
        Http.RequestBody body = mock(Http.RequestBody.class);
        when(request.body()).thenReturn(body);
        when(body.asJson()).thenReturn(jsonNode);
        Map<String, String[]> headersMap = new HashMap<>();
        headersMap.put("header1", new String[]{"value1"});
        Http.Headers headers = mock(Http.Headers.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.toMap()).thenReturn((Map) headersMap);

        DummyClass result = (DummyClass) RequestMapper.mapRequest(request, DummyClass.class);
        assertEquals("testName", result.name);
        assertNotNull(result.headers);
        assertTrue(result.headers.containsKey("header1"));
    }

    @Test(expected = ActorServiceException.InvalidRequestData.class)
    public void mapRequest_ThrowsExceptionWhenRequestIsNull() throws Exception {
        RequestMapper.mapRequest(null, DummyClass.class);
    }

    @Test(expected = ActorServiceException.InvalidRequestData.class)
    public void mapRequest_ThrowsExceptionWhenBodyIsNull() throws Exception {
        Http.Request request = mock(Http.Request.class);
        when(request.body()).thenReturn(null);
        RequestMapper.mapRequest(request, DummyClass.class);
    }

    @Test(expected = ActorServiceException.InvalidRequestData.class)
    public void mapRequest_ThrowsExceptionWhenBodyAsJsonIsNull() throws Exception {
        Http.Request request = mock(Http.Request.class);
        Http.RequestBody body = mock(Http.RequestBody.class);
        when(request.body()).thenReturn(body);
        when(body.asJson()).thenReturn(null);
        RequestMapper.mapRequest(request, DummyClass.class);
    }

    @Test(expected = ActorServiceException.InvalidRequestData.class)
    public void mapRequest_ThrowsExceptionWhenJsonCannotBeMapped() throws Exception {
        Http.Request request = mock(Http.Request.class);
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("invalid", "data");
        Http.RequestBody body = mock(Http.RequestBody.class);
        when(request.body()).thenReturn(body);
        when(body.asJson()).thenReturn(jsonNode);
        Http.Headers headers = mock(Http.Headers.class);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.toMap()).thenReturn(new HashMap<>());
        RequestMapper.mapRequest(request, Integer.class);
    }
}
