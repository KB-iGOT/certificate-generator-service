package controllers.certs;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Status;
import akka.actor.UntypedAbstractActor;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sunbird.BaseException;
import org.sunbird.JsonKeys;
import org.sunbird.cert.actor.operation.CertActorOperation;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import play.Application;
import play.Mode;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import utils.module.ACTOR_NAMES;
import utils.module.ActorStartModule;
import utils.module.StartModule;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static play.inject.Bindings.bind;

public class CertsGenerationControllerTest {
    private static final String CERTIFICATE_ID = "certificate-123";
    private static final String SVG = certificateSvg();
    private static final String PRINT_URI = "data:image/svg+xml,"
            + SVG.replace("%", "%25").replace("<", "%3C").replace(">", "%3E");

    private ActorSystem actorSystem;
    private Application application;
    private final AtomicReference<Object> reply = new AtomicReference<>();
    private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>();

    @Before
    public void setUp() {
        reply.set(certificateResponse(PRINT_URI));
        actorSystem = ActorSystem.create("certificate-render-test");
        ActorRef actor = actorSystem.actorOf(Props.create(ReplyActor.class, () -> new ReplyActor(reply, requests)));
        GuiceApplicationBuilder builder = new GuiceApplicationBuilder().in(Mode.TEST)
                .disable(StartModule.class, ActorStartModule.class);
        for (ACTOR_NAMES name : ACTOR_NAMES.values()) {
            builder = builder.overrides(bind(ActorRef.class).qualifiedWith(name.getActorName()).toInstance(actor));
        }
        application = builder.build();
        Helpers.start(application);
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (application != null) {
                Helpers.stop(application);
            }
        } finally {
            if (actorSystem != null) {
                actorSystem.terminate();
                actorSystem.getWhenTerminated().toCompletableFuture().get(10, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    public void renderRouteReturnsRealPngAndDispatchesLegacyOperationOnce() throws Exception {
        Result result = get("render");
        assertEquals(200, result.status());
        assertEquals("image/png", result.body().contentType().get());
        byte[] body = Helpers.contentAsBytes(result).toArray();
        assertArrayEquals(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}, Arrays.copyOf(body, 8));
        assertFalse(new String(body, StandardCharsets.UTF_8).startsWith("<svg"));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(body));
        assertNotNull("The response must be a decodable PNG", image);
        assertEquals(320, image.getWidth());
        assertEquals(180, image.getHeight());
        assertEquals(Color.RED.getRGB(), image.getRGB(25, 25));
        assertSingleLegacyDispatch();
    }

    @Test
    public void legacyGenerateRouteStillReturnsUnchangedPrintUriInJsonEnvelope() throws Exception {
        Result result = get("generate");
        assertEquals(200, result.status());
        assertEquals("application/json", result.contentType().get());
        JsonNode json = Json.parse(Helpers.contentAsString(result));
        assertEquals("OK", json.get("responseCode").asText());
        assertEquals(PRINT_URI, json.get("result").get("printUri").asText());
        assertEquals("v1", json.get("ver").asText());
        assertEquals("v1.certs.legacy.generate." + CERTIFICATE_ID, json.get("id").asText());
        assertNotNull(json.get("ts"));
        assertSingleLegacyDispatch();
    }

    @Test
    public void legacyGenerateDoesNotDecodeOrConvertItsPrintUri() throws Exception {
        String printUri = "data:image/svg+xml,%3Csvg";
        reply.set(certificateResponse(printUri));
        Result result = get("generate");
        assertEquals(200, result.status());
        assertEquals("application/json", result.contentType().get());
        assertEquals(printUri, Json.parse(Helpers.contentAsString(result)).get("result").get("printUri").asText());
        assertSingleLegacyDispatch();
    }

    @Test
    public void renderPreservesGeneration400500And503Errors() throws Exception {
        for (int status : new int[] {400, 500, 503}) {
            reply.set(new BaseException("existing.error", "Existing generation failure", status));
            Result result = get("render");
            assertEquals(status, result.status());
            assertEquals("application/json", result.contentType().get());
            JsonNode json = Json.parse(Helpers.contentAsString(result));
            assertEquals("Existing generation failure", status == 503
                    ? json.get("params").get("errmsg").asText() : json.get("result").get("message").asText());
            assertSingleLegacyDispatch();
        }
    }

    @Test
    public void exceptionalActorFailuresPreserveGenerationStatus() throws Exception {
        for (int status : new int[] {400, 500, 503}) {
            reply.set(new Status.Failure(new BaseException("existing.error", "Async generation failure", status)));
            Result result = get("render");
            assertEquals(status, result.status());
            assertEquals("application/json", result.contentType().get());
            JsonNode json = Json.parse(Helpers.contentAsString(result));
            assertEquals("Async generation failure", status == 503
                    ? json.get("params").get("errmsg").asText() : json.get("result").get("message").asText());
            assertSingleLegacyDispatch();
        }
    }

    @Test
    public void excessiveCertificateDimensionsReturnClearJson500() throws Exception {
        reply.set(certificateResponse("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg'"
                + " width='3000' height='3000'/%3E"));
        Result result = get("render");
        assertEquals(500, result.status());
        assertEquals("application/json", result.contentType().get());
        JsonNode json = Json.parse(Helpers.contentAsString(result));
        assertEquals("SERVER_ERROR", json.get("responseCode").asText());
        assertTrue(json.get("result").get("message").asText().contains("8000000 pixels"));
        assertSingleLegacyDispatch();
    }

    @Test
    public void missingOrMalformedPrintUriReturnsJson500() throws Exception {
        for (Object printUri : new Object[] {null, 123, "", "data:image/svg+xml,", "data:image/png,abc",
                "data:image/svg+xml;base64,PHN2Zy8+", "data:image/svg+xml,%GG", "data:image/svg+xml,%FF",
                "data:image/svg+xml,%3Csvg", "data:image/svg+xml,%3Chtml/%3E",
                "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='0' height='0'/%3E",
                "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10'%3E"
                        + "%3Cimage href='data:image/png;base64,invalid' width='10' height='10'/%3E%3C/svg%3E"}) {
            reply.set(certificateResponse(printUri));
            Result result = get("render");
            assertEquals(500, result.status());
            assertEquals("application/json", result.contentType().get());
            assertEquals("SERVER_ERROR", Json.parse(Helpers.contentAsString(result)).get("responseCode").asText());
            assertSingleLegacyDispatch();
        }
    }

    private static String certificateSvg() {
        try {
            BufferedImage logo = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    logo.setRGB(x, y, Color.RED.getRGB());
                }
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(logo, "png", png);
            return "<svg xmlns='http://www.w3.org/2000/svg' width='320' height='180' viewBox='0 0 320 180'>"
                    + "<rect width='320' height='180' fill='white'/>"
                    + "<text x='20' y='150' font-size='18'>A+B C#25% %20 é</text>"
                    + "<image x='20' y='20' width='16' height='16' href='data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(png.toByteArray()) + "'/></svg>";
        } catch (IOException ex) {
            throw new AssertionError("Unable to create certificate fixture", ex);
        }
    }

