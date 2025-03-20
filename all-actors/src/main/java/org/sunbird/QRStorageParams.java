package org.sunbird;

import org.apache.commons.lang3.StringUtils;
import org.mortbay.util.ajax.JSON;
import org.sunbird.incredible.processor.JsonKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class QRStorageParams {

    private Logger logger = LoggerFactory.getLogger(QRStorageParams.class);
    public Map<String, Object> storeParams;

    public QRStorageParams(String storageType) {
        storeParams = getStorageParamsFromEnv(storageType);
    }

    private Map<String, Object> getStorageParamsFromEnv(String type) {
        logger.info("QRStorageParams getting storage params from env ");
        Map<String, Object> storeParams = new HashMap<>();
        storeParams.put(JsonKey.TYPE, type);
        if (StringUtils.isNotBlank(type)) {
            if (type.equals(JsonKey.AZURE)) {
                storeParams.put(JsonKey.AZURE, getAzureParams());
            }
            if (type.equals(JsonKey.AWS)) {
                storeParams.put(JsonKey.AWS, getAwsParams());
            }
            if (type.equals(JsonKey.CEPHS3)) {
                storeParams.put(JsonKey.CEPHS3, getCephs3Params());
            }
            if (type.equals(JsonKey.GCP)) {
                storeParams.put(JsonKey.GCP, getGCPParams());
            }
        }
        return storeParams;
    }

    private Map<String, String> getAzureParams() {
        Map<String, String> azureParams = new HashMap<>();
        azureParams.put(JsonKey.containerName, System.getenv(JsonKey.PUBLIC_CONTAINER_NAME));
        azureParams.put(JsonKey.ACCOUNT, System.getenv(JsonKey.PUBLIC_AZURE_STORAGE_KEY));
        azureParams.put(JsonKey.KEY, System.getenv(JsonKey.PUBLIC_AZURE_STORAGE_SECRET));
        return azureParams;
    }

    private Map<String, String> getAwsParams() {
        Map<String, String> awsParams = new HashMap<>();
        awsParams.put(JsonKey.containerName, System.getenv(JsonKey.PUBLIC_CONTAINER_NAME));
        awsParams.put(JsonKey.ACCOUNT, System.getenv(JsonKey.PUBLIC_AWS_STORAGE_KEY));
        awsParams.put(JsonKey.KEY, System.getenv(JsonKey.PUBLIC_AWS_STORAGE_SECRET));
        return awsParams;
    }

    private Map<String, String> getCephs3Params() {
        Map<String, String> cephs3Params = new HashMap<>();
        cephs3Params.put(JsonKey.containerName, System.getenv(JsonKey.PUBLIC_CONTAINER_NAME));
        cephs3Params.put(JsonKey.ACCOUNT, System.getenv(JsonKey.PUBLIC_CEPHS3_STORAGE_KEY));
        cephs3Params.put(JsonKey.KEY, System.getenv(JsonKey.PUBLIC_CEPHS3_STORAGE_SECRET));
        cephs3Params.put(JsonKey.ENDPOINT, System.getenv(JsonKey.PUBLIC_CEPHS3_STORAGE_ENDPOINT));
        return cephs3Params;
    }

    private Map<String, String> getGCPParams() {
        Map<String, String> gcpParams = new HashMap<>();
        gcpParams.put(JsonKey.containerName, System.getenv(JsonKey.PUBLIC_CONTAINER_NAME));
        gcpParams.put(JsonKey.ACCOUNT, System.getenv(JsonKey.GCP_STORAGE_KEY));
        gcpParams.put(JsonKey.KEY, System.getenv(JsonKey.GCP_STORAGE_SECRET).replace("\\n", "\n"));
        gcpParams.put(JsonKey.ENDPOINT, System.getenv(JsonKey.GCP_STORAGE_ENDPOINT));
        return gcpParams;
    }
}
