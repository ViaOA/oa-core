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




/**
 * Client-side connection to a replication master.
 * <p>
 * This class owns the multiplexer connection, remote proxies, client registration handshake, and callbacks used by
 * {@link com.viaoa.replication.OAReplicationClient} to send and receive replication messages.
 * </p>
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

    /**
     * Creates a replication master connection descriptor.
     * @param guid client replication identifier
     * @param masterHostName master host name
     * @param masterHostPort master port
     * @param masterSeq last known master sequence
     * @param clientSeq last known client sequence
     */
    public OAReplClientConnection(String guid, String masterHostName, int masterHostPort, long masterSeq, long clientSeq) {
    	this.guid = guid;
    	this.masterHostName = masterHostName;
    	this.masterHostPort = masterHostPort;
    	this.initMasterSeq = masterSeq;
    	this.initClientSeq = clientSeq;
    }

    /**
     * Indicates whether this connection is currently connected.
     * @return {@code true} when connected
     */
    public boolean isConnected() {
    	return bIsConnected;
    }
    /**
     * Indicates whether this connection has been started.
     * @return {@code true} when started
     */
    public boolean isStarted() {
    	return bIsStarted;
    }
    /**
     * Indicates whether this connection has been stopped.
     * @return {@code true} when stopped
     */
    public boolean isStopped() {
    	return bIsStopped;
    }
    
    /**
     * Starts the multiplexer connection and registers with the replication master.
     * @throws Exception if connection or registration fails
     */
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
    
	/**
	 * Returns the remote multiplexer client, creating it when needed.
	 * @return remote multiplexer client
	 */
	public OARemoteMultiplexerClient getRemoteMultiplexerClient() {
		if (remoteMultiplexerClient == null) {
			remoteMultiplexerClient = new OARemoteMultiplexerClient(getMultiplexerClient());
		}
		return remoteMultiplexerClient;
	}
    
	/**
	 * Returns the underlying multiplexer client, creating it when needed.
	 * @return multiplexer client
	 */
	protected OAMultiplexerClient getMultiplexerClient() {
		if (multiplexerClient != null) return multiplexerClient;

    	LOG.fine(String.format("creating OAMultiplexerClient, serverHostName=%s, port=%d", getClientInfo().getServerHostName(), clientInfo.getServerHostPort()));
		multiplexerClient = new OAMultiplexerClient(getClientInfo().getServerHostName(), clientInfo.getServerHostPort()) {
			@Override
						/**
			 * Forwards socket exceptions to the owning replication connection.
			 * @param e socket exception
			 */
protected void onSocketException(Exception e) {
				OAReplClientConnection.this.onSocketException(e);
			}

			@Override
						/**
			 * Forwards socket-close events to the owning replication connection.
			 * @param bError true when the close was caused by an error
			 */
protected void onClose(boolean bError) {
				OAReplClientConnection.this.onSocketClose(bError);
			}
		};
		return multiplexerClient;
	}
    

	/**
	 * Stops the connection and closes the multiplexer client.
	 * @throws Exception if shutdown fails
	 */
	public void stop() throws Exception {
		if (bIsStopped || !bIsConnected) return;
		LOG.fine("stopping connection to Master");
		bIsStopped = true;
		bIsConnected = false;

		getClientInfo().setStarted(false);
		remoteMaster.setEnabled(false);
		getMultiplexerClient().close();
	}
	
    
	/**
	 * Returns client connection metadata, creating it when needed.
	 * @return client information
	 */
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

	/**
	 * Returns the remote master registration proxy.
	 * @return remote registration proxy
	 * @throws Exception if lookup fails
	 */
	public RemoteMasterRegisterInterface getRemoteMasterRegister() throws Exception {
		if (remoteMasterRegister == null) {
			RemoteMasterRegisterInterface reg = (RemoteMasterRegisterInterface) getRemoteMultiplexerClient().lookup(OAReplicationMaster.ReplicationMasterLookupName);
			remoteMasterRegister = (RemoteMasterRegisterInterface) reg;
		}
		return remoteMasterRegister;
	}
	
	/**
	 * Returns the registered remote master session proxy.
	 * @return remote master session proxy
	 * @throws Exception if registration fails
	 */
	public RemoteMasterInterface getRemoteMaster() throws Exception {
		if (remoteMaster == null) {
			remoteMaster = getRemoteMasterRegister().registerClient(guid, getRemoteClient(), this.initMasterSeq, this.initClientSeq);
		}
		return remoteMaster;
	}

	/**
	 * Returns the remote client callback proxy exposed to the master.
	 * @return remote client callback proxy
	 * @throws Exception if proxy creation fails
	 */
	public RemoteClientInterface getRemoteClient() throws Exception {
        if (remoteClient == null) {
        	remoteClient = new RemoteClientInterface() {
				@Override
								/**
				 * Forwards a master message to the owning replication connection.
				 * @param masterSeq master sequence number
				 * @param methodName remote sync method name
				 * @param args remote sync method arguments
				 */
public void processMessage(long masterSeq, String methodName, Object[] args) {
					OAReplClientConnection.this.processMessageFromMaster(masterSeq, methodName, args);
				}
			};
        }
        return remoteClient;
    }
	
	/**
	 * Called when the client socket reports an exception.
	 * @param e socket exception
	 */
	protected abstract void onSocketException(Exception e);
	/**
	 * Called when the client socket closes.
	 * @param bError true when the close was caused by an error
	 */
	protected abstract void onSocketClose(boolean bError);
	
	/**
	 * Processes a replication message received from the master.
	 * @param masterSeq master sequence number
	 * @param methodName remote sync method name
	 * @param args remote sync method arguments
	 */
	public abstract void processMessageFromMaster(long masterSeq, String methodName, Object[] args);
}

