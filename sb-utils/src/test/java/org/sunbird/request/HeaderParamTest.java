package org.sunbird.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeaderParamTest {

    @Test
    void testEnumConstantsHaveCorrectNames() {
        assertEquals("REQUEST_ID", HeaderParam.REQUEST_ID.getParamName());
        assertEquals("REQUEST_ID", HeaderParam.REQUEST_ID.name());

        assertEquals("x-consumer-id", HeaderParam.X_Consumer_ID.getName());
        assertEquals("X_Consumer_ID", HeaderParam.X_Consumer_ID.getParamName());

        assertEquals("x-session-id", HeaderParam.X_Session_ID.getName());
        assertEquals("x-device-id", HeaderParam.X_Device_ID.getName());
        assertEquals("x-authenticated-userid", HeaderParam.X_Authenticated_Userid.getName());
        assertEquals("ts", HeaderParam.ts.getName());
        assertEquals("content-type", HeaderParam.Content_Type.getName());
        assertEquals("x-authenticated-user-token", HeaderParam.X_Authenticated_User_Token.getName());
        assertEquals("x-authenticated-client-token", HeaderParam.X_Authenticated_Client_Token.getName());
        assertEquals("x-authenticated-client-id", HeaderParam.X_Authenticated_Client_Id.getName());
        assertEquals("x-app-id", HeaderParam.X_APP_ID.getName());
        assertEquals("x-channel-id", HeaderParam.CHANNEL_ID.getName());
        assertEquals("x-response-length", HeaderParam.X_Response_Length.getName());
    }

    @Test
    void testSetNameUpdatesValue() {
        HeaderParam param = HeaderParam.X_APP_ID;
        param.setName("updated-app-id");
        assertEquals("updated-app-id", param.getName());
    }

    @Test
    void testEnumConstantsWithoutCustomName() {
        assertNull(HeaderParam.REQUEST_ID.getName());
        assertNull(HeaderParam.REQUEST_PATH.getName());
        assertNull(HeaderParam.CURRENT_INVOCATION_PATH.getName());
        assertNull(HeaderParam.USER_ID.getName());
    }
}
