package com.viaoa.replication.client;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

import com.viaoa.remote.multiplexer.annotation.*;
import com.viaoa.replication.remote.*;
import com.viaoa.sync.model.ClientInfo;

class OAReplClientConnectionTest {

    @Test
    void constructorInitializesDisconnectedState() {
        TestConnection con = new TestConnection("guid-1", "127.0.0.1", 1234, 5L, 7L);

        assertFalse(con.isConnected());
        assertFalse(con.isStarted());
        assertFalse(con.isStopped());
    }

    @Test
    void startAgainstUnavailableHostMarksStartedButNotConnectedCurrentBehavior() {
        TestConnection con = new TestConnection("guid-1", "127.0.0.1", 1, 5L, 7L);

        assertThrows(Exception.class, con::start);

        assertTrue(con.isStarted());
        assertFalse(con.isConnected());
        assertFalse(con.isStopped());
    }

    @Test
    void getRemoteMultiplexerClientIsLazyAndStable() {
        TestConnection con = new TestConnection("guid-1", "127.0.0.1", 1234, 5L, 7L);

        assertSame(con.getRemoteMultiplexerClient(), con.getRemoteMultiplexerClient());
    }

    @Test
    void stopBeforeConnectedIsSafeAndLeavesStateUnchanged() throws Exception {
        TestConnection con = new TestConnection("guid-1", "127.0.0.1", 1234, 5L, 7L);

        con.stop();

        assertFalse(con.isStopped());
        assertFalse(con.isConnected());
    }

    @Test
    void getClientInfoInitializesConnectionMetadata() {
        TestConnection con = new TestConnection("guid-1", "localhost", 9876, 5L, 7L);

        ClientInfo info = con.getClientInfo();

        assertNotNull(info.getCreated());
        assertEquals("localhost", info.getServerHostName());
        assertEquals(9876, info.getServerHostPort());
        assertSame(info, con.getClientInfo());
    }

    @Test
    void getRemoteMasterUsesRegisterAndInitialSequences() throws Exception {
        TestConnection con = new TestConnection("guid-1", "localhost", 9876, 5L, 7L);
        TestRegister register = new TestRegister();
        con.setRemoteMasterRegister(register);

        RemoteMasterInterface master = con.getRemoteMaster();

        assertSame(register.master, master);
        assertEquals("guid-1", register.guid.get());
        assertEquals(5L, register.masterSeq.get());
        assertEquals(7L, register.clientSeq.get());
        assertSame(con.getRemoteClient(), register.remoteClient.get());
        assertSame(master, con.getRemoteMaster());
    }

    @Test
    void getRemoteClientForwardsMessagesToConnectionCallback() throws Exception {
        TestConnection con = new TestConnection("guid-1", "localhost", 9876, 5L, 7L);
        Object[] args = { "value" };

        con.getRemoteClient().processMessage(55L, "refresh", args);

        assertEquals(55L, con.lastMasterSeq.get());
        assertEquals("refresh", con.lastMethod.get());
        assertSame(args, con.lastArgs.get());
    }

    @Test
    void remoteClientInterfaceHasRemoteAnnotations() throws Exception {
        assertNotNull(RemoteClientInterface.class.getAnnotation(OARemoteInterface.class));
        assertNotNull(RemoteClientInterface.class.getMethod("processMessage", long.class, String.class, Object[].class)
                .getAnnotation(OARemoteMethod.class));
    }

    private static class TestConnection extends OAReplClientConnection {
        final AtomicLong lastMasterSeq = new AtomicLong(-1L);
        final AtomicReference<String> lastMethod = new AtomicReference<>();
        final AtomicReference<Object[]> lastArgs = new AtomicReference<>();

        TestConnection(String guid, String masterHostName, int masterHostPort, long masterSeq, long clientSeq) {
            super(guid, masterHostName, masterHostPort, masterSeq, clientSeq);
        }

        void setRemoteMasterRegister(RemoteMasterRegisterInterface register) throws Exception {
            Field f = OAReplClientConnection.class.getDeclaredField("remoteMasterRegister");
            f.setAccessible(true);
            f.set(this, register);
        }

        @Override
        protected void onSocketException(Exception e) {
        }

        @Override
        protected void onSocketClose(boolean bError) {
        }

        @Override
        public void processMessageFromMaster(long masterSeq, String methodName, Object[] args) {
            lastMasterSeq.set(masterSeq);
            lastMethod.set(methodName);
            lastArgs.set(args);
        }
    }

    private static class TestRegister implements RemoteMasterRegisterInterface {
        final TestMaster master = new TestMaster();
        final AtomicReference<String> guid = new AtomicReference<>();
        final AtomicReference<RemoteClientInterface> remoteClient = new AtomicReference<>();
        final AtomicLong masterSeq = new AtomicLong(-1L);
        final AtomicLong clientSeq = new AtomicLong(-1L);

        @Override
        public RemoteMasterInterface registerClient(String guid, RemoteClientInterface remoteClient, long lastSentMasterSeq,
                long lastSentClientSeq) {
            this.guid.set(guid);
            this.remoteClient.set(remoteClient);
            this.masterSeq.set(lastSentMasterSeq);
            this.clientSeq.set(lastSentClientSeq);
            return master;
        }
    }

    private static class TestMaster implements RemoteMasterInterface {
        @Override public void processMessage(long masterSeq, long clientSeq, String methodName, Object[] args) {}
        @Override public long getLastReceivedClientSeq() { return 0; }
        @Override public long getLastProcessedClientSeq() { return 0; }
        @Override public long getLastReceivedMasterSeq() { return 0; }
        @Override public void setLastReceivedMasterSeq(long seq) {}
        @Override public long getLastProcessedMasterSeq() { return 0; }
        @Override public void setEnabled(boolean b) {}
        @Override public boolean getEnabled() { return true; }
    }
}
