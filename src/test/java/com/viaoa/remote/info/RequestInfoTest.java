package com.viaoa.remote.info;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class RequestInfoTest {

    @Test
    void getTypeReturnsMatchingOrdinalOrNull() {
        for (RequestInfo.Type type : RequestInfo.Type.values()) {
            assertSame(type, RequestInfo.getType(type.ordinal()));
        }

        assertNull(RequestInfo.getType(-1));
        assertNull(RequestInfo.getType(RequestInfo.Type.values().length));
    }

    @Test
    void typeUsesQueueMatchesDeclaredSemantics() {
        assertFalse(RequestInfo.Type.CtoS_GetLookupInfo.usesQueue());
        assertTrue(RequestInfo.Type.CtoS_QueuedRequest.usesQueue());
        assertTrue(RequestInfo.Type.StoC_QueuedBroadcast.usesQueue());
        assertFalse(RequestInfo.Type.StoC_SocketRequest.usesQueue());
    }

    @Test
    void typeHasReturnValueMatchesDeclaredSemantics() {
        assertTrue(RequestInfo.Type.CtoS_GetLookupInfo.hasReturnValue());
        assertFalse(RequestInfo.Type.CtoS_SocketRequestNoResponse.hasReturnValue());
        assertTrue(RequestInfo.Type.StoC_SocketRequest.hasReturnValue());
        assertFalse(RequestInfo.Type.StoC_CloseObjectInputStream.hasReturnValue());
    }

    @Test
    void constructorAssignsIncreasingCount() {
        RequestInfo first = new RequestInfo();
        RequestInfo second = new RequestInfo();

        assertTrue(second.cnt > first.cnt);
    }

    @Test
    void toLogStringIncludesConfiguredRequestFieldsAndArguments() throws Exception {
        RequestInfo info = new RequestInfo();
        Method method = Remote.class.getMethod("call", String.class, int.class, Boolean.class, Class.class);
        info.msStart = 0;
        info.nsStart = 1_000_000L;
        info.nsEnd = 3_500_000L;
        info.connectionId = 7;
        info.bindName = "bind";
        info.type = RequestInfo.Type.CtoS_SocketRequest;
        info.method = method;
        info.args = new Object[] { "abcdefghijklmnopqrstuvwxyz0123456789", 42, Boolean.TRUE, String.class };
        info.exceptionMessage = "problem";

        String log = info.toLogString();

        assertTrue(log.contains("|2.5|7|bind|CtoS_SocketRequest|Remote|call|problem|"));
        assertTrue(log.contains("[0]=abcdefghijklmnopqrstuvwxyz01.."));
        assertTrue(log.contains("[1]=42"));
        assertTrue(log.contains("[2]=true"));
        assertTrue(log.contains("[3]=String"));
    }

    @Test
    void toLogStringCanUseMethodInfoWhenMethodIsNull() throws Exception {
        Method method = Remote.class.getMethod("noArgs");
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.method = method;
        RequestInfo info = new RequestInfo();
        info.msStart = 0;
        info.connectionId = 3;
        info.bindName = "bind";
        info.type = RequestInfo.Type.StoC_SocketRequest;
        info.methodInfo = methodInfo;

        String log = info.toLogString();

        assertTrue(log.contains("|3|bind|StoC_SocketRequest|Remote|noArgs|"));
        assertSame(method, info.method);
    }

    @Test
    void getLogHeaderMatchesToLogStringColumnOrder() {
        assertEquals("Date|Time|ms|ConnectionId|BindName|Type|Object|Method|exception|arguments", RequestInfo.getLogHeader());
    }

    private interface Remote {
        void noArgs();

        void call(String value, int number, Boolean flag, Class<?> type);
    }
}
