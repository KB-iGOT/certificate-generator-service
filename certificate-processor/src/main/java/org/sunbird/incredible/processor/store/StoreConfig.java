package org.sunbird.incredible.processor.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.incredible.processor.JsonKey;

import java.util.Map;

public class StoreConfig {

    private ObjectMapper mapper = new ObjectMapper();

    private String type;

    private String cloudRetryCount = "3";

    private AzureStoreConfig azureStoreConfig;

    private AwsStoreConfig awsStoreConfig;

    private CephStoreConfig cephStoreConfig;

    private CloudStoreConfig cloudStoreConfig;

    private StoreConfig() {
    }

    public StoreConfig(Map<String, Object> storeParams) {
        setType((String) storeParams.get(JsonKey.TYPE));
        if (storeParams.containsKey(JsonKey.AZURE)) {
            AzureStoreConfig azureStoreConfig = mapper.convertValue(storeParams.get(JsonKey.AZURE), AzureStoreConfig.class);
            setAzureStoreConfig(azureStoreConfig);
        } else if (storeParams.containsKey(JsonKey.AWS)) {
            AwsStoreConfig awsStoreConfig = mapper.convertValue(storeParams.get(JsonKey.AWS), AwsStoreConfig.class);
            setAwsStoreConfig(awsStoreConfig);
        } else if(storeParams.containsKey(JsonKey.CEPHS3)){
            CephStoreConfig cephStoreConfig = mapper.convertValue(storeParams.get(JsonKey.CEPHS3), CephStoreConfig.class);
            setCephStoreConfig(cephStoreConfig);
        } else if(storeParams.containsKey(JsonKey.GCP)){
            CloudStoreConfig cloudStoreConfig = mapper.convertValue(storeParams.get(JsonKey.GCP), CloudStoreConfig.class);
            setCloudStoreConfig(cloudStoreConfig);
        } else try {
            throw new Exception("ERR_INVALID_CLOUD_STORAGE Error while initialising cloud storage");
        } catch (Exception e) {
            
        }
    }

    public boolean isCloudStore() {
        return (azureStoreConfig != null || awsStoreConfig != null);
    }

    public String getContainerName() {
        String containerName = null;
        if (JsonKey.AZURE.equals(getType())) {
            containerName = azureStoreConfig.getContainerName();
        } else if (JsonKey.AWS.equals(getType())) {
            containerName = awsStoreConfig.getContainerName();
        } else if(JsonKey.CEPHS3.equals(getType())){
            containerName = cephStoreConfig.getContainerName();
        } else if(JsonKey.GCP.equals(getType())){
            containerName = cloudStoreConfig.getContainerName();
        } else try {
            throw new Exception("ERR_INVALID_CLOUD_STORAGE Error while initialising cloud storage");
        } catch (Exception e) {
            
        }
        return containerName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCloudRetryCount() {
        return cloudRetryCount;
    }

    public void setCloudRetryCount(String cloudRetryCount) {
        this.cloudRetryCount = cloudRetryCount;
    }

    public CloudStoreConfig getCloudStoreConfig() {
        return cloudStoreConfig;
    }

    public void setCloudStoreConfig(CloudStoreConfig cloudStoreConfig) {
        this.cloudStoreConfig = cloudStoreConfig;
    }

    public AzureStoreConfig getAzureStoreConfig() {
        return azureStoreConfig;
    }

    public void setAzureStoreConfig(AzureStoreConfig azureStoreConfig) {
        this.azureStoreConfig = azureStoreConfig;
    }

    public AwsStoreConfig getAwsStoreConfig() {
        return awsStoreConfig;
    }

    public void setAwsStoreConfig(AwsStoreConfig awsStoreConfig) {
        this.awsStoreConfig = awsStoreConfig;
    }

    public CephStoreConfig getCephStoreConfig() {
        return cephStoreConfig;
    }

    public void setCephStoreConfig(CephStoreConfig cephStoreConfig) {
        this.cephStoreConfig = cephStoreConfig;
    }

    @Override
    public String toString() {
        String stringRep = null;
        try {
            stringRep = mapper.writeValueAsString(this);
        } catch (JsonProcessingException jpe) {
        }
        return stringRep;
    }
}
