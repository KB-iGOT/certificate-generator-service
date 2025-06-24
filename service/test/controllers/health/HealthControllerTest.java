package controllers.health;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;
import akka.actor.ActorRef;
import play.mvc.Http;
import play.mvc.Result;
import utils.module.SignalHandler;
import java.util.concurrent.CompletionStage;


public class HealthControllerTest {

  private HealthController controller;
  private ActorRef healthActorRef;
  private SignalHandler signalHandler;
  private Http.Request request;

  @Before
  public void setUp() {
    healthActorRef = mock(ActorRef.class);
    signalHandler = mock(SignalHandler.class);
    request = mock(Http.Request.class);
    when(signalHandler.isShuttingDown()).thenReturn(false);
    controller = new HealthController(healthActorRef, signalHandler);
  }

  @Test
  public void getHealth_ReturnsSuccess_WhenNotShuttingDown() throws Exception {
    CompletionStage<Result> resultStage = controller.getHealth(request);
    assertNotNull(resultStage);
  }

  @Test
  public void getHealth_ReturnsServiceUnavailable_WhenShuttingDown() throws Exception {
    when(signalHandler.isShuttingDown()).thenReturn(true);
    CompletionStage<Result> resultStage = controller.getHealth(request);
    Result result = resultStage.toCompletableFuture().get();
    assertEquals(503, result.status());
  }

  @Test
  public void getServiceHealth_ReturnsOk_WhenServiceParamIsService() throws Exception {
    CompletionStage<Result> resultStage = controller.getServiceHealth("service", request);
    Result result = resultStage.toCompletableFuture().get();
    assertEquals(200, result.status());
  }

  @Test
  public void getServiceHealth_ReturnsBadRequest_WhenServiceParamIsNotService() throws Exception {
    CompletionStage<Result> resultStage = controller.getServiceHealth("other", request);
    Result result = resultStage.toCompletableFuture().get();
    assertEquals(400, result.status());
  }

  @Test
  public void getServiceHealth_ReturnsServiceUnavailable_WhenShuttingDown() throws Exception {
    when(signalHandler.isShuttingDown()).thenReturn(true);
    CompletionStage<Result> resultStage = controller.getServiceHealth("service", request);
    Result result = resultStage.toCompletableFuture().get();
    assertEquals(503, result.status());
  }

  @Test
  public void getLiveness_ReturnsOk_WhenNotShuttingDown() throws Exception {
    CompletionStage<Result> resultStage = controller.getLiveness(request);
    Result result = resultStage.toCompletableFuture().get();
    assertEquals(200, result.status());
  }

  @Test
  public void getLiveness_ReturnsServiceUnavailable_WhenShuttingDown() throws Exception {
    when(signalHandler.isShuttingDown()).thenReturn(true);
    CompletionStage<Result> resultStage = controller.getLiveness(request);
    Result result = resultStage.toCompletableFuture().get();
    assertEquals(503, result.status());
  }
}