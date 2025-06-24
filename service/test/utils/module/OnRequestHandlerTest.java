package utils.module;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Action;
import org.sunbird.incredible.processor.JsonKey;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class OnRequestHandlerTest {

    private OnRequestHandler handler;
    private Http.Request request;
    private Http.Headers headers;
    private Method method;

    @Before
    public void setUp() throws Exception {
        handler = new OnRequestHandler();
        request = mock(Http.Request.class);
        headers = mock(Http.Headers.class);
        when(request.getHeaders()).thenReturn(headers);
        method = String.class.getMethod("toString");
    }

    @Test
    public void createAction_SetsRequestIdHeaderIfAbsent() throws Exception {
        when(headers.get(JsonKey.X_REQUEST_ID)).thenReturn(Optional.empty());
        when(headers.addHeader(anyString(), anyString())).thenReturn(headers);
        Action action = handler.createAction(request, method);
        Action.Simple simple = (Action.Simple) action;
        Action delegate = mock(Action.class);
        simple.delegate = delegate;
        Result mockResult = mock(Result.class);
        when(delegate.call(any())).thenReturn(CompletableFuture.completedFuture(mockResult));
        when(mockResult.withHeader(anyString(), anyString())).thenReturn(mockResult);

        CompletionStage<Result> resultStage = simple.call(request);
        Result result = resultStage.toCompletableFuture().get();
        verify(headers).addHeader(eq(JsonKey.REQUEST_MESSAGE_ID), anyString());
        assertNotNull(result);
    }

    @Test
    public void createAction_UsesExistingRequestIdHeader() throws Exception {
        String reqId = UUID.randomUUID().toString();
        when(headers.get(JsonKey.X_REQUEST_ID)).thenReturn(Optional.of(reqId));
        when(headers.addHeader(anyString(), anyString())).thenReturn(headers);
        Action action = handler.createAction(request, method);
        Action.Simple simple = (Action.Simple) action;
        Action delegate = mock(Action.class);
        simple.delegate = delegate;
        Result mockResult = mock(Result.class);
        when(delegate.call(any())).thenReturn(CompletableFuture.completedFuture(mockResult));
        when(mockResult.withHeader(anyString(), anyString())).thenReturn(mockResult);

        CompletionStage<Result> resultStage = simple.call(request);
        Result result = resultStage.toCompletableFuture().get();
        verify(headers).addHeader(JsonKey.REQUEST_MESSAGE_ID, reqId);
        assertNotNull(result);
    }

    @Test
    public void createAction_AddsCORSHeaderToResponse() throws Exception {
        when(headers.get(JsonKey.X_REQUEST_ID)).thenReturn(Optional.empty());
        when(headers.addHeader(anyString(), anyString())).thenReturn(headers);
        Action action = handler.createAction(request, method);
        Action.Simple simple = (Action.Simple) action;
        Action delegate = mock(Action.class);
        simple.delegate = delegate;
        Result mockResult = mock(Result.class);
        when(delegate.call(any())).thenReturn(CompletableFuture.completedFuture(mockResult));
        when(mockResult.withHeader(anyString(), anyString())).thenReturn(mockResult);

        CompletionStage<Result> resultStage = simple.call(request);
        resultStage.toCompletableFuture().get();
        verify(mockResult).withHeader("Access-Control-Allow-Origin", "*");
    }
}