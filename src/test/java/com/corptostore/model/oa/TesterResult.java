// Generated by OABuilder
package com.corptostore.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.scheduler.*;
import com.viaoa.util.*;
import com.corptostore.delegate.oa.*;
import com.corptostore.model.oa.filter.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.Tester;
import com.corptostore.model.oa.TesterResult;
import com.corptostore.model.oa.TesterResultType;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;
 
@OAClass(
    lowerName = "testerResult",
    pluralName = "TesterResults",
    shortName = "tsr",
    displayName = "Tester Result",
    useDataSource = false,
    sortProperty = "testerResultType"
)
public class TesterResult extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(TesterResult.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Started = "started";
    public static final String P_Ended = "ended";
    public static final String P_ResultInt = "resultInt";
    public static final String P_ResultDateTime = "resultDateTime";
    public static final String P_ResultString = "resultString";
     
     
    public static final String P_Tester = "tester";
    public static final String P_TesterResultType = "testerResultType";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile OADateTime started;
    protected volatile OADateTime ended;
    protected volatile int resultInt;
    protected volatile OADateTime resultDateTime;
    protected volatile String resultString;
     
    // Links to other objects.
    protected volatile transient Tester tester;
    protected volatile transient TesterResultType testerResultType;
     
    public TesterResult() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public TesterResult(int id) {
        this();
        setId(id);
    }
     

    @OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId
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
    @OAProperty(displayLength = 15)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getStarted() {
        return started;
    }
    public void setStarted(OADateTime newValue) {
        OADateTime old = started;
        fireBeforePropertyChange(P_Started, old, newValue);
        this.started = newValue;
        firePropertyChange(P_Started, old, this.started);
    }
    @OAProperty(displayLength = 15)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getEnded() {
        return ended;
    }
    public void setEnded(OADateTime newValue) {
        OADateTime old = ended;
        fireBeforePropertyChange(P_Ended, old, newValue);
        this.ended = newValue;
        firePropertyChange(P_Ended, old, this.ended);
    }
    @OAProperty(displayName = "Result Int", displayLength = 6, columnLength = 10)
    @OAColumn(name = "result_int", sqlType = java.sql.Types.INTEGER)
    public int getResultInt() {
        return resultInt;
    }
    public void setResultInt(int newValue) {
        int old = resultInt;
        fireBeforePropertyChange(P_ResultInt, old, newValue);
        this.resultInt = newValue;
        firePropertyChange(P_ResultInt, old, this.resultInt);
    }
    @OAProperty(displayName = "Result Date Time", displayLength = 15, columnLength = 16)
    @OAColumn(name = "result_date_time", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getResultDateTime() {
        return resultDateTime;
    }
    public void setResultDateTime(OADateTime newValue) {
        OADateTime old = resultDateTime;
        fireBeforePropertyChange(P_ResultDateTime, old, newValue);
        this.resultDateTime = newValue;
        firePropertyChange(P_ResultDateTime, old, this.resultDateTime);
    }
    @OAProperty(displayName = "Result String", maxLength = 75, displayLength = 20, columnLength = 14)
    @OAColumn(name = "result_string", maxLength = 75)
    public String getResultString() {
        return resultString;
    }
    public void setResultString(String newValue) {
        String old = resultString;
        fireBeforePropertyChange(P_ResultString, old, newValue);
        this.resultString = newValue;
        firePropertyChange(P_ResultString, old, this.resultString);
    }
    @OAOne(
        reverseName = Tester.P_TesterResults, 
        required = true, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    @OAFkey(columns = {"tester_id"})
    public Tester getTester() {
        if (tester == null) {
            tester = (Tester) getObject(P_Tester);
        }
        return tester;
    }
    public void setTester(Tester newValue) {
        Tester old = this.tester;
        fireBeforePropertyChange(P_Tester, old, newValue);
        this.tester = newValue;
        firePropertyChange(P_Tester, old, this.tester);
    }
    @OAOne(
        displayName = "Tester Result Type", 
        reverseName = TesterResultType.P_TesterResults, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"tester_result_type_id"})
    public TesterResultType getTesterResultType() {
        if (testerResultType == null) {
            testerResultType = (TesterResultType) getObject(P_TesterResultType);
        }
        return testerResultType;
    }
    public void setTesterResultType(TesterResultType newValue) {
        TesterResultType old = this.testerResultType;
        fireBeforePropertyChange(P_TesterResultType, old, newValue);
        this.testerResultType = newValue;
        firePropertyChange(P_TesterResultType, old, this.testerResultType);
    }
}
 