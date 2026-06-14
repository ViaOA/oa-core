package com.viaoa.comm.ssl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.util.logging.*;


/**
 * Sample app that creates a server and client threads that read/write data using ssl
 * @author vvia
 *
 */
public class SSLTestOrig {
    private static Logger LOG = Logger.getLogger(SSLTestOrig.class.getName());
    
    OASslServer server;
    OASslClient client;
    volatile boolean bStop;
    volatile int cntServer, cntClient;

    @BeforeEach
    public void setup() throws Exception {
        server = new OASslServer("localhost", 1101) {
            @Override
            protected void sendOutput(byte[] bs, int offset, int len, boolean bHandshakeOnly) throws Exception {
                client.input(bs, len, bHandshakeOnly);
            }
        };
        server.initialize();
        
        client = new OASslClient("localhost", 1101) {
            @Override
            protected void sendOutput(byte[] bs, int offset, int len, boolean bHandshakeOnly) throws Exception {
                server.input(bs, len, bHandshakeOnly);
            }
        };
        client.initialize();
        test();
    }

    @AfterEach
    public void tearDown() throws Exception {
        System.out.println("Stopping SSLTest");
        bStop = true;
    }
    
    
    public void test() throws Exception {
        System.out.println("Starting SSLTest");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runServer();
                }
                catch (Exception e) {
                    System.out.println("runServer exception "+e);
                    e.printStackTrace();
                }
            }
        });
        t.start();

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runClient();
                }
                catch (Exception e) {
                    System.out.println("runServer exception "+e);
                    e.printStackTrace();
                }
            }
        });
        t2.start();
    }
    private void runServer() throws Exception {
        for (; !bStop; cntServer++) {
            byte[] bs = ("server."+cntServer).getBytes();
            System.out.println(cntServer+") server");            
            //System.out.println("server: sending output");            
            server.output(bs, 0, bs.length);
            //System.out.println("server: getting input");            
            byte[] bs2 = server.input();
            new String(bs2);
            
            if (cntServer % 500 == 0) server.resetSSL();  // test that causes ssl to re-handshake
        }
    }
        
    private void runClient() throws Exception {
        for (; !bStop; cntClient++) {
            System.out.println(cntClient+") client");            
            //System.out.println("client: getting input");            
            byte[] bs2 = client.input();
            String s = new String(bs2);
            

            //System.out.println("client: sending output");            
            byte[] bs = ("client."+cntClient).getBytes();
            client.output(bs, 0, bs.length);
            
            if (cntClient % 319 == 0) server.resetSSL();
        }
    }
    
    // @Test
    public void monitor() throws Exception {
        Thread.sleep(1000 * 10);
        bStop = false;
        assertTrue(cntClient > 10);
        assertTrue(cntServer > 10);
    }

    
    
    
    public static void main(String[] args) throws Exception {
        SSLTestOrig test = new SSLTestOrig();
        test.setup();
        test.test();
        for (;;) Thread.sleep(10 * 1000);
    }
}
