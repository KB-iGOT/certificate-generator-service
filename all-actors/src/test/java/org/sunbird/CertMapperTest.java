package org.sunbird;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.incredible.processor.CertModel;
import org.sunbird.incredible.processor.JsonKey;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CertMapperTest {

    private CertMapper certMapper;
    private Map<String, String> properties;

    @BeforeEach
    void setUp() {
        properties = new HashMap<>();
        properties.put(JsonKey.SIGNATORY_EXTENSION, "signatoryExtension");
        properties.put(JsonKey.CONTEXT, "https://example.org/context");
        properties.put(JsonKey.BASE_PATH, "https://base.url");

        certMapper = new CertMapper(properties);
    }

    @Test
    void test_toList_fullData() {
        Map<String, Object> signatory = new HashMap<>();
        signatory.put(JsonKey.ID, "sig-1");
        signatory.put(JsonKey.DESIGNATION, "Manager");
        signatory.put(JsonKey.SIGNATORY_IMAGE, "image.png");

        List<Map<String, Object>> signatoryList = Collections.singletonList(signatory);

        Map<String, Object> issuer = new HashMap<>();
        issuer.put(JsonKey.NAME, "IssuerName");
        issuer.put(JsonKey.URL, "https://issuer.url");
        issuer.put(JsonKey.PUBLIC_KEY, Collections.singletonList("abc123"));

        Map<String, Object> keys = new HashMap<>();
        keys.put(JsonKey.ID, "keyId");

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("narrative", "Test narrative");

        Map<String, Object> dataItem = new HashMap<>();
        dataItem.put(JsonKey.RECIPIENT_NAME, "John Doe");
        dataItem.put(JsonKey.RECIPIENT_EMAIl, "john@example.com");
        dataItem.put(JsonKey.RECIPIENT_PHONE, "1234567890");
        dataItem.put(JsonKey.RECIPIENT_ID, "id-123");
        dataItem.put(JsonKey.VALID_FROM, "2024-01-01");
        dataItem.put(JsonKey.EXPIRY, "2025-01-01");

        List<Map<String, Object>> dataList = Collections.singletonList(dataItem);

        Map<String, Object> certMap = new HashMap<>();
        certMap.put(JsonKey.DATA, dataList);
        certMap.put(JsonKey.ISSUER, issuer);
        certMap.put(JsonKey.KEYS, keys);
        certMap.put(JsonKey.SIGNATORY_LIST, signatoryList);
        certMap.put(JsonKey.CRITERIA, criteria);
        certMap.put(JsonKey.COURSE_NAME, "Java Course");
        certMap.put(JsonKey.DESCRIPTION, "Certificate description");
        certMap.put(JsonKey.LOGO, "logo.png");
        certMap.put(JsonKey.ISSUE_DATE, "2024-05-20");
        certMap.put(JsonKey.CERTIFICATE_NAME, "Completion Certificate");
        certMap.put("providerName", "Sunbird");

        Map<String, Object> request = new HashMap<>();
        request.put(JsonKey.CERTIFICATE, certMap);

        List<?> result = certMapper.toList(request);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void test_toList_withoutIssueDate_and_nullPublicKeys() {
        Map<String, Object> issuer = new HashMap<>();
        issuer.put(JsonKey.NAME, "IssuerName");
        issuer.put(JsonKey.URL, "https://issuer.url");
        issuer.put(JsonKey.PUBLIC_KEY, null); // null public keys

        Map<String, Object> keys = new HashMap<>();
        keys.put(JsonKey.ID, "defaultKey");

        Map<String, Object> dataItem = new HashMap<>();
        dataItem.put(JsonKey.RECIPIENT_NAME, "Jane Doe");
        dataItem.put(JsonKey.RECIPIENT_EMAIl, "jane@example.com");
        dataItem.put(JsonKey.RECIPIENT_PHONE, "9876543210");
        dataItem.put(JsonKey.RECIPIENT_ID, "id-456");
        dataItem.put(JsonKey.VALID_FROM, "2024-01-01");
        dataItem.put(JsonKey.EXPIRY, "2025-01-01");

        List<Map<String, Object>> dataList = Collections.singletonList(dataItem);

        Map<String, Object> certMap = new HashMap<>();
        certMap.put(JsonKey.DATA, dataList);
        certMap.put(JsonKey.ISSUER, issuer);
        certMap.put(JsonKey.KEYS, keys);
        certMap.put(JsonKey.SIGNATORY_LIST, new ArrayList<>());
        certMap.put(JsonKey.CRITERIA, new HashMap<>());
        certMap.put(JsonKey.COURSE_NAME, "Python Course");
        certMap.put(JsonKey.DESCRIPTION, "Desc");
        certMap.put(JsonKey.LOGO, "logo.png");
        certMap.put(JsonKey.ISSUE_DATE, ""); // blank issue date
        certMap.put(JsonKey.CERTIFICATE_NAME, "Cert Name");
        certMap.put("providerName", "Provider");

        Map<String, Object> request = new HashMap<>();
        request.put(JsonKey.CERTIFICATE, certMap);

        List<?> result = certMapper.toList(request);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void test_validatePublicKeys_nonHttp_withKeys() {
        List<String> publicKeys = Arrays.asList("myKey");
        Map<String, Object> keys = new HashMap<>();
        List<CertModel> result = invokeValidatePublicKeys(publicKeys, keys);
        assertEquals(0, result.size());
    }

    private List<CertModel> invokeValidatePublicKeys(List<String> keys, Map<String, Object> kmap) {
        return certMapper.toList(
                Collections.singletonMap(JsonKey.CERTIFICATE, Map.of(
                        JsonKey.DATA, Collections.emptyList(),
                        JsonKey.ISSUER, Map.of(JsonKey.PUBLIC_KEY, keys),
                        JsonKey.KEYS, kmap,
                        JsonKey.SIGNATORY_LIST, new ArrayList<>(),
                        JsonKey.CRITERIA, new HashMap<>()
                ))
        );
    }
}
