// Generated by OABuilder
package com.messagedesigner.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.util.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.scheduler.*;
import com.messagedesigner.delegate.*;
import com.messagedesigner.delegate.oa.*;
import com.messagedesigner.model.oa.filter.*;
import com.messagedesigner.model.oa.propertypath.*;
import com.messagedesigner.delegate.ModelDelegate;
import com.messagedesigner.delegate.oa.MessageTypeRecordDelegate;
import com.messagedesigner.model.oa.JsonType;
import com.messagedesigner.model.oa.MessageRecord;
import com.messagedesigner.model.oa.MessageSource;
import com.messagedesigner.model.oa.MessageTypeColumn;
import com.messagedesigner.model.oa.MessageTypeRecord;
import com.messagedesigner.model.oa.RpgMessage;
import com.messagedesigner.model.oa.RpgProgram;
import com.messagedesigner.model.oa.RpgType;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;

import java.io.*;
 
@OAClass(
    lowerName = "messageTypeRecord",
    pluralName = "MessageTypeRecords",
    shortName = "mtr",
    displayName = "Message Type Record",
    displayProperty = "display"
)
@OATable(
    indexes = {
        @OAIndex(name = "MessageTypeRecordMessageSource", fkey = true, columns = { @OAIndexColumn(name = "MessageSourceId") }), 
        @OAIndex(name = "MessageTypeRecordSubCodeColumn", fkey = true, columns = { @OAIndexColumn(name = "SubCodeColumnId") })
    }
)
public class MessageTypeRecord extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(MessageTypeRecord.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Code = "code";
    public static final String P_SubCode = "subCode";
    public static final String P_Name = "name";
    public static final String P_Description = "description";
    public static final String P_RepeatingCount = "repeatingCount";
    public static final String P_StatusType = "statusType";
    public static final String P_StatusTypeAsString = "statusTypeString";
    public static final String P_Notes = "notes";
    public static final String P_Disable = "disable";
    public static final String P_Seq = "seq";
    public static final String P_Layout = "layout";
    public static final String P_LayoutLoaded = "layoutLoaded";
    public static final String P_FollowUp = "followUp";
    public static final String P_Lock = "lock";
     
    public static final String P_EnableLoadLayout = "enableLoadLayout";
    public static final String P_IsDefined = "isDefined";
    public static final String P_PojoCode = "pojoCode";
    public static final String P_Display = "display";
    public static final String P_FixLoadEnabled = "fixLoadEnabled";
    public static final String P_CreateFromLayoutEnabled = "createFromLayoutEnabled";
     
    public static final String P_MessageRecords = "messageRecords";
    public static final String P_MessageSource = "messageSource";
    public static final String P_MessageTypeColumns = "messageTypeColumns";
    public static final String P_RpgMessages2 = "rpgMessages2";
    public static final String P_RpgPrograms = "rpgPrograms";
    public static final String P_SubCodeColumn = "subCodeColumn";
     
    public static final String M_Update = "update";
    public static final String M_FixLayout = "fixLayout";
    public static final String M_CreateFromLayout = "createFromLayout";
    public static final String M_Unlock = "unlock";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String code;
    protected volatile String subCode;
    protected volatile String name;
    protected volatile String description;
    protected volatile int repeatingCount;
    protected volatile int statusType;
    public static enum StatusType {
        Unknown("Unknown"),
        Mapped("Mapped"),
        Testing("Testing"),
        Verified("Verified");

        private String display;
        StatusType(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int STATUSTYPE_Unknown = 0;
    public static final int STATUSTYPE_Mapped = 1;
    public static final int STATUSTYPE_Testing = 2;
    public static final int STATUSTYPE_Verified = 3;
    public static final Hub<String> hubStatusType;
    static {
        hubStatusType = new Hub<String>(String.class);
        hubStatusType.addElement("Unknown");
        hubStatusType.addElement("Mapped");
        hubStatusType.addElement("Testing");
        hubStatusType.addElement("Verified");
    }
    protected volatile String notes;
    protected volatile boolean disable;
    protected volatile int seq;
    protected volatile String layout;
    protected volatile OADateTime layoutLoaded;
    protected volatile boolean followUp;
    protected volatile boolean lock;
     
    // Links to other objects.
    protected transient Hub<MessageRecord> hubMessageRecords;
    protected volatile transient MessageSource messageSource;
    protected transient Hub<MessageTypeColumn> hubMessageTypeColumns;
    protected transient Hub<RpgProgram> hubRpgPrograms;
    protected volatile transient MessageTypeColumn subCodeColumn;
     
    public MessageTypeRecord() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
        setRepeatingCount(0);
    }
     
    public MessageTypeRecord(int id) {
        this();
        setId(id);
    }
     
    @OAObjCallback(enabledProperty = MessageTypeRecord.P_Lock, enabledValue = false)
    public void callback(final OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }
    @OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId()
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }
    @OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }
    @OAProperty(maxLength = 10, displayLength = 4)
    @OAColumn(maxLength = 10)
    public String getCode() {
        return code;
    }
    public void setCode(String newValue) {
        String old = code;
        fireBeforePropertyChange(P_Code, old, newValue);
        this.code = newValue;
        firePropertyChange(P_Code, old, this.code);
    }
    @OAProperty(displayName = "Sub Code", maxLength = 10, displayLength = 4, columnLength = 6)
    @OAColumn(maxLength = 10)
    public String getSubCode() {
        return subCode;
    }
    public void setSubCode(String newValue) {
        String old = subCode;
        fireBeforePropertyChange(P_SubCode, old, newValue);
        this.subCode = newValue;
        firePropertyChange(P_SubCode, old, this.subCode);
    }
     
    @OAObjCallback(visibleProperty = MessageTypeRecord.P_SubCodeColumn)
    public void subCodeCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }
    @OAProperty(maxLength = 55, displayLength = 14)
    @OAColumn(maxLength = 55)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
    @OAProperty(maxLength = 250, displayLength = 20, columnLength = 25)
    @OAColumn(maxLength = 250)
    public String getDescription() {
        return description;
    }
    public void setDescription(String newValue) {
        String old = description;
        fireBeforePropertyChange(P_Description, old, newValue);
        this.description = newValue;
        firePropertyChange(P_Description, old, this.description);
    }
    @OAProperty(displayName = "Repeating Count", defaultValue = "0", displayLength = 3, columnLength = 9, columnName = "RepeatCnt")
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getRepeatingCount() {
        return repeatingCount;
    }
    public void setRepeatingCount(int newValue) {
        int old = repeatingCount;
        fireBeforePropertyChange(P_RepeatingCount, old, newValue);
        this.repeatingCount = newValue;
        firePropertyChange(P_RepeatingCount, old, this.repeatingCount);
    }
    @OAProperty(displayName = "Status Type", displayLength = 6, columnLength = 11, isNameValue = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getStatusType() {
        return statusType;
    }
    public void setStatusType(int newValue) {
        int old = statusType;
        fireBeforePropertyChange(P_StatusType, old, newValue);
        this.statusType = newValue;
        firePropertyChange(P_StatusType, old, this.statusType);
        firePropertyChange(P_StatusType + "String");
        firePropertyChange(P_StatusType + "Enum");
    }

    public String getStatusTypeString() {
        StatusType statusType = getStatusTypeEnum();
        if (statusType == null) return null;
        return statusType.name();
    }
    public void setStatusTypeString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            StatusType statusType = StatusType.valueOf(val);
            if (statusType != null) x = statusType.ordinal();
        }
        if (x < 0) setNull(P_StatusType);
        else setStatusType(x);
    }


    public StatusType getStatusTypeEnum() {
        if (isNull(P_StatusType)) return null;
        final int val = getStatusType();
        if (val < 0 || val >= StatusType.values().length) return null;
        return StatusType.values()[val];
    }

    public void setStatusTypeEnum(StatusType val) {
        if (val == null) {
            setNull(P_StatusType);
        }
        else {
            setStatusType(val.ordinal());
        }
    }
    @OAProperty(displayLength = 30, columnLength = 20, isHtml = true)
    @OAColumn(sqlType = java.sql.Types.CLOB)
    public String getNotes() {
        return notes;
    }
    public void setNotes(String newValue) {
        String old = notes;
        fireBeforePropertyChange(P_Notes, old, newValue);
        this.notes = newValue;
        firePropertyChange(P_Notes, old, this.notes);
    }
    @OAProperty(trackPrimitiveNull = false, displayLength = 5, columnLength = 7)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    public boolean getDisable() {
        return disable;
    }
    public boolean isDisable() {
        return getDisable();
    }
    public void setDisable(boolean newValue) {
        boolean old = disable;
        fireBeforePropertyChange(P_Disable, old, newValue);
        this.disable = newValue;
        firePropertyChange(P_Disable, old, this.disable);
    }
    @OAProperty(displayLength = 6, isAutoSeq = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getSeq() {
        return seq;
    }
    public void setSeq(int newValue) {
        int old = seq;
        fireBeforePropertyChange(P_Seq, old, newValue);
        this.seq = newValue;
        firePropertyChange(P_Seq, old, this.seq);
    }
    @OAProperty(displayLength = 30, columnLength = 20, hasCustomCode = true)
    @OAColumn(sqlType = java.sql.Types.CLOB)
    public String getLayout() {
        return layout;
    }
    public void setLayout(String newValue) {
        String old = layout;
        fireBeforePropertyChange(P_Layout, old, newValue);
        this.layout = newValue;
        firePropertyChange(P_Layout, old, this.layout);
        if (!isLoading()) {
            if (startServerOnly()) {
                setLayoutLoaded(new OADateTime());
                endServerOnly();
            }
        }
    }
    @OAProperty(displayName = "Layout Loaded", displayLength = 15, isProcessed = true)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getLayoutLoaded() {
        return layoutLoaded;
    }
    public void setLayoutLoaded(OADateTime newValue) {
        OADateTime old = layoutLoaded;
        fireBeforePropertyChange(P_LayoutLoaded, old, newValue);
        this.layoutLoaded = newValue;
        firePropertyChange(P_LayoutLoaded, old, this.layoutLoaded);
    }
    @OAProperty(displayName = "Follow Up", displayLength = 5, columnLength = 9)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    public boolean getFollowUp() {
        return followUp;
    }
    public boolean isFollowUp() {
        return getFollowUp();
    }
    public void setFollowUp(boolean newValue) {
        boolean old = followUp;
        fireBeforePropertyChange(P_FollowUp, old, newValue);
        this.followUp = newValue;
        firePropertyChange(P_FollowUp, old, this.followUp);
    }
    @OAProperty(displayLength = 5)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    public boolean getLock() {
        return lock;
    }
    public boolean isLock() {
        return getLock();
    }
    public void setLock(boolean newValue) {
        boolean old = lock;
        fireBeforePropertyChange(P_Lock, old, newValue);
        this.lock = newValue;
        firePropertyChange(P_Lock, old, this.lock);
    }
    @OACalculatedProperty(displayName = "Enable Load Layout", displayLength = 5, columnLength = 18, properties = {P_Layout, P_MessageTypeColumns})
    public boolean getEnableLoadLayout() {
        boolean enableLoadLayout;
        if (OAString.isEmpty(this.getLayout())) {
            return false;
        }
        /*
        if (this.getMessageTypeColumns().getSize() > 0) {
            return false;
        }
        */
        return true;
    }
    
    public boolean isEnableLoadLayout() {
        return getEnableLoadLayout();
    }
    @OACalculatedProperty(displayName = "Is Defined", displayLength = 5, columnLength = 10, properties = {P_MessageTypeColumns})
    public boolean getIsDefined() {
        return getMessageTypeColumns().getSize() > 0;
    }
    @OACalculatedProperty(displayName = "Pojo Code", displayLength = 30, columnLength = 20, properties = {P_MessageTypeColumns+"."+MessageTypeColumn.P_Name, P_MessageTypeColumns+"."+MessageTypeColumn.P_RpgType})
    public String getPojoCode() {
        String pojo = MessageTypeRecordDelegate.getPojoCode(this);
        return pojo;
    }
    @OACalculatedProperty(displayLength = 12, columnLength = 16, properties = {P_Code, P_Name})
    public String getDisplay() {
        String display = "";
        String code = this.getCode();
        display = OAString.concat(display, code, " ");
    
        String name = this.getName();
        display = OAString.concat(display, name, " ");
    
        return display;
    }
    @OACalculatedProperty(displayName = "Fix Load Enabled", displayLength = 5, columnLength = 16, properties = {P_Layout})
    public boolean getFixLoadEnabled() {
        return OAString.isNotEmpty(layout) && layout.indexOf('|') < 0;
    }
    public boolean isFixLoadEnabled() {
        return getFixLoadEnabled();
    }
    @OACalculatedProperty(displayName = "Create From Layout Enabled", displayLength = 5, columnLength = 26, properties = {P_Layout})
    public boolean getCreateFromLayoutEnabled() {
        return OAString.isNotEmpty(layout) && layout.indexOf('|') >= 0;
    }
    public boolean isCreateFromLayoutEnabled() {
        return getCreateFromLayoutEnabled();
    }
    @OAMany(
        displayName = "Message Records", 
        toClass = MessageRecord.class, 
        reverseName = MessageRecord.P_MessageTypeRecord
    )
    public Hub<MessageRecord> getMessageRecords() {
        if (hubMessageRecords == null) {
            hubMessageRecords = (Hub<MessageRecord>) getHub(P_MessageRecords);
        }
        return hubMessageRecords;
    }
    @OAOne(
        displayName = "Message Source", 
        reverseName = MessageSource.P_MessageTypeRecords, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"MessageSourceId"})
    public MessageSource getMessageSource() {
        if (messageSource == null) {
            messageSource = (MessageSource) getObject(P_MessageSource);
        }
        return messageSource;
    }
    public void setMessageSource(MessageSource newValue) {
        MessageSource old = this.messageSource;
        fireBeforePropertyChange(P_MessageSource, old, newValue);
        this.messageSource = newValue;
        firePropertyChange(P_MessageSource, old, this.messageSource);
    }
    @OAMany(
        displayName = "Message Type Columns", 
        toClass = MessageTypeColumn.class, 
        owner = true, 
        reverseName = MessageTypeColumn.P_MessageTypeRecord, 
        cascadeSave = true, 
        cascadeDelete = true, 
        seqProperty = MessageTypeColumn.P_Seq, 
        sortProperty = MessageTypeColumn.P_Seq
    )
    public Hub<MessageTypeColumn> getMessageTypeColumns() {
        if (hubMessageTypeColumns == null) {
            hubMessageTypeColumns = (Hub<MessageTypeColumn>) getHub(P_MessageTypeColumns);
        }
        return hubMessageTypeColumns;
    }
    @OAMany(
        displayName = "Rpg Messages", 
        toClass = RpgMessage.class, 
        reverseName = RpgMessage.P_MessageTypeRecord, 
        createMethod = false
    )
    private Hub<RpgMessage> getRpgMessages2() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    @OAMany(
        displayName = "RPG Programs", 
        toClass = RpgProgram.class, 
        reverseName = RpgProgram.P_MessageTypeRecords
    )
    @OALinkTable(name = "RpgProgramMessageTypeRecord", indexName = "RpgProgramMessageTypeRecord", columns = {"MessageTypeRecordId"})
    public Hub<RpgProgram> getRpgPrograms() {
        if (hubRpgPrograms == null) {
            hubRpgPrograms = (Hub<RpgProgram>) getHub(P_RpgPrograms);
        }
        return hubRpgPrograms;
    }
    @OAOne(
        displayName = "Sub Code Column", 
        reverseName = MessageTypeColumn.P_RecordSubCode, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"SubCodeColumnId"})
    public MessageTypeColumn getSubCodeColumn() {
        if (subCodeColumn == null) {
            subCodeColumn = (MessageTypeColumn) getObject(P_SubCodeColumn);
        }
        return subCodeColumn;
    }
    public void setSubCodeColumn(MessageTypeColumn newValue) {
        MessageTypeColumn old = this.subCodeColumn;
        fireBeforePropertyChange(P_SubCodeColumn, old, newValue);
        this.subCodeColumn = newValue;
        firePropertyChange(P_SubCodeColumn, old, this.subCodeColumn);
    }
    @OAMethod(displayName = "Update")
    public void update() {
        // no-op
    }

    @OAMethod(displayName = "Fix Layout")
    public void fixLayout() {
        if (!getFixLoadEnabled()) return;
          MessageTypeRecordDelegate.fixLayout(this);
    }
    @OAObjCallback(enabledProperty = MessageTypeRecord.P_FixLoadEnabled)
    public void fixLayoutCallback(OAObjectCallback cb) {
    }

    @OAMethod(displayName = "Create From Layout")
    public void createFromLayout() {
        if (!getCreateFromLayoutEnabled()) return;
        // use this to run on server (remote)
        /*
        if (isRemoteAvailable()) {
            //qqqqqqq remote();
            //qqqqqqq return;
        }
        */
    
        MessageTypeRecordDelegate.convertLayoutToColumns(this);
    
        if (true || false) return;  //qqqqqqqqqqqqqqqqqqqqqq
    
        String layout = getLayout();
        if (OAString.isEmpty(layout)) {
            return;
        }
    
        getMessageTypeColumns().deleteAll(); //qqqqqqqqqqqq
    
        StringReader sr = new StringReader(layout);
        BufferedReader br = new BufferedReader(sr);
    
        for (int i = 0;; i++) {
            String line = null;
            try {
                line = br.readLine();
            } catch (Exception e) {
            }
            if (line == null) {
                break;
            }
            try {
                createFromLayout(line);
            } catch (Exception e) {
                //qqqqqqqqqq
            }
        }
    }
    
    public static void createFromLayout(Hub<MessageTypeRecord> hub) {
        /*qqqq
        if (isRemoteAvailable(hub)) {
            callRemote(hub);
            return;
        }
        */
        for (MessageTypeRecord messageTypeRecord : hub) {
            messageTypeRecord.createFromLayout();
        }
    }
    
    public void createFromLayout(final String line) {
        if (OAString.isEmpty(line)) {
            return;
        }
    
        String[] ss = line.split("\\t");
    
        final String name = ss[0];
    
        OAFinder<MessageTypeRecord, MessageTypeColumn> finder = new OAFinder<>(MessageTypeRecord.P_MessageTypeColumns);
        finder.addLikeFilter(MessageTypeColumn.P_Name, name);
        MessageTypeColumn mc = finder.findFirst(this);
    
        MessageTypeColumn mcLast = getMessageTypeColumns().get(getMessageTypeColumns().getSize() - 1);
    
        if (mc == null) {
            mc = new MessageTypeColumn();
            mc.setRpgName(name);
            getMessageTypeColumns().add(mc);
        }
        mc.setDocType(line.replace('\t', ' '));
    
        int fromPos = 0;
        int toPos = 0;
        int size = 0;
        int deciPlaces = 0;
        boolean bNumeric = false;
        boolean bPacked = false;
        JsonType jt = null;
        boolean bValid = true;
        RpgType rpgType = null;
    
        if (!OAString.isNumber(ss[1]) || ss[1].indexOf(',') > 0) {
            /*
                POREC    2A    INZ
                POSTR#    6,0    INZ
                POPO#    10A    INZ
                timestamp
                JHCUSW    118    126 0  zoned #
             */
    
            String s2 = ss[1];
            int pos = s2.indexOf(',');
            if (pos > 0) {
                bNumeric = true;
                if (s2.indexOf('P') > 0) {
                    bPacked = true;
                    s2 = OAString.convert(s2, "P", "");
                }
                size = OAConv.toInt(OAString.field(s2, ',', 1));
                if (bPacked) {
                    size = (size / 2) + 1;
                }
                deciPlaces = OAConv.toInt(OAString.field(s2, ',', 2));
            } else if ("Date".equalsIgnoreCase(s2)) {
                rpgType = ModelDelegate.getRpgTypes().find(RpgType.P_Name, "Date");
                if (rpgType != null) {
                    size = rpgType.getDefaultSize();
                }
            } else if ("Time".equalsIgnoreCase(s2)) {
                rpgType = ModelDelegate.getRpgTypes().find(RpgType.P_Name, "Time");
                if (rpgType != null) {
                    size = rpgType.getDefaultSize();
                }
            } else {
                pos = s2.indexOf('A');
                if (pos > 0) {
                    size = OAConv.toInt(OAString.field(s2, 'A', 1));
                } else {
                    if (s2.toLowerCase().indexOf("timestamp") >= 0) {
                        // 2020-12-23-13.50.12.086000  (size=26)
                        rpgType = ModelDelegate.getRpgTypes().find(RpgType.P_Name, "Timestamp");
                        if (rpgType != null) {
                            size = rpgType.getDefaultSize();
                        }
                    } else {
                        bValid = false;
                    }
                }
            }
        } else {
            fromPos = OAConv.toInt(ss[1]);
    
            String s3 = ss.length > 2 ? ss[2] : "";
    
            if (s3.toLowerCase().equals("date")) {
                // find date
                jt = ModelDelegate.getJsonTypes().find(JsonType.P_Type, JsonType.TYPE_Date);
            } else if (s3.toLowerCase().indexOf("inz(") >= 0) {
                jt = ModelDelegate.getJsonTypes().find(JsonType.P_Type, JsonType.TYPE_String);
            } else if (s3.indexOf('P') > 0) {
                s3 = OAString.convert(s3, "P", ",");
                toPos = OAConv.toInt(OAString.field(s3, ',', 1));
                bNumeric = true;
                deciPlaces = OAConv.toInt(OAString.field(s3, ',', 2));
                bPacked = true;
            } else if (s3.indexOf(' ') > 0) {
                s3 = OAString.convert(s3, " ", ",");
                toPos = OAConv.toInt(OAString.field(s3, ',', 1));
                bNumeric = true;
                deciPlaces = OAConv.toInt(OAString.field(s3, ',', 2));
                bPacked = false;
            } else {
                s3 = OAString.convert(s3, "A", "");
                toPos = OAConv.toInt(s3);
            }
        }
    
        if (fromPos == 0) {
            fromPos = mcLast == null ? 1 : mcLast.getToPos() + 1;
        }
    
        if (toPos == 0) {
            toPos = fromPos + size - 1;
        }
    
        /*
        int fromPos = 0;
        int toPos = 0;
        int size = 0;
        int deciPlaces = 0;
        boolean bNumeric = false;
        boolean bPacked = false;
        JsonType jt;
        */
    
        mc.setFromPos(fromPos);
        mc.setToPos(toPos);
    
        if (bNumeric) {
            mc.setDecimalPlaces(deciPlaces);
        }
    
        int actualSize = toPos - fromPos + 1;
        if (bPacked) {
            size = actualSize * 2 - 1;
        }
        mc.setSize(actualSize);
    
        if (rpgType == null) {
            if (jt != null) {
                rpgType = ModelDelegate.getRpgTypes().find(RpgType.P_JsonType, jt);
            } else if (bNumeric) {
                jt = ModelDelegate.getJsonTypes().find(JsonType.P_Type, JsonType.TYPE_Number);
    
                if (jt != null) {
                    for (RpgType rt : ModelDelegate.getRpgTypes()) {
                        if (rt.getJsonType() != jt) {
                            continue;
                        }
                        if (bPacked && rt.getEncodeType() == rt.ENCODETYPE_PackedDecimal) {
                            rpgType = rt;
                            break;
                        }
                        if (!bPacked && rt.getEncodeType() == rt.ENCODETYPE_ZonedDecimal) {
                            rpgType = rt;
                            break;
                        }
                    }
                }
            } else if (bValid) {
                jt = ModelDelegate.getJsonTypes().find(JsonType.P_Type, JsonType.TYPE_String);
                RpgType rt = ModelDelegate.getRpgTypes().find(RpgType.P_JsonType, jt);
                if (rt != null) {
                    rpgType = rt;
                }
            }
        }
        if (rpgType != null) {
            mc.setRpgType(rpgType);
        }
    }
    @OAObjCallback(enabledProperty = MessageTypeRecord.P_CreateFromLayoutEnabled)
    public void createFromLayoutCallback(final OAObjectCallback callback) {
        switch (callback.getType()) {
        case SetConfirmForCommand:
            if (getMessageTypeColumns().getSize() > 0) {
                callback.setConfirmMessage("Columns already exist, is it ok to replace them"); 
                callback.setConfirmTitle("confirm"); 
            }
            break;
        }
    }

    @OAMethod(displayName = "Unlock")
    public void unlock() {
        setLock(false);
    }
    @OAObjCallback(enabledProperty = MessageTypeRecord.P_Lock, visibleProperty = MessageTypeRecord.P_Lock
    )
    public void unlockCallback(final OAObjectCallback callback) {
        if (callback == null) {
            return;
        }
        switch (callback.getType()) {
        case AllowEnabled:
            callback.ack();
            if (!callback.getAllowed() && getLock()) {
                callback.setAllowed(true);
            }
            break;
        }
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.code = rs.getString(3);
        this.subCode = rs.getString(4);
        this.name = rs.getString(5);
        this.description = rs.getString(6);
        this.repeatingCount = (int) rs.getInt(7);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, MessageTypeRecord.P_RepeatingCount, true);
        }
        this.statusType = (int) rs.getInt(8);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, MessageTypeRecord.P_StatusType, true);
        }
        this.notes = rs.getString(9);
        this.disable = rs.getBoolean(10);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, MessageTypeRecord.P_Disable, true);
        }
        this.seq = (int) rs.getInt(11);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, MessageTypeRecord.P_Seq, true);
        }
        this.layout = rs.getString(12);
        timestamp = rs.getTimestamp(13);
        if (timestamp != null) this.layoutLoaded = new OADateTime(timestamp);
        this.followUp = rs.getBoolean(14);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, MessageTypeRecord.P_FollowUp, true);
        }
        this.lock = rs.getBoolean(15);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, MessageTypeRecord.P_Lock, true);
        }
        int messageSourceFkey = rs.getInt(16);
        if (!rs.wasNull() && messageSourceFkey > 0) {
            setProperty(P_MessageSource, new OAObjectKey(messageSourceFkey));
        }
        int subCodeColumnFkey = rs.getInt(17);
        if (!rs.wasNull() && subCodeColumnFkey > 0) {
            setProperty(P_SubCodeColumn, new OAObjectKey(subCodeColumnFkey));
        }
        if (rs.getMetaData().getColumnCount() != 17) {
            throw new SQLException("invalid number of columns for load method");
        }

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 