    private Result get(String action) {
        return Helpers.route(application, new Http.RequestBuilder().method("GET")
                .uri("/v1/certs/legacy/" + action + "/" + CERTIFICATE_ID));
    }

    private void assertSingleLegacyDispatch() throws Exception {
        Request request = requests.poll(5, TimeUnit.SECONDS);
        assertNotNull("The generation actor must receive the request", request);
        assertEquals(CertActorOperation.GENERATE_CERTIFICATES_LEGACY_APP.getOperation(), request.getOperation());
        assertEquals(CERTIFICATE_ID, request.getRequest().get(JsonKeys.UID));
        assertEquals(JsonKey.VERSION_1, request.getContext().get(JsonKey.VERSION));
        assertNull("The wrapper must dispatch exactly once", requests.poll(100, TimeUnit.MILLISECONDS));
    }

    private static Response certificateResponse(Object printUri) {
        Response response = new Response();
        if (printUri != null) {
            response.put(JsonKeys.PRINT_URI, printUri);
        }
        return response;
    }

    public static class ReplyActor extends UntypedAbstractActor {
        private final AtomicReference<Object> reply;
        private final BlockingQueue<Request> requests;

        public ReplyActor(AtomicReference<Object> reply, BlockingQueue<Request> requests) {
            this.reply = reply;
            this.requests = requests;
        }

        @Override
        public void onReceive(Object message) {
            if (message instanceof Request) {
                requests.add((Request) message);
                sender().tell(reply.get(), self());
            } else {
                unhandled(message);
            }
        }
    }
}
