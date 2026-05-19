package com.viaoa.replication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.concurrent.OAThrottle;
import com.viaoa.datetime.OADateTime;
import com.viaoa.io.OAFile;
import com.viaoa.lang.OAStr;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.replication.remote.RemoteClientInterface;
import com.viaoa.replication.remote.RemoteMasterInterface;
import com.viaoa.replication.remote.RemoteMasterRegisterInterface;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.runtime.thread.OAThread;
import com.viaoa.sync.OASyncServer;


/*qqqqqqqqqqqqqqq
CODEX

2. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:174 RemoteMasterInterface.processMessage

  concrete bug
  The master advances lastReceivedClientSeq immediately after enqueueing a client message, before the message is
  applied or durably represented in master replication state.

  runtime scenario
  Client sends a change. Master processMessage() enqueues ClientMsg and updates lastReceivedClientSeq. Master crashes
  before ReplClientSession.process() applies it. On reconnect, client asks master for last received client seq and
  skips resending that change.

  why this violates OA/OG replication semantics
  The master claims receipt/ack durability before the replication change is committed. This can silently drop a valid
  client change.

  minimal fix direction
  Only advance the durable/authoritative “received/acked” sequence after successful apply/log commitment, or maintain
  separate queued vs applied/acked sequence state.

  suggested CODEX comment location
  Inside RemoteMasterInterface.processMessage() before updating lastReceivedClientSeq.


3. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:239 ReplClientSession.process

  concrete bug
  A failed client message is removed from alClientMsg before successful apply and is not requeued on failure.

  runtime scenario
  alClientMsg.poll() removes the message. method.invoke(...) throws because the object/reference is not currently
  resolvable or another runtime failure occurs. The catch logs and breaks, but the failed message is gone. Later
  processing can continue with subsequent messages.

  why this violates OA/OG replication semantics
  Replay/apply failure becomes message loss. Retry cannot correctly apply the missing change, and later messages can
  create divergent object/Hub state.

  minimal fix direction
  Do not permanently remove/ack the message until successful apply, or requeue/retain failed message with explicit
  retry/failure state.

  suggested CODEX comment location
  Around ClientMsg cm = alClientMsg.poll() and the catch block in ReplClientSession.process().

>> fix now
4. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:287 onNewMessageFromMaster

  concrete bug
  Duplicate master message filtering compares incoming masterSeq to lastSentMasterSeq instead of the client’s last
  applied this.masterSeq, and it uses < rather than <=.

  runtime scenario
  Master resends or retries a message with the same masterSeq the client already applied. If lastSentMasterSeq is
  unrelated/lower, the client invokes the remote sync method again.

  why this violates OA/OG replication semantics
  Replay is not idempotent at the sequence boundary. Duplicate master messages can duplicate Hub operations or object
  lifecycle changes.

  minimal fix direction
  Reject already-applied master messages using if (masterSeq <= this.masterSeq) return;, with any expected gap/order
  handling made explicit.

  suggested CODEX comment location
  At the first guard in onNewMessageFromMaster().


>> fix this
3. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:193 ReplClientSession constructor

  concrete bug
  The constructor receives the client’s last received master sequence but does not initialize this.lastSentMasterSeq
  from it.

  runtime scenario
  A replication client reconnects and passes lastSentMasterSeq. The session stores it in lastReceivedMasterSeq, but
  lastSentMasterSeq remains 0 until the client later calls setLastReceivedMasterSeq(...). Before that call, the master
  processing thread can resend old master TLogs.

  why this violates OA/OG replication semantics
  Reconnect replay can duplicate already-applied master messages during the registration window.

  minimal fix direction
  Initialize this.lastSentMasterSeq from the constructor argument, or otherwise make registration atomically establish
  the client’s master replay position.

  suggested CODEX comment location
  Inside ReplClientSession(...) after setting lastReceivedMasterSeq.

>> fix this
4. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:360 onClientDisconnected

  concrete bug
  The cleanup hook exists but is not wired to the sync/remote session removal path.

  runtime scenario
  A replication client disconnects. OASyncServer.onSessionRemoved(...) is a no-op, and no main-source reference calls
  OAReplicationMaster.onClientDisconnected(...). The old ReplClientSession remains in hmClientSession.

  why this violates OA/OG replication semantics
  Disconnected sessions can continue to be processed, causing stale remote calls, delayed live-client processing, and
  retained replication state.

  minimal fix direction
  Wire replication master session removal into the sync server session-removal callback used by the owning replication
  service.

  suggested CODEX comment location
  At onClientDisconnected(...) and the replication service startup/wiring point.


1. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:262 ReplClientSession.remoteMaster.processMessage

  concrete bug
  If alClientMsg.put(msg) fails, the method still advances lastReceivedClientSeq.

  runtime scenario
  The remote invocation thread is interrupted while calling LinkedBlockingQueue.put. The catch logs the enqueue
  failure, but execution continues to:

  if (clientSeq > lastReceivedClientSeq) {
      lastReceivedClientSeq = clientSeq;
  }

  why this violates OA/OG replication semantics
  The master can acknowledge receipt of a client replication message that was never queued, applied, or made
  retryable. This is a silent lost-change path.

  minimal fix direction
  Return/throw after enqueue failure. Only advance received/ack state after successful enqueue, and eventually after
  the stronger durable/apply rule already noted.

  suggested CODEX comment location
  Inside RemoteMasterInterface.processMessage, immediately after the catch around alClientMsg.put(msg).


3. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:243 ReplClientSession constructor

  concrete bug
  A reconnecting client’s lastSentClientSeq initializes lastReceivedClientSeq, but not lastProcessedClientSeq.

  runtime scenario
  A client reconnects after the master already applied client sequence N, but the client resends sequence N because
  the previous send result was uncertain. The new ReplClientSession has lastProcessedClientSeq == 0, so process() does
  not skip the duplicate and invokes the sync method again.

  why this violates OA/OG replication semantics
  Client sequence idempotency is not preserved across sessions. Reconnect/retry can duplicate object or Hub changes.

  minimal fix direction
  Initialize the duplicate-suppression state from the reconnect handshake, or maintain durable per-client processed
  sequence state.

  suggested CODEX comment location
  Inside ReplClientSession(...) after setting lastReceivedClientSeq.



1. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:451 ReplClientSession.process and src/main/java/com/
     viaoa/replication/OAReplicationClient.java:388 onNewMessageFromMaster

  concrete bug
  The master treats remoteClient.processMessage(...) returning as successful client delivery, but the client’s durable
  replication TLog update happens later through the async sync queue capture path.

  runtime scenario
  Master sends masterSeq=N to a client. Client applies the remote sync method and returns from
  onNewMessageFromMaster(), setting only in-memory this.masterSeq = N. Before the client queue-capture thread writes
  the generated sync event to the client TLog, the client process crashes. The master has already advanced
  lastSentMasterSeq, so it believes the client received the message.

  why this violates OA/OG replication semantics
  Client delivery acknowledgment is earlier than the client durable replay/checkpoint point. This can create either
  missing durable client state or duplicate replay after reconnect, depending on what object/datasource state
  survived.

  minimal fix direction
  Make client receipt durable before the remote method returns, or add an explicit ack phase that only advances master
  send state after client durable TLog/checkpoint is complete.

  suggested CODEX comment location
  At master remoteClient.processMessage(...) calls and client onNewMessageFromMaster() after method.invoke(...).

5. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:274 stop

  concrete bug
  Master stop closes the TLog stream before stopping the base sync queue capture thread.

  runtime scenario
  OAReplicationMaster.stop() closes objectOutputStream, then calls super.stop(). During that window, the queue-capture
  thread can still call onNewSyncMessage(), which calls writeTLog() against a closed/null stream and stops the capture
  thread with an exception.

  why this violates OA/OG replication semantics
  A sync event arriving during normal shutdown can fail after replication has already closed its durable log,
  producing shutdown-time message loss or noisy false failure.

  minimal fix direction
  Stop/unregister the queue capture first, then close the TLog stream, ideally with thread join or another barrier
  that prevents new onNewSyncMessage() calls.

  suggested CODEX comment location
  At the beginning of OAReplicationMaster.stop() before closing objectOutputStream.

2. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:465 ReplClientSession.process

  concrete bug
  The master does not detect client sequence gaps before applying a client message.

  runtime scenario
  Master has processed client sequence 5. The next queued message has clientSeq=7 because seq 6 was lost/dropped
  during reconnect or enqueue failure. The current guard only skips <= lastProcessedClientSeq, so seq 7 is applied and
  lastProcessedClientSeq becomes 7.

  why this violates OA/OG replication semantics
  Client-originated changes are ordered. Applying a later client sequence while silently skipping an earlier one can
  diverge Store/Corp state.

  minimal fix direction
  Require cm.clientSeq == lastProcessedClientSeq + 1 for normal apply, with explicit failure/resync handling on gaps.

  suggested CODEX comment location
  Inside ReplClientSession.process() before invoking the client message.

3. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:467 onNewMessageFromMaster and src/main/java/com/
     viaoa/replication/OAReplicationMaster.java:461 ReplClientSession.process

  concrete bug
  Replication ThreadLocal source is cleared to null instead of restoring the previous value.

  runtime scenario
  Replication processing runs on a thread that already has a replication source set by an outer OA remote/sync
  context. These methods call setReplicationSource(guid) or setReplicationSource(this.guid), then clear with
  setReplicationSource(null). The outer source is lost.

  why this violates OA/OG replication semantics
  Originator filtering depends on replication source being balanced. Clobbering a previous source can cause echo
  suppression mistakes or legitimate downstream messages to be misclassified.

  minimal fix direction
  Capture String hold = srvcOAThreadLocal.getReplicationSource() before setting, and restore hold in finally.

  suggested CODEX comment location
  At both setReplicationSource(...) blocks.


4. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:461 ReplClientSession.process

  concrete bug
  The master’s replication source restoration is not protected by an outer finally.

  runtime scenario
  After setReplicationSource(this.guid), an unexpected runtime failure outside the inner per-message catch path
  prevents line 482 from running. The replication master worker thread then continues future processing with the wrong
  replication source.

  why this violates OA/OG replication semantics
  A leaked replication source can suppress or misroute later replication messages for other clients, causing silent
  missing changes.

  minimal fix direction
  Wrap the whole client-message apply section in try/finally, restore the previous source, and keep the master-to-
  client send section outside that source context.

  suggested CODEX comment location
  Around srvcOAThreadLocal.setReplicationSource(this.guid) in ReplClientSession.process().


3. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:387 stop

  concrete bug
  Master stop() does not wait for threadRepl to exit.

  runtime scenario
  stop() closes the TLog stream and calls super.stop(), then returns. The master processing thread can still be inside
  runProcessClients() or sleeping before observing bStop.

  why this violates OA/OG replication semantics
  Replication can appear stopped while the master still sends messages to clients or processes queued client messages.
  This breaks lifecycle determinism and restart safety.

  minimal fix direction
  Signal stop before closing resources, then join threadRepl before stop() returns.

  suggested CODEX comment location
  Inside OAReplicationMaster.stop().

4. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:370 _start / src/main/java/com/viaoa/replication/
     OAReplicationMaster.java:387 stop

  concrete bug
  The replication master lookup is created on start but never removed on stop.

  runtime scenario
  Replication master stops, but oaReplicationMaster remains registered in the remote multiplexer lookup table. A
  replication client can still look up the stopped master object and call registerClient, creating sessions against a
  stopped/closed replication master.

  why this violates OA/OG replication semantics
  Stopped replication can still accept remote registration, leaving stale sessions and false liveness. Restart can
  also interact with an old lookup binding.

  minimal fix direction
  Call removeLookup(ReplicationMasterLookupName) during master stop, after rejecting/closing active sessions.

  suggested CODEX comment location
  Where _start() calls createLookup(...) and in stop().


1. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:665 onNewSyncMessage

  concrete bug
  currentMasterSeq is incremented before the TLog write succeeds.

  runtime scenario
  The master queue-capture thread receives a sync event, increments currentMasterSeq, then writeTLog(tlog) throws
  because the log stream is closed or disk write fails. The async queue thread catches/logs and stops, but the in-
  memory currentMasterSeq is now ahead of the durable master log.

  why this violates OA/OG replication semantics
  The master sequence can advance without a durable replication record. If the same replication object is later
  restarted/reused, the next successful record can create a master sequence gap that clients cannot replay correctly.

  minimal fix direction
  Allocate/commit the sequence at the same durable boundary: either increment only after successful write, or roll
  back/mark failed sequence explicitly and force resync.

  suggested CODEX comment location
  At currentMasterSeq++ in OAReplicationMaster.onNewSyncMessage.


3. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationMaster.java:407 registerClient

  concrete bug
  A new session for the same replication guid does not disable or reconcile any existing live session for that same
  guid.

  runtime scenario
  A client reconnects with a new connection id before the old connection is fully removed, or two Store runtimes
  accidentally start with the same replication guid. The master keeps both sessions in hmClientSession. Source
  filtering then treats both sessions as the same origin, and both sessions can also contend for outbound replay state
  independently.

  why this violates OA/OG replication semantics
  Replication source identity is guid-based, but session ownership is connection-id-based. Duplicate live sessions for
  one guid can suppress legitimate downstream messages or duplicate delivery attempts.

  minimal fix direction
  Track active sessions by guid as well as connection id. On registration, reject duplicate guid sessions or
  atomically replace/disable the prior session.

  suggested CODEX comment location
  Inside registerClient(...) before hmClientSession.put(...).


*/


