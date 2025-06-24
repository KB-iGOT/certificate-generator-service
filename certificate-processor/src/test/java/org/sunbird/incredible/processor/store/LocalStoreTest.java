package org.sunbird.incredible.processor.store;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.sunbird.incredible.processor.JsonKey;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalStoreTest {

    private LocalStore localStore;
    private String domainUrl = "http://localhost:8080";
    private File testFile;

    @BeforeEach
    void setUp() throws IOException {
        localStore = new LocalStore(domainUrl);
        testFile = File.createTempFile("test", ".txt");
        FileWriter writer = new FileWriter(testFile);
        writer.write("test content");
        writer.close();
    }

    @AfterEach
    void tearDown() {
        testFile.delete();
    }

    @Test
    void testSave() throws IOException {
        File destDir = new File(System.getProperty("java.io.tmpdir") + "/certs");
        destDir.mkdir();

        String result = localStore.save(testFile, destDir.getAbsolutePath());
        assertEquals(domainUrl + "/" + JsonKey.ASSETS + "/" + testFile.getName(), result);

        File copiedFile = new File(destDir, testFile.getName());
        assertTrue(copiedFile.exists());
        copiedFile.delete();
        destDir.delete();
    }

    @Test
    void testGetPublicLinkDelegatesToSave() throws IOException {
        File destDir = new File(System.getProperty("java.io.tmpdir") + "/certs");
        destDir.mkdir();

        String link = localStore.getPublicLink(testFile, destDir.getAbsolutePath());
        assertEquals(domainUrl + "/" + JsonKey.ASSETS + "/" + testFile.getName(), link);

        File copiedFile = new File(destDir, testFile.getName());
        assertTrue(copiedFile.exists());
        copiedFile.delete();
        destDir.delete();
    }

    @Test
    void testGetDownloadsFile() throws Exception {
        String fileName = "fetched.txt";
        String localPath = System.getProperty("java.io.tmpdir") + "/";
        String testContent = "Download test";

        // Mock URL and HttpURLConnection
        URL urlMock = mock(URL.class);
        HttpURLConnection connMock = mock(HttpURLConnection.class);
        InputStream inputStream = new ByteArrayInputStream(testContent.getBytes());

        // Inject mocks
        try (MockedConstruction<URL> mockedUrl = mockConstruction(URL.class, (mock, context) -> {
            when(mock.openConnection()).thenReturn(connMock);
        })) {
            when(connMock.getInputStream()).thenReturn(inputStream);

            String testUrl = "http://mocked-url.com/resource.txt";
            localStore.get(testUrl, fileName, localPath);

            File downloaded = new File(localPath + fileName);
            assertTrue(downloaded.exists());
            String content = new String(FileUtils.readFileToByteArray(downloaded));
            assertEquals(testContent, content);

            downloaded.delete();
        }
    }

    @Test
    void testGetOverloadedMethodDoesNothing() {
        assertDoesNotThrow(() -> localStore.get("just-a-filename.txt"));
    }

    @Test
    void testInitDoesNothing() {
        assertDoesNotThrow(() -> localStore.init());
    }

    @Test
    void testCloseDoesNothing() {
        assertDoesNotThrow(() -> localStore.close());
    }
}
