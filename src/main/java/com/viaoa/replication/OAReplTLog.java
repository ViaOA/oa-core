package com.viaoa.replication;

import java.io.Serializable;

import com.viaoa.datetime.OADateTime;

/**
 * Serializable transaction-log record used by OA replication.
 * <p>
 * A record captures the source participant, timestamp, master/client sequence numbers, replicated remote-sync method,
 * and method arguments needed for durable replay or forwarding.
 * </p>
 */
public class OAReplTLog implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
 * Participant/source identifier that created this log record.
 */
protected String source;
	/**
 * Timestamp assigned when the replication record was created.
 */
protected OADateTime dt;
	/**
 * Master-side sequence number associated with this record.
 */
protected long masterSeq;
	/**
 * Client-side sequence number associated with this record.
 */
protected long clientSeq;
	/**
 * Remote sync method name to invoke during replay or forwarding.
 */
protected String methodName;
	/**
 * Arguments for the replicated remote sync method.
 */
protected Object[] args;
	
	/**
	 * Creates a transaction-log record.
	 *
	 * @param source participant/source identifier
	 * @param dt record timestamp
	 * @param masterSeq master-side sequence number
	 * @param clientSeq client-side sequence number
	 * @param methodName remote sync method name
	 * @param args remote sync method arguments
	 */
	public OAReplTLog(String source, OADateTime dt, long masterSeq, long clientSeq, String methodName, Object[] args) {
		super();
		this.source = source;
		this.dt = dt;
		this.masterSeq = masterSeq;
		this.clientSeq = clientSeq;
		this.methodName = methodName;
		this.args = args;
	}
	
	/**
	 * Returns the source participant identifier.
	 * @return source participant identifier
	 */
	public String getSource() {
		return source;
	}
	/**
	 * Sets the source participant identifier.
	 * @param source value to assign
	 */
	public void setSource(String source) {
		this.source = source;
	}
	
	/**
	 * Returns the record timestamp.
	 * @return record timestamp
	 */
	public OADateTime getDt() {
		return dt;
	}
	/**
	 * Sets the record timestamp.
	 * @param dt value to assign
	 */
	public void setDt(OADateTime dt) {
		this.dt = dt;
	}
	/**
	 * Returns the master-side sequence number.
	 * @return master-side sequence number
	 */
	public long getMasterSeq() {
		return masterSeq;
	}
	/**
	 * Sets the master-side sequence number.
	 * @param seq value to assign
	 */
	public void setMasterSeq(long seq) {
		this.masterSeq = seq;
	}
	/**
	 * Returns the client-side sequence number.
	 * @return client-side sequence number
	 */
	public long getClientSeq() {
		return clientSeq;
	}
	/**
	 * Sets the client-side sequence number.
	 * @param seq value to assign
	 */
	public void setClientSeq(long seq) {
		this.clientSeq = seq;
	}
	/**
	 * Returns the remote sync method name.
	 * @return remote sync method name
	 */
	public String getMethodName() {
		return methodName;
	}
	/**
	 * Sets the remote sync method name.
	 * @param methodName value to assign
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	/**
	 * Returns the remote sync method arguments.
	 * @return remote sync method arguments
	 */
	public Object[] getArgs() {
		return args;
	}
	/**
	 * Sets the remote sync method arguments.
	 * @param args value to assign
	 */
	public void setArgs(Object[] args) {
		this.args = args;
	}
	
}
