package com.viaoa.repl;

import java.io.Serializable;

import com.viaoa.util.OADateTime;

public class OAReplTLog implements Serializable {

	private static final long serialVersionUID = 1L;
	
	protected OADateTime dt;
	protected long masterSeq;
	protected long clientSeq;
	protected String methodName;
	protected Object[] args;
	
	public OAReplTLog(OADateTime dt, long masterSeq, long clientSeq, String methodName, Object[] args) {
		super();
		this.dt = dt;
		this.masterSeq = masterSeq;
		this.clientSeq = clientSeq;
		this.methodName = methodName;
		this.args = args;
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
		this.masterSeq = masterSeq;
	}
	public long getClientSeq() {
		return clientSeq;
	}
	public void setClientSeq(long seq) {
		this.clientSeq = clientSeq;
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
