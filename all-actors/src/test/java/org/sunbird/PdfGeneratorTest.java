package org.sunbird;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.incredible.pojos.CertificateExtension;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.incredible.processor.views.HTMLVarResolver;
import org.sunbird.incredible.processor.views.HTMLVars;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PdfGeneratorTest {

    private static MockedStatic<HTMLVars> htmlVarsMockedStatic;
    private static MockedStatic<EntityUtils> entityUtilsMockedStatic;
    private static MockedStatic<HTMLVarResolver> htmlVarResolverMockedStatic;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private HttpEntity httpEntity;

    private CertificateExtension certExt;

    @BeforeAll
    static void setupAll() {
        htmlVarsMockedStatic = mockStatic(HTMLVars.class);
        entityUtilsMockedStatic = mockStatic(EntityUtils.class);
        htmlVarResolverMockedStatic = mockStatic(HTMLVarResolver.class);
    }

    @AfterAll
    static void tearDownAll() {
        htmlVarsMockedStatic.close();
        entityUtilsMockedStatic.close();
        htmlVarResolverMockedStatic.close();
    }

    @BeforeEach
    void setup() {
        certExt = new CertificateExtension("");
    }

    @Test
    void testGenerateResponse() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put(JsonKey.PDF_URL, "http://example.com/certificate.pdf");
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put(JsonKey.RESULT, result);

        byte[] bytes = new ObjectMapper().writeValueAsBytes(wrapper);

        when(httpResponse.getEntity()).thenReturn(httpEntity);
        entityUtilsMockedStatic.when(() -> EntityUtils.toByteArray(httpEntity)).thenReturn(bytes);

        Method method = PdfGenerator.class.getDeclaredMethod("generateResponse", CloseableHttpResponse.class);
        method.setAccessible(true);
        String pdfUrl = (String) method.invoke(null, httpResponse);

        assertEquals("http://example.com/certificate.pdf", pdfUrl);
    }

}
