package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class MessageTypeChange implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected int type;
    public static final int TYPE_Unknown = 0;
    public static final int TYPE_AddRecord = 1;
    public static final int TYPE_RemoveRecord = 2;
    public static final int TYPE_ChangeRecord = 3;
    public static final int TYPE_AddColumn = 4;
    public static final int TYPE_RemoveColumn = 5;
    public static final int TYPE_ChangeColumn = 6;
    protected String name;
    protected String description;
     
    // References to other objects.
    protected MessageType messageType;
     
     
    public MessageTypeChange() {
    }
     
    public MessageTypeChange(int id) {
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
     
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        this.type = newValue;
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
     
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType newValue) {
        this.messageType = newValue;
    }
    
     
}
 
