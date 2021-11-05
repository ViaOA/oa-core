package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class PypeProcessor implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
     
    // References to other objects.
    protected ArrayList<Batch> alBatches;
    protected ProcessorStatus processorStatus;
     
     
    public PypeProcessor() {
    }
     
    public PypeProcessor(int id) {
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
     
    public ArrayList<Batch> getBatches() {
        if (alBatches == null) {
            alBatches = new ArrayList<Batch>();
        }
        return alBatches;
    }
    
     
    public ProcessorStatus getProcessorStatus() {
        return processorStatus;
    }
    
    public void setProcessorStatus(ProcessorStatus newValue) {
        this.processorStatus = newValue;
    }
    
     
}
 
