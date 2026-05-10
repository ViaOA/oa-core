package com.viaoa.replication.client;

import java.net.InetAddress;
import java.util.logging.Logger;
import com.viaoa.comm.multiplexer.OAMultiplexerClient;
import com.viaoa.datetime.OADateTime;
import com.viaoa.remote.multiplexer.OARemoteMultiplexerClient;
import com.viaoa.replication.OAReplicationMaster;
import com.viaoa.replication.remote.RemoteClientInterface;
import com.viaoa.replication.remote.RemoteMasterInterface;
import com.viaoa.replication.remote.RemoteMasterRegisterInterface;
import com.viaoa.sync.model.ClientInfo;

public abstract class OAReplClientConnection {
    private static Logger LOG = Logger.getLogger(OAReplClientConnection.class.getName());

    private final String guid;
	private final String masterHostName;
	private final int masterHostPort;
	
	private volatile boolean bIsStarted;
	private volatile boolean bIsConnected;
	private volatile boolean bIsStopped;

	private OAMultiplexerClient multiplexerClient;
	private OARemoteMultiplexerClient remoteMultiplexerClient;
	private ClientInfo clientInfo;
	
	private RemoteClientInterface remoteClient;
	private RemoteMasterRegisterInterface remoteMasterRegister;
	private RemoteMasterInterface remoteMaster;
	
	private final long initMasterSeq, initClientSeq;

    public OAReplClientConnection(String guid, String masterHostName, int masterHostPort, long masterSeq, long clientSeq) {
    	this.guid = guid;
    	this.masterHostName = masterHostName;
    	this.masterHostPort = masterHostPort;
    	this.initMasterSeq = masterSeq;
    	this.initClientSeq = clientSeq;
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
    	LOG.fine(String.format("starting client guid=%s", guid));
    	if (bIsStarted) throw new Exception("already called, cant start again");
    	bIsStarted = true;
    	if (bIsStopped) throw new Exception("already stopped, cant start again");
    	if (bIsConnected) throw new Exception("already connected, cant start again");

        getClientInfo();
        getMultiplexerClient().setKeepAlive(115);

        LOG.fine("starting multiplexer client");
        getMultiplexerClient().start(); // this will connect to server using multiplexer

        LOG.fine("multiplexer client connected to replication master, connectionId=" + getMultiplexerClient().getConnectionId());

        clientInfo.setConnectionId(getMultiplexerClient().getConnectionId());

        getRemoteMaster();
        
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

    	LOG.fine(String.format("creating OAMultiplexerClient, serverHostName=%s, port=%d", getClientInfo().getServerHostName(), clientInfo.getServerHostPort()));
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
		LOG.fine("stopping connection to Master");
		bIsStopped = true;
		bIsConnected = false;

		getClientInfo().setStarted(false);
		remoteMaster.setEnabled(false);
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

	public RemoteMasterRegisterInterface getRemoteMasterRegister() throws Exception {
		if (remoteMasterRegister == null) {
			RemoteMasterRegisterInterface reg = (RemoteMasterRegisterInterface) getRemoteMultiplexerClient().lookup(OAReplicationMaster.ReplicationMasterLookupName);
			remoteMasterRegister = (RemoteMasterRegisterInterface) reg;
		}
		return remoteMasterRegister;
	}
	
	public RemoteMasterInterface getRemoteMaster() throws Exception {
		if (remoteMaster == null) {
			remoteMaster = getRemoteMasterRegister().registerClient(guid, getRemoteClient(), this.initMasterSeq, this.initClientSeq);
		}
		return remoteMaster;
	}

	public RemoteClientInterface getRemoteClient() throws Exception {
        if (remoteClient == null) {
        	remoteClient = new RemoteClientInterface() {
				@Override
				public void processMessage(long masterSeq, String methodName, Object[] args) {
					OAReplClientConnection.this.processMessageFromMaster(masterSeq, methodName, args);
				}
			};
        }
        return remoteClient;
    }
	
	protected abstract void onSocketException(Exception e);
	protected abstract void onSocketClose(boolean bError);
	
	public abstract void processMessageFromMaster(long masterSeq, String methodName, Object[] args);
}

