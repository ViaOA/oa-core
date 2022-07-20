package com.messagedesigner.model.pojo;
 
import java.util.*;

import com.messagedesigner.model.pojo.MessageTypeRecord;

import java.time.LocalDateTime;
 
public class RpgProgram implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected String name;
     
    // References to other objects.
    protected ArrayList<MessageTypeRecord> alMessageTypeRecords;
     
     
    public RpgProgram() {
    }
     
    public RpgProgram(int id) {
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
     
    public ArrayList<MessageTypeRecord> getMessageTypeRecords() {
        if (alMessageTypeRecords == null) {
            alMessageTypeRecords = new ArrayList<MessageTypeRecord>();
        }
        return alMessageTypeRecords;
    }
    
     
}
 
