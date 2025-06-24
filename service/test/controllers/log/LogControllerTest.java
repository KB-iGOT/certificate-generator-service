package controllers.log;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class LogControllerTest {

    private LogController controller;

    @Before
    public void setUp() {
        controller = spy(new LogController());
        doReturn(CompletableFuture.completedFuture(mock(Result.class))).when(controller).handleLogRequest();
    }

    @Test
    public void setLogLevel_DelegatesToHandleLogRequest() throws Exception {
        CompletionStage<Result> resultStage = controller.setLogLevel();
        assertNotNull(resultStage);
        verify(controller, times(1)).handleLogRequest();
    }
}
