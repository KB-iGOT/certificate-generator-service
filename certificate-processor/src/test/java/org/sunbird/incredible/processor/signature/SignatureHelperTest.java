package org.sunbird.incredible.processor.signature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.incredible.processor.signature.exceptions.SignatureException;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignatureHelperTest {

    private SignatureHelper signatureHelper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        signatureHelper = new SignatureHelper("http://localhost:8080");
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGenerateSignature_success() throws Exception {
        JsonNode node = objectMapper.readTree("{\"key\":\"value\"}");

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
            CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
            HttpEntity mockEntity = mock(HttpEntity.class);

            mocked.when(HttpClients::createDefault).thenReturn(mockClient);
            when(mockClient.execute(any())).thenReturn(mockResponse);
            when(mockResponse.getEntity()).thenReturn(mockEntity);
            when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream("{\"signed\":true}".getBytes()));

            Map<String, Object> response = signatureHelper.generateSignature(node, "key123");
            assertTrue((Boolean) response.get("signed"));
        }
    }

    @Test
    void testGenerateSignature_UnreachableException() throws Exception {
        JsonNode node = objectMapper.readTree("{\"key\":\"value\"}");

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
            mocked.when(HttpClients::createDefault).thenReturn(mockClient);
            when(mockClient.execute(any())).thenThrow(new org.apache.http.client.ClientProtocolException("Protocol error"));

            SignatureException.UnreachableException ex = assertThrows(SignatureException.UnreachableException.class,
                    () -> signatureHelper.generateSignature(node, "key123"));
            assertTrue(ex.getMessage().contains("Unable to reach service"));
        }
    }

    @Test
    void testGenerateSignature_CreationException() throws Exception {
        JsonNode node = objectMapper.readTree("{\"key\":\"value\"}");

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
            mocked.when(HttpClients::createDefault).thenReturn(mockClient);
            when(mockClient.execute(any())).thenThrow(new java.io.IOException("IO error"));

            SignatureException.CreationException ex = assertThrows(SignatureException.CreationException.class,
                    () -> signatureHelper.generateSignature(node, "key123"));
            assertTrue(ex.getMessage().contains("Unable to create signature"));
        }
    }

    @Test
    void testVerifySignature_success() throws Exception {
        JsonNode node = objectMapper.readTree("{\"key\":\"value\"}");

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
            CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
            HttpEntity mockEntity = mock(HttpEntity.class);

            mocked.when(HttpClients::createDefault).thenReturn(mockClient);
            when(mockClient.execute(any())).thenReturn(mockResponse);
            when(mockResponse.getEntity()).thenReturn(mockEntity);
            when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream("true".getBytes()));

            boolean result = signatureHelper.verifySignature(node);
            assertTrue(result);
        }
    }

    @Test
    void testVerifySignature_UnreachableException() throws Exception {
        JsonNode node = objectMapper.readTree("{\"key\":\"value\"}");

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
            mocked.when(HttpClients::createDefault).thenReturn(mockClient);
            when(mockClient.execute(any())).thenThrow(new org.apache.http.client.ClientProtocolException("Protocol error"));

            SignatureException.UnreachableException ex = assertThrows(SignatureException.UnreachableException.class,
                    () -> signatureHelper.verifySignature(node));
            assertTrue(ex.getMessage().contains("Unable to reach service"));
        }
    }

    @Test
    void testVerifySignature_VerificationException() throws Exception {
        JsonNode node = objectMapper.readTree("{\"key\":\"value\"}");

        try (MockedStatic<HttpClients> mocked = Mockito.mockStatic(HttpClients.class)) {
            CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
            CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
            HttpEntity mockEntity = mock(HttpEntity.class);

            mocked.when(HttpClients::createDefault).thenReturn(mockClient);
            when(mockClient.execute(any())).thenReturn(mockResponse);
            when(mockResponse.getEntity()).thenReturn(mockEntity);
            when(mockEntity.getContent()).thenThrow(new RuntimeException("read error"));

            SignatureException.VerificationException ex = assertThrows(SignatureException.VerificationException.class,
                    () -> signatureHelper.verifySignature(node));
            assertEquals("Unable to verify signature ", ex.getMessage());
        }
    }
}
