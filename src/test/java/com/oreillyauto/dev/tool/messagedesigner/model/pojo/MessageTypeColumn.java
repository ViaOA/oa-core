package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
 
public class MessageTypeColumn implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected String name;
    protected String rpgName;
    protected boolean key;
    protected int keyPos;
    protected int fromPos;
    protected int toPos;
    protected int size;
    protected String description;
    protected int decimalPlaces;
    protected String format;
    protected int specialType;
    public static final int SPECIALTYPE_Unknown = 0;
    public static final int SPECIALTYPE_NewInvoice = 1;
    public static final int SPECIALTYPE_InvoiceChange = 2;
    public static final int SPECIALTYPE_newPurchaseOrder = 3;
    public static final int SPECIALTYPE_purchaseOrderChange = 4;
    protected int nullValueType;
    public static final int NULLVALUETYPE_Default = 0;
    public static final int NULLVALUETYPE_Zero = 1;
    public static final int NULLVALUETYPE_Empty = 2;
    protected String docType;
    protected boolean notUsed;
    protected int seq;
    protected String note;
    protected boolean followUp;
     
    // References to other objects.
    protected RpgType rpgType;
     
     
    public MessageTypeColumn() {
    }
     
    public MessageTypeColumn(int id) {
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
     
    public String getRpgName() {
        return rpgName;
    }
    public void setRpgName(String newValue) {
        this.rpgName = newValue;
    }
     
    public boolean getKey() {
        return key;
    }
    public void setKey(boolean newValue) {
        this.key = newValue;
    }
     
    public int getKeyPos() {
        return keyPos;
    }
    public void setKeyPos(int newValue) {
        this.keyPos = newValue;
    }
     
    public int getFromPos() {
        return fromPos;
    }
    public void setFromPos(int newValue) {
        this.fromPos = newValue;
    }
     
    public int getToPos() {
        return toPos;
    }
    public void setToPos(int newValue) {
        this.toPos = newValue;
    }
     
    public int getSize() {
        return size;
    }
    public void setSize(int newValue) {
        this.size = newValue;
    }
     
    public String getDescription() {
        return description;
    }
    public void setDescription(String newValue) {
        this.description = newValue;
    }
     
    public int getDecimalPlaces() {
        return decimalPlaces;
    }
    public void setDecimalPlaces(int newValue) {
        this.decimalPlaces = newValue;
    }
     
    public String getFormat() {
        return format;
    }
    public void setFormat(String newValue) {
        this.format = newValue;
    }
     
    public int getSpecialType() {
        return specialType;
    }
    public void setSpecialType(int newValue) {
        this.specialType = newValue;
    }
     
    public int getNullValueType() {
        return nullValueType;
    }
    public void setNullValueType(int newValue) {
        this.nullValueType = newValue;
    }
     
    public String getDocType() {
        return docType;
    }
    public void setDocType(String newValue) {
        this.docType = newValue;
    }
     
    public boolean getNotUsed() {
        return notUsed;
    }
    public void setNotUsed(boolean newValue) {
        this.notUsed = newValue;
    }
     
    public int getSeq() {
        return seq;
    }
    public void setSeq(int newValue) {
        this.seq = newValue;
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
     
    public RpgType getRpgType() {
        return rpgType;
    }
    
    public void setRpgType(RpgType newValue) {
        this.rpgType = newValue;
    }
    
     
}
 
