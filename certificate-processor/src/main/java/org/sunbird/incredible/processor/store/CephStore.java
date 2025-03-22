package org.sunbird.incredible.processor.store;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;

import scala.Option;
/**
 * used to upload or downloads files to cephs3
 */
public class CephStore extends CloudStore {

    private StoreConfig cephStoreConfig;

    private Logger logger = LoggerFactory.getLogger(AwsStore.class);

    private BaseStorageService storageService = null;

    private CloudStorage cloudStorage = null;

    private int retryCount = 0;

    public CephStore(StoreConfig cephStoreConfig) {
        this.cephStoreConfig = cephStoreConfig;
        retryCount = Integer.parseInt(cephStoreConfig.getCloudRetryCount());
        init();
    }


    @Override
    public String upload(File file, String path) {
        String uploadPath = getPath(path);
        return cloudStorage.uploadFile(cephStoreConfig.getCephStoreConfig().getContainerName(), uploadPath, file, false, retryCount);
    }

    @Override
    public void download(String fileName, String localPath) {
        cloudStorage.downloadFile(cephStoreConfig.getCephStoreConfig().getContainerName(), fileName, localPath, false);
    }

    private String getPath(String path) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(path);
        if (StringUtils.isNotBlank(cephStoreConfig.getCephStoreConfig().getPath())) {
            stringBuilder.append(cephStoreConfig.getCephStoreConfig().getPath() + "/");
        }
        return stringBuilder.toString();
    }

    @Override
    public String getPublicLink(File file, String uploadPath) {
        String path = getPath(uploadPath);
        return cloudStorage.upload(cephStoreConfig.getCephStoreConfig().getContainerName(), path, file, false, retryCount);
    }

    @Override
    public void init() {
        if (StringUtils.isNotBlank(cephStoreConfig.getType())) {
            String storageKey = cephStoreConfig.getCephStoreConfig().getAccount();
            String storageSecret = cephStoreConfig.getCephStoreConfig().getKey();
            String storageEndpoint = cephStoreConfig.getCephStoreConfig().getKey();
            StorageConfig storageConfig = new StorageConfig(cephStoreConfig.getType(), storageKey, storageSecret,Option.apply(storageEndpoint), Option.empty());
            logger.info("StorageParams:init:all storage params initialized for aws block");
            storageService = StorageServiceFactory.getStorageService(storageConfig);
            cloudStorage = new CloudStorage(storageService);
        } else {
            logger.error("StorageParams:init:provided cloud store type doesn't match supported storage devices:".concat(cephStoreConfig.getType()));
        }

    }

    @Override
    public void close(){
        cloudStorage.closeConnection();
    }
}
