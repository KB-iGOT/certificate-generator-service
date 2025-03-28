package org.sunbird.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.common.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.JsonKeys;
import org.sunbird.PropertiesCache;

import java.util.Collections;
import java.util.Map;

public class AccessTokenValidator {

    private static ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(AccessTokenValidator.class.getName());
    private static PropertiesCache propertiesCache = PropertiesCache.getInstance();

    private static Map<String, Object> validateToken(String token, boolean checkActive) throws JsonProcessingException {
        try {
            String[] tokenElements = token.split("\\.");
            String header = tokenElements[0];
            String body = tokenElements[1];
            String signature = tokenElements[2];
            String payLoad = header + JsonKeys.DOT_SEPARATOR + body;
            Map<Object, Object> headerData =
                    mapper.readValue(new String(decodeFromBase64(header)), Map.class);
            String keyId = headerData.get("kid").toString();
            boolean isValid =
                    CryptoUtil.verifyRSASign(
                            payLoad,
                            decodeFromBase64(signature),
                            KeyManager.getPublicKey(keyId).getPublicKey(),
                            JsonKeys.SHA_256_WITH_RSA);
            if (isValid) {
                Map<String, Object> tokenBody =
                        mapper.readValue(new String(decodeFromBase64(body)), Map.class);
                if(checkActive) {
                    boolean isExp = isExpired((Integer) tokenBody.get("exp"));
                    if (isExp) {
                        return Collections.EMPTY_MAP;
                    }
                }
                return tokenBody;
            }
        } catch (Exception ex) {
            logger.error("Exception in AccessTokenValidator: verify ", ex);
        }
        return Collections.EMPTY_MAP;
    }

    /**
     * managedtoken is validated and requestedByUserID, requestedForUserID values are validated
     * aganist the managedEncToken
     *
     * @param managedEncToken
     * @param requestedByUserId
     * @param requestedForUserId
     * @return
     */
    public static String verifyManagedUserToken(
            String managedEncToken, String requestedByUserId, String requestedForUserId, String loggingHeaders) {
        String managedFor = JsonKeys.UNAUTHORIZED;
        try {
            Map<String, Object> payload = validateToken(managedEncToken, true);
            if (MapUtils.isNotEmpty(payload)) {
                String parentId = (String) payload.get(JsonKeys.PARENT_ID);
                String muaId = (String) payload.get(JsonKeys.SUB);
                logger.info(
                        "AccessTokenValidator: parent uuid: "
                                + parentId
                                + " managedBy uuid: "
                                + muaId
                                + " requestedByUserID: "
                                + requestedByUserId
                                + " requestedForUserId: "
                                + requestedForUserId);
                boolean isValid =
                        parentId.equalsIgnoreCase(requestedByUserId);
                if(!muaId.equalsIgnoreCase(requestedForUserId)) {
                    logger.info("RequestedFor userid : " + requestedForUserId + " is not matching with the muaId : " + muaId + " Headers: " + loggingHeaders);
                }
                if (isValid) {
                    managedFor = muaId;
                }
            }
        } catch (Exception ex) {
            logger.error("Exception in AccessTokenValidator: verify ", ex);
        }
        return managedFor;
    }

    public static String verifyUserToken(String token, boolean checkActive) {
        logger.debug("Inside the verifyToken Method");
        String userId = JsonKeys.UNAUTHORIZED;
        try {
            Map<String, Object> payload = validateToken(token, checkActive);
            logger.debug("The token body is" + mapper.writeValueAsString(payload));
            if (MapUtils.isNotEmpty(payload) && checkIss((String) payload.get("iss"))) {
                userId = (String) payload.get(JsonKeys.SUB);
                if (StringUtils.isNotBlank(userId)) {
                    int pos = userId.lastIndexOf(":");
                    userId = userId.substring(pos + 1);
                }
            }
        } catch (Exception ex) {
            logger.error("Exception in verifyUserAccessToken: verify ", ex);
        }
        return userId;
    }

    private static boolean checkIss(String iss) {
        String realmUrl =
                propertiesCache.getProperty(JsonKeys.SUNBIRD_SSO_URL) + "realms/" + propertiesCache.getProperty(JsonKeys.SUNBIRD_SSO_RELAM);
        logger.info("the realm url is" + realmUrl);
        return (realmUrl.equalsIgnoreCase(iss));
    }

    private static boolean isExpired(Integer expiration) {
        return (Time.currentTime() > expiration);
    }

    private static byte[] decodeFromBase64(String data) {
        return Base64Util.decode(data, 11);
    }
}
