package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.datetime.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.lang.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.viaoa.datetime.OADate;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "storeSchedule",
    pluralName = "StoreSchedules",
    shortName = "sts",
    displayName = "Store Schedule",
    displayProperty = "calcDisplay",
    sortProperty = "date",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StoreScheduleStore", fkey = true, columns = { @OAIndexColumn(name = "StoreId") })
    }
)
public class StoreSchedule extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StoreSchedule.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Date = "date";
    public static final String P_NextStep = "nextStep";
    public static final String P_NextStepString = "nextStepString";
    public static final String P_NextStepEnum = "nextStepEnum";
    public static final String P_NextStepDisplay = "nextStepDisplay";
    public static final String P_VerifySchedule = "verifySchedule";
    public static final String P_TillAuditCompleted = "tillAuditCompleted";
     
    public static final String P_CalcDisplay = "calcDisplay";
     
    public static final String P_Store = "store";
    public static final String P_StoreId = "storeId"; // fkey
    public static final String P_StoreDayEnd = "storeDayEnd";
    public static final String P_StoreDayOpen = "storeDayOpen";
    public static final String P_TeamMembers = "teamMembers";
    public static final String P_TeamMembersId = "teamMembersId"; // fkey
     
    public static final String M_RunNextStep = "runNextStep";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile OADate date;
    protected volatile int nextStep;

    public static enum NextStep {
        Start("Start"),
        VerifySchedule("Verify Schedule"),
        TillAudit("Till Audit");

        private String display;
        NextStep(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int NEXTSTEP_Start = 0;
    public static final int NEXTSTEP_VerifySchedule = 1;
    public static final int NEXTSTEP_TillAudit = 2;

    protected volatile OADateTime verifySchedule;
    protected volatile OADateTime tillAuditCompleted;
     
    // Links to other objects.
    protected volatile transient Store store;
    protected volatile transient StoreDayEnd storeDayEnd;
    protected volatile transient StoreDayOpen storeDayOpen;
    protected transient Hub<TeamMember> hubTeamMembers;
     
    public StoreSchedule() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
        getStoreDayEnd(); // have it autoCreated
        getStoreDayOpen(); // have it autoCreated
    }
     
    public StoreSchedule(int id) {
        this();
        setId(id);
    }

    @OAProperty(lowerName = "id", isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId
    @OAColumn(name = "Id", sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }

    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "Created", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }

    @OAProperty(lowerName = "date", isUnique = true, displayLength = 8)
    @OAColumn(name = "DateValue", sqlType = java.sql.Types.DATE)
    public OADate getDate() {
        return date;
    }
    public void setDate(OADate newValue) {
        OADate old = date;
        fireBeforePropertyChange(P_Date, old, newValue);
        this.date = newValue;
        firePropertyChange(P_Date, old, this.date);
    }

    @OAProperty(lowerName = "nextStep", displayName = "Next Step", displayLength = 6, uiColumnLength = 9, isNameValue = true)
    @OAColumn(name = "NextStep", sqlType = java.sql.Types.INTEGER)
    public int getNextStep() {
        return nextStep;
    }
    public void setNextStep(int newValue) {
        int old = nextStep;
        fireBeforePropertyChange(P_NextStep, old, newValue);
        this.nextStep = newValue;
        firePropertyChange(P_NextStep, old, this.nextStep);
    }

    @OAProperty(enumPropertyName = P_NextStep)
    public String getNextStepString() {
        NextStep nextStep = getNextStepEnum();
        if (nextStep == null) return null;
        return nextStep.name();
    }
    public void setNextStepString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            NextStep nextStep = NextStep.valueOf(val);
            if (nextStep != null) x = nextStep.ordinal();
        }
        if (x < 0) setNull(P_NextStep);
        else setNextStep(x);
    }
    @OAProperty(enumPropertyName = P_NextStep)
    public NextStep getNextStepEnum() {
        if (isNull(P_NextStep)) return null;
        final int val = getNextStep();
        if (val < 0 || val >= NextStep.values().length) return null;
        return NextStep.values()[val];
    }
    public void setNextStepEnum(NextStep val) {
        if (val == null) {
            setNull(P_NextStep);
        }
        else {
            setNextStep(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_NextStep, displayName = "Next Step", displayLength = 6, columnLength = 9, properties = {P_NextStep} )
    public String getNextStepDisplay() {
        NextStep nextStep = getNextStepEnum();
        if (nextStep == null) return null;
        return nextStep.getDisplay();
    }

    @OAProperty(lowerName = "verifySchedule", displayName = "Verify Schedule", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "VerifySchedule", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getVerifySchedule() {
        return verifySchedule;
    }
    public void setVerifySchedule(OADateTime newValue) {
        OADateTime old = verifySchedule;
        fireBeforePropertyChange(P_VerifySchedule, old, newValue);
        this.verifySchedule = newValue;
        firePropertyChange(P_VerifySchedule, old, this.verifySchedule);
    }

    @OAProperty(lowerName = "tillAuditCompleted", displayName = "Till Audit Completed", displayLength = 15, uiColumnLength = 20, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "TillAuditCompleted", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getTillAuditCompleted() {
        return tillAuditCompleted;
    }
    public void setTillAuditCompleted(OADateTime newValue) {
        OADateTime old = tillAuditCompleted;
        fireBeforePropertyChange(P_TillAuditCompleted, old, newValue);
        this.tillAuditCompleted = newValue;
        firePropertyChange(P_TillAuditCompleted, old, this.tillAuditCompleted);
    }
    @OACalculatedProperty(displayName = "Schedule", displayLength = 25, columnLength = 20, properties = {P_Date, P_TeamMembers})
    public String getCalcDisplay() {
        OADate date = this.getDate();
        if (date == null) return "no date set";
        String s = date.toString();
        s += " - " + this.getTeamMembers().getSize() + " Employee scheduled";
        return s;
    }

    @OAOne(
        reverseName = Store.P_StoreSchedules, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreId, toProperty = Store.P_Id)}
    )
    public Store getStore() {
        if (store == null) {
            store = (Store) getObject(P_Store);
        }
        return store;
    }
    public void setStore(Store newValue) {
        Store old = this.store;
        fireBeforePropertyChange(P_Store, old, newValue);
        this.store = newValue;
        firePropertyChange(P_Store, old, this.store);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreId")
    public Integer getStoreId() {
        return (Integer) getFkeyProperty(P_StoreId);
    }
    public void setStoreId(Integer newValue) {
        this.store = null;
        setFkeyProperty(P_StoreId, newValue);
    }

    @OAOne(
        displayName = "Store Day End", 
        owner = true, 
        reverseName = StoreDayEnd.P_StoreSchedule, 
        cascadeSave = true, 
        cascadeDelete = true, 
        autoCreateNew = true, 
        allowAddExisting = false
    )
    public StoreDayEnd getStoreDayEnd() {
        if (storeDayEnd == null) {
            storeDayEnd = (StoreDayEnd) getObject(P_StoreDayEnd);
        }
        return storeDayEnd;
    }
    public void setStoreDayEnd(StoreDayEnd newValue) {
        StoreDayEnd old = this.storeDayEnd;
        fireBeforePropertyChange(P_StoreDayEnd, old, newValue);
        this.storeDayEnd = newValue;
        firePropertyChange(P_StoreDayEnd, old, this.storeDayEnd);
    }

    @OAOne(
        displayName = "Store Day Open", 
        owner = true, 
        reverseName = StoreDayOpen.P_StoreSchedule, 
        cascadeSave = true, 
        cascadeDelete = true, 
        autoCreateNew = true, 
        allowAddExisting = false
    )
    public StoreDayOpen getStoreDayOpen() {
        if (storeDayOpen == null) {
            storeDayOpen = (StoreDayOpen) getObject(P_StoreDayOpen);
        }
        return storeDayOpen;
    }
    public void setStoreDayOpen(StoreDayOpen newValue) {
        StoreDayOpen old = this.storeDayOpen;
        fireBeforePropertyChange(P_StoreDayOpen, old, newValue);
        this.storeDayOpen = newValue;
        firePropertyChange(P_StoreDayOpen, old, this.storeDayOpen);
    }

    @OAMany(
        displayName = "Team Members", 
        toClass = TeamMember.class, 
        reverseName = TeamMember.P_StoreSchedules
    )
    @OALinkTable(name = "StoreScheduleTeamMember", indexName = "TeamMemberStoreSchedule", columns = {"StoreScheduleId"})
    public Hub<TeamMember> getTeamMembers() {
        if (hubTeamMembers == null) {
            hubTeamMembers = (Hub<TeamMember>) getHub(P_TeamMembers);
        }
        return hubTeamMembers;
    }
    @OAMethod(displayName = "Run Next Step")
    public void runNextStep() throws Exception {
        // custom code
        StoreScheduleDelegate.runNextStep(this);
    }
    @OAObjCallback
    public void runNextStepCallback(final OAObjectCallback callback) {
        if (callback == null) return;
        boolean b;
        switch (callback.getType()) {
        case AllowEnabled:
            callback.ack();
            b = StoreScheduleDelegate.isRunNextStepEnabled(this, callback);
            callback.setAllowed(b); 
            // qqqqqq set this is disabled: qqqqqq callback.setResponse("");
            break;
        }
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        java.sql.Date date;
        date = rs.getDate(3);
        if (date != null) this.date = new OADate(date);
        this.nextStep = rs.getInt(4);
        setPrimitiveNull(P_NextStep, rs.wasNull());
        timestamp = rs.getTimestamp(5);
        if (timestamp != null) this.verifySchedule = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(6);
        if (timestamp != null) this.tillAuditCompleted = new OADateTime(timestamp);
        int storeFkey = rs.getInt(7);
        setFkeyProperty(P_Store, rs.wasNull() ? null : storeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
