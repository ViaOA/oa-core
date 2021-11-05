package com.oreillyauto.dev.tool.messagedesigner.model.pojo;

import java.time.LocalDateTime;

public class RpgMessage implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	protected int id;
	protected LocalDateTime created;
	protected String code;
	protected byte[] binary;
	protected String json;
	protected LocalDateTime processed;
	protected LocalDateTime cancelled;
	protected String error;

	// References to other objects.
	protected Message message;
	protected MessageTypeRecord messageTypeRecord;

	public RpgMessage() {
	}

	public RpgMessage(int id) {
		this();
		setId(id);
	}

	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		this.id = newValue;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public void setCreated(LocalDateTime newValue) {
		this.created = newValue;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String newValue) {
		this.code = newValue;
	}

	public byte[] getBinary() {
		return binary;
	}

	public void setBinary(byte[] newValue) {
		this.binary = newValue;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String newValue) {
		this.json = newValue;
	}

	public LocalDateTime getProcessed() {
		return processed;
	}

	public void setProcessed(LocalDateTime newValue) {
		this.processed = newValue;
	}

	public LocalDateTime getCancelled() {
		return cancelled;
	}

	public void setCancelled(LocalDateTime newValue) {
		this.cancelled = newValue;
	}

	public String getError() {
		return error;
	}

	public void setError(String newValue) {
		this.error = newValue;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message newValue) {
		this.message = newValue;
	}

	public MessageTypeRecord getMessageTypeRecord() {
		return messageTypeRecord;
	}

	public void setMessageTypeRecord(MessageTypeRecord newValue) {
		this.messageTypeRecord = newValue;
	}

}
