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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static play.inject.Bindings.bind;

public class CertsGenerationControllerTest {
    private static final String CERTIFICATE_ID = "certificate-123";
    private static final String SVG = "<svg xmlns='http://www.w3.org/2000/svg'><text>A+B C#25% %20 é</text>"
            + "<image href='data:image/png;base64,ab+c/def=='/></svg>";
    private static final String PRINT_URI = "data:image/svg+xml,"
            + "%3Csvg xmlns='http://www.w3.org/2000/svg'%3E%3Ctext%3EA+B%20C%2325%25%20%2520%20%C3%A9%3C/text%3E"
            + "%3Cimage href='data:image/png;base64,ab+c/def=='/%3E%3C/svg%3E";

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
    public void renderRouteReturnsExactUtf8SvgAndDispatchesLegacyOperationOnce() throws Exception {
        Result result = get("render");
        assertEquals(200, result.status());
        assertEquals("image/svg+xml", result.body().contentType().get());
        assertArrayEquals(SVG.getBytes(StandardCharsets.UTF_8), Helpers.contentAsBytes(result).toArray());
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
    public void missingOrMalformedPrintUriReturnsJson500() throws Exception {
        for (Object printUri : new Object[] {null, 123, "", "data:image/svg+xml,", "data:image/png,abc",
                "data:image/svg+xml;base64,PHN2Zy8+", "data:image/svg+xml,%GG", "data:image/svg+xml,%FF"}) {
            reply.set(certificateResponse(printUri));
            Result result = get("render");
            assertEquals(500, result.status());
            assertEquals("application/json", result.contentType().get());
            assertEquals("SERVER_ERROR", Json.parse(Helpers.contentAsString(result)).get("responseCode").asText());
            assertSingleLegacyDispatch();
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
