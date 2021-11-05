package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class MessageGroup implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected String name;
     
    // References to other objects.
    protected ArrayList<MessageRecord> alMessageRecords;
     
     
    public MessageGroup() {
    }
     
    public MessageGroup(int id) {
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
     
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        this.name = newValue;
    }
     
    public ArrayList<MessageRecord> getMessageRecords() {
        if (alMessageRecords == null) {
            alMessageRecords = new ArrayList<MessageRecord>();
        }
        return alMessageRecords;
    }
    
     
}
 
