/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.sync.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.sync.OASync;
import com.viaoa.util.OAFile;

/**
 * Server-side file transfer service used by {@link com.viaoa.sync.OASyncServer}.
 * <p>
 * {@code ServerFile} allows clients to upload or download files from a single
 * server-controlled directory. It runs two dedicated daemon threads:
 * <ul>
 *   <li>an upload listener (for files sent from clients),</li>
 *   <li>a download listener (for files requested by clients).</li>
 * </ul>
 * Each accepted socket is processed on its own worker thread.
 *
 * <h2>Directory Safety</h2>
 * All filenames are normalized using {@link com.viaoa.util.OAFile} and checked
 * for disallowed patterns (such as {@code ".."}) to prevent directory traversal
 * or access to files outside the configured server directory.
 *
 * <h2>Download Handling</h2>
 * When a client requests a file:
 * <ul>
 *   <li>the filename is validated and normalized,</li>
 *   <li>a status code is returned,</li>
 *   <li>binary data is streamed in length-prefixed blocks.</li>
 * </ul>
 *
 * <h2>Upload Handling</h2>
 * During upload:
 * <ul>
 *   <li>the server validates and normalizes the filename,</li>
 *   <li>ensures the directory structure exists,</li>
 *   <li>creates or overwrites the destination file,</li>
 *   <li>streams blocks of binary data from the client.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * {@link #start()} launches both upload and download listener threads.
 * {@link #stop()} closes the associated server sockets.
 *
 * <p>
 * This class provides a lightweight, efficient file-transfer protocol that
 * operates independently of remote-method queue ordering.
 */
public class ServerFile {
    private static Logger LOG = Logger.getLogger(ServerFile.class.getName());
    
    /**
     * Multiplexer socket name used for file upload operations.
     */
    public static final String FileUpload = "fileUpload";
    
    /**
     * Multiplexer socket name used for file download operations.
     */
    public static final String FileDownload = "fileDownload";
    
    /**
     * Base directory on the server used for all file upload and download operations.
     */
    private final String directory;
    
    /**
     * Server socket used to accept file upload connections.
     */
    private ServerSocket ssUpload;
    
    /**
     * Server socket used to accept file download connections.
     */
    private ServerSocket ssDownload;
    
    /**
     * Creates a new server-side file transfer service.
     *
     * @param directory the base directory used to store and retrieve files
     */
    public ServerFile(String directory) {
        this.directory = directory;
    }

    /**
     * Flag used to control startup and shutdown of the file transfer service.
     */
    private final AtomicBoolean abStart = new AtomicBoolean();
    
