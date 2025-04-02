package controllers.health;

import akka.actor.ActorRef;
import controllers.BaseController;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import controllers.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.BaseException;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.ResponseCode;
import org.sunbird.response.Response;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import utils.module.SignalHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This controller class is responsible for checking the health of the services.
 *
 * @author Anmol
 */
public class HealthController extends BaseController {
  Logger logger = LoggerFactory.getLogger(HealthController.class);
  private static final String service = "service";
  private static final String HEALTH_ACTOR_OPERATION_NAME = "health";

  private final ActorRef healthActorRef;
  private final SignalHandler signalHandler;

  @Inject
  public HealthController(@Named("health-actor") ActorRef healthActorRef, SignalHandler signalHandler) {
    this.healthActorRef = healthActorRef;
    this.signalHandler = signalHandler;
  }

  /**
   * This action method is responsible for checking complete service and dependency health.
   *
   * @return a CompletableFuture of success response
   */
  public CompletionStage<Result> getHealth(Http.Request request) throws BaseException {
    try {
      handleSigTerm();
      logger.info("complete health method called.");
      CompletionStage<Result> response = handleRequest(healthActorRef, request, null, HEALTH_ACTOR_OPERATION_NAME);
      return response;
    } catch (Exception e) {
      return CompletableFuture.completedFuture(RequestHandler.handleFailureResponse(e, request));
    }
  }

  /**
   * This action method is responsible for checking certs-service health.
   *
   * @return a CompletableFuture of success response
   */
  public CompletionStage<Result> getServiceHealth(String health, Http.Request request) throws BaseException {
    CompletableFuture<JsonNode> cf = new CompletableFuture<>();
    try {
      handleSigTerm();
      Response response = new Response();
      response.put(RESPONSE, SUCCESS);
      cf.complete(Json.toJson(response));
      return service.equalsIgnoreCase(health)
              ? cf.thenApplyAsync(Results::ok)
              : cf.thenApplyAsync(Results::badRequest);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(RequestHandler.handleFailureResponse(e, request));
    }
  }

  private void handleSigTerm() throws BaseException {
    if (signalHandler.isShuttingDown()) {
      logger.info(
              "SIGTERM is "
                      + signalHandler.isShuttingDown()
                      + ", so Play server will not allow any new request.");
      throw new BaseException(
              IResponseMessage.SERVICE_UNAVAILABLE,
              IResponseMessage.SERVICE_UNAVAILABLE,
              ResponseCode.SERVICE_UNAVAILABLE.getCode());
    }
  }

  /**
   * This action method is responsible for checking liveness for the pod.
   *
   * @return a CompletableFuture of success response
   */
  public CompletionStage<Result> getLiveness(Http.Request request) throws BaseException {
    CompletableFuture<JsonNode> cf = new CompletableFuture<>();
    try {
      handleSigTerm();
      Response response = new Response();
      response.put(RESPONSE, SUCCESS);
      cf.complete(Json.toJson(response));
      return cf.thenApplyAsync(Results::ok);
    } catch (Exception e) {
      return CompletableFuture.completedFuture(RequestHandler.handleFailureResponse(e, request));
    }
  }
}