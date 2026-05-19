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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.datetime.OADateTime;
import com.viaoa.io.OAFile;
import com.viaoa.lang.OAStr;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.replication.client.OAReplClientConnection;
import com.viaoa.runtime.OARuntime;
import com.viaoa.runtime.OAThreadLocalService;
import com.viaoa.runtime.OAThreadService;
import com.viaoa.runtime.thread.OAThread;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.remote.RemoteSyncImpl;

/*qqqqqqqqqqqqqqq
CODEX

1. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:324 loadTLogFile

  concrete bug
  Client restart skips all locally sourced TLog records, including client-originated changes that were written locally
  but not yet confirmed by the master.

  runtime scenario
  onNewSyncMessage writes client-originated TLogs with source = guid. On restart, loadTLogFile() uses
  OAStr.equals(guid, tlog.getSource()) to decide bDontSend, so those unsent local changes are not requeued to
  alTLogToMaster.

  why this violates OA/OG replication semantics
  A durable local replication log record can be silently lost after client restart, causing Store/Corp divergence.

  minimal fix direction
  Requeue client-originated TLogs based on sequence/ack state, not just source == guid. The log needs to distinguish
  “originated locally and pending master ack” from “came from master replay”.

  suggested CODEX comment location
  Near loadTLogFile() where bDontSend is computed.

>> fix this
5. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:174 runSendSyncMessagesToMaster / src/main/java/com/
     viaoa/replication/OAReplicationClient.java:430 writeTLog

  concrete bug
  Client TLog rewrite and normal sync-message TLog writes use the same objectOutputStream without synchronization.

  runtime scenario
  During shutdown, the send thread rewrites the TLog file to .tmp and replaces objectOutputStream. At the same time,
  the queue thread can still be inside onNewSyncMessage() writing a new local TLog record.

  why this violates OA/OG replication semantics
  Concurrent writes/closes against the same stream can corrupt the client TLog or silently omit a valid replication
  record.

  minimal fix direction
  Use a single TLog lock around open/createNew/write/close/rewrite, or stop and join the queue-capture thread before
  rewriting.

  suggested CODEX comment location
  At client writeTLog(...) and the rewrite block in runSendSyncMessagesToMaster().

>> fix this
6. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:81 start and src/main/java/com/viaoa/replication/
     OAReplicationMaster.java:88 start

  concrete bug
  Subclass _start() starts replication resources/threads before OAReplicationBase.start() starts the sync queue
  capture. If super.start() fails, partial replication remains running while start reports failure.

  runtime scenario
  Client _start() opens the TLog and starts the send thread. Master _start() opens the TLog, registers lookup, and
  starts the processing thread. Then super.start() fails while registering/reading the sync queue.

  why this violates OA/OG replication semantics
  A failed replication start can leave live background replication threads and partially registered remote state,
  causing false lifecycle state and retry corruption.

  minimal fix direction
  Either start the base queue capture first, or wrap startup in cleanup so any earlier thread/lookup/TLog resources
  are stopped/closed when later startup fails.

  suggested CODEX comment location
  At both start() methods where _start() is called before super.start().



2. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:136 runSendSyncMessagesToMaster

  concrete bug
  bGotSeqFromMaster is global for the client lifetime and is not reset when a new replication connection is created.

  runtime scenario
  Client sends a TLog to master. Master receives/applies it, but the connection fails before the client updates local
  sent state or before the caller can know the outcome. The client reconnects with a new OAReplClientConnection, but
  bGotSeqFromMaster remains true, so it does not ask the master for the current received client sequence and can
  resend an already-applied client change.

  why this violates OA/OG replication semantics
  Reconnect reconciliation is skipped after the first connection. That can duplicate client-originated changes after
  an uncertain send/disconnect boundary.

  minimal fix direction
  Reset bGotSeqFromMaster when rcc != rccLast, or make the ack handshake per connection/session.

  suggested CODEX comment location
  Around the if (rcc != rccLast) block before if (!bGotSeqFromMaster).

2. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:476 writeTLog and src/main/java/com/viaoa/
     replication/OAReplicationMaster.java:625 writeTLog

  concrete bug
  The same ObjectOutputStream writes multiple TLog records without resetting object handles between records.

  runtime scenario
  A TLog record serializes an OAObjectSerializer or another object-bearing argument. A later TLog record contains the
  same underlying object instance after changes. Java serialization can emit a back-reference to the previously
  serialized object instead of serializing the later state.

  why this violates OA/OG replication semantics
  The transaction log can replay stale object payloads even though later records were written successfully. This
  breaks deterministic replay and can corrupt object/Hub state after resync.

  minimal fix direction
  Call objectOutputStream.reset() before each independent TLog record, or write records with an explicit snapshot/
  unshared serialization contract.

  suggested CODEX comment location
  Inside both writeTLog(...) methods immediately before writeObject(tlog).


3. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:306 onNewSyncMessage and src/main/java/com/viaoa/
     replication/OAReplicationMaster.java:512 onNewSyncMessage

  concrete bug
  The in-memory queued OAReplTLog keeps the original mutable ri.args array/object references after the durable TLog
  write.

  runtime scenario
  A sync event containing an OAObjectSerializer is written to disk, then the same OAReplTLog instance is queued in
  memory for later send. Before the queued record is sent, the OAObject changes again. The later remote send can
  serialize the current object state, not the state represented by the TLog record.

  why this violates OA/OG replication semantics
  Connected/delayed clients can receive payloads that differ from the durable transaction log for the same sequence
  number. Replay and live forwarding no longer have the same semantics.

  minimal fix direction
  Snapshot/copy the TLog payload used for in-memory forwarding, or forward from the same serialized representation
  committed to the TLog.

  suggested CODEX comment location
  Where OAReplTLog is constructed from ri.args, before alTLogToMaster.put(tlog) and addTLog(tlog).


4. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:284 runSendSyncMessagesToMaster

  concrete bug
  The shutdown rewrite ignores the boolean result from File.renameTo(...) and logs success even if the temp TLog did
  not replace the real TLog.

  runtime scenario
  On shutdown, pending records are drained into tlogFileName.tmp. renameTo(new File(fn)) fails because the target
  exists or the filesystem rejects replacement. The method logs “rewrote file” and exits; the pending compacted file
  remains as .tmp, while restart reads the old TLog.

  why this violates OA/OG replication semantics
  Shutdown can falsely claim the replication log checkpoint/rewrite succeeded. Restart can use stale sequence/header/
  log state and miss or duplicate replication records.

  minimal fix direction
  Check renameTo result. Use an explicit replace strategy and fail visibly if the committed TLog file was not
  replaced.

  suggested CODEX comment location
  At f1.renameTo(new File(fn)).

1. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:459 onNewMessageFromMaster

  concrete bug
  The client does not detect master sequence gaps before applying a master message.

  runtime scenario
  Client has applied masterSeq=10. Because of disconnect/retry/log corruption/previous lost send, the next received
  message is masterSeq=12. The guard only rejects old messages, so seq 12 is applied and this.masterSeq becomes 12.
  Seq 11 is now silently skipped.

  why this violates OA/OG replication semantics
  Replication replay must not silently skip ordered master changes. Applying a later sequence makes the client look
  caught up while its object/Hub state is missing an earlier change.

  minimal fix direction
  Require masterSeq == this.masterSeq + 1 for normal apply, with explicit resync/replay handling for gaps.

  suggested CODEX comment location
  At the beginning of onNewMessageFromMaster(...), next to the existing master sequence guard.


1. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:515 loadTLogFile and src/main/java/com/viaoa/
     replication/OAReplicationMaster.java:651 loadTLogFile

  concrete bug
  A partial trailing TLog record is treated as normal EOF but is not truncated before appending new records.

  runtime scenario
  Process crashes during writeObject(tlog) after some bytes are written but before a complete object record is
  durable. On restart, loadTLogFile() catches EOFException and breaks, then openTLogFile() appends after the corrupted
  partial bytes. Later records are written after the bad tail.

  why this violates OA/OG replication semantics
  The transaction log can become permanently unreadable past the partial tail. Future replay may silently stop before
  valid appended records or fail startup, causing missing replication changes.

  minimal fix direction
  When EOF occurs while reading records, treat it as a truncated-tail recovery point: rewrite/truncate the file to the
  last known-good record boundary before appending.

  suggested CODEX comment location
  At each catch (EOFException e) { break; } in client and master loadTLogFile().

2. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:390 setDisconnectFromMaster /
     getReplClientConnection

  concrete bug
  bDisconnectFromMaster is read by the sender thread but is not volatile or synchronized.

  runtime scenario
  One thread calls setDisconnectFromMaster(true) to intentionally keep the replication client offline. The send thread
  may not see the updated flag and can create a new OAReplClientConnection anyway after the current connection is
  nulled/stopped.

  why this violates OA/OG replication semantics
  Manual disconnect can falsely appear accepted while replication reconnects and resumes sending changes. That breaks
  explicit offline/disconnect control.

  minimal fix direction
  Make bDisconnectFromMaster volatile or guard it with the same synchronization used for connection lifecycle.

  suggested CODEX comment location
  At the bDisconnectFromMaster field declaration.


1. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:538 onNewMessageFromMaster and src/main/java/com/
     viaoa/replication/OAReplicationMaster.java:470 ReplClientSession.process

  concrete bug
  Replication ignores boolean return values from RemoteSyncInterface methods.

  runtime scenario
  A replicated addToHub, removeFromHub, propertyChange, moveObjectInHub, etc. invokes RemoteSyncImpl and returns false
  because the target object/hub/reference is not available or the operation was rejected. Replication ignores the
  return value and advances masterSeq or lastProcessedClientSeq.

  why this violates OA/OG replication semantics
  A failed sync apply can be recorded as successfully processed. That creates silent divergence and prevents retry/
  resync from knowing the operation did not apply.

  minimal fix direction
  Capture Object result = method.invoke(...); if the method returns Boolean.FALSE, treat it as failed/incomplete and
  do not advance processed sequence without explicit retry/resync handling.

  suggested CODEX comment location
  Immediately after both method.invoke(...) calls.

2. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:339 stop

  concrete bug
  Client stop() does not wait for the sender thread to finish its shutdown rewrite.

  runtime scenario
  stop() sets bStop through super.stop() and returns after closing the current connection. The sender thread may still
  be draining alTLogToMaster, rewriting the TLog temp file, closing streams, or replacing the file. Caller can
  immediately restart replication while the old sender thread is still mutating the same TLog.

  why this violates OA/OG replication semantics
  Replication lifecycle can falsely report stopped while durable log state is still being rewritten. Immediate restart
  can load stale/partial state or race with old file replacement.

  minimal fix direction
  Store the sender thread, signal stop, join it before stop() returns, and only then allow restart.

  suggested CODEX comment location
  At _start() where the sender thread is created and at stop() after super.stop().


 2. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:482 onNewSyncMessage

  concrete bug
  clientSeq is incremented before the client TLog write succeeds.

  runtime scenario
  A local client-originated sync event increments clientSeq, then writeTLog(tlog) throws. The queue-capture thread
  stops, but the in-memory client sequence has advanced without a durable or sendable record.

  why this violates OA/OG replication semantics
  The client can create a local sequence gap after a failed log write. On retry/restart with the same object, later
  client messages can appear to skip a sequence, making master-side replay/gap handling ambiguous.

  minimal fix direction
  Increment client sequence only when the TLog record is successfully committed, or explicitly record failed sequence
  state and force reconnect/resync.


1. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:572 onNewMessageFromMaster / src/main/java/com/
     viaoa/replication/OAReplicationClient.java:482 onNewSyncMessage

  concrete bug
  The client updates this.masterSeq after invoking the remote sync method, but the sync event caused by that method
  can be captured and written to the client TLog before masterSeq is advanced.

  runtime scenario
  Client receives master message masterSeq=20. onNewMessageFromMaster() sets replication source and invokes
  RemoteSyncImpl. That invocation emits a sync event. The replication queue-capture thread processes that event
  immediately, sees bCausedByMasterMsg == true, and writes a TLog record using the old masterSeq=19. Only after
  method.invoke(...) returns does onNewMessageFromMaster() set this.masterSeq = 20.

  why this violates OA/OG replication semantics
  The durable client TLog can record the wrong applied master sequence for a master-originated change. On restart,
  loadTLogFile() restores masterSeq from the TLog record and can make the client appear one message behind, causing
  duplicate replay or incorrect reconnect state.

  minimal fix direction
  Advance or stage the applied master sequence before the emitted sync event can be captured, or pass the
  authoritative incoming masterSeq through the replication context so onNewSyncMessage() writes the correct sequence
  for master-caused events.

  suggested CODEX comment location
  At onNewMessageFromMaster() before method.invoke(...), and in onNewSyncMessage() where the TLog is constructed with
  masterSeq.


*/