    /**
     * Starts the server-side file transfer service.
     * <p>
     * Launches dedicated daemon threads for upload and download server sockets.
     * </p>
     */
    public void start() {
        if (!abStart.compareAndSet(false, true)) return;
        LOG.fine("Starting");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startUploadServerSocket();
                }
                catch (Exception e) {
                    LOG.log(Level.WARNING, "exception in ServerFile.uploadServerSocket", e);
                }
                
            }
        }, "UploadServerSocket");
        t.setDaemon(true);
        t.start();
        
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startDownloadServerSocket();
                }
                catch (Exception e) {
                    LOG.log(Level.WARNING, "exception in ServerFile.downloadServerSocket", e);
                }
                
            }
        }, "DownloadServerSocket");
        t.setDaemon(true);
        t.start();
        LOG.fine("start completed, started 2 threads");
    }
    
    /**
     * Stops the server-side file transfer service.
     * <p>
     * Closes server sockets and prevents further connections.
     * </p>
     */
    public void stop() {
        if (!abStart.compareAndSet(true, false)) return;
        try {
            ssUpload.close();
            ssDownload.close();
        }
        catch (Exception e) {
        }
        ssUpload = null;
        ssDownload = null;
    }
    
    
    /**
     * Starts the download server socket loop.
     * <p>
     * Accepts incoming download connections and processes each on a worker thread.
     * </p>
     *
     * @throws Exception if the server socket cannot be created
     */
    protected void startDownloadServerSocket() throws Exception {
        LOG.fine("Starting");
        ssDownload = OASync.getSyncServer().getMultiplexerServer().createServerSocket(FileDownload);
        for ( ; abStart.get(); ) {
            final Socket socket = ssDownload.accept();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        downloadFile(socket);
                    }
                    catch (Exception e) {
                        LOG.log(Level.WARNING, "ServerFile.fileDownload exception", e);
                    }
                }
            }, "DownloadFileSocket").start();
        }
    }

    /**
     * Sends a server-side file to a connected client.
     * <p>
     * Validates the requested filename, writes a status code, and streams
     * the file contents to the client using length-prefixed blocks.
     * </p>
     *
     * @param socket the client socket requesting the file
     * @throws Exception if an I/O or protocol error occurs
     */
    public void downloadFile(Socket socket) throws Exception {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        
        String fn = dis.readUTF();
        LOG.fine("requesting download, file="+fn);
        fn = directory + "/" + fn;
        fn = OAFile.convertFileName(fn);
        File file = new File(fn);

        if (fn.indexOf("..") >= 0) {
            LOG.fine("requesting download, illegal directory, file="+fn);
            dos.writeInt(0);
        }
        else if (!file.exists()) {
            LOG.fine("requesting download, file does not exist, file="+fn);
            dos.writeInt(1);
        }
        else {
            dos.writeInt(2);  // valid
            dos.flush();
            LOG.fine("requesting download starting, file="+fn);
            
            final BufferedInputStream bisFile = new BufferedInputStream(new FileInputStream(file));
            final byte[] bs = new byte[8196];
     
            int total = 0;
            for (int i=0; ;i++) {
                int x = bisFile.read(bs, 0, bs.length);
                dos.writeInt(Math.max(x, 0));
                if (x <= 0) break;
                dos.write(bs, 0, x);
                total += x;
            }
            bisFile.close();
            LOG.fine("requesting download completed, file="+fn+", size="+total);
        }
        dos.flush();
        dis.readInt();

        dis.close();
        dos.close();
        socket.close();
    }
    

    /**
     * Starts the upload server socket loop.
     * <p>
     * Accepts incoming upload connections and processes each on a worker thread.
     * </p>
     *
     * @throws Exception if the server socket cannot be created
     */
    protected void startUploadServerSocket() throws Exception {
        LOG.fine("Starting");
        ssUpload = OASync.getSyncServer().getMultiplexerServer().createServerSocket(FileUpload);
        for ( ; abStart.get(); ) {
            final Socket socket = ssUpload.accept();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        uploadFile(socket);
                    }
                    catch (Exception e) {
                        // TODO: handle exception
                        LOG.log(Level.WARNING, "ServerFile.fileUpload exception", e);
                    }
                }
            }, "UploadFileSocket").start();
        }
    }

    /**
     * Receives a file from a client and saves it to the server directory.
     * <p>
     * Validates the filename, ensures directories exist, and streams
     * the uploaded file contents from the client.
     * </p>
     *
     * @param socket the client socket uploading the file
     * @throws Exception if an I/O or protocol error occurs
     */
    public void uploadFile(Socket socket) throws Exception {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        String fn = dis.readUTF();
        LOG.fine("requesting upload, file="+fn);
        fn = OAFile.convertFileName(directory + "/" + fn);
        LOG.fine("requesting upload, filePath="+fn);

        if (fn.indexOf("..") >= 0) {
            LOG.fine("requesting upload, illegal directory, file="+fn);
            dos.writeInt(0);
        }
        else { 
            OAFile.mkdirsForFile(fn);
            File fileSaveAs = new File(fn);
            LOG.fine("requesting download starting, file="+fn);
            dos.writeInt(1);
            dos.flush();
            
            if (fileSaveAs.exists()) fileSaveAs.delete();
            fileSaveAs.createNewFile();
    
            FileOutputStream osFile = new FileOutputStream(fileSaveAs);
            final byte[] bs = new byte[8196];
            
            int total = 0;
            for (int i=0;;i++) {
                final int xAmt = dis.readInt();
                if (xAmt <= 0) break;
                
                int tot = 0;
                for ( ;tot < xAmt; ) {
                    int x = Math.min(bs.length, xAmt-tot);
                    x = dis.read(bs, 0, x);
                    osFile.write(bs, 0, x);
                    tot += x;
                }
                total += xAmt;
            }
            osFile.close();
            LOG.fine("requesting download completed, file="+fn+", size="+total);
        }
        dos.flush();
        dos.writeInt(0);
        dos.flush();
        
        dis.close();
        dos.close();
        socket.close();
    }
}
