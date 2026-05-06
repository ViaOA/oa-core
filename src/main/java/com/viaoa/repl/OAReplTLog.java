package com.viaoa.repl;

import java.io.Serializable;

import com.viaoa.datetime.OADateTime;

public class OAReplTLog implements Serializable {

	private static final long serialVersionUID = 1L;
	
	protected String source;
	protected OADateTime dt;
	protected long masterSeq;
	protected long clientSeq;
	protected String methodName;
	protected Object[] args;
	
	public OAReplTLog(String source, OADateTime dt, long masterSeq, long clientSeq, String methodName, Object[] args) {
		super();
		this.source = source;
		this.dt = dt;
		this.masterSeq = masterSeq;
		this.clientSeq = clientSeq;
		this.methodName = methodName;
		this.args = args;
	}
	
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	
	public OADateTime getDt() {
		return dt;
	}
	public void setDt(OADateTime dt) {
		this.dt = dt;
	}
	public long getMasterSeq() {
		return masterSeq;
	}
	public void setMasterSeq(long seq) {
		this.masterSeq = seq;
	}
	public long getClientSeq() {
		return clientSeq;
	}
	public void setClientSeq(long seq) {
		this.clientSeq = seq;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	
}
