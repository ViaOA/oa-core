package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class MessageRecord implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected int relationshipType;
    public static final int RELATIONSHIPTYPE_One = 0;
    public static final int RELATIONSHIPTYPE_ZeroOrOne = 1;
    public static final int RELATIONSHIPTYPE_ZeroOrMore = 2;
    public static final int RELATIONSHIPTYPE_OneOrMore = 3;
    public static final int RELATIONSHIPTYPE_Two = 4;
    public static final int RELATIONSHIPTYPE_Three = 5;
    public static final int RELATIONSHIPTYPE_Four = 6;
    protected int seq;
     
    // References to other objects.
    protected MessageGroup messageGroup;
    protected MessageTypeRecord messageTypeRecord;
     
     
    public MessageRecord() {
    }
     
    public MessageRecord(int id) {
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
     
    public int getRelationshipType() {
        return relationshipType;
    }
    public void setRelationshipType(int newValue) {
        this.relationshipType = newValue;
    }
     
    public int getSeq() {
        return seq;
    }
    public void setSeq(int newValue) {
        this.seq = newValue;
    }
     
    public MessageGroup getMessageGroup() {
        return messageGroup;
    }
    
    public void setMessageGroup(MessageGroup newValue) {
        this.messageGroup = newValue;
    }
    
     
    public MessageTypeRecord getMessageTypeRecord() {
        return messageTypeRecord;
    }
    
    public void setMessageTypeRecord(MessageTypeRecord newValue) {
        this.messageTypeRecord = newValue;
    }
    
     
}
 
