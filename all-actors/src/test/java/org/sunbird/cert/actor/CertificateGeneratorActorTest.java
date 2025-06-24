package org.sunbird.cert.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Dispatchers;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.auth.AccessTokenValidator;
import org.sunbird.cert.helper.CertRegistryHelper;
import org.sunbird.cert.helper.IssueCertificateContentHelper;
import org.sunbird.cert.helper.UserEnrolmentHelper;
import org.sunbird.request.Request;
import scala.collection.JavaConverters;

import scala.collection.immutable.Seq;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class CertificateGeneratorActorTest {

    private static ActorSystem system;
    private static IssueCertificateContentHelper contentHelper;
    private static CertRegistryHelper certRegistryHelper;
    private static UserEnrolmentHelper userEnrolmentHelper;

    private static MockedStatic<IssueCertificateContentHelper> mockIssueCertificateContentHelper;
    private static MockedStatic<CertRegistryHelper> mockCertRegistryHelper;
    private static MockedStatic<UserEnrolmentHelper> mockUserEnrolmentHelper;

    private static MockedStatic<AccessTokenValidator> mockAccessTokenValidator;

    @BeforeAll
    static void beforeAll() {
        system = ActorSystem.create("test-system");

        contentHelper = mock(IssueCertificateContentHelper.class);
        certRegistryHelper = mock(CertRegistryHelper.class);
        userEnrolmentHelper = mock(UserEnrolmentHelper.class);

        mockIssueCertificateContentHelper = mockStatic(IssueCertificateContentHelper.class);
        mockIssueCertificateContentHelper.when(IssueCertificateContentHelper::getInstance).thenReturn(contentHelper);

        mockCertRegistryHelper = mockStatic(CertRegistryHelper.class);
        mockCertRegistryHelper.when(CertRegistryHelper::getInstance).thenReturn(certRegistryHelper);

        mockUserEnrolmentHelper = mockStatic(UserEnrolmentHelper.class);
        mockUserEnrolmentHelper.when(UserEnrolmentHelper::getInstance).thenReturn(userEnrolmentHelper);

        // ✅ Static mock made global
        mockAccessTokenValidator = mockStatic(AccessTokenValidator.class);
        mockAccessTokenValidator.when(() -> AccessTokenValidator.verifyUserToken(anyString(), eq(true)))
                .thenReturn("uid");
    }

    @AfterAll
    static void afterAll() {
        TestKit.shutdownActorSystem(system);
        mockIssueCertificateContentHelper.close();
        mockCertRegistryHelper.close();
        mockUserEnrolmentHelper.close();
        mockAccessTokenValidator.close(); // don't forget this
    }

    @BeforeEach
    void resetMocks() {
        reset(contentHelper, certRegistryHelper, userEnrolmentHelper);
    }

    private Request buildGenerateCertRequest() {
        Request req = new Request();
        req.setOperation("generateCert");
        Map<String, Object> map = new HashMap<>();
        map.put("courseId", "cid");
        map.put("userId", "uid");
        map.put("batchId", "bid");
        req.setRequest(map);
        return req;
    }


    private Request buildGenerateCertRequestWithHeaders() {
        Request req = new Request();
        req.setOperation("generateCert");
        Map<String, Object> requestMap = Map.of(
                "courseId", "cid",
                "userId", "uid",
                "batchId", "bid"
        );
        req.setRequest(requestMap);

        List<String> javaList = Collections.singletonList("Bearer test-token-123");
        Seq<String> scalaSeq = (Seq<String>) JavaConverters.collectionAsScalaIterableConverter(javaList).asScala().toSeq();
        Map<String, Object> headers = new HashMap<>();
        headers.put("x-authenticated-user-token", scalaSeq);
        req.setHeaders(headers);
        return req;
    }


    @Test
    void onReceiveProcessesCertificateGenerationRequestSuccessfully() {
        new TestKit(system) {{
            Props props = Props.create(CertificateGeneratorActor.class)
                    .withDispatcher(Dispatchers.DefaultDispatcherId());
            ActorRef actorRef = system.actorOf(props);

            Request req = buildGenerateCertRequestWithHeaders();
            actorRef.tell(req, getRef());
        }};
    }


    @Test
    void onReceiveHandlesUnknownOperationGracefully() {
        new TestKit(system) {{
            Props.create(CertificateGeneratorActor.class);
            ActorRef actorRef = system.actorOf(Props.create(CertificateGeneratorActor.class));

            Request req = new Request();
            req.setOperation("unknown_operation");

            actorRef.tell(req, getRef());

            expectNoMessage(Duration.ofSeconds(2)); // Actor should not crash or respond
        }};
    }

    @Test
    void onReceiveHandlesExceptionDuringCertificateGeneration() throws Exception {
        new TestKit(system) {{
            Props.create(CertificateGeneratorActor.class);
            ActorRef actorRef = system.actorOf(Props.create(CertificateGeneratorActor.class));

            when(contentHelper.generateCertificateMap(anyMap(), anyMap()))
                    .thenThrow(new RuntimeException("mock exception"));

            actorRef.tell(buildGenerateCertRequest(), getRef());

        }};
    }
}
