package org.sunbird;

import org.junit.jupiter.api.*;
import org.sunbird.incredible.processor.JsonKey;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CertsConstantTest {

    @BeforeEach
    void clearEnv() {
        System.clearProperty(JsonKey.DOMAIN_URL);
        System.clearProperty(JsonKey.CONTAINER_NAME);
        System.clearProperty(JsonKey.ENC_SERVICE_URL);
        System.clearProperty(JsonKey.CLOUD_STORAGE_TYPE);
        System.clearProperty(JsonKey.AZURE_STORAGE_SECRET);
        System.clearProperty(JsonKey.AZURE_STORAGE_KEY);
        System.clearProperty(JsonKey.AWS_STORAGE_SECRET);
        System.clearProperty(JsonKey.AWS_STORAGE_KEY);
        System.clearProperty(JsonKey.CEPHS3_STORAGE_SECRET);
        System.clearProperty(JsonKey.CEPHS3_STORAGE_KEY);
        System.clearProperty(JsonKey.CEPHS3_STORAGE_ENDPOINT);
        System.clearProperty(JsonKey.GCP_STORAGE_SECRET);
        System.clearProperty(JsonKey.GCP_STORAGE_KEY);
        System.clearProperty(JsonKey.GCP_STORAGE_ENDPOINT);
        System.clearProperty(JsonKey.SLUG);
    }

    @Test
    void getBADGE_URLReturnsCorrectPath() {
        CertsConstant c = new CertsConstant();
        c.setBasePath("base");
        assertEquals("base/Badge.json", c.getBADGE_URL(""));
        assertEquals("base/tag/Badge.json", c.getBADGE_URL("tag"));
    }

    @Test
    void setBasePathSetsBasePathCorrectly() {
        CertsConstant c = new CertsConstant();
        c.setBasePath("custom");
        assertEquals("custom", c.getBasePath());
        c.setBasePath("");
        assertTrue(c.getBasePath().contains("https://dev.sunbirded.org"));
    }

    @Test
    void getISSUER_URLReturnsIssuerPath() {
        CertsConstant c = new CertsConstant();
        c.setBasePath("base");
        assertEquals("base/Issuer.json", c.getISSUER_URL());
    }

    @Test
    void getEVIDENCE_URLReturnsEvidencePath() {
        CertsConstant c = new CertsConstant();
        c.setBasePath("base");
        assertEquals("base/Evidence.json", c.getEVIDENCE_URL());
    }

    @Test
    void getCONTEXTReturnsContextPath() {
        CertsConstant c = new CertsConstant();
        c.setBasePath("base");
        assertEquals("base/v1/context.json", c.getCONTEXT());
    }

    @Test
    void getPUBLIC_KEY_URLReturnsCorrectPath() {
        CertsConstant c = new CertsConstant();
        c.setBasePath("base");
        assertEquals("base/keys/key1_publicKey.json", c.getPUBLIC_KEY_URL("key1"));
    }

    @Test
    void getVERIFICATION_TYPEReturnsConstant() {
        CertsConstant c = new CertsConstant();
        assertEquals("SignedBadge", c.getVERIFICATION_TYPE());
    }

    @Test
    void getCLOUD_UPLOAD_RETRY_COUNTReturnsDefaultOrEnv() {
        CertsConstant c = new CertsConstant();
        assertEquals("3", c.getCLOUD_UPLOAD_RETRY_COUNT());
        System.setProperty(JsonKey.CLOUD_UPLOAD_RETRY_COUNT, "5");
        assertEquals("3", c.getCLOUD_UPLOAD_RETRY_COUNT());
    }

    @Test
    void getACCESS_CODE_LENGTHReturnsConstant() {
        CertsConstant c = new CertsConstant();
        assertEquals("6", c.getACCESS_CODE_LENGTH());
    }

    @Test
    void getDOMAIN_URLReturnsDefaultOrEnv() {
        CertsConstant c = new CertsConstant();
        assertTrue(c.getDOMAIN_URL().contains("sunbirded.org"));
    }

    @Test
    void getCONTAINER_NAMEReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getCONTAINER_NAME());
    }

    @Test
    void getEncSignUrlReturnsCorrectUrl() {
        CertsConstant c = new CertsConstant();
        assertTrue(c.getEncSignUrl().contains("enc-service"));
    }

    @Test
    void getEncSignVerifyUrlReturnsCorrectUrl() {
        CertsConstant c = new CertsConstant();
        assertTrue(c.getEncSignVerifyUrl().contains("enc-service"));
    }

    @Test
    void getSignCreatorReturnsCorrectPath() {
        CertsConstant c = new CertsConstant();
        c.setBasePath("base");
        assertEquals("base/key1_publicKey.json", c.getSignCreator("key1"));
    }

    @Test
    void getEncryptionServiceUrlReturnsDefaultOrEnv() {
        CertsConstant c = new CertsConstant();
        assertTrue(c.getEncryptionServiceUrl().contains("enc-service"));
    }

    @Test
    void getExpiryLinkReturnsDefaultOrEnv() {
        assertEquals("600", CertsConstant.getExpiryLink("notfound"));
    }

    @Test
    void getCloudStorageTypeReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getCloudStorageType());
    }

    @Test
    void getAzureStorageSecretReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getAzureStorageSecret());
    }

    @Test
    void getAzureStorageKeyReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getAzureStorageKey());
    }

    @Test
    void getAwsStorageSecretReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getAwsStorageSecret());
    }

    @Test
    void getAwsStorageKeyReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getAwsStorageKey());
    }

    @Test
    void getCephs3StorageSecretReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getCephs3StorageSecret());
    }

    @Test
    void getCephs3StorageKeyReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getCephs3StorageKey());
    }

    @Test
    void getCephs3StorageEndPointReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getCephs3StorageEndPoint());
    }

    @Test
    void getSignatoryExtensionUrlReturnsCorrectUrl() {
        CertsConstant c = new CertsConstant();
        c.setBasePath("base");
        assertEquals("base/v1/extensions/SignatoryExtension/context.json", c.getSignatoryExtensionUrl());
    }

    @Test
    void getGCPStorageSecretReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertThrows(NullPointerException.class, c::getGCPStorageSecret);
    }

    @Test
    void getGCPStorageKeyReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getGCPStorageKey());
    }

    @Test
    void getGCPStorageEndPointReturnsNullOrEnv() {
        CertsConstant c = new CertsConstant();
        assertNull(c.getGCPStorageEndPoint());
    }

    @Test
    void getSlugReturnsDefaultOrEnv() {
        CertsConstant c = new CertsConstant();
        assertEquals("certs", c.getSlug());
    }

    @Test
    void getPreviewReturnsPreviewOrFalse() {
        CertsConstant c = new CertsConstant();
        assertEquals("true", c.getPreview("true"));
        assertEquals("false", c.getPreview(""));
    }

    @Test
    void getStorageParamsFromEvnReturnsMapWithType() {
        CertsConstant c = new CertsConstant();
        Map<String, Object> params = c.getStorageParamsFromEvn();
        assertTrue(params.containsKey(JsonKey.TYPE));
    }

    @Test
    void getAzureStorageReturnsConstant() {
        CertsConstant c = new CertsConstant();
        assertEquals(JsonKey.AZURE, c.getAzureStorage());
    }

    @Test
    void getAwsStorageReturnsConstant() {
        CertsConstant c = new CertsConstant();
        assertEquals(JsonKey.AWS, c.getAwsStorage());
    }

    @Test
    void getCephs3StorageReturnsConstant() {
        CertsConstant c = new CertsConstant();
        assertEquals(JsonKey.CEPHS3, c.getCephs3Storage());
    }

    @Test
    void getGCPStorageReturnsConstant() {
        CertsConstant c = new CertsConstant();
        assertEquals(JsonKey.GCP, c.getGCPStorage());
    }
}
