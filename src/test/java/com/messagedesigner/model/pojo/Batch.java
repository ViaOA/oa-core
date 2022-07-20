package com.messagedesigner.model.pojo;
 
import java.util.*;

import com.messagedesigner.model.pojo.MessageBatchProcessor;
import com.messagedesigner.model.pojo.PypeProcessor;

import java.time.LocalDateTime;
 
public class Batch implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected LocalDateTime sessionStart;
    protected int sessionSequenceNumber;
    protected LocalDateTime previousSessionStart;
    protected int previousSessionSequenceNumber;
     
    // References to other objects.
    protected MessageBatchProcessor messageBatchProcessor;
    protected PypeProcessor pypeProcessor;
     
     
    public Batch() {
    }
     
    public Batch(int id) {
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
     
    public LocalDateTime getSessionStart() {
        return sessionStart;
    }
    public void setSessionStart(LocalDateTime newValue) {
        this.sessionStart = newValue;
    }
     
    public int getSessionSequenceNumber() {
        return sessionSequenceNumber;
    }
    public void setSessionSequenceNumber(int newValue) {
        this.sessionSequenceNumber = newValue;
    }
     
    public LocalDateTime getPreviousSessionStart() {
        return previousSessionStart;
    }
    public void setPreviousSessionStart(LocalDateTime newValue) {
        this.previousSessionStart = newValue;
    }
     
    public int getPreviousSessionSequenceNumber() {
        return previousSessionSequenceNumber;
    }
    public void setPreviousSessionSequenceNumber(int newValue) {
        this.previousSessionSequenceNumber = newValue;
    }
     
    public MessageBatchProcessor getMessageBatchProcessor() {
        return messageBatchProcessor;
    }
    
    public void setMessageBatchProcessor(MessageBatchProcessor newValue) {
        this.messageBatchProcessor = newValue;
    }
    
     
    public PypeProcessor getPypeProcessor() {
        return pypeProcessor;
    }
    
    public void setPypeProcessor(PypeProcessor newValue) {
        this.pypeProcessor = newValue;
    }
    
     
}
 
