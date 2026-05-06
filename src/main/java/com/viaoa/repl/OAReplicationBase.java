package com.viaoa.repl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.graph.OAGraphInternal;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.queue.OACircularQueue;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.serialize.OAObjectSerializer;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.remote.RemoteSyncInterface;

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
									final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(c);
									OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(c);
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
