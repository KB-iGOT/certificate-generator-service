package org.sunbird;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.incredible.processor.JsonKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * this constant file is used to get the Constants which is used by entire actors
 */
public class CertsConstant {

    private static Logger logger = LoggerFactory.getLogger(CertsConstant.class);

    private static final String BADGE_URL = "Badge.json";
    private static final String ISSUER_URL = "Issuer.json";
    private static final String EVIDENCE_URL = "Evidence.json";
    private static final String CONTEXT = "v1/context.json";
    private static final String PUBLIC_KEY_URL = "_publicKey.json";
    private static final String VERIFICATION_TYPE = "SignedBadge";
    private static final String CLOUD_UPLOAD_RETRY_COUNT = "3";
    private static final String ACCESS_CODE_LENGTH = "6";
    public static final String DOWNLOAD_LINK_EXPIRY_TIMEOUT = "download_link_expiry_timeout";
    private static final String LINK_TIMEOUT = "600";
    private static final String SIGNATORY_EXTENSION = "v1/extensions/SignatoryExtension";
    private static String DOMAIN_URL = getDomainUrlFromEnv();
    private String CONTAINER_NAME;
    private static final String ENC_SERVICE_URL = getEncServiceUrl();
    private String CLOUD_STORAGE_TYPE;
    private static String BASE_PATH;
    private static final String SLUG = getSlugFormEnv();
    private static final String DOMAIN_SLUG = DOMAIN_URL + "/" + SLUG;

