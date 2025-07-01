package org.sunbird.cert.actor;

import akka.event.DiagnosticLoggingAdapter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.BaseException;
import org.sunbird.CertsConstant;
import org.sunbird.cloud.storage.BaseStorageService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.request.Request;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CertificateGeneratorActor2Test {

    private static CertsConstant certVarMock;
    private static MockedStatic<StorageServiceFactory> storageServiceFactoryStaticMock;
    private BaseStorageService mockStorageService;
    private Object actorInstance;

    @BeforeAll
    void setup() throws Exception {
        certVarMock = mock(CertsConstant.class);
        setStaticFieldUsingUnsafe(CertificateGeneratorActor.class, "certVar", certVarMock);
    }

    @BeforeEach
    void beforeEach() throws Exception {
        mockStorageService = mock(BaseStorageService.class);

        storageServiceFactoryStaticMock = mockStatic(StorageServiceFactory.class);
        storageServiceFactoryStaticMock.when(() -> StorageServiceFactory.getStorageService(any(StorageConfig.class)))
                .thenReturn(mockStorageService);

        actorInstance = createActorByUnsafe(); // Bypass constructor restrictions
    }

    @AfterEach
    void afterEach() {
        if (storageServiceFactoryStaticMock != null) {
            storageServiceFactoryStaticMock.close();
        }
    }

    @Test
    void testGetStorageService_withAzure() throws Exception {
        // 1. Set up mocks
        when(certVarMock.getCloudStorageType()).thenReturn("azure");
        when(certVarMock.getAzureStorage()).thenReturn("azure");
        when(certVarMock.getAzureStorageKey()).thenReturn("key");
        when(certVarMock.getAzureStorageSecret()).thenReturn("secret");

        setStaticFieldUsingUnsafe(CertificateGeneratorActor.class, "certVar", certVarMock);

        storageServiceFactoryStaticMock.when(() -> StorageServiceFactory.getStorageService(any()))
                .thenReturn(mockStorageService);

        // 2. Create actor after mocks are ready
        Object actorInstance = createActorByUnsafe();

        // 3. Inject mock logger
        DiagnosticLoggingAdapter mockLogger = mock(DiagnosticLoggingAdapter.class);
        setField(actorInstance, "logger", mockLogger);

        // 4. Invoke private method
        Method method = CertificateGeneratorActor.class.getDeclaredMethod("getStorageService");
        method.setAccessible(true);
        BaseStorageService result = (BaseStorageService) method.invoke(actorInstance);

        // 5. Assert
        assertNotNull(result);
        assertEquals(mockStorageService, result);
    }

    @Test
    void testGetStorageService_withAws() throws Exception {
        when(certVarMock.getCloudStorageType()).thenReturn("aws");
        when(certVarMock.getAwsStorage()).thenReturn("aws");
        when(certVarMock.getAwsStorageKey()).thenReturn("aws-key");
        when(certVarMock.getAwsStorageSecret()).thenReturn("aws-secret");

        setStaticFieldUsingUnsafe(CertificateGeneratorActor.class, "certVar", certVarMock);

        storageServiceFactoryStaticMock.when(() -> StorageServiceFactory.getStorageService(any()))
                .thenReturn(mockStorageService);

        Object actorInstance = createActorByUnsafe();
        setField(actorInstance, "logger", mock(DiagnosticLoggingAdapter.class));

        Method method = CertificateGeneratorActor.class.getDeclaredMethod("getStorageService");
        method.setAccessible(true);
        BaseStorageService result = (BaseStorageService) method.invoke(actorInstance);
        assertNotNull(result);
    }

    @Test
    void testGetStorageService_withCephS3() throws Exception {
        when(certVarMock.getCloudStorageType()).thenReturn("ceph");
        when(certVarMock.getCephs3Storage()).thenReturn("ceph");
        when(certVarMock.getCephs3StorageKey()).thenReturn("ceph-key");
        when(certVarMock.getCephs3StorageSecret()).thenReturn("ceph-secret");
        when(certVarMock.getCephs3StorageEndPoint()).thenReturn("http://ceph-endpoint");

        setStaticFieldUsingUnsafe(CertificateGeneratorActor.class, "certVar", certVarMock);

        storageServiceFactoryStaticMock.when(() -> StorageServiceFactory.getStorageService(any()))
                .thenReturn(mockStorageService);

        Object actorInstance = createActorByUnsafe();
        setField(actorInstance, "logger", mock(DiagnosticLoggingAdapter.class));

        Method method = CertificateGeneratorActor.class.getDeclaredMethod("getStorageService");
        method.setAccessible(true);
        BaseStorageService result = (BaseStorageService) method.invoke(actorInstance);
        assertNotNull(result);
    }

    @Test
    void testGetStorageService_withGcp() throws Exception {
        when(certVarMock.getCloudStorageType()).thenReturn("gcp");
        when(certVarMock.getGCPStorage()).thenReturn("gcp");
        when(certVarMock.getGCPStorageKey()).thenReturn("gcp-key");
        when(certVarMock.getGCPStorageSecret()).thenReturn("gcp-secret");
        when(certVarMock.getGCPStorageEndPoint()).thenReturn("https://gcp-endpoint");

        setStaticFieldUsingUnsafe(CertificateGeneratorActor.class, "certVar", certVarMock);

        storageServiceFactoryStaticMock.when(() -> StorageServiceFactory.getStorageService(any()))
                .thenReturn(mockStorageService);

        Object actorInstance = createActorByUnsafe();
        setField(actorInstance, "logger", mock(DiagnosticLoggingAdapter.class));

        Method method = CertificateGeneratorActor.class.getDeclaredMethod("getStorageService");
        method.setAccessible(true);
        BaseStorageService result = (BaseStorageService) method.invoke(actorInstance);
        assertNotNull(result);
    }

    @Test
    void testGetStorageService_withInvalidType_shouldThrow() throws Exception {
        // Arrange
        when(certVarMock.getCloudStorageType()).thenReturn("invalid");
        when(certVarMock.getAzureStorage()).thenReturn("azure");
        when(certVarMock.getAwsStorage()).thenReturn("aws");
        when(certVarMock.getCephs3Storage()).thenReturn("ceph");
        when(certVarMock.getGCPStorage()).thenReturn("gcp");

        setStaticFieldUsingUnsafe(CertificateGeneratorActor.class, "certVar", certVarMock);
        Object actorInstance = createActorByUnsafe();
        setField(actorInstance, "logger", mock(DiagnosticLoggingAdapter.class));

        Method method = CertificateGeneratorActor.class.getDeclaredMethod("getStorageService");
        method.setAccessible(true);

        // Act & Assert
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> method.invoke(actorInstance));
        Throwable cause = ex.getCause();
        assertTrue(cause instanceof BaseException);
        assertEquals("INTERNAL_ERROR", ((BaseException) cause).getCode());
    }

    @Test
    void testPopulatePropertiesMap_success() throws Exception {
        // 1. Setup actor
        Object actorInstance = createActorByUnsafe();
        prepareLogger(actorInstance);
        setStaticFieldUsingUnsafe(CertificateGeneratorActor.class, "certVar", certVarMock);

        // 2. Mock data
        String tag = "exampleTag";
        String preview = "false";
        String keyId = "key123";

        Map<String, Object> keys = new HashMap<>();
        keys.put(JsonKey.ID, keyId);

        Map<String, Object> certificateMap = new HashMap<>();
        certificateMap.put(JsonKey.TAG, tag);
        certificateMap.put(JsonKey.PREVIEW, preview);
        certificateMap.put(JsonKey.KEYS, keys);
        certificateMap.put(JsonKey.BASE_PATH, "some/base/path");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.CERTIFICATE, certificateMap);

        Request mockRequest = mock(Request.class);
        when(mockRequest.get(JsonKey.CERTIFICATE)).thenReturn(certificateMap);
        when(mockRequest.getRequest()).thenReturn(requestMap);

        // 3. Stub certVar calls
        when(certVarMock.getSignCreator(keyId)).thenReturn("creator");
        when(certVarMock.getPUBLIC_KEY_URL(keyId)).thenReturn("publicUrl");
        when(certVarMock.getCONTAINER_NAME()).thenReturn("container");
        when(certVarMock.getBADGE_URL(tag)).thenReturn("badge");
        when(certVarMock.getISSUER_URL()).thenReturn("issuer");
        when(certVarMock.getEVIDENCE_URL()).thenReturn("evidence");
        when(certVarMock.getCONTEXT()).thenReturn("context");
        when(certVarMock.getVERIFICATION_TYPE()).thenReturn("type");
        when(certVarMock.getACCESS_CODE_LENGTH()).thenReturn("6");
        when(certVarMock.getEncSignUrl()).thenReturn("signUrl");
        when(certVarMock.getEncSignVerifyUrl()).thenReturn("verifyUrl");
        when(certVarMock.getEncryptionServiceUrl()).thenReturn("encUrl");
        when(certVarMock.getSignatoryExtensionUrl()).thenReturn("signExt");
        when(certVarMock.getSlug()).thenReturn("slug");
        when(certVarMock.getPreview(preview)).thenReturn("false");
        when(certVarMock.getBasePath()).thenReturn("some/base/path");

        // 4. Invoke
        Method method = CertificateGeneratorActor.class.getDeclaredMethod("populatePropertiesMap", Request.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, String> result = (HashMap<String, String>) method.invoke(actorInstance, mockRequest);

        // 5. Assert expected properties
        assertEquals("exampleTag", result.get(JsonKey.TAG));
        assertEquals("container", result.get(JsonKey.CONTAINER_NAME));
        assertEquals("badge", result.get(JsonKey.BADGE_URL));
        assertEquals("creator", result.get(JsonKey.SIGN_CREATOR));
        assertEquals("slug", result.get(JsonKey.SLUG));
        assertEquals("false", result.get(JsonKey.PREVIEW));
        assertEquals("some/base/path", result.get(JsonKey.BASE_PATH));
        assertEquals("key123", result.get(JsonKey.KEY_ID));
    }

    @Test
    void testPopulatePropertiesMap_whenKeysObjectEmpty() throws Exception {
        // 1. Setup actor
        Object actorInstance = createActorByUnsafe();
        prepareLogger(actorInstance);
        setStaticFieldUsingUnsafe(CertificateGeneratorActor.class, "certVar", certVarMock);

        // 2. Setup mocks
        Map<String, Object> certificateMap = new HashMap<>();
        certificateMap.put(JsonKey.TAG, "tag");
        certificateMap.put(JsonKey.PREVIEW, "true");
        certificateMap.put(JsonKey.KEYS, new HashMap<>());  // empty
        certificateMap.put(JsonKey.BASE_PATH, "base");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(JsonKey.CERTIFICATE, certificateMap);

        Request mockRequest = mock(Request.class);
        when(mockRequest.get(JsonKey.CERTIFICATE)).thenReturn(certificateMap);
        when(mockRequest.getRequest()).thenReturn(requestMap);

        // minimal stubs
        when(certVarMock.getCONTAINER_NAME()).thenReturn("container");
        when(certVarMock.getBADGE_URL("tag")).thenReturn("badge");
        when(certVarMock.getISSUER_URL()).thenReturn("issuer");
        when(certVarMock.getEVIDENCE_URL()).thenReturn("evidence");
        when(certVarMock.getCONTEXT()).thenReturn("context");
        when(certVarMock.getVERIFICATION_TYPE()).thenReturn("type");
        when(certVarMock.getACCESS_CODE_LENGTH()).thenReturn("6");
        when(certVarMock.getEncSignUrl()).thenReturn("signUrl");
        when(certVarMock.getEncSignVerifyUrl()).thenReturn("verifyUrl");
        when(certVarMock.getEncryptionServiceUrl()).thenReturn("encUrl");
        when(certVarMock.getSignatoryExtensionUrl()).thenReturn("signExt");
        when(certVarMock.getSlug()).thenReturn("slug");
        when(certVarMock.getPreview("true")).thenReturn("true");
        when(certVarMock.getBasePath()).thenReturn("base");

        // 3. Invoke
        Method method = CertificateGeneratorActor.class.getDeclaredMethod("populatePropertiesMap", Request.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, String> result = (HashMap<String, String>) method.invoke(actorInstance, mockRequest);

        // 4. Assert keys that would still be filled
        assertEquals("tag", result.get(JsonKey.TAG));
        assertEquals("badge", result.get(JsonKey.BADGE_URL));
        assertEquals("true", result.get(JsonKey.PREVIEW));
        assertFalse(result.containsKey(JsonKey.KEY_ID)); // keyId is skipped
    }


    // Use Unsafe to instantiate the actor bypassing constructor
    private Object createActorByUnsafe() throws Exception {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe) theUnsafe.get(null);
        return unsafe.allocateInstance(CertificateGeneratorActor.class);
    }

    @Test
    void testCleanup_deletesMatchingFiles() throws Exception {
        // Create temp directory and file
        File tempDir = Files.createTempDirectory("cert-test").toFile();
        File tempFile = new File(tempDir, "test123.txt");
        assertTrue(tempFile.createNewFile());

        Object actorInstance = createActorByUnsafe();
        prepareLogger(actorInstance);

        invokeCleanup(actorInstance, tempDir.getAbsolutePath(), "test");

        assertFalse(tempFile.exists()); // File should be deleted
    }

    @Test
    void testCleanup_noMatchingFiles() throws Exception {
        File tempDir = Files.createTempDirectory("cert-test").toFile();
        File tempFile = new File(tempDir, "xyz.txt");
        assertTrue(tempFile.createNewFile());

        Object actorInstance = createActorByUnsafe();
        prepareLogger(actorInstance);

        invokeCleanup(actorInstance, tempDir.getAbsolutePath(), "test"); // file doesn't start with "test"

        assertTrue(tempFile.exists()); // File should not be deleted
        tempFile.delete();
    }

    @Test
    void testCleanup_directoryEmptyOrNull() throws Exception {
        File nonExistentDir = new File("/path/that/does/not/exist");

        Object actorInstance = createActorByUnsafe();
        prepareLogger(actorInstance);

        invokeCleanup(actorInstance, nonExistentDir.getAbsolutePath(), "test");

        // No exception thrown = success
    }

    @Test
    void testCleanup_exceptionThrown() throws Exception {
        Object actorInstance = createActorByUnsafe();
        prepareLogger(actorInstance);

        // Pass null path to force exception
        invokeCleanup(actorInstance, null, "test");
    }

    @Test
    void testEncodeQrCode_throwsIOException() throws Exception {
        // 1. Create dummy file
        File tempFile = File.createTempFile("badfile", ".tmp");
        File spyFile = spy(tempFile);

        // 2. Mock static FileUtils to throw IOException
        MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class);
        fileUtilsMock.when(() -> FileUtils.readFileToByteArray(spyFile))
                .thenThrow(new IOException("read error"));

        // 3. Create actor
        Object actorInstance = createActorByUnsafe();
        prepareLogger(actorInstance);

        // 4. Reflect and invoke method
        Method method = CertificateGeneratorActor.class.getDeclaredMethod("encodeQrCode", File.class);
        method.setAccessible(true);

        // 5. Assert real exception via getCause()
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(actorInstance, spyFile));

        Throwable cause = ex.getCause();
        assertTrue(cause instanceof IOException);
        assertEquals("read error", cause.getMessage());

        fileUtilsMock.close();
    }


    @Test
    void testEncodeQrCode_success() throws Exception {
        // 1. Create temp file with content
        File tempFile = File.createTempFile("qrcode", ".tmp");
        Files.write(tempFile.toPath(), "test-data".getBytes());

        // 2. Prepare actor
        Object actorInstance = createActorByUnsafe();
        prepareLogger(actorInstance);

        // 3. Execute
        String result = invokeEncodeQrCode(actorInstance, tempFile);

        // 4. Assert
        assertNotNull(result);
        assertEquals(Base64.getEncoder().encodeToString("test-data".getBytes()), result);
        assertFalse(tempFile.exists()); // file should be deleted
    }


    private String invokeEncodeQrCode(Object actor, File file) throws Exception {
        Method method = CertificateGeneratorActor.class.getDeclaredMethod("encodeQrCode", File.class);
        method.setAccessible(true);
        return (String) method.invoke(actor, file);
    }

    // Replace static field using Unsafe
    private static void setStaticFieldUsingUnsafe(Class<?> clazz, String fieldName, Object newValue) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);

        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        Object staticFieldBase = unsafe.staticFieldBase(field);
        long staticFieldOffset = unsafe.staticFieldOffset(field);
        unsafe.putObject(staticFieldBase, staticFieldOffset, newValue);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getSuperclass().getDeclaredField(fieldName); // from BaseActor
        field.setAccessible(true);
        field.set(target, value);
    }

    private void prepareLogger(Object actorInstance) throws Exception {
        DiagnosticLoggingAdapter mockLogger = mock(DiagnosticLoggingAdapter.class);
        setField(actorInstance, "logger", mockLogger);
    }

    private void invokeCleanup(Object actorInstance, String path, String filePrefix) throws Exception {
        Method method = CertificateGeneratorActor.class.getDeclaredMethod("cleanup", String.class, String.class);
        method.setAccessible(true);
        method.invoke(actorInstance, path, filePrefix);
    }

}