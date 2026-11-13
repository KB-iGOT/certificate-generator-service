package controllers.certs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import akka.actor.ActorRef;
import org.sunbird.JsonKeys;
import org.sunbird.BaseException;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.cert.actor.operation.CertActorOperation;

import controllers.BaseController;
import controllers.RequestHandler;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import play.mvc.Http;
import play.mvc.Result;
import utils.SvgToPngConverter;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This controller is responsible for certificate generation.
 *
 * @author manzarul
 *
 */
public class CertsGenerationController extends BaseController {


    @Inject
    @Named("certificate-generator_actor")
    private ActorRef certGenerateActorRef;

    /**
     * This method will accept request for certificate generation.
     * it will do request validation and processing of request.
     *
     * @return a CompletableFuture of success response
     */
    public CompletionStage<Result> generateCertificate(Http.Request httpRequest) {
        CompletionStage<Result> response = handleRequest(certGenerateActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    Map<String, Object> context = new HashMap<>();
                    context.put(JsonKey.VERSION, JsonKey.VERSION_1);
                    req.setContext(context);
                    new CertValidator().validateGenerateCertRequest(req);
                    return null;
                },
                CertActorOperation.GENERATE_CERTIFICATE.getOperation());
        return response;
    }

    /**
     * This method will accept request for certificate generation for old mobile app..
     *
     * @return a CompletableFuture of success response
     */
    public CompletionStage<Result> generateCertificatesLegacyApp(String uid, Http.Request httpRequest) {
        return generateLegacyCertificate(uid, httpRequest, null);
    }

    /** Returns the existing generated certificate as a PNG image. */
    public CompletionStage<Result> renderCertificatesLegacyApp(String uid, Http.Request httpRequest) {
        return generateLegacyCertificate(uid, httpRequest, response -> {
            Object printUri = response.getResult().get(JsonKeys.PRINT_URI);
            if (!(printUri instanceof String)) {
                throw new IllegalArgumentException("Generated certificate printUri must be a string");
            }
            try {
                return ok(SvgToPngConverter.convertDataUri((String) printUri)).as("image/png");
            } catch (SvgToPngConverter.RenderLimitException ex) {
                return RequestHandler.handleFailureResponse(
                        new BaseException("CERTIFICATE_RENDER_LIMIT", ex.getMessage(), ex.getStatus()), httpRequest);
            }
        }).exceptionally(ex -> {
            Throwable cause = ex;
            while (cause instanceof CompletionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            return RequestHandler.handleFailureResponse(cause, httpRequest);
        });
    }

    private CompletionStage<Result> generateLegacyCertificate(String uid, Http.Request httpRequest,
            Function<Response, Result> successMapper) {
        return handleRequest(certGenerateActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    Map<String, Object> context = new HashMap<>();
                    context.put(JsonKey.VERSION, JsonKey.VERSION_1);
                    req.getRequest().put(JsonKeys.UID, uid);
                    req.setContext(context);
                    return null;
                },
                CertActorOperation.GENERATE_CERTIFICATES_LEGACY_APP.getOperation(), successMapper);
    }

    /**
     * This method will accept request for certificate generation by Admin.
     * it will do request validation and processing of request.
     *
     * @return a CompletableFuture of success response
     */
    public CompletionStage<Result> generateCertificatesByAdmin(Http.Request httpRequest) {
        CompletionStage<Result> response = handleRequest(certGenerateActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    Map<String, Object> context = new HashMap<>();
                    context.put(JsonKey.VERSION, JsonKey.VERSION_1);
                    req.setContext(context);
                    new CertValidator().validateGenerateCertRequest(req);
                    return null;
                },
                CertActorOperation.GENERATE_CERTIFICATE_ADMIN.getOperation());
        return response;
    }
}