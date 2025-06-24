package controllers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;
import akka.actor.ActorRef;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import java.util.concurrent.CompletionStage;
import org.sunbird.RequestValidatorFunction;

public class BaseControllerTest {

    private BaseController baseController;
    private HttpExecutionContext httpExecutionContext;

    @Before
    public void setUp() {
        httpExecutionContext = mock(HttpExecutionContext.class);
        when(httpExecutionContext.current()).thenReturn(runnable -> runnable.run());
        baseController = new BaseController();
        baseController.httpExecutionContext = httpExecutionContext;
    }

    @Test
    public void handleRequest_ReturnsSuccessResponse() throws Exception {
        CompletionStage<Result> resultStage = baseController.handleRequest();
        Result result = resultStage.toCompletableFuture().get();
        assertEquals(200, result.status());
    }

    @Test
    public void getTimeStamp_ReturnsCurrentTime() {
        long before = System.currentTimeMillis();
        long ts = baseController.getTimeStamp();
        long after = System.currentTimeMillis();
        assertTrue(ts >= before && ts <= after);
    }

    @Test
    public void handleRequest_WithValidRequestAndValidator_ReturnsCompletionStage() throws Exception {
        ActorRef actorRef = mock(ActorRef.class);
        Http.Request req = mock(Http.Request.class);
        when(req.body()).thenReturn(mock(Http.RequestBody.class));
        when(req.body().asJson()).thenReturn(null);
        RequestValidatorFunction validator = mock(RequestValidatorFunction.class);

        CompletionStage<Result> resultStage = baseController.handleRequest(actorRef, req, validator, "operation");
        assertNotNull(resultStage);
    }

    @Test
    public void handleRequest_WithExceptionInValidator_ReturnsFailureResponse() throws Exception {
        ActorRef actorRef = mock(ActorRef.class);
        Http.Request req = mock(Http.Request.class);
        when(req.body()).thenReturn(mock(Http.RequestBody.class));
        when(req.body().asJson()).thenReturn(null);
        RequestValidatorFunction validator = mock(RequestValidatorFunction.class);
        doThrow(new RuntimeException("Validation failed")).when(validator).apply(any());

        CompletionStage<Result> resultStage = baseController.handleRequest(actorRef, req, validator, "operation");
        Result result = resultStage.toCompletableFuture().get();
        assertEquals(500, result.status());
    }

    @Test
    public void handleRequest_WithBaseException_ReturnsFailureResponse() throws Exception {
        ActorRef actorRef = mock(ActorRef.class);
        Http.Request req = mock(Http.Request.class);
        when(req.body()).thenReturn(mock(Http.RequestBody.class));
        when(req.body().asJson()).thenReturn(null);
        RequestValidatorFunction validator = mock(RequestValidatorFunction.class);
        doThrow(new org.sunbird.BaseException("ERR", "msg", 400)).when(validator).apply(any());

        CompletionStage<Result> resultStage = baseController.handleRequest(actorRef, req, validator, "operation");
        Result result = resultStage.toCompletableFuture().get();
        assertEquals(400, result.status());
    }
}