public class OAReplicationMaster extends OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationMaster.class.getName());

	public static final String ReplicationMasterLookupName = "oaReplicationMaster";

	private Map<Integer, ReplClientSession> hmClientSession = new ConcurrentHashMap<Integer, ReplClientSession>();
	
	private final List<List<OAReplTLog>> alListReplTLog = new ArrayList<>();
	private final int RequestInfoListSize = 1000;
	
	protected final String tlogFileName;
    private FileOutputStream fileOutputStream;
    private ObjectOutputStream objectOutputStream;
	
	private long currentMasterSeq;

	private Thread threadRepl;
	
    public OAReplicationMaster(OASyncServer syncServer, String tlogFilename) {
    	super(syncServer);
    	this.tlogFileName = tlogFilename;
    }

    @Override
    public void start() throws Exception {
    	LOG.fine("starting ReplMaster");
    	this._start();
    	super.start();
    }    
    
    protected void _start() throws Exception {
    	loadTLogFile();
        openTLogFile();

    	// register Remote lookup object for clients to get.
    	final RemoteMasterRegisterInterface remoteMasterRegister = new RemoteMasterRegisterInterface() {
			@Override
			public RemoteMasterInterface registerClient(String guid, RemoteClientInterface remoteClient, long lastSentMasterSeq, long lastSentClientSeq) {
				final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
				RequestInfo ri = srvcOAThreadLocal.getRemoteRequestInfo();
				if (ri == null) throw new RuntimeException("RequestInfo is null");
				
				final ReplClientSession cs = new ReplClientSession(guid, ri.connectionId, remoteClient, lastSentMasterSeq, lastSentClientSeq);
				hmClientSession.put(ri.connectionId, cs);
				return cs.remoteMaster;
			}
    	};
    	
    	syncServer.getRemoteMultiplexerServer().createLookup(ReplicationMasterLookupName, remoteMasterRegister, RemoteMasterRegisterInterface.class);
    	LOG.fine("created remote RemoteMasterRegister, lookup name=" + ReplicationMasterLookupName);
    	
    	// start thread that will repl each client 
        final String threadName = "OAReplicationMaster";
        threadRepl = new Thread(new Runnable() {
            @Override
            public void run() {
            	runProcessClients();
            }
        });
        threadRepl.setName(threadName);
        threadRepl.setDaemon(true);
        threadRepl.start();
    	LOG.fine("thread started to Replicate Clients with this Master, thread name="+threadName);
    }

	public void stop() throws Exception {
    	LOG.fine("Stop called"); 
    	
		synchronized (lockTLogFile) {
			if (objectOutputStream != null) {
		    	objectOutputStream.close();
				objectOutputStream = null;
			}
		}    	
		super.stop();
    }

	protected void runProcessClients() {
    	final OAThrottle throttle = new OAThrottle(250);
    	try {
    		for (; !this.bStop; ) {
    	    	LOG.fine("checking to sync Repli Clients with Master");
    			long ms = System.currentTimeMillis();
        		for (int id : hmClientSession.keySet()) {
        			if (OAReplicationMaster.this.bStop) break;
        			ReplClientSession ci = hmClientSession.get(id);
        			
        			LOG.fine("processing client " +id);
        			if (ci == null) continue;
        			try {
        				ci.process();
        			}
                    catch (Exception e) {
                    	if (throttle.check()) {
                    		LOG.log(Level.WARNING, "exception while processing client merge for connection: " + id +", will continue", e);
                    	}
                    }
        		}
    			long diff = System.currentTimeMillis() - ms;
        		if (diff < 2000) OAThread.sleep(2000 - diff);
    		}
        }
        catch (Exception e) {
            String s = "ProcesClient Thread is stopping, which will stop replicating with clients.";
            LOG.log(Level.WARNING, s, e);
        }
	}
    
	protected class ReplClientSession {
		final int sessionId;  // same as connectionId
		final RemoteClientInterface remoteClient;
		
		final LinkedBlockingQueue<ClientMsg> alClientMsg = new LinkedBlockingQueue<>();

		final String guid;  // unique replClient name
		
		volatile long lastReceivedMasterSeq;
		volatile long lastProcessedMasterSeq;
		volatile long lastSentMasterSeq;
		
		volatile long lastReceivedClientSeq; 
		volatile long lastProcessedClientSeq;

		volatile long msLastProcessed;
		volatile int lastRequestInfoSize;
		
		volatile boolean bEnabled = true;
		
		ReplClientSession(String guid, int sessionId, RemoteClientInterface remoteClient, long lastSentMasterSeq, long lastSentClientSeq) {
			this.guid = guid;
			this.sessionId = sessionId;
			this.remoteClient = remoteClient;
			this.lastReceivedMasterSeq = lastSentMasterSeq;
			this.lastReceivedClientSeq = lastSentClientSeq;
		}
		
		// ReplClient uses this as remote object to send sync messages to ReplMaster
		final RemoteMasterInterface remoteMaster = new RemoteMasterInterface() {
			@Override
			public void processMessage(long masterSeq, long clientSeq, String methodName, Object[] args) {
				LOG.fine("received message from Client.session="+sessionId+", method="+methodName);
				
				ClientMsg msg = new ClientMsg();
				msg.masterSeq = masterSeq;
				msg.clientSeq = clientSeq;
				msg.methodName = methodName;
				msg.args = args;
				try {
					alClientMsg.put(msg);
				}
				catch (Exception ex) {
					LOG.log(Level.WARNING, "error adding ClientMsg to blocking que", ex);
				}
				if (clientSeq > lastReceivedClientSeq) {
					lastReceivedClientSeq = clientSeq;
					lastReceivedMasterSeq = masterSeq;
				}
			}
			@Override
			public long getLastReceivedClientSeq() {
				return lastReceivedClientSeq;
			}
			@Override
			public long getLastProcessedClientSeq() {
				return lastProcessedClientSeq;
			}
			@Override
			public long getLastReceivedMasterSeq() {
				return lastReceivedMasterSeq;
			}
			@Override
			public long getLastProcessedMasterSeq() {
				return lastProcessedMasterSeq;
			}
			@Override
			public void setEnabled(boolean b) {
				bEnabled = b;
			}
			@Override
			public boolean getEnabled() {
				return bEnabled;
			}
			@Override
			public void setLastReceivedMasterSeq(long seq) {
				lastSentMasterSeq = seq;
			}
		}; 
 
		void process() {
			if (!bEnabled) return;
			long msNow = System.currentTimeMillis();
			int size = alClientMsg.size();
			int size2 = getRequestInfoSize();
			
			LOG.fine("processing msgs from Client.session="+sessionId+" clientMsg.size="+size+", masterMsg.size="+(size2 - lastRequestInfoSize));
			if (msLastProcessed != 0L && msLastProcessed + 1000 > msNow) {
				if (size < 50 && (size2 - lastRequestInfoSize) < 50) {
					return;
				}
			}
			lastRequestInfoSize = size2;

			
			// invoke client changes on master.
			final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();
			srvcOAThreadLocal.setReplicationSource(this.guid);
			for (int i=0; ; i++) {
				try {
					ClientMsg cm = alClientMsg.poll();
					if (cm == null) break;
					
					if (cm.clientSeq <= lastProcessedClientSeq) continue;
					
					Method method = getMethod(cm.methodName);
					LOG.fine("invoking message from Client.session="+sessionId+", method="+method.getName());
					method.invoke(syncServer.getRemoteSyncImpl(), cm.args);

					lastProcessedClientSeq = cm.clientSeq;
					lastProcessedMasterSeq = cm.masterSeq;
				}
				catch (Exception ex) {
					LOG.log(Level.WARNING, "exception invoking client message", ex);
					break;
				}
			}
			srvcOAThreadLocal.setReplicationSource(null);
			
			// send master server msgs to client
			final int x = alListReplTLog.size();
			for (int i=0; i < (x-1); i++) {
				List<OAReplTLog> al;
				synchronized (alListReplTLog) {
					al = alListReplTLog.get(i);
				}
				if (al.get(RequestInfoListSize-1).masterSeq <= lastProcessedMasterSeq) continue;
				for (OAReplTLog tlog : al) {
					if (tlog.masterSeq <= lastSentMasterSeq) continue;
					
					String s = tlog.getSource();
					if (OAStr.equals(s,  guid)) continue;
					
					LOG.fine("sending Master message to Client.session="+sessionId+", method="+tlog.methodName);
					remoteClient.processMessage(tlog.masterSeq, tlog.methodName, tlog.args);
					lastSentMasterSeq = tlog.masterSeq;
				}
			}

			if (x > 0) {
				List<OAReplTLog> al;
				synchronized (alListReplTLog) {
					al = alListReplTLog.get(x-1);
				}
				synchronized (al) {
					for (OAReplTLog tlog : al) {
						if (tlog.masterSeq <= lastSentMasterSeq) continue;

						String s = tlog.getSource();
						if (OAStr.equals(s,  guid)) continue;
						
						LOG.fine("sending Master message to Client.session="+sessionId+", method="+tlog.methodName);
						remoteClient.processMessage(tlog.masterSeq, tlog.methodName, tlog.args);
						lastSentMasterSeq = tlog.masterSeq;
					}
				}
			}
			msLastProcessed = System.currentTimeMillis();;
		}
	}
	
	protected static class ClientMsg {
		long masterSeq;
		long clientSeq;
		String methodName;
		Object[] args;
	}

	
	private int getRequestInfoSize() {
		int x = alListReplTLog.size();
		if (x == 0) return 0;
		int tot = 0;
		if (x > 1) {
			tot = RequestInfoListSize * (x-1);
		}
		List<OAReplTLog> al = null;
		synchronized (alListReplTLog) {
			al = alListReplTLog.get(x - 1);
		}
		synchronized (al) {
			tot += al.size();
		}
		return tot;
	}
	
	// Note:  needs to be called from OASyncServer	
	public void onClientDisconnected(int clientId) {
		hmClientSession.remove(clientId);
	}

	@Override
	protected void onNewSyncMessage(RequestInfo ri) {
		currentMasterSeq++;
		
        final OAReplTLog tlog = new OAReplTLog(ri.replicationSource, new OADateTime(), currentMasterSeq, 0L, ri.method.getName(), ri.args);
		
		writeTLog(tlog);
		addTLog(tlog);
	}
	

	protected void loadTLogFile() {
		try {
			String fn = OAFile.convertFileName(tlogFileName);
	        File file = new File(fn);
			
	    	LOG.fine(String.format("tlogFileName=%s, exists=%b", fn, file.exists()));
	        if (file.exists()) {
	            FileInputStream fis = new FileInputStream(file);
	            BufferedInputStream bis = new BufferedInputStream(fis, 64 * 1024);
	            ObjectInputStream ois = new ObjectInputStream(bis);
	            currentMasterSeq = ois.readLong();
	            
	            int cnt = 0;
	            for (; ; cnt++) {
	            	OAReplTLog tlog;
	            	try {
	                	tlog = (OAReplTLog) ois.readObject();
	                	currentMasterSeq = tlog.getMasterSeq();
	            	}
	            	catch (EOFException e) {
	            		break;
	            	}
	            	catch (IOException e) {
	            		throw new RuntimeException("Exception loading TLog file", e);
	            	}
	        		
        			addTLog(tlog);
			    	LOG.fine(String.format("%,d) masterSeq=%,d, methodName=%s", cnt+1, tlog.getMasterSeq(), tlog.getMethodName()));
	            }
	            ois.close();
		    	LOG.fine(String.format("tlogFileName=%s, total tlog records=%,d", fn, cnt));
	    	}
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}

	
	protected void openTLogFile() {
		synchronized (lockTLogFile) {
			_openTLogFile();
		}		
	}
	protected void _openTLogFile() {
		try {
			if (objectOutputStream != null) {
				objectOutputStream.close();
			}
			
			String fn = OAFile.convertFileName(tlogFileName);
			OAFile.mkdirsForFile(fn);
	        File file = new File(fn);
	        
	    	LOG.fine(String.format("tlogFileName=%s, exists=%b", fn, file.exists()));
	        final boolean bAppend = file.exists() && file.length() > 0;
	        fileOutputStream = new FileOutputStream(file, bAppend);
	        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream, 64 * 1024);

	        objectOutputStream = new ObjectOutputStream(bos) {
	        	@Override
	            protected void writeStreamHeader() throws IOException {
	                if (bAppend) reset(); // writes a TC_RESET token into the stream, avoids duplicate stream header when appending
	                else super.writeStreamHeader();
	            }
	        };
	        if (!bAppend) {
			    LOG.fine(String.format("wrote header: masterSeq=%,d", currentMasterSeq));
	        	objectOutputStream.writeLong(currentMasterSeq);
		        objectOutputStream.flush();
		        fileOutputStream.getFD().sync();
	        }
		}
		catch (Exception e) {
			throw new RuntimeException("exception opening tlog file", e);
		}
	}

	protected void createNewTLogFile(String newFileName) {
		try {
			String fn = OAFile.convertFileName(newFileName);
			OAFile.mkdirsForFile(fn);
	        File file = new File(fn);
			
	    	LOG.fine(String.format("fileName=%s, exists=%b, wasOpen=%b", fn, file.exists(), (objectOutputStream != null)));
			if (objectOutputStream != null) {
				objectOutputStream.close();
			}
	        
	        fileOutputStream = new FileOutputStream(file); 
	        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream, 64 * 1024);
	        objectOutputStream = new ObjectOutputStream(bos);
        	objectOutputStream.writeLong(currentMasterSeq);
	        objectOutputStream.flush();
	        fileOutputStream.getFD().sync();

		    LOG.fine(String.format("wrote header: masterSeq=%,d", currentMasterSeq));
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}

	private final Object lockTLogFile = new Object();

	
	protected void writeTLog(final OAReplTLog tlog) {
		try {
			synchronized (lockTLogFile) {
		        objectOutputStream.writeObject(tlog);
		        objectOutputStream.flush();                            
		        fileOutputStream.getFD().sync();
			}
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}
	
	protected void addTLog(final OAReplTLog tlog) {
        LOG.fine("new message from Sync que");
		synchronized (alListReplTLog) {
			List<OAReplTLog> al = null;
			int x = alListReplTLog.size();
			if (x > 0) {
				al = alListReplTLog.get(x - 1);
				if (al.size() == RequestInfoListSize) al = null;
			}
			if (al == null) {
				al = new ArrayList<>(RequestInfoListSize);
				alListReplTLog.add(al);
			}
			synchronized (al) {
				al.add(tlog);
			}
		}
	}
}
