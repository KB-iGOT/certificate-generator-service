package org.sunbird.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeaderParamTest {

    @Test
    void getParamNameReturnsEnumName() {
        assertEquals("REQUEST_ID", HeaderParam.REQUEST_ID.getParamName());
        assertEquals("X_Consumer_ID", HeaderParam.X_Consumer_ID.getParamName());
    }

    @Test
    void getNameReturnsNullForNoArgConstructorEnums() {
        assertNull(HeaderParam.REQUEST_ID.getName());
        assertNull(HeaderParam.USER_ID.getName());
    }

    @Test
    void getNameReturnsCustomNameForOneArgConstructorEnums() {
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
    void setNameUpdatesNameField() {
        HeaderParam param = HeaderParam.X_Consumer_ID;
        param.setName("new-name");
        assertEquals("new-name", param.getName());
    }
}