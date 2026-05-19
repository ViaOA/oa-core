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


/*qqqqqqqqqqqqqqqqqqqqq
CODEX

5. file/class/method
     src/main/java/com/viaoa/replication/client/OAReplClientConnection.java:53 start

  concrete bug
  bIsStarted is set before connection/proxy setup succeeds, and partial startup failure does not clean up opened
  resources.

  runtime scenario
  getMultiplexerClient().start() succeeds, then getRemoteMaster() or remote setup fails. The object remains bIsStarted
  = true, bIsConnected = false; stop() returns early because !bIsConnected, so partial resources can remain open and
  the instance cannot be safely restarted.

  why this violates OA/OG replication semantics
  Reconnect/retry after partial connection failure can leak transport state or leave stale replication connection
  state.

  minimal fix direction
  Set started/connected only after successful setup, and wrap startup in try/catch that closes any partially opened
  multiplexer/socket resources.

  suggested CODEX comment location
  At the top of OAReplClientConnection.start() before bIsStarted = true.

6. file/class/method
     src/main/java/com/viaoa/replication/client/OAReplClientConnection.java:104 stop

  concrete bug
  If remoteMaster.setEnabled(false) throws, getMultiplexerClient().close() is skipped.

  runtime scenario
  Connection is broken while stopping. The remote disable call throws. Because close is after that call and there is
  no finally, socket/multiplexer cleanup does not run.

  why this violates OA/OG replication semantics
  Replication disconnect cleanup can leave resources and stale connection state alive, affecting reconnect and worker
  shutdown behavior.

  minimal fix direction
  Always close the multiplexer in a finally; treat remote disable as best-effort cleanup.

  suggested CODEX comment location
  Inside stop() around remoteMaster.setEnabled(false).

  suggested regression test
  testReplClientConnectionStopClosesMultiplexerWhenRemoteDisableFails

*/


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

