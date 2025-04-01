package utils.module;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
/**
 * This class will be called on each request.
 * any request pre-filter can be done here.
 * @author manzarul
 *
 */
public class OnRequestHandler extends Action.Simple {
    private static final Logger logger = LoggerFactory.getLogger(OnRequestHandler.class);

    @Override
    public CompletionStage<Result> call(Http.Request request) {  // ✅ Use `Http.Request` instead of `Context`
        Optional<String> requestIdHeader = request.getHeaders().get("X-Request-ID");
        String reqId = requestIdHeader.orElseGet(() -> UUID.randomUUID().toString());

        MDC.clear();
        MDC.put("REQUEST_MESSAGE_ID", reqId);

        CompletionStage<Result> result = delegate.call(request);  // ✅ Use `request` instead of `context`
        return result.thenApply(res -> res.withHeader("Access-Control-Allow-Origin", "*"));
    }
}