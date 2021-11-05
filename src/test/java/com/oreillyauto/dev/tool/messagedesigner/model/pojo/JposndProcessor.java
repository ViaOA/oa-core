package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class JposndProcessor implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
     
    // References to other objects.
    protected ProcessorStatus processorStatus;
    protected ArrayList<RpgMessage> alRpgMessages;
     
     
    public JposndProcessor() {
    }
     
    public JposndProcessor(int id) {
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
     
    public ProcessorStatus getProcessorStatus() {
        return processorStatus;
    }
    
    public void setProcessorStatus(ProcessorStatus newValue) {
        this.processorStatus = newValue;
    }
    
     
    public ArrayList<RpgMessage> getRpgMessages() {
        if (alRpgMessages == null) {
            alRpgMessages = new ArrayList<RpgMessage>();
        }
        return alRpgMessages;
    }
    
     
}
 
