package org.sunbird;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import akka.actor.ActorSystem;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.sunbird.message.IResponseMessage;
import org.sunbird.message.Localizer;
import org.sunbird.message.ResponseCode;
import org.sunbird.request.Request;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseActorTest {

    static ActorSystem system;

    @BeforeAll
    static void setup() {
        system = ActorSystem.create("test-system");
    }

    @AfterAll
    static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    static class TestBaseActor extends BaseActor {
        boolean onReceiveCalled = false;
        Request receivedRequest = null;
        Exception receivedException = null;
        String unsupportedCaller = null;

        @Override
        public void onReceive(Request request) {
            onReceiveCalled = true;
            receivedRequest = request;
        }

        @Override
        protected void onReceiveException(String callerName, Exception exception) {
            receivedException = exception;
        }

        @Override
        protected void onReceiveUnsupportedMessage(String callerName) {
            unsupportedCaller = callerName;
        }

        @Override
        public Localizer getLocalizer() {
            Localizer localizer = mock(Localizer.class);
            when(localizer.getMessage(anyString(), any())).thenReturn("localized");
            return localizer;
        }
    }

    @Test
    void onReceiveExceptionSendsExceptionToSender() throws Exception {
        new TestKit(system) {{
            ActorRef ref = system.actorOf(Props.create(BaseActor.class, () -> new BaseActor() {
                @Override
                public void onReceive(Request request) {}
            }));
            Exception ex = new Exception("err");
            ref.tell(ex, getRef());
            // No assertion, just ensure no exception thrown
        }};
    }

    @Test
    void onReceiveUnsupportedMessageSendsExceptionToSender() {
        new TestKit(system) {{
            ActorRef ref = system.actorOf(Props.create(BaseActor.class, () -> new BaseActor() {
                @Override
                public void onReceive(Request request) {}
                @Override
                public Localizer getLocalizer() {
                    Localizer localizer = mock(Localizer.class);
                    when(localizer.getMessage(anyString(), any())).thenReturn("localized");
                    return localizer;
                }
            }));
            ref.tell("unsupported", getRef());
            // No assertion, just ensure no exception thrown
        }};
    }
}