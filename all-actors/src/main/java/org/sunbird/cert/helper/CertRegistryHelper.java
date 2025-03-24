package org.sunbird.cert.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.BaseException;
import org.sunbird.HttpUtil;
import org.sunbird.JsonKeys;
import org.sunbird.PropertiesCache;
import org.sunbird.cache.util.RedisCacheUtil;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.response.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class CertRegistryHelper {

    private static final Logger logger = Logger.getLogger(CertRegistryHelper.class.getName());

    private static final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private static final RedisCacheUtil contentCache = new RedisCacheUtil();
    private static ObjectMapper mapper = new ObjectMapper();
    private static PropertiesCache propertiesCache = PropertiesCache.getInstance();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private static CertRegistryHelper instance = null;

    public static synchronized CertRegistryHelper getInstance() {
        if (instance == null) {
            instance = new CertRegistryHelper();
        }
        return instance;
    }

    private CertRegistryHelper() {}

    public Map<String, Object> addCertToRegistry(Map<String, Object> addReq) throws Exception {
        String url = propertiesCache.getProperty(JsonKeys.CERT_REGISTRY_BASE_PATH) + propertiesCache.getProperty(JsonKeys.ADD_CERT_REG_API);
        Map<String, Object> responseObject = postAPICall(url, addReq);
        if (JsonKeys.OK.equalsIgnoreCase((String) responseObject.get(JsonKeys.RESPONSE_CODE))) {
            Map<String, Object> resultObject = (Map<String, Object>) responseObject.get(JsonKeys.RESULT);
            Map<String, Object> response = (Map<String, Object>) resultObject.get(JsonKeys.RESPONSE);
            if (MapUtils.isNotEmpty(response)) {
                return response;
            }
        }
        return null;
    }

    public Map<String, Object> postAPICall(
            String url, Map<String, Object> requestBody) throws Exception {
        Map<String, String> defaultHeader = new HashMap<>();
        defaultHeader.put("Content-Type", "application/json");
        String response = HttpUtil.sendPostRequest(url, mapper.writeValueAsString(requestBody), defaultHeader);
        Map<String, Object> data = mapper.readValue(response, Map.class);
        if (MapUtils.isNotEmpty(data)) {
            return data;
        } else {
            throw new RuntimeException("Error from get API: " + url + ", with response: " + response);
        }
    }

    public Map<String, Object> getCertificateRegistryUsingIdentifier(String identifier) throws BaseException {
        Map<String, Object> primaryKey = new HashMap<>();
        primaryKey.put(JsonKeys.IDENTIFIER, identifier);
        Response row = cassandraOperation.getRecordsByProperties(JsonKeys.SUNBIRD, JsonKeys.CERT_REGISTRY_V2, primaryKey, null);
        if (row != null) {
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) row.get(JsonKeys.RESPONSE);
            if (CollectionUtils.isNotEmpty(mapList)) {
                Map<String, Object> map = mapList.get(0);
                if (MapUtils.isNotEmpty(map)) {
                    return map;
                }
            }
        }
        return null;
    }
}
