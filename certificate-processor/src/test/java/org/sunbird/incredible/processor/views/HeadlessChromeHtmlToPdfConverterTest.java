package org.sunbird.incredible.processor.views;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HeadlessChromeHtmlToPdfConverterTest {

    private File mockHtmlFile;
    private File mockPdfFile;
    private Process mockProcess;

    @BeforeEach
    public void setUp() throws IOException {
        mockHtmlFile = File.createTempFile("test", ".html");
        mockPdfFile = File.createTempFile("test", ".pdf");
        mockProcess = mock(Process.class);
    }

    private void mockProcessStreams() throws IOException, InterruptedException {
        InputStream mockInputStream = new ByteArrayInputStream("Mock Output".getBytes());
        InputStream mockErrorStream = new ByteArrayInputStream("Mock Error".getBytes());

        when(mockProcess.getInputStream()).thenReturn(mockInputStream);
        when(mockProcess.getErrorStream()).thenReturn(mockErrorStream);
        when(mockProcess.waitFor()).thenReturn(0);
    }

    @Test
    public void testConvert_Windows() throws Exception {
        System.setProperty("os.name", "Windows 10");
        mockProcessStreams();

        Runtime mockRuntime = mock(Runtime.class);
        when(mockRuntime.exec(any(String[].class))).thenReturn(mockProcess);

        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);

            HeadlessChromeHtmlToPdfConverter.convert(mockHtmlFile, mockPdfFile);

            verify(mockRuntime).exec(any(String[].class));
            verify(mockProcess).waitFor();
        }
    }

    @Test
    public void testConvert_Mac() throws Exception {
        System.setProperty("os.name", "Mac OS X");
        mockProcessStreams();

        Runtime mockRuntime = mock(Runtime.class);
        when(mockRuntime.exec(anyString())).thenReturn(mockProcess);

        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);

            HeadlessChromeHtmlToPdfConverter.convert(mockHtmlFile, mockPdfFile);

            verify(mockRuntime).exec(anyString());
            verify(mockProcess).waitFor();
        }
    }

    @Test
    public void testConvert_Linux() throws Exception {
        System.setProperty("os.name", "Linux");
        mockProcessStreams();

        Runtime mockRuntime = mock(Runtime.class);
        when(mockRuntime.exec(any(String[].class))).thenReturn(mockProcess);

        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);

            HeadlessChromeHtmlToPdfConverter.convert(mockHtmlFile, mockPdfFile);

            verify(mockRuntime).exec(any(String[].class));
            verify(mockProcess).waitFor();
        }
    }

    @Test
    public void testConvert_WithException() {
        System.setProperty("os.name", "Linux");

        try {
            HeadlessChromeHtmlToPdfConverter.convert(new File("/invalid.html"), new File("/invalid.pdf"));
        } catch (Exception e) {
            // should not throw to outside
            assertTrue(false, "Should not throw exception");
        }
    }
}
