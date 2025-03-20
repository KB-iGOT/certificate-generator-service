package org.sunbird.incredible.processor.views;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.incredible.processor.store.LocalStore;
import org.sunbird.incredible.pojos.CertificateExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Some;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SvgGenerator {

    private Logger logger = LoggerFactory.getLogger(SvgGenerator.class);
    private String svgTemplate;
    private String directory;
    private static Map<String, String> encoderMap = new HashMap<>();
    private static Map<String, String> cachedSvgTemplates = new HashMap<>();


    static {
        encoderMap.put("<", "%3C");
        encoderMap.put(">", "%3E");
        encoderMap.put("#", "%23");
        encoderMap.put("%", "%25");
        encoderMap.put("\"", "\'");
    }

    public SvgGenerator(String svgTemplate, String directory) {
        this.svgTemplate = svgTemplate;
        this.directory = directory;
    }

    public String generate(CertificateExtension certificateExtension, String encodedQrCode, BaseStorageService storageService) throws IOException {
        String svgFileName = getSvgFileName();
        String svgContent = "";
        File file = new File(directory + svgFileName);
        if (!file.exists()) {
            logger.info("{} file does not exits , downloading", svgFileName);
            svgContent = download(svgTemplate, storageService);
        }
        if (!cachedSvgTemplates.containsKey(this.svgTemplate)) {
            logger.info("svg data is not cached , read svf file");
            //svgContent = readSvgContent(file.getAbsolutePath());
            String encodedSvg = "data:image/svg+xml," + encodeData(svgContent);
            encodedSvg = encodedSvg.replaceAll("\n", "").replaceAll("\t", "");
            cachedSvgTemplates.put(this.svgTemplate, encodedSvg);
        }
        logger.info("svg template is cached {}", cachedSvgTemplates.containsKey(this.svgTemplate));
        String svgData = replaceTemplateVars(cachedSvgTemplates.get(this.svgTemplate), certificateExtension, encodedQrCode);
        logger.info("svg template string creation completed {}", StringUtils.isNotBlank(svgData));
        return svgData;
    }


    private String replaceTemplateVars(String svgContent, CertificateExtension certificateExtension, String encodeQrCode) {
        HTMLVarResolver htmlVarResolver = new HTMLVarResolver(certificateExtension);
        Map<String, String> certData = htmlVarResolver.getCertMetaData();
        certData.put("qrCodeImage", "data:image/png;base64," + encodeQrCode);
        StringSubstitutor sub = new StringSubstitutor(certData);
        String resolvedString = sub.replace(svgContent);
        logger.info("replacing temp vars completed");
        return resolvedString;
    }

    private String encodeData(String data) {
        StringBuffer stringBuffer = new StringBuffer();
        Pattern pattern = Pattern.compile("[<>#%\"]");
        Matcher matcher = pattern.matcher(data);
        while (matcher.find()) {
            matcher.appendReplacement(stringBuffer, encoderMap.get(matcher.group()));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    private String readSvgContent(String path) throws IOException {
        FileInputStream fis;
        String svgContent = null;
        fis = new FileInputStream(path);
        svgContent = IOUtils.toString(fis, StandardCharsets.UTF_8);
        fis.close();
        return svgContent;
    }

    private void download(String fileName) throws IOException {
        LocalStore localStore = new LocalStore("");
        localStore.get(svgTemplate, fileName, directory);
    }

    private String getSvgFileName() {
        String fileName = null;
        try {
            URI uri = new URI(svgTemplate);
            String path = uri.getPath();
            fileName = path.substring(path.lastIndexOf('/') + 1);
            if (!fileName.endsWith(".svg"))
                return fileName.concat(".svg");
        } catch (URISyntaxException e) {
            logger.debug("Exception while getting file name from template url : {}", e.getMessage());
        }
        return fileName;
    }


    public static String download(String svgTemplate, BaseStorageService storageService) throws FileNotFoundException {
        System.out.println("Got svgTemplate: " + svgTemplate);
        String container = "igotdev";
        String relativePath;

        if (svgTemplate.startsWith("http")) {
            try {
                String uri = StringUtils.substringAfter(new URL(svgTemplate).getPath(), "/");
                container = StringUtils.substringBefore(uri, "/");
                relativePath = StringUtils.substringAfter(uri, "/");
            } catch (Exception e) {
                throw new FileNotFoundException("Invalid URL: " + svgTemplate);
            }
        } else {
            relativePath = svgTemplate;
        }

        System.out.println("Got svgTemplate with relative path: " + relativePath);
        String downloadableUrl = storageService.getPutSignedURL(container, relativePath, Some.apply(30),  Some.apply("r"), Some.empty() );
        System.out.println("Got downloadable svgTemplate url: " + downloadableUrl);

        StringBuilder svgString = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(downloadableUrl).openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                svgString.append(line).append("\n");
            }
        } catch (Exception e) {
            throw new FileNotFoundException("Failed to download SVG template from URL: " + downloadableUrl);
        }

        return svgString.toString();
    }

}
