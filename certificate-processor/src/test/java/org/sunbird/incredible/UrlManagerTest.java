package org.sunbird.incredible;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.*;

class UrlManagerTest {

    @Test
    void testGetSharableUrl() {
        String inputUrl = "https://example.com/containerName/path/to/file.pdf?token=abc";
        String result = UrlManager.getSharableUrl(inputUrl, "containerName");
        assertEquals("/path/to/file.pdf", result);
    }

    @Test
    void testRemoveQueryParamsWithQuery() {
        String url = "https://example.com/file.pdf?param=value";
        String result = UrlManager.removeQueryParams(url);
        assertEquals("https://example.com/file.pdf", result);
    }

    @Test
    void testRemoveQueryParamsWithoutQuery() {
        String url = "https://example.com/file.pdf";
        String result = UrlManager.removeQueryParams(url);
        assertEquals("https://example.com/file.pdf", result);
    }

    @Test
    void testRemoveQueryParamsWithBlank() {
        String url = "";
        String result = UrlManager.removeQueryParams(url);
        assertEquals("", result);
    }

    @Test
    void testRemoveContainerName() {
        String url = "/containerName/path/to/file.pdf";
        String result = UrlManager.getSharableUrl(url, "containerName");
        assertEquals("", result);
    }

    @Test
    void testFetchFileFromUrl_InvalidUrl() {
        // This is indirectly tested by simulating a malformed URL to trigger exception
        String malformedUrl = "ht!tp://@@malformed-url";
        String result = UrlManager.getSharableUrl(malformedUrl, "containerName");
        assertEquals("", result); // should fallback to empty string after logging
    }

    @Test
    void testGetContainerRelativePath_WithHttpUrl() throws MalformedURLException {
        String url = "https://example.com/sunbird-cb/issuers/do_12345/abc123.pdf";
        String result = UrlManager.getContainerRelativePath(url);
        assertEquals("do_12345/abc123.pdf", result);
    }

    @Test
    void testGetContainerRelativePath_NonHttpUrl() throws MalformedURLException {
        String url = "/path/to/file.pdf";
        String result = UrlManager.getContainerRelativePath(url);
        assertEquals(url, result);
    }

    @Test
    void testGetContainerRelativePath_MalformedURL() {
        assertThrows(MalformedURLException.class, () -> {
            UrlManager.getContainerRelativePath("http://::malformed-url");
        });
    }
}
