package controllers;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.BaseException;
import org.sunbird.message.ResponseCode;
import play.mvc.Http;
import play.mvc.Result;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RequestHandlerTest {

    private RequestHandler handler;
    private Http.Request httpRequest;

    @BeforeEach
    public void setUp() {
        handler = new RequestHandler();
        httpRequest = mock(Http.Request.class);
        when(httpRequest.path()).thenReturn("/v1/cert/generate");
    }

    @Test
    public void handleSuccessResponse_ReturnsOkWithApiId() {
        Response response = new Response();
        Result result = RequestHandler.handleSuccessResponse(response, httpRequest);
        assertEquals(200, result.status());
    }

    @Test
    public void handleFailureResponse_ReturnsBadRequestForBaseExceptionWith400() {
        BaseException ex = new BaseException("ERR", "bad request", 400);
        Result result = RequestHandler.handleFailureResponse(ex, httpRequest);
        assertEquals(400, result.status());
    }

    @Test
    public void handleFailureResponse_ReturnsServiceUnavailableForBaseExceptionWith503() {
        BaseException ex = new BaseException("ERR", "unavailable", 503);
        Result result = RequestHandler.handleFailureResponse(ex, httpRequest);
        assertEquals(503, result.status());
    }

    @Test
    public void handleFailureResponse_ReturnsInternalServerErrorForOtherBaseException() {
        BaseException ex = new BaseException("ERR", "error", 500);
        Result result = RequestHandler.handleFailureResponse(ex, httpRequest);
        assertEquals(500, result.status());
    }

    @Test
    public void handleFailureResponse_ReturnsInternalServerErrorForNonBaseException() {
        Exception ex = new Exception("unexpected");
        Result result = RequestHandler.handleFailureResponse(ex, httpRequest);
        assertEquals(500, result.status());
    }

    @Test
    public void createResponseOnException_SetsResponseCodeAndParams() {
        BaseException ex = new BaseException("ERR", "error", 400);
        Response response = RequestHandler.createResponseOnException(ex);
        assertEquals(ResponseCode.CLIENT_ERROR, response.getResponseCode());
        assertEquals("CLIENT_ERROR", response.getParams().getErr());
        assertEquals("error", response.getParams().getErrmsg());
    }

    @Test
    public void createResponseParamObj_SetsErrorFieldsForNon200() {
        ResponseCode code = ResponseCode.CLIENT_ERROR;
        String message = "bad";
        assertEquals("CLIENT_ERROR", RequestHandler.createResponseParamObj(code, message).getErr());
        assertEquals("bad", RequestHandler.createResponseParamObj(code, message).getErrmsg());
        assertEquals("CLIENT_ERROR", RequestHandler.createResponseParamObj(code, message).getStatus());
    }

    @Test
    public void createResponseParamObj_SetsStatusFor200() {
        ResponseCode code = ResponseCode.OK;
        String message = "ok";
        assertNull(RequestHandler.createResponseParamObj(code, message).getErr());
        assertNull(RequestHandler.createResponseParamObj(code, message).getErrmsg());
        assertEquals("OK", RequestHandler.createResponseParamObj(code, message).getStatus());
    }

    @Test
    public void handleResponse_DelegatesToHandleSuccessResponseForResponseObject() {
        Response response = new Response();
        Result result = RequestHandler.handleResponse(response, httpRequest);
        assertEquals(200, result.status());
    }

    @Test
    public void handleResponse_DelegatesToHandleFailureResponseForException() {
        Exception ex = new Exception("fail");
        Result result = RequestHandler.handleResponse(ex, httpRequest);
        assertEquals(500, result.status());
    }

    @Test
    public void getApiId_ReturnsDotSeparatedApiId() {
        String apiId = RequestHandler.getApiId("/v1/cert/generate");
        assertEquals("v1.cert.generate", apiId);
    }

    @Test
    public void getApiId_ReturnsEmptyStringForBlankUri() {
        String apiId = RequestHandler.getApiId("");
        assertEquals("", apiId);
    }

    @Test
    public void handleRequest_UsesActorRefAndReturnsCompletionStage() throws Exception {
        Request req = new Request();
        req.setTimeout(5);
        ActorRef actorRef = mock(ActorRef.class);
        Promise<Object> promise = Promise.apply();
        Future<Object> future = promise.future();
        doAnswer(invocation -> {
            promise.success(new Response());
            return null;
        }).when(actorRef).tell(any(), any());
        CompletionStage<Result> resultStage = handler.handleRequest(req, actorRef, "operation", httpRequest);
        assertNotNull(resultStage);
    }

    @Test
    public void handleRequest_UsesActorSelectionAndReturnsCompletionStage() throws Exception {
        Request req = new Request();
        req.setTimeout(5);
        ActorSelection actorSel = mock(ActorSelection.class);
        Promise<Object> promise = Promise.apply();
        Future<Object> future = promise.future();
        doAnswer(invocation -> {
            promise.success(new Response());
            return null;
        }).when(actorSel).tell(any(), any());
        CompletionStage<Result> resultStage = handler.handleRequest(req, actorSel, "operation", httpRequest);
        assertNotNull(resultStage);
    }
}
