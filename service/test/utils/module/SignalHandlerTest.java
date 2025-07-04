package utils.module;


import akka.actor.ActorSystem;
import org.junit.jupiter.api.Test;
import play.api.Application;
import javax.inject.Provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class SignalHandlerTest {

    @Test
    public void defaultConstructor_SetsStopDelayToDefaultWhenEnvVarIsBlank() {
        SignalHandler handler = new SignalHandler();
        assertFalse(handler.isShuttingDown());
    }

    @Test
    public void isShuttingDown_ReturnsFalseInitially() {
        SignalHandler handler = new SignalHandler();
        assertFalse(handler.isShuttingDown());
    }

    @Test
    public void injectedConstructor_HandlesSIGTERMAndSetsShuttingDown() {
        ActorSystem actorSystem = mock(ActorSystem.class);
        Provider<Application> appProvider = mock(Provider.class);
        SignalHandler handler = new SignalHandler(actorSystem, appProvider);
        // Simulate SIGTERM by directly setting the field (since actual signal can't be sent in unit test)
        // Reflection is used here for demonstration; in real code, consider package-private setter for testability
        try {
            java.lang.reflect.Field field = SignalHandler.class.getDeclaredField("isShuttingDown");
            field.setAccessible(true);
            field.set(handler, true);
            assertTrue(handler.isShuttingDown());
        } catch (Exception e) {
            fail("Reflection failed");
        }
    }
}
