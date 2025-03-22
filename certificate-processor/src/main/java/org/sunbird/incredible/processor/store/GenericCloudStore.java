package org.sunbird.incredible.processor.store;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import scala.Option;

import java.io.File;

/**
 * used to upload or downloads files to any cloud storage if used properly
 */
public class GenericCloudStore extends CloudStore {

    private StoreConfig cloudStoreConfig;

    private Logger logger = LoggerFactory.getLogger(AwsStore.class);

    private BaseStorageService storageService = null;

    private CloudStorage cloudStorage = null;

    private int retryCount = 0;

    public GenericCloudStore(StoreConfig cloudStoreConfig) {
        this.cloudStoreConfig = cloudStoreConfig;
        retryCount = Integer.parseInt(cloudStoreConfig.getCloudRetryCount());
        init();
    }


    @Override
    public String upload(File file, String path) {
        String uploadPath = getPath(path);
        return cloudStorage.uploadFile(cloudStoreConfig.getCephStoreConfig().getContainerName(), uploadPath, file, false, retryCount);
    }

    @Override
    public void download(String fileName, String localPath) {
        cloudStorage.downloadFile(cloudStoreConfig.getCephStoreConfig().getContainerName(), fileName, localPath, false);
    }

    private String getPath(String path) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(path);
        if (StringUtils.isNotBlank(cloudStoreConfig.getCephStoreConfig().getPath())) {
            stringBuilder.append(cloudStoreConfig.getCephStoreConfig().getPath() + "/");
        }
        return stringBuilder.toString();
    }

    @Override
    public String getPublicLink(File file, String uploadPath) {
        String path = getPath(uploadPath);
        return cloudStorage.upload(cloudStoreConfig.getCephStoreConfig().getContainerName(), path, file, false, retryCount);
    }

    @Override
    public void init() {
        if (StringUtils.isNotBlank(cloudStoreConfig.getType())) {
            String storageKey = cloudStoreConfig.getCloudStoreConfig().getAccount();
            String storageSecret = cloudStoreConfig.getCloudStoreConfig().getKey();
            String storageEndpoint = cloudStoreConfig.getCloudStoreConfig().getKey();
            StorageConfig storageConfig = new StorageConfig(cloudStoreConfig.getType(), storageKey, storageSecret, Option.apply(storageEndpoint), Option.empty());
            logger.info("StorageParams:init:all storage params initialized for aws block");
            storageService = StorageServiceFactory.getStorageService(storageConfig);
            cloudStorage = new CloudStorage(storageService);
        } else {
            logger.error("StorageParams:init:provided cloud store type doesn't match supported storage devices:".concat(cloudStoreConfig.getType()));
        }

    }

    @Override
    public void close(){
        cloudStorage.closeConnection();
    }
}
