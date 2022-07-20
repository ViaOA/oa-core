package com.messagedesigner.model.pojo;
 
import java.util.*;

import com.messagedesigner.model.pojo.Message;
import com.messagedesigner.model.pojo.ProcessorStatus;

import java.time.LocalDateTime;
 
public class MessageReorderProcessor implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
     
    // References to other objects.
    protected ArrayList<Message> alMessages;
    protected ProcessorStatus processorStatus;
     
     
    public MessageReorderProcessor() {
    }
     
    public MessageReorderProcessor(int id) {
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
     
    public ArrayList<Message> getMessages() {
        if (alMessages == null) {
            alMessages = new ArrayList<Message>();
        }
        return alMessages;
    }
    
     
    public ProcessorStatus getProcessorStatus() {
        return processorStatus;
    }
    
    public void setProcessorStatus(ProcessorStatus newValue) {
        this.processorStatus = newValue;
    }
    
     
}
 
