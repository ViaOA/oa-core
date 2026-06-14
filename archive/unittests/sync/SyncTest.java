package com.viaoa.sync;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.net.Socket;

import com.viaoa.OAUnitTest;
import com.viaoa.runtime.OARuntime;

import test.xice.tsac.model.oa.Server;


/**
 * NOT Done:
 * 
 *       might not be able to run syncServer and syncClient together
 * 
 */
public class SyncTest extends OAUnitTest {
    
    private OASyncServer syncServer;
    private OASyncClient syncClient;
    private Server serverTest;
    public final int port = 1099;
    
    //@Before
    public void setup() throws Exception {
        // setup server
        
        syncServer = new OASyncServer(port);
        syncServer.start();
        
        syncClient = new OASyncClient("localhost", port) {

			@Override
			protected void createRemoteDataSource() {
				// TODO Auto-generated method stub
				
			}

			@Override
			protected void closeRemoteDataSource() {
				// TODO Auto-generated method stub
				
			}
        	
        };
        
        syncClient.start();
//        ServerRoot serverRoot = (ServerRoot) syncClient.getRemoteServer().getObject(ServerRoot.class, new OAObjectKey(777));
        

        // create sample object on server
        try {
            OARuntime.thread().getThreadLocalService().setLoading(true);
            serverTest = new Server();
            serverTest.setId(1);
        }
        finally {
            OARuntime.thread().getThreadLocalService().setLoading(false);
        }
        serverTest.setName("test");
        
        int xx = 4;
        xx++;
    }

    //@After
    public void tearDown() throws Exception {
        syncClient.stop();
        syncServer.stop();
    }
    
    //@Test
    //(timeout=5000)
    public void test() throws Exception {
        
        
    }
}
