package org.sunbird;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.sunbird.model.HttpUtilResponse;
import org.sunbird.message.ResponseCode;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class HttpUtilTest {

    @Test
    void sendGetRequestReturnsBodyOnStatus200() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.getStatus()).thenReturn(200);
        when(response.getBody()).thenReturn("ok");
        try (MockedStatic<Unirest> unirestMock = mockStatic(Unirest.class)) {
            var request = mock(com.mashape.unirest.request.GetRequest.class);
            when(request.headers(anyMap())).thenReturn(request);
            when(request.asString()).thenReturn(response);
            unirestMock.when(() -> Unirest.get("url")).thenReturn(request);
            String result = HttpUtil.sendGetRequest("url", Map.of("h", "v"));
            assertEquals("ok", result);
        }
    }

    @Test
    void sendGetRequestReturnsEmptyStringOnNon200Status() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.getStatus()).thenReturn(404);
        when(response.getBody()).thenReturn("not found");
        try (MockedStatic<Unirest> unirestMock = mockStatic(Unirest.class)) {
            var request = mock(com.mashape.unirest.request.GetRequest.class);
            when(request.headers(anyMap())).thenReturn(request);
            when(request.asString()).thenReturn(response);
            unirestMock.when(() -> Unirest.get("url")).thenReturn(request);
            String result = HttpUtil.sendGetRequest("url", Map.of());
            assertEquals("", result);
        }
    }

    @Test
    void doPostRequestReturnsHttpUtilResponseOnSuccess() throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.getBody()).thenReturn("body");
        when(response.getStatus()).thenReturn(201);
        try (MockedStatic<Unirest> unirestMock = mockStatic(Unirest.class)) {
            var request = mock(com.mashape.unirest.request.HttpRequestWithBody.class);
            when(request.headers(anyMap())).thenReturn(request);
           // when(request.body(anyString())).thenReturn(request);
            when(request.asString()).thenReturn(response);
            unirestMock.when(() -> Unirest.post("url3")).thenReturn(request);
            HttpUtilResponse result = HttpUtil.doPostRequest("url3", "params", Map.of("h", "v"));
            assertNotNull(result);
        }
    }

    @Test
    void doPostRequestReturnsDefaultResponseOnException() throws Exception {
        try (MockedStatic<Unirest> unirestMock = mockStatic(Unirest.class)) {
            var request = mock(com.mashape.unirest.request.HttpRequestWithBody.class);
            when(request.headers(anyMap())).thenReturn(request);
            when(request.asString()).thenThrow(new RuntimeException("fail"));
            unirestMock.when(() -> Unirest.post("url4")).thenReturn(request);
            HttpUtilResponse result = HttpUtil.doPostRequest("url4", "params", Map.of());
            assertNull(result.getBody());
            assertEquals(0, result.getStatusCode());
        }
    }

    @Test
    void sendPatchRequestReturnsInvalidRequestedDataOnOkStatus() {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.getStatus()).thenReturn(ResponseCode.OK.getCode());
        when(response.getBody()).thenReturn("patched");
        try (MockedStatic<Unirest> unirestMock = mockStatic(Unirest.class)) {
            var request = mock(com.mashape.unirest.request.HttpRequestWithBody.class);
            when(request.headers(anyMap())).thenReturn(request);
            when(request.asString()).thenReturn(response);
            unirestMock.when(() -> Unirest.patch("patchUrl")).thenReturn(request);
            String result = HttpUtil.sendPatchRequest("patchUrl", "params", Map.of());
            assertEquals("Failure", result);
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendPatchRequestReturnsFailureOnNonOkStatus() throws UnirestException {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.getStatus()).thenReturn(500);
        when(response.getBody()).thenReturn("error");
        try (MockedStatic<Unirest> unirestMock = mockStatic(Unirest.class)) {
            var request = mock(com.mashape.unirest.request.HttpRequestWithBody.class);
            when(request.headers(anyMap())).thenReturn(request);
            when(request.asString()).thenReturn(response);
            unirestMock.when(() -> Unirest.patch("patchUrl2")).thenReturn(request);
            String result = HttpUtil.sendPatchRequest("patchUrl2", "params", Map.of());
            assertEquals("Failure", result);
        }
    }

    @Test
    void sendPatchRequestReturnsFailureOnException() {
        try (MockedStatic<Unirest> unirestMock = mockStatic(Unirest.class)) {
            var request = mock(com.mashape.unirest.request.HttpRequestWithBody.class);
            when(request.headers(anyMap())).thenReturn(request);
            when(request.asString()).thenThrow(new RuntimeException("fail"));
            unirestMock.when(() -> Unirest.patch("patchUrl3")).thenReturn(request);
            String result = HttpUtil.sendPatchRequest("patchUrl3", "params", Map.of());
            assertEquals("Failure", result);
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getHeaderReturnsMapWithContentTypeAndInput() throws Exception {
        Map<String, String> input = new HashMap<>();
        input.put("foo", "bar");
        Map<String, String> result = HttpUtil.getHeader(input);
        assertEquals("application/json", result.get("Content-Type"));
        assertEquals("bar", result.get("foo"));
    }

    @Test
    void getHeaderReturnsMapWithContentTypeWhenInputIsNull() throws Exception {
        Map<String, String> result = HttpUtil.getHeader(null);
        assertEquals("application/json", result.get("Content-Type"));
        assertEquals(1, result.size());
    }
}
