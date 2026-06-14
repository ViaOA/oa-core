package com.viaoa.comm.multiplexer.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class MultiplexerOutputStreamControllerTest {

    @Test
    void setThrottleLimitRoundTripsAndCanBeCleared() {
        MultiplexerOutputStreamController controller = new MultiplexerOutputStreamController();

        controller.setThrottleLimit(4);
        assertEquals(4, controller.getThrottleLimit());

        controller.setThrottleLimit(0);
        assertEquals(0, controller.getThrottleLimit());
    }

    @Test
    void setDataOutputStreamAllowsWrites() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        MultiplexerOutputStreamController controller = new MultiplexerOutputStreamController();
        controller.setDataOutputStream(new DataOutputStream(bout));
        TestVirtualSocket socket = new TestVirtualSocket(1, 7, "service");

        controller.write(socket, new byte[] { 1, 2, 3, 4 }, 1, 2);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bout.toByteArray()));
        assertEquals(7, in.readInt());
        assertEquals(2, in.readInt());
        assertEquals(2, in.readUnsignedByte());
        assertEquals(3, in.readUnsignedByte());
        assertEquals(1, controller.getWriteCount());
        assertEquals(2, controller.getWriteSize());
    }

    @Test
    void closeCausesFutureCommandWritesToFail() throws Exception {
        MultiplexerOutputStreamController controller = new MultiplexerOutputStreamController();
        controller.setDataOutputStream(new DataOutputStream(new ByteArrayOutputStream()));

        controller.close();

        assertThrows(IOException.class, controller::sendPingCommand);
    }

    @Test
    void getWriteCountAndSizeStartAtZero() {
        MultiplexerOutputStreamController controller = new MultiplexerOutputStreamController();

        assertEquals(0, controller.getWriteCount());
        assertEquals(0, controller.getWriteSize());
    }

    @Test
    void writeBreaksLargePayloadIntoFramesButCountsSingleLogicalWrite() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        MultiplexerOutputStreamController controller = new MultiplexerOutputStreamController();
        controller.setDataOutputStream(new DataOutputStream(bout));
        TestVirtualSocket socket = new TestVirtualSocket(1, 7, "service");
        byte[] payload = new byte[33000];

        controller.write(socket, payload, 0, payload.length);

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bout.toByteArray()));
        assertEquals(7, in.readInt());
        assertEquals(32768, in.readInt());
        assertEquals(1, controller.getWriteCount());
        assertEquals(payload.length, controller.getWriteSize());
    }

    @Test
    void sendPingCommandWritesCommandFrame() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        MultiplexerOutputStreamController controller = new MultiplexerOutputStreamController();
        controller.setDataOutputStream(new DataOutputStream(bout));

        controller.sendPingCommand();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bout.toByteArray()));
        assertEquals(MultiplexerSocketController.CMD_Command, in.readInt());
        assertEquals(MultiplexerSocketController.CMD_Ping, in.readInt());
        assertEquals(0, in.readInt());
    }

    @Test
    void sendCommandWithNameWritesNamePayload() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        TestController controller = new TestController();
        controller.setDataOutputStream(new DataOutputStream(bout));

        controller.exposeSendCommand(MultiplexerSocketController.CMD_CreateVSocket, 5, "svc");

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bout.toByteArray()));
        assertEquals(MultiplexerSocketController.CMD_Command, in.readInt());
        assertEquals(MultiplexerSocketController.CMD_CreateVSocket, in.readInt());
        assertEquals(5, in.readInt());
        assertEquals(3, in.readInt());
        assertEquals('s', in.readUnsignedByte());
        assertEquals('v', in.readUnsignedByte());
        assertEquals('c', in.readUnsignedByte());
    }

    private static class TestController extends MultiplexerOutputStreamController {
        void exposeSendCommand(int cmd, int param, String name) throws IOException {
            sendCommand(cmd, param, name);
        }
    }

    private static class TestVirtualSocket extends VirtualSocket {
        TestVirtualSocket(int connectionId, int id, String serverSocketName) {
            super(connectionId, id, serverSocketName);
        }

        @Override
        public int read(byte[] bs, int off, int len) { return 0; }

        @Override
        public int read() { return 0; }

        @Override
        public void write(byte[] bs, int off, int len) { }

        @Override
        public void write(int b) { }

        @Override
        public void close(boolean bSendCommand) throws IOException { super.close(); }
    }
}
