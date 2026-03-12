package com.viaoa.repl.client;

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.comm.multiplexer.OAMultiplexerClient;
import com.viaoa.remote.multiplexer.OARemoteMultiplexerClient;
import com.viaoa.repl.OAReplicationMaster;
import com.viaoa.repl.remote.RemoteReplInterface;
import com.viaoa.repl.remote.RemoteServerInterface;
import com.viaoa.sync.model.ClientInfo;
import com.viaoa.sync.remote.RemoteSyncImpl;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.util.OADateTime;

public abstract class OAReplClientConnection {
    private static Logger LOG = Logger.getLogger(OAReplClientConnection.class.getName());

	private String masterHostName;
	private int masterHostPort;
	
	private volatile boolean bIsStarted;
	private volatile boolean bIsConnected;
	private volatile boolean bIsStopped;
	

	private OAMultiplexerClient multiplexerClient;
	private OARemoteMultiplexerClient remoteMultiplexerClient;
	private ClientInfo clientInfo;
	
	private RemoteServerInterface remoteReplServer;
	private RemoteReplInterface remoteReplInterface;
	private RemoteReplInterface remoteReplImpl;

	
    public OAReplClientConnection(String masterHostName, int masterHostPort) {
    	this.masterHostName = masterHostName;
    	this.masterHostPort = masterHostPort;
    }
		
    public boolean isConnected() {
    	return bIsConnected;
    }
    public boolean isStarted() {
    	return bIsStarted;
    }
    public boolean isStopped() {
    	return bIsStopped;
    }
    
    
    public void start() throws Exception {
    	if (bIsStarted) throw new Exception("already called, cant start again");
    	bIsStarted = true;
    	if (bIsStopped) throw new Exception("already stopped, cant start again");
    	if (bIsConnected) throw new Exception("already connected, cant start again");
        LOG.config("starting");

        getClientInfo();
        getMultiplexerClient().setKeepAlive(115);

        LOG.fine("starting multiplexer client");
        getMultiplexerClient().start(); // this will connect to server using multiplexer

        LOG.fine("multiplexer client connected to replication master, connectionId=" + getMultiplexerClient().getConnectionId());

        clientInfo.setConnectionId(getMultiplexerClient().getConnectionId());

        getRemoteRepl();

        clientInfo.setStarted(true);
        LOG.config("startup completed successful");
        bIsConnected = true;
    }
    
    
	public OARemoteMultiplexerClient getRemoteMultiplexerClient() {
		if (remoteMultiplexerClient == null) {
			remoteMultiplexerClient = new OARemoteMultiplexerClient(getMultiplexerClient());
		}
		return remoteMultiplexerClient;
	}
    
	protected OAMultiplexerClient getMultiplexerClient() {
		if (multiplexerClient != null) return multiplexerClient;

		multiplexerClient = new OAMultiplexerClient(getClientInfo().getServerHostName(), clientInfo.getServerHostPort()) {
			@Override
			protected void onSocketException(Exception e) {
				OAReplClientConnection.this.onSocketException(e);
			}

			@Override
			protected void onClose(boolean bError) {
				OAReplClientConnection.this.onSocketClose(bError);
			}
		};
		return multiplexerClient;
	}
    

	public void stop() throws Exception {
		if (bIsStopped || !bIsConnected) return;
		bIsStopped = true;
		bIsConnected = false;

		getClientInfo().setStarted(false);
		getMultiplexerClient().close();
	}
	
    
	public ClientInfo getClientInfo() {
		if (clientInfo == null) {
			clientInfo = new ClientInfo();
			clientInfo.setCreated(new OADateTime());
			clientInfo.setServerHostName(this.masterHostName);
			clientInfo.setServerHostPort(this.masterHostPort);

			try {
				InetAddress localHost = InetAddress.getLocalHost();
				clientInfo.setHostName(localHost.getHostName());
				clientInfo.setIpAddress(localHost.getHostAddress());
			} catch (Exception e) {
			}

		}
		return clientInfo;
	}

	public RemoteServerInterface getRemoteReplServer() throws Exception {
		if (remoteReplServer == null) {
			remoteReplServer = (RemoteServerInterface) getRemoteMultiplexerClient().lookup(OAReplicationMaster.ReplicationServerLookupName);
		}
		return remoteReplServer;
	}
	
	public RemoteReplInterface getRemoteRepl() throws Exception {
		if (remoteReplInterface == null) {
			remoteReplInterface = getRemoteReplServer().registerClient(getRemoteReplImpl());
		}
		return remoteReplInterface;
	}
    
    public RemoteReplInterface getRemoteReplImpl() throws Exception {
        if (remoteReplImpl == null) {
            remoteReplImpl = new RemoteReplInterface() {
				@Override
				public long processMessage(long myPositionId, long yourLastPositionId, String methodName, Object[] args) {
					return OAReplClientConnection.this.processMessage(myPositionId, yourLastPositionId, methodName, args);
				}
			};
        }
        return remoteReplImpl;
    }
	
	protected abstract void onSocketException(Exception e);
	protected abstract void onSocketClose(boolean bError);
	
	public abstract long processMessage(long posFrom, long posTo, String methodName, Object[] args);
	
}
