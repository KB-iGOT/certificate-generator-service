package controllers.certs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import akka.actor.ActorRef;
import org.sunbird.JsonKeys;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.cert.actor.operation.CertActorOperation;

import controllers.BaseController;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

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
        CompletionStage<Result> response = handleRequest(certGenerateActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    Map<String, Object> context = new HashMap<>();
                    context.put(JsonKey.VERSION, JsonKey.VERSION_1);
                    req.getRequest().put(JsonKeys.UID, uid);
                    req.setContext(context);
                    return null;
                },
                CertActorOperation.GENERATE_CERTIFICATES_LEGACY_APP.getOperation());
        return response;
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