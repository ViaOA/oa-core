package com.messagedesigner.model.pojo;
 
import java.util.*;

import com.messagedesigner.delegate.oa.MessageTypeDelegate;
import com.messagedesigner.model.pojo.MessageGroup;
import com.messagedesigner.model.pojo.MessageRecord;
import com.messagedesigner.model.pojo.MessageTypeChange;

import java.time.LocalDateTime;
 
public class MessageType implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected String name;
    protected String description;
    protected int commonColumnCount;
    protected String note;
    protected boolean followUp;
    protected LocalDateTime verified;
    protected int seq;
     
    // References to other objects.
    protected ArrayList<MessageGroup> alMessageGroups;
    protected ArrayList<MessageRecord> alMessageRecords;
    protected ArrayList<MessageTypeChange> alMessageTypeChanges;
     
     
    public MessageType() {
    }
     
    public MessageType(int id) {
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
     
    public String getDescription() {
        return description;
    }
    public void setDescription(String newValue) {
        this.description = newValue;
    }
     
    public int getCommonColumnCount() {
        return commonColumnCount;
    }
    public void setCommonColumnCount(int newValue) {
        this.commonColumnCount = newValue;
    }
     
    public String getNote() {
        return note;
    }
    public void setNote(String newValue) {
        this.note = newValue;
    }
     
    public boolean getFollowUp() {
        return followUp;
    }
    public void setFollowUp(boolean newValue) {
        this.followUp = newValue;
    }
     
    public LocalDateTime getVerified() {
        return verified;
    }
    public void setVerified(LocalDateTime newValue) {
        this.verified = newValue;
    }
     
    public int getSeq() {
        return seq;
    }
    public void setSeq(int newValue) {
        this.seq = newValue;
    }
     
    public ArrayList<MessageGroup> getMessageGroups() {
        if (alMessageGroups == null) {
            alMessageGroups = new ArrayList<MessageGroup>();
        }
        return alMessageGroups;
    }
    
     
    public ArrayList<MessageRecord> getMessageRecords() {
        if (alMessageRecords == null) {
            alMessageRecords = new ArrayList<MessageRecord>();
        }
        return alMessageRecords;
    }
    
     
    public ArrayList<MessageTypeChange> getMessageTypeChanges() {
        if (alMessageTypeChanges == null) {
            alMessageTypeChanges = new ArrayList<MessageTypeChange>();
        }
        return alMessageTypeChanges;
    }
    
     
}
 
