package com.viaoa.replication;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraph;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.queue.OACircularQueue;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.serialize.OAObjectSerializer;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.remote.RemoteSyncInterface;


/*qqqqqqqqqqqqqqqqqqqqq
CODEX

1. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationBase.java:92 processQueue

  concrete bug
  The circular queue position is advanced before onNewSyncMessage(ri) successfully commits the replication record.

  runtime scenario
  A sync RequestInfo is read, aiQueuePos.incrementAndGet() runs, then onNewSyncMessage() throws while writing the
  replication TLog. The replication thread logs and stops. On restart, the queue session starts from a newer/head
  position, so the failed record can be skipped.

  why this violates OA/OG replication semantics
  Replication observes the sync event as consumed before the replication durable/observable commit point. A write
  failure can become silent lost replication.

  minimal fix direction
  Advance aiQueuePos only after non-filtered message handling succeeds, or retain/retry the same queue position on
  failure.

  suggested CODEX comment location
  Near aiQueuePos.incrementAndGet() before onNewSyncMessage(ri).

  suggested regression test
  testReplicationQueuePositionDoesNotAdvanceWhenTLogWriteFails

  2. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationBase.java:64 processQueue

  concrete bug
  If the replication queue thread exits by exception, the circular queue session is not unregistered.

  runtime scenario
  getMessages() overrun or onNewSyncMessage() failure throws. The catch logs and exits, but
  cqueSync.unregisterSession(SyncQueueSessionId) is only called from stop().

  why this violates OA/OG replication semantics
  A dead replication consumer can remain registered and hold circular queue low-position/session state, which can
  affect queue cleanup and later replication restarts.

  minimal fix direction
  Register/unregister the session inside processQueue() with a finally, while keeping stop() idempotent.

  suggested CODEX comment location
  Around registerSession(...) and the outer catch/final exit path.


4. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationBase.java:47 start

  concrete bug
  start() starts the queue thread before clearing bStop.

  runtime scenario
  Replication is stopped, so bStop == true. A later start() creates and starts the queue thread. The new thread can
  enter processQueue(), register the circular queue session, then immediately exit because bStop is still true. After
  that, start() sets bStarted = true and bStop = false, so replication appears started with no live queue capture
  thread.

  why this violates OA/OG replication semantics
  Restart can silently disable replication capture while lifecycle state says it is running.

  minimal fix direction
  Set bStop = false before starting the thread, under the lifecycle lock.

  suggested CODEX comment location
  At the top of OAReplicationBase.start() before t.start().



 5. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationBase.java:142 stop

  concrete bug
  stop() can dereference cqueSync before the queue thread initializes it.

  runtime scenario
  Caller invokes start() and immediately invokes stop(). bStarted is already true, but the background thread has not
  yet assigned cqueSync. stop() calls cqueSync.unregisterSession(...) and throws NullPointerException.

  why this violates OA/OG replication semantics
  A normal start/stop race can leave replication lifecycle cleanup incomplete and caller-visible as an unrelated NPE
  instead of a clean stop.

  minimal fix direction
  Guard cqueSync != null, or move queue registration into synchronous startup before marking started.

  suggested CODEX comment location
  Inside stop() before cqueSync.unregisterSession(...).

3. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationBase.java:250 getMethod

  concrete bug
  The method cache is a plain HashMap lazily populated without synchronization.

  runtime scenario
  Replication client receives concurrent master remote calls, or master/client processing reaches getMethod()
  concurrently during startup. Both threads see the map empty and mutate hmNameToMethod at the same time.

  why this violates OA/OG replication semantics
  Remote sync dispatch can race during method-cache initialization, causing wrong/null method lookup or HashMap
  corruption. That can make valid replication messages fail or apply unpredictably.

  minimal fix direction
  Initialize the method map eagerly, or synchronize/cache with a thread-safe publication pattern.

  suggested CODEX comment location
  At getMethod(String name) before the if (hmNameToMethod.isEmpty()) block.


4. file/class/method
     src/main/java/com/viaoa/replication/OAReplicationClient.java:486 loadTLogFile and src/main/java/com/viaoa/
     replication/OAReplicationMaster.java:632 loadTLogFile

  concrete bug
  Input streams are not closed if TLog loading fails before normal completion.

  runtime scenario
  A TLog file has a bad header, mismatched client guid, or an IOException while reading a record. The method throws
  before reaching ois.close()/fis.close().

  why this violates OA/OG replication semantics
  Startup/retry after a bad or partial TLog can leak the file handle. On some platforms this can block log
  replacement/truncation recovery and make replication restart/recovery fail repeatedly.

  minimal fix direction
  Use try-with-resources for FileInputStream, BufferedInputStream, and ObjectInputStream.

  suggested CODEX comment location
  At the stream creation block in both loadTLogFile() methods.


*/


