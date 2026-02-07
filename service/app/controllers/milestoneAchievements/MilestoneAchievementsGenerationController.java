package controllers.milestoneAchievements;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import akka.actor.ActorRef;
import org.sunbird.incredible.processor.JsonKey;
import org.sunbird.cert.actor.operation.MilestoneAchievementActorOperation;

import controllers.BaseController;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This controller is responsible for milestone achievement generation.
 *
 * @author system
 *
 */
public class MilestoneAchievementsGenerationController extends BaseController {


    @Inject
    @Named("milestone-achievement-generator_actor")
    private ActorRef milestoneAchievementGenerateActorRef;

    /**
     * This method will accept request for milestone achievement generation.
     * it will do request validation and processing of request.
     *
     * @return a CompletableFuture of success response
     */
    public CompletionStage<Result> generateMilestoneAchievement(Http.Request httpRequest) {
        CompletionStage<Result> response = handleRequest(milestoneAchievementGenerateActorRef, httpRequest,
                request -> {
                    Request req = (Request) request;
                    Map<String, Object> context = new HashMap<>();
                    context.put(JsonKey.VERSION, JsonKey.VERSION_1);
                    req.setContext(context);
                    new MilestoneAchievementValidator().validateGenerateMilestoneAchievementRequest(req);
                    return null;
                },
                MilestoneAchievementActorOperation.GENERATE_MILESTONE_ACHIEVEMENT.getOperation());
        return response;
    }

    public CompletionStage<Result> download(String uid, Http.Request httpRequest) {

        return handleRequest(
                milestoneAchievementGenerateActorRef,
                httpRequest,
                request -> {
                    Request req = (Request) request;
                    Map<String, Object> context = new HashMap<>();
                    context.put(JsonKey.VERSION, JsonKey.VERSION_1);
                    req.getRequest().put(JsonKey.UUID, uid);
                    req.setContext(context);
                    return null;
                },
                MilestoneAchievementActorOperation
                        .MILESTONE_ACHIEVEMENT_DOWNLOAD.getOperation()
        );
    }
}

