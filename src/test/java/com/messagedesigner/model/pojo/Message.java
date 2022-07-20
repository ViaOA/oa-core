package com.messagedesigner.model.pojo;
 
import java.util.*;

import com.messagedesigner.model.pojo.MessageType;
import com.messagedesigner.model.pojo.RpgMessage;

import java.time.LocalDateTime;
 
public class Message implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected String json;
    protected LocalDateTime processed;
    protected LocalDateTime cancelled;
    protected long seqNumber;
     
    // References to other objects.
    protected MessageType messageType;
    protected ArrayList<RpgMessage> alRpgMessages;
     
     
    public Message() {
    }
     
    public Message(int id) {
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
     
    public long getSeqNumber() {
        return seqNumber;
    }
    public void setSeqNumber(long newValue) {
        this.seqNumber = newValue;
    }
     
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType newValue) {
        this.messageType = newValue;
    }
    
     
    public ArrayList<RpgMessage> getRpgMessages() {
        if (alRpgMessages == null) {
            alRpgMessages = new ArrayList<RpgMessage>();
        }
        return alRpgMessages;
    }
    
     
}
 
