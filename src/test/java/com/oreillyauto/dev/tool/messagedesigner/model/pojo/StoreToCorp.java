package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class StoreToCorp implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected int storeNumber;
     
    // References to other objects.
    protected JposndProcessor jposndProcessor;
    protected MessageBatchProcessor messageBatchProcessor;
    protected MessageRecordProcessor messageRecordProcessor;
    protected MessageReorderProcessor messageReorderProcessor;
    protected ProcessorStatus processorStatus;
    protected PypeProcessor pypeProcessor;
    protected SaveMessageProcessor saveMessageProcessor;
     
     
    public StoreToCorp() {
    }
     
    public StoreToCorp(int id) {
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
     
    public int getStoreNumber() {
        return storeNumber;
    }
    public void setStoreNumber(int newValue) {
        this.storeNumber = newValue;
    }
     
    public JposndProcessor getJposndProcessor() {
        return jposndProcessor;
    }
    
    public void setJposndProcessor(JposndProcessor newValue) {
        this.jposndProcessor = newValue;
    }
    
     
    public MessageBatchProcessor getMessageBatchProcessor() {
        return messageBatchProcessor;
    }
    
    public void setMessageBatchProcessor(MessageBatchProcessor newValue) {
        this.messageBatchProcessor = newValue;
    }
    
     
    public MessageRecordProcessor getMessageRecordProcessor() {
        return messageRecordProcessor;
    }
    
    public void setMessageRecordProcessor(MessageRecordProcessor newValue) {
        this.messageRecordProcessor = newValue;
    }
    
     
    public MessageReorderProcessor getMessageReorderProcessor() {
        return messageReorderProcessor;
    }
    
    public void setMessageReorderProcessor(MessageReorderProcessor newValue) {
        this.messageReorderProcessor = newValue;
    }
    
     
    public ProcessorStatus getProcessorStatus() {
        return processorStatus;
    }
    
    public void setProcessorStatus(ProcessorStatus newValue) {
        this.processorStatus = newValue;
    }
    
     
    public PypeProcessor getPypeProcessor() {
        return pypeProcessor;
    }
    
    public void setPypeProcessor(PypeProcessor newValue) {
        this.pypeProcessor = newValue;
    }
    
     
    public SaveMessageProcessor getSaveMessageProcessor() {
        return saveMessageProcessor;
    }
    
    public void setSaveMessageProcessor(SaveMessageProcessor newValue) {
        this.saveMessageProcessor = newValue;
    }
    
     
}
 
