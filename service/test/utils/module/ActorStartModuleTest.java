package utils.module;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ActorStartModuleTest {

    private ActorStartModule module;

    @BeforeEach
    public void setUp() {
        module = spy(new ActorStartModule());
    }

    @Test
    public void configure_BindsAllActorsWithRouterConfig() {
        doNothing().when(module).bindActor(any(), anyString(), any());
        module.configure();
        for (ACTOR_NAMES actor : ACTOR_NAMES.values()) {
            verify(module).bindActor(eq(actor.getActorClass()), eq(actor.getActorName()), any());
        }
    }

    @Test
    public void configure_LogsBindingStartAndCompletion() {
        Logger logger = mock(Logger.class);
        module.logger = logger;
        doNothing().when(module).bindActor(any(), anyString(), any());
        module.configure();
        verify(logger).info("binding actors for dependency injection");
        verify(logger).info("binding completed");
    }
}
