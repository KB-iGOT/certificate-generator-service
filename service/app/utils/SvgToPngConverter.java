package utils;

import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.EmbededExternalResourceSecurity;
import org.apache.batik.bridge.ExternalResourceSecurity;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.svg12.SVG12BridgeContext;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.ParsedURL;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/** Rasterizes only the wrapper's SVG. Limits apply per JVM and do not rescale certificates.
 * 4 MiB SVG/embedded bytes, 4096 px per edge, 8 MP output/embedded rasters,
 * 20,000 elements, depth 128, and two active conversions with no waiting queue.
 * Two 8 MP render buffers cost about 61 MiB per conversion, before DOM/assets/PNG overhead.
 */
public final class SvgToPngConverter {
    public static final int MAX_SVG_BYTES = 4 * 1024 * 1024;
    public static final int MAX_DIMENSION = 4096;
    public static final long MAX_PIXELS = 8_000_000;
    public static final int MAX_CONCURRENT_RENDERS = 2;
    public static final int MAX_SVG_ELEMENTS = 20_000;
    public static final int MAX_SVG_DEPTH = 128;
    // Percent escaping can use three ASCII characters per decoded byte, plus the URI prefix.
    private static final int MAX_DATA_URI_CHARS = 3 * MAX_SVG_BYTES + 64;
    private static final Semaphore RENDER_PERMITS = new Semaphore(MAX_CONCURRENT_RENDERS);
    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";
    private static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";

    private SvgToPngConverter() {
    }

    /** Admission covers URI decoding too, avoiding an unbounded queue of decoded SVGs. */
    public static byte[] convertDataUri(String printUri) {
        if (printUri != null && printUri.length() > MAX_DATA_URI_CHARS) {
            throw limit("Certificate SVG data URI exceeds the encoded input limit");
        }
        return withRenderPermit(() -> render(SvgDataUriDecoder.decode(printUri)));
    }

    public static byte[] convert(byte[] svg) {
        validateInputSize(svg);
        return withRenderPermit(() -> render(svg));
    }

    static byte[] withRenderPermit(Supplier<byte[]> operation) {
        if (!RENDER_PERMITS.tryAcquire()) {
            throw new RenderLimitException(503, "Certificate PNG renderer is busy; retry later");
        }
        try {
            return operation.get();
        } finally {
            RENDER_PERMITS.release();
        }
    }

