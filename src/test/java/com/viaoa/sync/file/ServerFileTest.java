package com.viaoa.sync.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerFileTest {
    @TempDir
    Path tempDir;

    @Test
    void constantsExposeExpectedMultiplexerSocketNames() {
        assertEquals("fileUpload", ServerFile.FileUpload);
        assertEquals("fileDownload", ServerFile.FileDownload);
    }

    @Test
    void stopBeforeStartIsSafe() {
        ServerFile sf = new ServerFile(tempDir.toString());

        assertDoesNotThrow(sf::stop);
    }

    @Test
    void uploadFileReceivesLengthPrefixedContent() throws Exception {
        ServerFile sf = new ServerFile(tempDir.toString());
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try (ServerSocket ss = new ServerSocket(0)) {
            Future<?> future = exec.submit(() -> {
                try (Socket socket = ss.accept()) {
                    sf.uploadFile(socket);
                }
                return null;
            });

            try (Socket socket = new Socket("127.0.0.1", ss.getLocalPort())) {
                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                byte[] bytes = "sync upload".getBytes("UTF-8");

                dos.writeUTF("nested/upload.txt");
                dos.flush();
                assertEquals(1, dis.readInt());
                dos.writeInt(bytes.length);
                dos.write(bytes);
                dos.writeInt(0);
                dos.flush();
                assertEquals(0, dis.readInt());
            }
            future.get(2, TimeUnit.SECONDS);
        }
        finally {
            exec.shutdownNow();
        }

        assertEquals("sync upload", Files.readString(tempDir.resolve("nested/upload.txt")));
    }

    @Test
    void downloadFileSendsLengthPrefixedContent() throws Exception {
        Files.writeString(tempDir.resolve("download.txt"), "sync download");
        ServerFile sf = new ServerFile(tempDir.toString());
        ExecutorService exec = Executors.newSingleThreadExecutor();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ServerSocket ss = new ServerSocket(0)) {
            Future<?> future = exec.submit(() -> {
                try (Socket socket = ss.accept()) {
                    sf.downloadFile(socket);
                }
                return null;
            });

            try (Socket socket = new Socket("127.0.0.1", ss.getLocalPort())) {
                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                dos.writeUTF("download.txt");
                dos.flush();
                assertEquals(2, dis.readInt());
                for (;;) {
                    int len = dis.readInt();
                    if (len <= 0) break;
                    byte[] bytes = dis.readNBytes(len);
                    baos.write(bytes);
                }
                dos.writeInt(0);
                dos.flush();
            }
            future.get(2, TimeUnit.SECONDS);
        }
        finally {
            exec.shutdownNow();
        }

        assertEquals("sync download", baos.toString("UTF-8"));
    }

    @Test
    void downloadFileReturnsNotFoundStatusForMissingFile() throws Exception {
        ServerFile sf = new ServerFile(tempDir.toString());
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try (ServerSocket ss = new ServerSocket(0)) {
            Future<?> future = exec.submit(() -> {
                try (Socket socket = ss.accept()) {
                    sf.downloadFile(socket);
                }
                return null;
            });

            try (Socket socket = new Socket("127.0.0.1", ss.getLocalPort())) {
                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                dos.writeUTF("missing.txt");
                dos.flush();
                assertEquals(1, dis.readInt());
                dos.writeInt(0);
                dos.flush();
            }
            future.get(2, TimeUnit.SECONDS);
        }
        finally {
            exec.shutdownNow();
        }
    }
}