/**
 * Connects to OAReplicationMaster
 * gets Sync messages from this.server.oasync and sends them to OAReplicationMaster.
 * gets oasync messages from OAReplicationMaster, using remote RemoteRepInterface.processMessage, and calls this.RemoteSyncImpl method.
 * Keeps track of current message (oasync) position for client and master sync queue.
 * 
 */
public class OAReplicationClient extends OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationClient.class.getName());

	private final String masterHostName;
	private final int masterHostPort;

	private final String guid;

	private volatile long masterSeq; // last value from Master
	private long lastSentMasterSeq;
	private long clientSeq; // current value (last created/received from circque)(
	private long lastSentClientSeq; // last value sent to Master
	private boolean bGotSeqFromMaster;
	
    private RemoteSyncImpl remoteSyncImpl;
	
	private volatile OAReplClientConnection replClientConnection;
	
	// to Master
	private final LinkedBlockingQueue<OAReplTLog> alTLogToMaster = new LinkedBlockingQueue<>();

    private FileOutputStream fileOutputStream;
    private ObjectOutputStream objectOutputStream;
	protected final String tlogFileName;
	
    public OAReplicationClient(String tlogFileName, String guid, OASyncServer syncServer, String masterHostName, int masterHostPort) {
    	super(syncServer);
    	this.guid = guid;
    	this.tlogFileName = tlogFileName;     	
    	this.masterHostName = masterHostName;
    	this.masterHostPort = masterHostPort;
    	
    	LOG.fine(String.format("OAReplicationClient guid=%s, tlogFileName=%s, masterHostName=%s, masterHostPort=%d", 
    		guid, tlogFileName, masterHostName, masterHostPort
    	));
    }
    
    @Override
    public void start() throws Exception {
    	LOG.fine("starting OAReplicationClient");
    	this._start();
    	super.start();
    }

    
    protected void _start() throws Exception {
    	loadTLogFile();
        openTLogFile();
        
        
        // send Sync message from Client to Master
        String threadName = "OAReplicationClient";
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
            	OAReplicationClient.this.runSendSyncMessagesToMaster();
            }
        });
        t.setName(threadName);
        t.setDaemon(false);
        t.start();
    	LOG.fine(String.format("thread started to send sync msgs to Master, thread=%s", threadName)); 
    }
    
	public void stop() throws Exception {
    	LOG.fine("Stop called"); 
		super.stop();
		if (replClientConnection != null) {
			replClientConnection.stop();
			replClientConnection = null;
		}
    }

	
	protected void runSendSyncMessagesToMaster() {
    	OAReplTLog tlog = null;
		OAReplClientConnection rccLast = null;
    	for ( ; !bStop ; ) {
    		OAReplClientConnection rcc = null;
    		
    		rcc = getReplClientConnection(); // will be null if cant connect to master
    		if (rcc == null ) {
    			for (int i=0; i < 10 && !bStop; i++) OAThread.sleep(500);
    			continue;
    		}
    		
    		try {
    			if (rcc != rccLast) {
            		rcc.getRemoteMaster().setLastReceivedMasterSeq(this.masterSeq);
            		rccLast = rcc;
    			}
    			
    			if (!bGotSeqFromMaster) {
            		long x = rcc.getRemoteMaster().getLastReceivedClientSeq();
					if (x > this.lastSentClientSeq) {
						this.lastSentClientSeq = x;
					}
					/* add this back when Master keeps track of last know Client GUID lastSentMasterSeq
					x = rcc.getRemoteMaster().getLastReceivedMasterSeq();
					if (x > this.lastSentMasterSeq) {
						this.lastSentMasterSeq = x;
					}
					*/
					bGotSeqFromMaster = true;
				}
				if (tlog == null) {
					tlog = alTLogToMaster.poll(1, TimeUnit.SECONDS);
					if (tlog == null && !bStop) tlog = alTLogToMaster.poll(1, TimeUnit.SECONDS); 
					if (tlog == null) continue;
				}

				if (tlog.getClientSeq() > lastSentClientSeq) {
					LOG.fine("sending message to Master, method="+tlog.methodName);
	
					rcc.getRemoteMaster().processMessage(tlog.getMasterSeq(), tlog.getClientSeq(), tlog.getMethodName(), tlog.getArgs());
					lastSentClientSeq = tlog.getClientSeq();
					lastSentMasterSeq = tlog.getMasterSeq();
				}				
				tlog = null;
			}
			catch (Exception ex) {
				String s = String.format("exception calling OAReplicationClient.runSendSyncMessagesToMaster, Stop=%b, exception=%s",  bStop, ex.toString());
				if (!bStop) {
					LOG.log(Level.WARNING, s, ex);
					OAThread.sleep(1000);
				}
				else LOG.log(Level.WARNING, s, ex); 
			}
    	}

    	// re-write file
    	LOG.fine(String.format("rewriting to temp file %s.tmp", tlogFileName)); 
    	try {
	    	createNewTLogFile(tlogFileName + ".tmp");
	    	int cnt = 0;
	    	for (;; cnt++) {
	    		if (tlog == null) {
	    			tlog = alTLogToMaster.poll();
		    		if (tlog == null) break;
	    		}
	    		writeTLog(tlog);
	    		tlog = null;
	    	}
    		objectOutputStream.close();
    		objectOutputStream = null;
    		
			String fn = OAFile.convertFileName(tlogFileName + ".tmp");
    		File f1 = new File(fn);
    		
			fn = OAFile.convertFileName(tlogFileName);
    		f1.renameTo(new File(fn));
	    	LOG.fine(String.format("rewrote file %s, %,d tlog records", fn, cnt)); 
    	}
    	catch (Exception e) {
    		LOG.log(Level.WARNING, "exception rewriting tlog file, will use original one (no data loss)", e);
    	}
	}

	// Sync message from this.Server's queue
	@Override
	protected void onNewSyncMessage(RequestInfo ri) {
		boolean bCausedByMasterMsg = (OAStr.equals(guid, ri.replicationSource)); 
		
        if (!bCausedByMasterMsg) {
        	clientSeq++;
        }
		LOG.fine(String.format("new OASync message from this server, skipping=%b, methodName=%s, masterSeq=%,d, clientSeq=%,d", bCausedByMasterMsg, ri.method.getName(), masterSeq, clientSeq));

        final OAReplTLog tlog = new OAReplTLog(guid, new OADateTime(), masterSeq, clientSeq, ri.method.getName(), ri.args);
		try {
			writeTLog(tlog);
			if (!bCausedByMasterMsg) { // happened when processing Master msg, dont send back to Master			
				alTLogToMaster.put(tlog);
			}
		}
		catch (Exception e2) {
    		throw new RuntimeException("exception writing to tlog", e2);
		}
	}
	
	
	private boolean bDisconnectFromMaster;	
	public void setDisconnectFromMaster(boolean b) {
		this.bDisconnectFromMaster = b;
		if (b) {
			try {
				replClientConnection.stop();
			}
			catch (Exception e) {}
			replClientConnection = null;
		}
	}
	
	
    /**
     * returns null if connection can not be made, and is set to null if connected is stopped.
     */
    protected OAReplClientConnection getReplClientConnection() {
    	if (replClientConnection != null && !replClientConnection.isStopped()) return replClientConnection;
    	if (bDisconnectFromMaster) return null;
    	
    	LOG.fine("creating new ReplClientConnection");
    	replClientConnection = new OAReplClientConnection(guid, masterHostName, masterHostPort, lastSentMasterSeq, lastSentClientSeq) {
			@Override
			public void processMessageFromMaster(long masterSeq, String methodName, Object[] args) {
				OAReplicationClient.this.onNewMessageFromMaster(masterSeq, methodName, args);
			}
			
			@Override
			protected void onSocketException(Exception e) {
				LOG.fine("stopping connection to Master");
				try {
					this.stop();
				}
				catch (Exception e2) {}
			}
			
			@Override
			protected void onSocketClose(boolean bError) {
				LOG.fine("stopping connection to Master");
				try {
					this.stop();
				}
				catch (Exception e2) {}
			}
			
			@Override
			public void stop() throws Exception {
				OAReplicationClient.this.replClientConnection = null;
				super.stop();
			}
		};
		
		try {
			replClientConnection.start();
			LOG.fine("connection server is available"); 
		}
		catch (Exception e) {
	    	LOG.log(Level.FINE, "could not start Repl Client connection, will briefly wait and try again, exception: " + e.toString());
			this.replClientConnection = null;
		};
    	return replClientConnection;
    }

    /**
     * New message from OARepl Master.  This will add it to the queue that the OAReplicationThread
     * @param masterSeq
     * @param methodName
     * @param args
     */
    protected void onNewMessageFromMaster(long masterSeq, String methodName, Object[] args) {
    	if (remoteSyncImpl == null) {
    		remoteSyncImpl = new RemoteSyncImpl();
    	}
    	if (masterSeq < lastSentMasterSeq) {
    		return; 
    	}
    	
		final OAThreadLocalService srvcOAThreadLocal = ((OAThreadService) OARuntime.thread()).getThreadLocalService();  
		try {
			srvcOAThreadLocal.setReplicationSource(guid);
			LOG.fine(String.format("received msg from Master, masterSeq=%,d, methodName=%s", masterSeq, methodName)); 
			
			Method method = getMethod(methodName);
			method.invoke(remoteSyncImpl, args);   

			this.masterSeq = masterSeq;
		}
		catch (Exception e) {
    		throw new RuntimeException("Exception onNewMessageFromMaster", e);
		}
		finally {
			srvcOAThreadLocal.setReplicationSource(null);
		}
    }

	
	protected void loadTLogFile() throws Exception {
		String fn = OAFile.convertFileName(tlogFileName);
        File file = new File(fn);
    	LOG.fine(String.format("tlogFileName=%s, exists=%b", fn, file.exists()));
        if (!file.exists()) return;
        
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis, 64 * 1024);
        ObjectInputStream ois = new ObjectInputStream(bis);
        
    	String guidx = ois.readUTF();
        masterSeq = ois.readLong();
        lastSentMasterSeq = ois.readLong();
        clientSeq = ois.readLong();
        lastSentClientSeq = ois.readLong();
    	LOG.fine(String.format("guid=%s, masterSeq=%,d, clientSeq=%,d, lastClientSeqOnMaster=%,d", guidx, masterSeq, clientSeq, lastSentClientSeq));

    	if (OAStr.compare(guid, guidx) != 0) {
    		throw new RuntimeException(String.format("TLogFile guid=%s, does not match runtime guid=%s", guidx, guid));
    	}
    	
        int cnt = 0;
        for (; ; cnt++) {
        	OAReplTLog tlog;
        	try {
            	tlog = (OAReplTLog) ois.readObject();
            	clientSeq = tlog.getClientSeq();
            	masterSeq = tlog.getMasterSeq();
        	}
        	catch (EOFException e) {
        		break;
        	}
        	catch (IOException e) {
        		throw new RuntimeException("Exception loading TLog file", e);
        	}
    		
        	boolean bDontSend = (OAStr.equals(guid, tlog.getSource())); 
			if (!bDontSend) {
				alTLogToMaster.put(tlog);
		    	LOG.fine(String.format("%,d) guid=%s, masterSeq=%,d, clientSeq=%,d, methodName=%s", cnt+1, 
			    		guid, tlog.getMasterSeq(), tlog.getClientSeq(), tlog.getMethodName()));
			}
        }
        ois.close();
        bis.close();
        fis.close();
    	LOG.fine(String.format("tlogFileName=%s, total tlog records=%,d", fn, cnt));
	}

	protected void openTLogFile() throws Exception {
		if (objectOutputStream != null) {
			objectOutputStream.close();
		}
		
		String fn = OAFile.convertFileName(tlogFileName);
		OAFile.mkdirsForFile(fn);
        File file = new File(fn);
    	LOG.fine(String.format("tlogFileName=%s, exists=%b", fn, file.exists()));
        final boolean bAppend = file.exists() && file.length() > 0;
        fileOutputStream = new FileOutputStream(file, true); // append
        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream, 64 * 1024);
        objectOutputStream = new ObjectOutputStream(bos) {
        	@Override
            protected void writeStreamHeader() throws IOException {
                if (bAppend) reset(); // writes a TC_RESET token into the stream, avoids duplicate stream header when appending
                else super.writeStreamHeader();
            }
        };
        if (!bAppend) {
		    LOG.fine(String.format("wrote header: masterSeq=%,d, clientSeq=%,d, guid=%s", masterSeq, clientSeq, guid));
        	objectOutputStream.writeUTF(this.guid);
        	objectOutputStream.writeLong(masterSeq);
        	objectOutputStream.writeLong(lastSentMasterSeq);
        	objectOutputStream.writeLong(clientSeq);
        	objectOutputStream.writeLong(lastSentClientSeq);
	        objectOutputStream.flush();
	        fileOutputStream.getFD().sync();
        }
	}

	protected void createNewTLogFile(String fileName) {
		try {
			String fn = OAFile.convertFileName(fileName);
			OAFile.mkdirsForFile(fn);
	        File file = new File(fn);
			
	    	LOG.fine(String.format("fileName=%s, exists=%b, wasOpen=%b", fn, file.exists(), (objectOutputStream != null)));
			if (objectOutputStream != null) {
				objectOutputStream.close();
			}
	        
	        fileOutputStream = new FileOutputStream(file); 
	        BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream, 64 * 1024);
	        objectOutputStream = new ObjectOutputStream(bos);
        	objectOutputStream.writeUTF(this.guid);
        	objectOutputStream.writeLong(masterSeq);
        	objectOutputStream.writeLong(lastSentMasterSeq);
        	objectOutputStream.writeLong(clientSeq);
        	objectOutputStream.writeLong(lastSentClientSeq);
	        objectOutputStream.flush();
	        fileOutputStream.getFD().sync();

		    LOG.fine(String.format("wrote header: masterSeq=%,d, clientSeq=%,d, guid=%s", masterSeq, clientSeq, guid));
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}

	protected void writeTLog(final OAReplTLog tlog) {
    	LOG.fine(String.format(" masterSeq=%,d, clientSeq=%,d, methodName=%s", tlog.getMasterSeq(), tlog.getClientSeq(), tlog.getMethodName()));
		try {
	        objectOutputStream.writeObject(tlog);
	        objectOutputStream.flush();
	        fileOutputStream.getFD().sync();
		}
		catch (Exception e) {
			throw new RuntimeException("exception appending to tlog file", e);
		}
	}
}
