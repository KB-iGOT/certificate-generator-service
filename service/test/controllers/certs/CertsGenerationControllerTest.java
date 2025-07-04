package controllers.certs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.mvc.Http;
import play.mvc.Result;
import akka.actor.ActorRef;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CertsGenerationControllerTest {

    private CertsGenerationController controller;
    private ActorRef mockActorRef;
    private Http.Request mockRequest;

    @BeforeEach
    public void setUp() {
        controller = new CertsGenerationController();
        mockActorRef = mock(ActorRef.class);
        mockRequest = mock(Http.Request.class);
        // Use reflection to inject the mock actor ref
        try {
            java.lang.reflect.Field field = CertsGenerationController.class.getDeclaredField("certGenerateActorRef");
            field.setAccessible(true);
            field.set(controller, mockActorRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void generateCertificate_ReturnsCompletionStageResult_OnValidRequest() {
        // Arrange
        CertsGenerationController spyController = spy(controller);
        doReturn(CompletableFuture.completedFuture(mock(Result.class)))
                .when(spyController)
                .handleRequest(any(), any(), any(), anyString());

        // Act
        CompletionStage<Result> result = spyController.generateCertificate(mockRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.toCompletableFuture().isDone());
    }

    @Test
    public void generateCertificate_ThrowsException_OnInvalidRequest() {
        CertsGenerationController spyController = spy(controller);
        doAnswer(invocation -> {
            throw new RuntimeException("Invalid request");
        }).when(spyController)
                .handleRequest(any(), any(), any(), anyString());

        try {
            spyController.generateCertificate(mockRequest).toCompletableFuture().join();
            fail("Expected exception not thrown");
        } catch (Exception e) {
            String message = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            assertTrue(message.contains("Invalid request"));
        }
    }
}