    public String getBADGE_URL(String tag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BASE_PATH);
        if (StringUtils.isNotEmpty(tag))
            stringBuilder.append("/" + tag);
        return stringBuilder.append("/" + BADGE_URL).toString();
    }

    public void setBasePath(String basePath) {
        if (StringUtils.isNotBlank(basePath)) {
            BASE_PATH = basePath;
        } else {
            BASE_PATH = DOMAIN_URL + "/" + SLUG;
        }
    }

    public String getBasePath() {
        return BASE_PATH;
    }

    public String getISSUER_URL() {
        return BASE_PATH + "/" + ISSUER_URL;
    }

    public String getEVIDENCE_URL() {
        return BASE_PATH + "/" + EVIDENCE_URL;
    }


    public String getCONTEXT() {
        return String.format("%s/%s", BASE_PATH, CONTEXT);
    }

    public String getPUBLIC_KEY_URL(String keyId) {
        return BASE_PATH + "/" + JsonKey.KEYS + "/" + keyId + PUBLIC_KEY_URL;
    }

    public String getVERIFICATION_TYPE() {
        return VERIFICATION_TYPE;
    }

    public String getCLOUD_UPLOAD_RETRY_COUNT() {
        String retryCount = getPropertyFromEnv(JsonKey.CLOUD_UPLOAD_RETRY_COUNT);
        return StringUtils.isNotBlank(retryCount) ? retryCount : CLOUD_UPLOAD_RETRY_COUNT;
    }

    public String getACCESS_CODE_LENGTH() {
        return ACCESS_CODE_LENGTH;
    }

    public String getDOMAIN_URL() {
        return DOMAIN_URL;
    }

    public String getCONTAINER_NAME() {
        CONTAINER_NAME = getContainerNameFromEnv();
        return CONTAINER_NAME;
    }

    private static String getDomainUrlFromEnv() {
        String domainUrl = getPropertyFromEnv(JsonKey.DOMAIN_URL);
        return StringUtils.isNotBlank(domainUrl) ? domainUrl : "https://dev.sunbirded.org";
    }

    private static String getContainerNameFromEnv() {
        return getPropertyFromEnv(JsonKey.CONTAINER_NAME);
    }

    private static String getPropertyFromEnv(String property) {
        return System.getenv(property);
    }

    private static void validateEnvProperty(String property) {
        if (StringUtils.isBlank(property)) {
            printErrorForMissingEnv(property);
            System.exit(-1);
        }
    }

    private static void printErrorForMissingEnv(String env) {
        logger.error("Constant:printErrorForMissingEnv:No env variable found ".concat(env));
    }

    private static String getEncServiceUrl() {
        String encServiceUrl = getPropertyFromEnv(JsonKey.ENC_SERVICE_URL);
        return StringUtils.isNotBlank(encServiceUrl) ? encServiceUrl : "http://enc-service:8013";

    }

    public String getEncSignUrl() {
        return String.format("%s/%s", ENC_SERVICE_URL, JsonKey.SIGN);
    }

    public String getEncSignVerifyUrl() {
        return String.format("%s/%s", ENC_SERVICE_URL, JsonKey.VERIFY);

    }


    public String getSignCreator(String keyId) {
        return BASE_PATH + "/" + keyId + PUBLIC_KEY_URL;
    }

    public String getEncryptionServiceUrl() {
        return getEncServiceUrl();
    }

    public static String getExpiryLink(String key) {
        return getPropertyFromEnv(key) != null ? getPropertyFromEnv(key) : LINK_TIMEOUT;
    }


    private static String getCloudStorageTypeFromEnv() {
        return getPropertyFromEnv(JsonKey.CLOUD_STORAGE_TYPE);
    }


    public String getCloudStorageType() {
        CLOUD_STORAGE_TYPE = getCloudStorageTypeFromEnv();
        return CLOUD_STORAGE_TYPE;
    }

    public String getAzureStorageSecret() {
        return getPropertyFromEnv(JsonKey.AZURE_STORAGE_SECRET);
    }

    public String getAzureStorageKey() {
        return getPropertyFromEnv(JsonKey.AZURE_STORAGE_KEY);
    }

    public String getAwsStorageSecret() {
        return getPropertyFromEnv(JsonKey.AWS_STORAGE_SECRET);
    }

    public String getAwsStorageKey() {
        return getPropertyFromEnv(JsonKey.AWS_STORAGE_KEY);
    }

    public String getCephs3StorageSecret() {
        return getPropertyFromEnv(JsonKey.CEPHS3_STORAGE_SECRET);
    }

    public String getCephs3StorageKey() {
        return getPropertyFromEnv(JsonKey.CEPHS3_STORAGE_KEY);
    }

    public String getCephs3StorageEndPoint() {
        return getPropertyFromEnv(JsonKey.CEPHS3_STORAGE_ENDPOINT);
    }

    public String getSignatoryExtensionUrl() {
        return String.format("%s/%s/%s", BASE_PATH, SIGNATORY_EXTENSION, "context.json");
    }

    public String getGCPStorageSecret() {
        return getPropertyFromEnv(JsonKey.GCP_STORAGE_SECRET).replace("\\n", "\n");
    }

    public String getGCPStorageKey() {
        return getPropertyFromEnv(JsonKey.GCP_STORAGE_KEY);
    }

    public String getGCPStorageEndPoint() {
        return getPropertyFromEnv(JsonKey.GCP_STORAGE_ENDPOINT);
    }

    private static String getSlugFormEnv() {
        String slug = getPropertyFromEnv(JsonKey.SLUG);
        return StringUtils.isNotBlank(slug) ? slug : "certs";
    }

    public String getSlug() {
        return SLUG;
    }

    public String getPreview(String preview) {
        if (StringUtils.isNotBlank(preview))
            return preview;
        return Boolean.toString(false);
    }

    public Map<String, Object> getStorageParamsFromEvn() {
        logger.info("getting storage params from env");
        String type = getCloudStorageType();
        Map<String, Object> storeParams = new HashMap<>();
        storeParams.put(JsonKey.TYPE, type);
        if (StringUtils.isNotBlank(type)) {
            if (type.equals(JsonKey.AZURE)) {
                storeParams.put(JsonKey.AZURE, getAzureParams());
            }
            if (type.equals(JsonKey.AWS)) {
                storeParams.put(JsonKey.AWS, getAwsParams());
            }
        }
        return storeParams;
    }

    private Map<String, String> getAzureParams() {
        Map<String, String> azureParams = new HashMap<>();
        azureParams.put(JsonKey.containerName, getCONTAINER_NAME());
        azureParams.put(JsonKey.ACCOUNT, getAzureStorageKey());
        azureParams.put(JsonKey.KEY, getAzureStorageSecret());
        return azureParams;
    }

    private Map<String, String> getAwsParams() {
        Map<String, String> awsParams = new HashMap<>();
        awsParams.put(JsonKey.containerName, getCONTAINER_NAME());
        awsParams.put(JsonKey.ACCOUNT, getAwsStorageKey());
        awsParams.put(JsonKey.KEY, getAwsStorageSecret());
        return awsParams;
    }

    private Map<String, String> getCephs3Params() {
        Map<String, String> cephs3Params = new HashMap<>();
        cephs3Params.put(JsonKey.containerName, getCONTAINER_NAME());
        cephs3Params.put(JsonKey.ACCOUNT, getCephs3StorageKey());
        cephs3Params.put(JsonKey.KEY, getCephs3StorageSecret());
        cephs3Params.put(JsonKey.ENDPOINT, getCephs3StorageEndPoint());
        return cephs3Params;
    }

    private Map<String, String> getGCPParams() {
        Map<String, String> gcpParams = new HashMap<>();
        gcpParams.put(JsonKey.containerName, getCONTAINER_NAME());
        gcpParams.put(JsonKey.ACCOUNT, getGCPStorageKey());
        gcpParams.put(JsonKey.KEY, getGCPStorageSecret());
        gcpParams.put(JsonKey.ENDPOINT, getGCPStorageEndPoint());
        return gcpParams;
    }


    public String getAzureStorage() {
        return JsonKey.AZURE;
    }

    public String getAwsStorage() {
        return JsonKey.AWS;
    }

    public String getCephs3Storage() {
        return JsonKey.CEPHS3;
    }

    public String getGCPStorage() {
        return JsonKey.GCP;
    }

}
