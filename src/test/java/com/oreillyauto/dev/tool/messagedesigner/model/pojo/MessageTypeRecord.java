package com.oreillyauto.dev.tool.messagedesigner.model.pojo;
 
import java.util.*;
import java.time.LocalDateTime;
import java.io.*;
import com.oreillyauto.dev.tool.messagedesigner.delegate.*;
import com.oreillyauto.dev.tool.messagedesigner.delegate.oa.*;
 
public class MessageTypeRecord implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected LocalDateTime created;
    protected String code;
    protected String subCode;
    protected String name;
    protected String description;
    protected int repeatingCount;
    protected int statusType;
    public static final int STATUSTYPE_Unknown = 0;
    public static final int STATUSTYPE_Mapped = 1;
    public static final int STATUSTYPE_Testing = 2;
    public static final int STATUSTYPE_Verified = 3;
    protected String notes;
    protected boolean disable;
    protected int seq;
    protected String layout;
    protected LocalDateTime layoutLoaded;
    protected boolean followUp;
    protected boolean lock;
     
    // References to other objects.
    protected ArrayList<MessageRecord> alMessageRecords;
    protected ArrayList<MessageTypeColumn> alMessageTypeColumns;
    protected ArrayList<RpgProgram> alRpgPrograms;
    protected MessageTypeColumn subCodeColumn;
     
     
    public MessageTypeRecord() {
    }
     
    public MessageTypeRecord(int id) {
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
     
    public String getCode() {
        return code;
    }
    public void setCode(String newValue) {
        this.code = newValue;
    }
     
    public String getSubCode() {
        return subCode;
    }
    public void setSubCode(String newValue) {
        this.subCode = newValue;
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
     
    public int getRepeatingCount() {
        return repeatingCount;
    }
    public void setRepeatingCount(int newValue) {
        this.repeatingCount = newValue;
    }
     
    public int getStatusType() {
        return statusType;
    }
    public void setStatusType(int newValue) {
        this.statusType = newValue;
    }
     
    public String getNotes() {
        return notes;
    }
    public void setNotes(String newValue) {
        this.notes = newValue;
    }
     
    public boolean getDisable() {
        return disable;
    }
    public void setDisable(boolean newValue) {
        this.disable = newValue;
    }
     
    public int getSeq() {
        return seq;
    }
    public void setSeq(int newValue) {
        this.seq = newValue;
    }
     
    public String getLayout() {
        return layout;
    }
    public void setLayout(String newValue) {
        this.layout = newValue;
    }
     
    public LocalDateTime getLayoutLoaded() {
        return layoutLoaded;
    }
    public void setLayoutLoaded(LocalDateTime newValue) {
        this.layoutLoaded = newValue;
    }
     
    public boolean getFollowUp() {
        return followUp;
    }
    public void setFollowUp(boolean newValue) {
        this.followUp = newValue;
    }
     
    public boolean getLock() {
        return lock;
    }
    public void setLock(boolean newValue) {
        this.lock = newValue;
    }
     
    public ArrayList<MessageRecord> getMessageRecords() {
        if (alMessageRecords == null) {
            alMessageRecords = new ArrayList<MessageRecord>();
        }
        return alMessageRecords;
    }
    
     
    public ArrayList<MessageTypeColumn> getMessageTypeColumns() {
        if (alMessageTypeColumns == null) {
            alMessageTypeColumns = new ArrayList<MessageTypeColumn>();
        }
        return alMessageTypeColumns;
    }
    
     
    public ArrayList<RpgProgram> getRpgPrograms() {
        if (alRpgPrograms == null) {
            alRpgPrograms = new ArrayList<RpgProgram>();
        }
        return alRpgPrograms;
    }
    
     
    public MessageTypeColumn getSubCodeColumn() {
        return subCodeColumn;
    }
    
    public void setSubCodeColumn(MessageTypeColumn newValue) {
        this.subCodeColumn = newValue;
    }
    
     
}
 
