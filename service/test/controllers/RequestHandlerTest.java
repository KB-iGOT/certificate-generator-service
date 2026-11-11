package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.sunbird.BaseException;
import org.sunbird.message.ResponseCode;
import org.sunbird.response.Response;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import play.test.Helpers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.*;

public class RequestHandlerTest {
    private final Http.Request request = new Http.RequestBuilder().uri("/v1/certs/legacy/render/id").build();

    @Test
    public void defaultAndNullMapperKeepJsonContract() {
        Response response = new Response();
        response.put("printUri", "data:image/svg+xml,%3Csvg/%3E");
        assertJsonResponse(RequestHandler.handleResponse(response, request));
        assertJsonResponse(RequestHandler.handleResponse(response, request, null));
    }

    @Test
    public void optionalMapperFormatsSuccessExactlyOnce() {
        AtomicInteger calls = new AtomicInteger();
        Response response = new Response();
        Result result = RequestHandler.handleResponse(response, request, value -> {
            assertSame(response, value);
            calls.incrementAndGet();
            return Results.ok("<svg/>").as("image/svg+xml");
        });
        assertEquals(1, calls.get());
        assertEquals(200, result.status());
        assertEquals("image/svg+xml", result.contentType().get());
        assertEquals("<svg/>", Helpers.contentAsString(result));
    }

    @Test
    public void generationErrorsBypassMapperAndPreserveExistingFormatting() {
        for (int status : new int[] {400, 500, 503}) {
            BaseException error = new BaseException("existing.error", "Existing generation failure", status);
            Function<Response, Result> mapper = response -> {
                fail("Failure reply must not invoke the success mapper");
                return null;
            };
            Result expected = RequestHandler.handleResponse(error, request);
            Result actual = RequestHandler.handleResponse(error, request, mapper);
            assertEquals(status, actual.status());
            assertEquals("application/json", actual.contentType().get());
            JsonNode expectedJson = Json.parse(Helpers.contentAsString(expected));
            JsonNode actualJson = Json.parse(Helpers.contentAsString(actual));
            ((com.fasterxml.jackson.databind.node.ObjectNode) expectedJson).remove("ts");
            ((com.fasterxml.jackson.databind.node.ObjectNode) actualJson).remove("ts");
            assertEquals(expectedJson, actualJson);
        }
    }

    @Test
    public void nonOkResponseBypassesMapperAndKeepsDefaultFormatting() {
        Response response = new Response();
        response.setResponseCode(ResponseCode.SERVER_ERROR);
        Result result = RequestHandler.handleResponse(response, request, value -> {
            fail("Only OK responses should invoke the success mapper");
            return null;
        });
        assertEquals("application/json", result.contentType().get());
        assertEquals("SERVER_ERROR", Json.parse(Helpers.contentAsString(result)).get("responseCode").asText());
    }

    @Test
    public void mapperFailureUsesExistingJsonFailureFormatter() {
        Result actual = RequestHandler.handleResponse(new Response(), request, response -> {
            throw new IllegalArgumentException("Malformed generated printUri");
        });
        assertEquals(500, actual.status());
        assertEquals("application/json", actual.contentType().get());
        assertEquals("SERVER_ERROR", Json.parse(Helpers.contentAsString(actual)).get("responseCode").asText());
    }

    private static void assertJsonResponse(Result result) {
        assertEquals(200, result.status());
        assertEquals("application/json", result.contentType().get());
        JsonNode json = Json.parse(Helpers.contentAsString(result));
        assertEquals("OK", json.get("responseCode").asText());
        assertEquals("v1", json.get("ver").asText());
        assertEquals("v1.certs.legacy.render.id", json.get("id").asText());
        assertEquals("data:image/svg+xml,%3Csvg/%3E", json.get("result").get("printUri").asText());
        assertNotNull(json.get("ts"));
    }
}