    private static byte[] render(byte[] svg) {
        validateInputSize(svg);
        EmbeddedImageBudget images = new EmbeddedImageBudget();
        try (ByteArrayInputStream input = new ByteArrayInputStream(svg);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setAttribute("http://www.oracle.com/xml/jaxp/properties/maxElementDepth", MAX_SVG_DEPTH);
            Document document = factory.newDocumentBuilder().parse(input);
            Element root = document.getDocumentElement();
            if (!SVG_NAMESPACE.equals(root.getNamespaceURI()) || !"svg".equals(root.getLocalName())) {
                throw new IllegalArgumentException("Expected an SVG document");
            }
            if (document.getElementsByTagName("*").getLength() > MAX_SVG_ELEMENTS) {
                throw limit("Certificate SVG exceeds " + MAX_SVG_ELEMENTS + " elements");
            }
            prepareImages(root);

            PNGTranscoder transcoder = new PNGTranscoder() {
                @Override
                protected UserAgent createUserAgent() {
                    return new SVGAbstractTranscoderUserAgent() {
                        @Override
                        public Dimension2D getViewportSize() {
                            // Viewport context only: default/percentage SVG dimensions resolve against it.
                            return new Dimension(1920, 1080);
                        }

                        @Override
                        public ExternalResourceSecurity getExternalResourceSecurity(ParsedURL resource, ParsedURL documentUrl) {
                            return () -> {
                                new EmbededExternalResourceSecurity(resource).checkLoadExternalResource();
                                images.validate(resource);
                            };
                        }
                    };
                }

                @Override
                protected BridgeContext createBridgeContext(String version) {
                    return boundedContext(userAgent, "1.2".equals(version));
                }

                @Override
                protected void setImageSize(float documentWidth, float documentHeight) {
                    validateDimensions(documentWidth, documentHeight);
                    super.setImageSize(documentWidth, documentHeight);
                    validateDimensions(width, height);
                }
            };
            transcoder.addTranscodingHint(PNGTranscoder.KEY_EXECUTE_ONLOAD, false);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_ALLOWED_SCRIPT_TYPES, "");
            transcoder.addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, Color.WHITE);
            // No output size hints or viewBox-to-pixel substitution.
            // Reject excessive resolved dimensions rather than silently shrinking a certificate.
            transcoder.transcode(new TranscoderInput(document), new TranscoderOutput(output));
            if (images.failure != null) {
                throw images.failure;
            }
            return output.toByteArray();
        } catch (Exception ex) {
            if (images.failure != null) {
                throw images.failure;
            }
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof RenderLimitException) {
                    throw (RenderLimitException) cause;
                }
            }
            throw new IllegalArgumentException("Unable to convert certificate SVG to PNG", ex);
        }
    }

    private static void validateInputSize(byte[] svg) {
        if (svg == null || svg.length == 0) {
            throw new IllegalArgumentException("Certificate SVG must not be empty");
        }
        if (svg.length > MAX_SVG_BYTES) {
            throw limit("Certificate SVG exceeds the 4 MiB input limit");
        }
    }

    private static void validateDimensions(double width, double height) {
        if (!Double.isFinite(width) || !Double.isFinite(height) || width <= 0 || height <= 0) {
            throw limit("Certificate image dimensions must be finite and positive");
        }
        long pixelsWide = (long) Math.ceil(width);
        long pixelsHigh = (long) Math.ceil(height);
        if (pixelsWide > MAX_DIMENSION || pixelsHigh > MAX_DIMENSION) {
            throw limit("Certificate image exceeds " + MAX_DIMENSION + " pixels per edge");
        }
        if (pixelsWide * pixelsHigh > MAX_PIXELS) {
            throw limit("Certificate image exceeds " + MAX_PIXELS + " pixels");
        }
    }

    // Batik resolves physical/default/percentage lengths here, before building child images or filters.
    // Subdocuments use the same guarded contexts so data:SVG cannot bypass output dimension checks.
    private static BridgeContext boundedContext(UserAgent agent, boolean svg12) {
        if (svg12) {
            return new SVG12BridgeContext(agent) {
                @Override
                protected void setDocumentSize(Dimension2D size) {
                    validateDimensions(size.getWidth(), size.getHeight());
                    super.setDocumentSize(size);
                }

                @Override
                public BridgeContext createBridgeContext(SVGOMDocument document) {
                    return boundedContext(getUserAgent(), document.isSVG12());
                }
            };
        }
        return new BridgeContext(agent) {
            @Override
            protected void setDocumentSize(Dimension2D size) {
                validateDimensions(size.getWidth(), size.getHeight());
                super.setDocumentSize(size);
            }

            @Override
            public BridgeContext createBridgeContext(SVGOMDocument document) {
                return boundedContext(getUserAgent(), document.isSVG12());
            }
        };
    }

    private static void prepareImages(Element root) {
        NodeList images = root.getElementsByTagNameNS(SVG_NAMESPACE, "image");
        for (int i = 0; i < images.getLength(); i++) {
            Element image = (Element) images.item(i);
            if (!image.hasAttributeNS(XLINK_NAMESPACE, "href") && image.hasAttribute("href")) {
                image.setAttributeNS(XLINK_NAMESPACE, "xlink:href", image.getAttribute("href"));
            }
        }
    }

    private static RenderLimitException limit(String message) {
        return new RenderLimitException(500, message);
    }

    public static final class RenderLimitException extends IllegalArgumentException {
        private final int status;

        private RenderLimitException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    /** Check raster metadata, not pixels, before Batik can expand a compressed embedded image. */
    private static final class EmbeddedImageBudget {
        private final Set<String> checked = new HashSet<>();
        private long bytes;
        private long pixels;
        private RenderLimitException failure;

        private void validate(ParsedURL resource) {
            if (!checked.add(resource.toString())) {
                return;
            }
            try (InputStream input = resource.openStream()) {
                byte[] data = input.readNBytes(MAX_SVG_BYTES + 1);
                bytes += data.length;
                if (bytes > MAX_SVG_BYTES) {
                    throw limit("Certificate embedded resources exceed the 4 MiB decoded limit");
                }
                try (MemoryCacheImageInputStream imageInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                    if (readers.hasNext()) {
                        ImageReader reader = readers.next();
                        try {
                            reader.setInput(imageInput, true, true);
                            int width = reader.getWidth(0);
                            int height = reader.getHeight(0);
                            validateDimensions(width, height);
                            pixels += (long) width * height;
                            if (pixels > MAX_PIXELS) {
                                throw limit("Certificate embedded images exceed " + MAX_PIXELS + " decoded pixels");
                            }
                        } finally {
                            reader.dispose();
                        }
                    }
                }
            } catch (RenderLimitException ex) {
                failure = ex;
                throw ex;
            } catch (IOException ex) {
                throw new IllegalArgumentException("Unable to inspect embedded certificate image", ex);
            }
        }
    }
}
