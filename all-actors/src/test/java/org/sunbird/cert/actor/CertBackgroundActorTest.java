package org.sunbird.cert.actor;//package org.sunbird.cert.actor;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import akka.testkit.TestActorRef;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.BaseException;
import org.sunbird.cert.helper.CertRegistryHelper;
import org.sunbird.cert.helper.IssueCertificateContentHelper;
import org.sunbird.cert.helper.IssueCertificateEventHelper;
import org.sunbird.cert.helper.UserEnrolmentHelper;
import org.sunbird.request.Request;

import java.util.*;

import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class CertBackgroundActorTest {

    private static ActorSystem system;
    private static TestActorRef<CertBackgroundActor> actorRef;
    private static CertBackgroundActor actor;

    private static MockedStatic<CertRegistryHelper> certRegistryHelperStatic;
    private static MockedStatic<IssueCertificateContentHelper> contentHelperStatic;
    private static MockedStatic<IssueCertificateEventHelper> eventHelperStatic;
    private static MockedStatic<UserEnrolmentHelper> enrolmentHelperStatic;

    private static CertRegistryHelper certRegistryHelper;
    private static IssueCertificateContentHelper issueCertificateContentHelper;
    private static IssueCertificateEventHelper issueCertificateEventHelper;
    private static UserEnrolmentHelper userEnrolmentHelper;

    @BeforeAll
    static void setup() {
        system = ActorSystem.create("testSystem");

        certRegistryHelper = mock(CertRegistryHelper.class);
        issueCertificateContentHelper = mock(IssueCertificateContentHelper.class);
        issueCertificateEventHelper = mock(IssueCertificateEventHelper.class);
        userEnrolmentHelper = mock(UserEnrolmentHelper.class);

        certRegistryHelperStatic = mockStatic(CertRegistryHelper.class);
        contentHelperStatic = mockStatic(IssueCertificateContentHelper.class);
        eventHelperStatic = mockStatic(IssueCertificateEventHelper.class);
        enrolmentHelperStatic = mockStatic(UserEnrolmentHelper.class);

        certRegistryHelperStatic.when(CertRegistryHelper::getInstance).thenReturn(certRegistryHelper);
        contentHelperStatic.when(IssueCertificateContentHelper::getInstance).thenReturn(issueCertificateContentHelper);
        eventHelperStatic.when(IssueCertificateEventHelper::getInstance).thenReturn(issueCertificateEventHelper);
        enrolmentHelperStatic.when(UserEnrolmentHelper::getInstance).thenReturn(userEnrolmentHelper);

        actorRef = TestActorRef.create(system, akka.actor.Props.create(CertBackgroundActor.class));
        actor = actorRef.underlyingActor();
    }

    @AfterAll
    static void tearDown() {
        TestKit.shutdownActorSystem(system);
        certRegistryHelperStatic.close();
        contentHelperStatic.close();
        eventHelperStatic.close();
        enrolmentHelperStatic.close();
    }

    @BeforeEach
    void resetMocksAndStaticContentCache() {
        reset(certRegistryHelper, issueCertificateContentHelper, issueCertificateEventHelper, userEnrolmentHelper);
    }

    @Test
    void testOnReceive_AddRegistryReq() throws Throwable {
        Request request = new Request();
        request.setOperation("add_registry_req");

        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("courseId", "c1");
        reqMap.put("batchId", "b1");
        reqMap.put("userId", "u1");
        reqMap.put("uuid", "12345");
        reqMap.put("certificate", Map.of("name", "Cert A"));
        reqMap.put("accessCode", "xyz123");
        reqMap.put("userCertifiedList", new ArrayList<>());
        reqMap.put("isEvent", false);

        request.setRequest(reqMap);

        when(certRegistryHelper.addCertToRegistry(anyMap())).thenReturn(Map.of("result", "ok"));

        actor.onReceive(request);

    }

    @Test
    void testUpdateUserEnrolmentRecord_eventTrue() throws BaseException {
        actor.updateUserEnrolmentRecord("uid", "cid", "bid", List.of(Map.of()), true);
        verify(userEnrolmentHelper).updateUserEventEnrollmentRecord(any(), any(), any(), anyMap());
    }

    @Test
    void testUpdateUserEnrolmentRecord_eventFalse() throws BaseException {
        actor.updateUserEnrolmentRecord("uid", "cid", "bid", List.of(Map.of()), false);
        verify(userEnrolmentHelper).updateUserEnrollmentRecord(any(), any(), any(), anyMap());
    }
}
