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
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;

import com.viaoa.comm.multiplexer.OAMultiplexerClient;
import com.viaoa.lang.OAString;

/**
 * Client-side file transfer helper used by {@link com.viaoa.sync.OASyncClient}.
 * <p>
 * {@code ClientFile} communicates with {@link ServerFile} using a dedicated
 * multiplexer socket for high-throughput binary transfer. It supports:
 * <ul>
 *   <li>downloading a file from the server into a local {@link File},</li>
 *   <li>uploading a local file to a server directory,</li>
 *   <li>length-prefixed block streaming to avoid partial read/write issues,</li>
 *   <li>progress reporting through {@link #status(int)}.</li>
 * </ul>
 *
 * <h2>Download Protocol</h2>
 * <ol>
 *   <li>Client sends the requested filename.</li>
 *   <li>Server returns a status code:
 *       <ul>
 *         <li>0 – illegal directory,</li>
 *         <li>1 – file not found,</li>
 *         <li>2 – OK; data follows.</li>
 *       </ul>
 *   </li>
 *   <li>If valid, client reads repeated:
 *       <pre>{@code length → bytes}</pre>
 *   </li>
 *   <li>Client writes bytes into the destination file.</li>
 * </ol>
 *
 * <h2>Upload Protocol</h2>
 * <ol>
 *   <li>Client sends the target filename.</li>
 *   <li>Server replies with 1 (valid) or 0 (invalid).</li>
 *   <li>If valid, client streams:
 *       <pre>{@code length → bytes}</pre>
 *       until EOF.</li>
 * </ol>
 *
 * <p>
 * {@link #status(int)} may be overridden to provide progress UI or logging.
 */
public class ClientFile {
    
    private static Logger LOG = Logger.getLogger(ClientFile.class.getName());
 
    /**
     * Downloads a file from the server and saves it to a local file.
     * <p>
     * Opens a dedicated multiplexer socket, requests the named file,
     * receives length-prefixed data blocks, and writes them to the
     * destination file while reporting progress.
     * </p>
     *
     * @param fname the name of the file to download from the server
     * @param fileSaveAs the local file to save the downloaded contents to
     * @return {@code true} if the file was successfully downloaded, otherwise {@code false}
     * @throws Exception if an I/O or protocol error occurs
     */
    public boolean download(String fname, File fileSaveAs, final OAMultiplexerClient mc) throws Exception {
        LOG.fine("download fname="+fname+", save as file="+fileSaveAs);
        if (OAString.isEmpty(fname) || fileSaveAs == null) return false;
        final Socket socket = mc.createSocket(ServerFile.FileDownload);
        
        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        dos.writeUTF(fname);
        dos.flush();
        
        if (fileSaveAs.exists()) fileSaveAs.delete();
        fileSaveAs.createNewFile();
        
        int x = dis.readInt();
        final boolean bValid = (x == 2);
        LOG.fine("download fname="+fname+", save as file="+fileSaveAs+", return value="+2+", valid="+bValid);

        if (bValid) {
            OutputStream osFile = new FileOutputStream(fileSaveAs);
            
            final byte[] bs = new byte[8196];
            int total = 0;
            for ( ; ; ) {
                final int xAmt = dis.readInt();
                if (xAmt <= 0) break;

                int tot = 0;
                for ( ;tot < xAmt; ) {
                    x = Math.min(bs.length, xAmt-tot);
                    x = dis.read(bs, 0, x);
                    osFile.write(bs, 0, x);
                    tot += x;
                }
                total += xAmt;
                status(total);
            }
            osFile.close();
            LOG.fine("download finished, fname="+fname+", save as file="+fileSaveAs+", size="+total);
        }
        dos.writeInt(0); // done
        dos.flush();
        
        dos.close();
        dis.close();
        socket.close();
        return bValid;
    }
    
    
    /**
     * Uploads a local file to the server.
     * <p>
     * Opens a dedicated multiplexer socket, sends the target filename,
     * and streams the local file to the server using length-prefixed
     * data blocks while reporting progress.
     * </p>
     *
     * @param fname the target filename on the server
     * @param fileOpen the local file to upload
     * @return {@code true} if the file was successfully uploaded, otherwise {@code false}
     * @throws IOException if an I/O error occurs
     */
    public boolean upload(String fname, File fileOpen, final OAMultiplexerClient mc) throws IOException {
        LOG.fine("upload to fname="+fname+", from file="+fileOpen);
        if (OAString.isEmpty(fname) || fileOpen == null) return false;
        Socket socket = mc.createSocket(ServerFile.FileUpload);

        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        
        dos.writeUTF(fname);
        dos.flush();

        final boolean bValid = (dis.readInt() == 1);
        LOG.fine("upload to fname="+fname+", from file="+fileOpen+", valid="+bValid);
        
        if (bValid) {
            final BufferedInputStream bisFile = new BufferedInputStream(new FileInputStream(fileOpen));
            final byte[] bs = new byte[8196];
            
            int total = 0;
            for (int i=0;;i++) {
                int x = bisFile.read(bs, 0, bs.length);
                dos.writeInt(Math.max(x, 0));
                if (x <= 0) break;
                dos.write(bs, 0, x);
                total += x;
                status(total);
            }
            bisFile.close();
            LOG.fine("upload finished, to fname="+fname+", from file="+fileOpen+", size="+total);
            dos.flush();
        }
        // wait for server to finsih reading
        dis.readInt();
        
        dos.close();
        dis.close();
        socket.close();
        return bValid;
    }
    
    /**
     * Reports progress during upload or download operations.
     * <p>
     * This method is called with the cumulative number of bytes
     * transferred and may be overridden to provide progress feedback.
     * </p>
     *
     * @param x the total number of bytes transferred so far
     */
    protected void status(int x) {
    }
}