public abstract class OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationBase.class.getName());
	
	protected final OASyncServer syncServer;
	protected OACircularQueue<RequestInfo> cqueSync;
	private static final int SyncQueueSessionId = 7777777; // wont be used by others
	private final Object lock = new Object();
	
	
    protected volatile boolean bStarted;
	protected volatile boolean bStop;

	
	private final Map<String, Method> hmNameToMethod = new HashMap<>();
	
    public OAReplicationBase(OASyncServer syncServer) {
    	this.syncServer = syncServer;
    }
    
    
    protected AtomicLong aiQueuePos = new AtomicLong(); 
    public long getCirularQueuePos() {
    	return aiQueuePos.get();
    }
    
    
    // capture all sync messages created on this server's circ queue.
    public void start() throws Exception {
        final String threadName = "OAReplication.getSyncMsgs";
    	LOG.fine("starting thread="+threadName);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
            	OAReplicationBase.this.processQueue();
            }
        });
        t.setName(threadName);
        t.setDaemon(true);
        t.start();
    	LOG.fine("thread started to capture Sync messages, thread name="+threadName);
    	bStarted = true;
    	bStop = false;
    }
    
    protected void processQueue() {
    	final String qname = OASyncServer.SyncQueueName;

    	cqueSync = syncServer.getRemoteMultiplexerServer().getCircularQueue(qname);
        final long qposInitial = cqueSync.registerSession(SyncQueueSessionId);
    	LOG.fine("qposInitial="+qposInitial);

    	aiQueuePos.set(qposInitial);
        try {
            long cnt = 0;
        	for (int i=0; !bStop; i++) {
                RequestInfo[] ris = null;
                try {
                    ris = cqueSync.getMessages(SyncQueueSessionId, aiQueuePos.get(), 100, 2000);
                }
                catch (Exception e) {
                    LOG.log(Level.WARNING, "Message queue overrun with OAReplication, circularQueue=" + qname, e);
                    throw e;
                }
            	synchronized (lock) {
            		if (bStop) break;
            	}
                if (ris == null) {
                    continue;
                }

                
                
                for (RequestInfo ri : ris) {
                	aiQueuePos.incrementAndGet();
                    if (!ri.bind.isOASync) continue;
                    
                    if (ri.args != null) {
                        boolean bFound = false;
                        boolean bUse = false;
	                    for (Object arg : ri.args) {
	                    	if (arg instanceof OAObjectSerializer) {
	                    		arg = ((OAObjectSerializer) arg).getObject();
	                    	}
	                    	if (arg != null) {
	                    		Class c = arg.getClass();
	                    		if (OAObject.class.isAssignableFrom(c)) {
	                    			arg = c;
	                    		}
	                    	}
	                    	
	                    	if (arg instanceof Class) {
	                    		Class<?> c = (Class<?>) arg;
	                    		if (OAObject.class.isAssignableFrom(c)) {
	                    			bFound = true;
									final OAGraph og = OARuntime.graph(c);
									OAObjectInfo oi = og.internal().objects().info().getOAObjectInfo(c);
	                    			bUse |= (oi.getUseDataSource() && !oi.getLocalOnly());
	                    		}
	                    	}
	                    }
	                    if (bFound && !bUse) {
	                    	continue;
	                    }
                    }
                    
                    cnt++;
                    OAReplicationBase.this.onNewSyncMessage(ri);
                    
                    String s = String.format("%,d ) %s", cnt, ri.toLogString());
                    LOG.fine("OAReplicationBase, new Sync message from queue, "+ s);
                }                    	
            }
        }
        catch (Exception e) {
            String s = "async queue thread exception, thread is stopping, "
                + "which will stop message from being sent to this OAReplication, queue=" + qname;
            LOG.log(Level.WARNING, s, e);
        }
    	LOG.fine("Thread for OAReplicationBase stopped");
    }
    
    
    public void stop() throws Exception {
    	LOG.fine("stop called");
    	if (bStop || !bStarted) return;
    	synchronized (lock) {
    		bStop = true;
    		bStarted = false;
    	}
        cqueSync.unregisterSession(SyncQueueSessionId);
    }

    
	protected Method getMethod(String name) {
		if (hmNameToMethod.isEmpty()) {
			Method[] methods = RemoteSyncInterface.class.getMethods();
			for (Method method : methods) {
				if (hmNameToMethod.containsKey(method.getName())) throw new RuntimeException("Overloaded method names in RemoteSyncInterface is not supported");
				hmNameToMethod.put(method.getName(), method);
		    	LOG.fine("methodName=" + method.getName());
			}
		}
		return hmNameToMethod.get(name);
	}
    
    
    protected abstract void onNewSyncMessage(RequestInfo ri); 
}
