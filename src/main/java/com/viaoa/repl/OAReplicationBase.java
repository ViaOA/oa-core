package com.viaoa.repl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.remote.info.RequestInfo;
import com.viaoa.sync.OASyncServer;
import com.viaoa.util.OACircularQueue;

public abstract class OAReplicationBase {
    private static Logger LOG = Logger.getLogger(OAReplicationBase.class.getName());
	
	private final OASyncServer syncServer;
	public static final int qid = 7777777;
	private OACircularQueue<RequestInfo> cque;
	private final Object lock = new Object();
	protected volatile boolean bStop;
	
    public OAReplicationBase(OASyncServer syncServer) {
    	this.syncServer = syncServer;
    }
    
    
    // capture all sync messages created on this server.
    public void start() {
    	final String qname = OASyncServer.SyncQueueName;

    	cque = syncServer.getRemoteMultiplexerServer().getCircularQueue(qname);
        final long qposInitial = cque.registerSession(qid);
    	
        final String threadName = "OAReplication.circQue." + qname;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
            	long qpos = qposInitial;
                try {
                    for (int i=0;;i++) {
                        RequestInfo[] ris = null;
                        try {
                            ris = cque.getMessages(qid, qpos, 100, 2000);
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
                            
                            OAReplicationBase.this.onNewRequestInfo(qpos, ri);
                            
                            String s = String.format("%,d ) " + ri.toLogString());
                            System.out.println(s);
                            
                        }                    	
                    }
                }
                catch (Exception e) {
	                String s = "async queue thread exception, thread=" + threadName + ", thread is stopping, "
                        + "which will stop message from being sent to this OAReplication, queue=" + qname;
	                LOG.log(Level.WARNING, s, e);
                }
            }
        });
        t.setName(threadName);
        t.setDaemon(true);
        t.start();
    }
    
    
    
    public void stop() throws Exception {
    	synchronized (lock) {
    		bStop = true;
    	}
        cque.unregisterSession(qid);
        
    }
    
    
    public abstract void onNewRequestInfo(long qpos, RequestInfo ri); 
    
    
}
