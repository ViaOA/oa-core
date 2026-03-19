package com.viaoa.repl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.remote.info.RequestInfo;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.remote.RemoteSyncInterface;
import com.viaoa.util.OACircularQueue;

public abstract class OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationBase.class.getName());
	
	protected final OASyncServer syncServer;
	public static final int ReplSessionId = 7777777; // wont be used by others
	private OACircularQueue<RequestInfo> cque;
	private final Object lock = new Object();
	protected volatile boolean bStop;
	private final Map<String, Method> hmNameToMethod = new HashMap<>();
	
    public OAReplicationBase(OASyncServer syncServer) {
    	this.syncServer = syncServer;
    }
    
    // capture all sync messages created on this server circ queue.
    public void start() throws Exception {
    	LOG.fine("starting");
    	final String qname = OASyncServer.SyncQueueName;

    	cque = syncServer.getRemoteMultiplexerServer().getCircularQueue(qname);
        final long qposInitial = cque.registerSession(ReplSessionId);
    	LOG.fine("qposInitial="+qposInitial);

        final String threadName = "OAReplication.getSyncRemoteMsgs";
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
            	long qpos = qposInitial;
                try {
                    for (int i=0;;i++) {
                        RequestInfo[] ris = null;
                        try {
                            ris = cque.getMessages(ReplSessionId, qpos, 100, 2000);
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
                            qpos++;
                            if (!ri.bind.isOASync) continue;

                            OAReplicationBase.this.onNewRequestInfoMessage(qpos, ri);
                            
                            String s = String.format("%,d ) %s", qpos, ri.toLogString());
                            LOG.fine("OAReplicationBase, new Sync message from sync que, "+ s);
                        }                    	
                    }
                }
                catch (Exception e) {
	                String s = "async queue thread exception, thread=" + threadName + ", thread is stopping, "
                        + "which will stop message from being sent to this OAReplication, queue=" + qname;
	                LOG.log(Level.WARNING, s, e);
                }
            	LOG.fine("Thread for OAReplicationBase stopped, thread name="+threadName);
            }
        });
        t.setName(threadName);
        t.setDaemon(true);
        t.start();
    	LOG.fine("thread started to capture Sync messages, thread name="+threadName);
    }
    
    
    
    public void stop() throws Exception {
    	LOG.fine("stop called");
    	synchronized (lock) {
    		bStop = true;
    	}
        cque.unregisterSession(ReplSessionId);
    }

    
	protected Method getMethod(String name) {
		if (hmNameToMethod.isEmpty()) {
			Method[] methods = RemoteSyncInterface.class.getMethods();
			for (Method method : methods) {
				int sig = 0; // create a dummy signature, to recognize method overloading
				if (hmNameToMethod.containsKey(method.getName())) throw new RuntimeException("Overloaded method names in RemoteSyncInterface is not supported");
				hmNameToMethod.put(method.getName(), method);
		    	LOG.fine("methodName=" + method.getName());
			}
		}
		return hmNameToMethod.get(name);
	}
    
    
    protected abstract void onNewRequestInfoMessage(long qpos, RequestInfo ri); 
}
