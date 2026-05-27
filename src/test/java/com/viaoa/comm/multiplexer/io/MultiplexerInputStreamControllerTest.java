package com.viaoa.comm.multiplexer.io;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.viaoa.OAUnitTest;

public class MultiplexerInputStreamControllerTest extends OAUnitTest {

    private MultiplexerInputStreamController isc;
    
    @Test
    public void test() {
        
        
    }
    
    public MultiplexerInputStreamController createMultiplexerInputStreamController() {
        
        MultiplexerInputStreamController isc = new MultiplexerInputStreamController(1) {
            @Override
            protected VirtualSocket getSocket(int id) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            protected int getMaxSocketId() {
                // TODO Auto-generated method stub
                return 0;
            }
            
            @Override
            protected void createNewSocket(int connectionId, int id, String serverSocketName) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            protected void closeSocket(int id, boolean bSendCommand) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            protected void closeRealSocket() {
                // TODO Auto-generated method stub
                
            }
        };
        return isc;
    }
    
}
