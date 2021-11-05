package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class RpgType implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected String name;
    protected int encodeType;
    public static final int ENCODETYPE_None = 0;
    public static final int ENCODETYPE_PackedDecimal = 1;
    public static final int ENCODETYPE_ZonedDecimal = 2;
    public static final int ENCODETYPE_Integer = 3;
    public static final int ENCODETYPE_Float = 4;
    protected int defaultSize;
    protected String defaultFormat;
    protected int nullValueType;
    public static final int NULLVALUETYPE_Default = 0;
    public static final int NULLVALUETYPE_Zero = 1;
    public static final int NULLVALUETYPE_Empty = 2;
    protected String note;
    protected String samples;
    protected int seq;
     
    // References to other objects.
    protected JsonType jsonType;
     
     
    public RpgType() {
    }
     
    public RpgType(int id) {
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
     
    public int getEncodeType() {
        return encodeType;
    }
    public void setEncodeType(int newValue) {
        this.encodeType = newValue;
    }
     
    public int getDefaultSize() {
        return defaultSize;
    }
    public void setDefaultSize(int newValue) {
        this.defaultSize = newValue;
    }
     
    public String getDefaultFormat() {
        return defaultFormat;
    }
    public void setDefaultFormat(String newValue) {
        this.defaultFormat = newValue;
    }
     
    public int getNullValueType() {
        return nullValueType;
    }
    public void setNullValueType(int newValue) {
        this.nullValueType = newValue;
    }
     
    public String getNote() {
        return note;
    }
    public void setNote(String newValue) {
        this.note = newValue;
    }
     
    public String getSamples() {
        return samples;
    }
    public void setSamples(String newValue) {
        this.samples = newValue;
    }
     
    public int getSeq() {
        return seq;
    }
    public void setSeq(int newValue) {
        this.seq = newValue;
    }
     
    public JsonType getJsonType() {
        return jsonType;
    }
    
    public void setJsonType(JsonType newValue) {
        this.jsonType = newValue;
    }
    
     
}
 
