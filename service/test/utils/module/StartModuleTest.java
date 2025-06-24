package utils.module;

import static org.mockito.Mockito.*;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ApplicationStart;

class StartModuleTest {

    private StartModule module;
    private Binder binder;

    @BeforeEach
    public void setUp() {
        module = new StartModule();
        binder = mock(Binder.class);
        AnnotatedBindingBuilder<?> bindingBuilder = mock(AnnotatedBindingBuilder.class);
        when(binder.bind(any(Class.class))).thenReturn(bindingBuilder);
        }

    @Test
    public void configure_BindsSignalHandlerAndApplicationStartAsEagerSingletons() {
        StartModule spyModule = spy(module);
        spyModule.configure(binder);
        verify(binder, times(1)).bind(SignalHandler.class);
        verify(binder, times(1)).bind(ApplicationStart.class);
    }
}
