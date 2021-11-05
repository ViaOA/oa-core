package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class JsonType implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected String name;
    protected int type;
    public static final int TYPE_Unassigned = 0;
    public static final int TYPE_String = 1;
    public static final int TYPE_Number = 2;
    public static final int TYPE_Boolean = 3;
    public static final int TYPE_Array = 4;
    public static final int TYPE_DateTime = 5;
    public static final int TYPE_Date = 6;
    public static final int TYPE_Time = 7;
    public static final int TYPE_Timestamp = 8;
    public static final int TYPE_Other = 9;
    protected int seq;
     
    // References to other objects.
     
     
    public JsonType() {
    }
     
    public JsonType(int id) {
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
     
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        this.type = newValue;
    }
     
    public int getSeq() {
        return seq;
    }
    public void setSeq(int newValue) {
        this.seq = newValue;
    }
     
}
 
