// Generated by OABuilder
package com.cdi.model.oa;
 
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.annotation.*;
import com.cdi.delegate.oa.*;
import com.cdi.model.oa.filter.*;
import com.cdi.model.oa.propertypath.*;
import com.viaoa.util.OADateTime;
 
@OAClass(
    shortName = "qb",
    displayName = "Quickbook",
    isLookup = true,
    isPreSelect = true,
    estimatedTotal = 1
)
@OATable(
)
public class Quickbook extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Quickbook.class.getName());

    public static final String PROPERTY_Id = "Id";
    public static final String P_Id = "Id";
    public static final String PROPERTY_Started = "Started";
    public static final String P_Started = "Started";
    public static final String PROPERTY_Enabled = "Enabled";
    public static final String P_Enabled = "Enabled";
    public static final String PROPERTY_CurrentStep = "CurrentStep";
    public static final String P_CurrentStep = "CurrentStep";
    public static final String PROPERTY_CurrentStepAsString = "CurrentStepAsString";
    public static final String P_CurrentStepAsString = "CurrentStepAsString";
    public static final String PROPERTY_TsCurrentStep = "TsCurrentStep";
    public static final String P_TsCurrentStep = "TsCurrentStep";
    public static final String PROPERTY_AllowSendOrders = "AllowSendOrders";
    public static final String P_AllowSendOrders = "AllowSendOrders";
    public static final String PROPERTY_AllowGetOrders = "AllowGetOrders";
    public static final String P_AllowGetOrders = "AllowGetOrders";
    public static final String PROPERTY_AllowUpdateOrders = "AllowUpdateOrders";
    public static final String P_AllowUpdateOrders = "AllowUpdateOrders";
    public static final String PROPERTY_AllowDisableOrders = "AllowDisableOrders";
    public static final String P_AllowDisableOrders = "AllowDisableOrders";
    public static final String PROPERTY_AllowItemAdjustments = "AllowItemAdjustments";
    public static final String P_AllowItemAdjustments = "AllowItemAdjustments";
    public static final String PROPERTY_AllowUpdateCustomers = "AllowUpdateCustomers";
    public static final String P_AllowUpdateCustomers = "AllowUpdateCustomers";
    public static final String PROPERTY_AllowDisableCustomers = "AllowDisableCustomers";
    public static final String P_AllowDisableCustomers = "AllowDisableCustomers";
    public static final String PROPERTY_AllowUpdateItems = "AllowUpdateItems";
    public static final String P_AllowUpdateItems = "AllowUpdateItems";
    public static final String PROPERTY_AllowDisableItems = "AllowDisableItems";
    public static final String P_AllowDisableItems = "AllowDisableItems";
    public static final String PROPERTY_AllowUpdateCustomersNow = "AllowUpdateCustomersNow";
    public static final String P_AllowUpdateCustomersNow = "AllowUpdateCustomersNow";
    public static final String PROPERTY_AllowUpdateItemsNow = "AllowUpdateItemsNow";
    public static final String P_AllowUpdateItemsNow = "AllowUpdateItemsNow";
    public static final String PROPERTY_LastUpdated = "LastUpdated";
    public static final String P_LastUpdated = "LastUpdated";
    public static final String PROPERTY_LastGetOrders = "LastGetOrders";
    public static final String P_LastGetOrders = "LastGetOrders";
    public static final String PROPERTY_QbwcLastCalled = "QbwcLastCalled";
    public static final String P_QbwcLastCalled = "QbwcLastCalled";
    public static final String PROPERTY_Console = "Console";
    public static final String P_Console = "Console";
    public static final String PROPERTY_WarningConsole = "WarningConsole";
    public static final String P_WarningConsole = "WarningConsole";
    public static final String PROPERTY_QbwcConsole = "QbwcConsole";
    public static final String P_QbwcConsole = "QbwcConsole";
    public static final String PROPERTY_FinerConsole = "FinerConsole";
    public static final String P_FinerConsole = "FinerConsole";
     
     
    protected volatile int id;
    protected volatile OADateTime started;
    protected volatile boolean enabled;
    protected volatile int currentStep;
    public static final int CURRENTSTEP_Unknown = 0;
    public static final int CURRENTSTEP_SendSubmittedOrders = 1;
    public static final int CURRENTSTEP_GetNewChangedQbOrders = 2;
    public static final int CURRENTSTEP_InventoryAdjustments = 3;
    public static final int CURRENTSTEP_UpdateChangedItems = 4;
    public static final int CURRENTSTEP_UpdateServiceItems = 5;
    public static final int CURRENTSTEP_UpdateCustomers = 6;
    public static final int CURRENTSTEP_UpdateOrders = 7;
    public static final int CURRENTSTEP_UpdateAllItems = 8;
    public static final int CURRENTSTEP_UpdateAllCustomers = 9;
    public static final int CURRENTSTEP_SleepingOneMinute = 10;
    public static final Hub<String> hubCurrentStep;
    static {
        hubCurrentStep = new Hub<String>(String.class);
        hubCurrentStep.addElement("Unknown");
        hubCurrentStep.addElement("Send Submitted Orders");
        hubCurrentStep.addElement("Get New/Changed QB Orders");
        hubCurrentStep.addElement("Inventory Adjustments");
        hubCurrentStep.addElement("Update Changed Items");
        hubCurrentStep.addElement("Update Service Items");
        hubCurrentStep.addElement("Update Customers");
        hubCurrentStep.addElement("Update Orders");
        hubCurrentStep.addElement("Update All Items");
        hubCurrentStep.addElement("Update All Customers");
        hubCurrentStep.addElement("Sleeping One Minute");
    }
    protected volatile OADateTime tsCurrentStep;
    protected volatile boolean allowSendOrders;
    protected volatile boolean allowGetOrders;
    protected volatile boolean allowUpdateOrders;
    protected volatile boolean allowDisableOrders;
    protected volatile boolean allowItemAdjustments;
    protected volatile boolean allowUpdateCustomers;
    protected volatile boolean allowDisableCustomers;
    protected volatile boolean allowUpdateItems;
    protected volatile boolean allowDisableItems;
    protected volatile boolean allowUpdateCustomersNow;
    protected volatile boolean allowUpdateItemsNow;
    protected volatile OADateTime lastUpdated;
    protected volatile OADateTime lastGetOrders;
    protected volatile OADateTime qbwcLastCalled;
    protected volatile String console;
    protected volatile String warningConsole;
    protected volatile String qbwcConsole;
    protected volatile String finerConsole;
     
    public Quickbook() {
    }
     
    public Quickbook(int id) {
        this();
        setId(id);
    }
     

    @OAProperty(isUnique = true, displayLength = 6)
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
     
    @OAProperty(displayLength = 15, isProcessed = true)
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
     
    @OAProperty(displayLength = 5, columnLength = 7)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    public boolean getEnabled() {
        return enabled;
    }
    public void setEnabled(boolean newValue) {
        boolean old = enabled;
        fireBeforePropertyChange(P_Enabled, old, newValue);
        this.enabled = newValue;
        firePropertyChange(P_Enabled, old, this.enabled);
    }
     
    @OAProperty(displayName = "Current Step", trackPrimitiveNull = false, displayLength = 6, columnLength = 12, hasCustomCode = true, isProcessed = true, isNameValue = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getCurrentStep() {
        return currentStep;
    }
    public void setCurrentStep(int newValue) {
        int old = currentStep;
        fireBeforePropertyChange(P_CurrentStep, old, newValue);
        this.currentStep = newValue;
        firePropertyChange(P_CurrentStep, old, this.currentStep);
        if (!isLoading() && isServer()) setTsCurrentStep(new OADateTime());
    }
    public String getCurrentStepAsString() {
        if (isNull(P_CurrentStep)) return "";
        String s = hubCurrentStep.getAt(getCurrentStep());
        if (s == null) s = "";
        return s;
    }
     
    @OAProperty(displayName = "ts Current Step", displayLength = 15)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getTsCurrentStep() {
        return tsCurrentStep;
    }
    public void setTsCurrentStep(OADateTime newValue) {
        OADateTime old = tsCurrentStep;
        fireBeforePropertyChange(P_TsCurrentStep, old, newValue);
        this.tsCurrentStep = newValue;
        firePropertyChange(P_TsCurrentStep, old, this.tsCurrentStep);
    }
     
    @OAProperty(displayName = "Allow Send Orders", description = "send submitted Sales Orders to QB", displayLength = 5, columnLength = 17)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    /**
      send submitted Sales Orders to QB
    */
    public boolean getAllowSendOrders() {
        return allowSendOrders;
    }
    public void setAllowSendOrders(boolean newValue) {
        boolean old = allowSendOrders;
        fireBeforePropertyChange(P_AllowSendOrders, old, newValue);
        this.allowSendOrders = newValue;
        firePropertyChange(P_AllowSendOrders, old, this.allowSendOrders);
    }
     
    @OAProperty(displayName = "Allow Get Orders", description = "get QB Orders and create Order & WorkOrder", displayLength = 5, columnLength = 16)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    /**
      get QB Orders and create Order & WorkOrder
    */
    public boolean getAllowGetOrders() {
        return allowGetOrders;
    }
    public void setAllowGetOrders(boolean newValue) {
        boolean old = allowGetOrders;
        fireBeforePropertyChange(P_AllowGetOrders, old, newValue);
        this.allowGetOrders = newValue;
        firePropertyChange(P_AllowGetOrders, old, this.allowGetOrders);
    }
     
    @OAProperty(displayName = "Allow Update Orders", description = "update Orders with QB orders", displayLength = 5, columnLength = 19)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    /**
      update Orders with QB orders
    */
    public boolean getAllowUpdateOrders() {
        return allowUpdateOrders;
    }
    public void setAllowUpdateOrders(boolean newValue) {
        boolean old = allowUpdateOrders;
        fireBeforePropertyChange(P_AllowUpdateOrders, old, newValue);
        this.allowUpdateOrders = newValue;
        firePropertyChange(P_AllowUpdateOrders, old, this.allowUpdateOrders);
    }
     
    @OAProperty(displayName = "Allow Disable Orders", description = "ok to disable orders that have been deleted from QB?", displayLength = 5, columnLength = 20)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    /**
      ok to disable orders that have been deleted from QB?
    */
    public boolean getAllowDisableOrders() {
        return allowDisableOrders;
    }
    public void setAllowDisableOrders(boolean newValue) {
        boolean old = allowDisableOrders;
        fireBeforePropertyChange(P_AllowDisableOrders, old, newValue);
        this.allowDisableOrders = newValue;
        firePropertyChange(P_AllowDisableOrders, old, this.allowDisableOrders);
    }
     
    @OAProperty(displayName = "Allow Item Adjustments", description = "send OrderItem on Hand changes to QB", displayLength = 5, columnLength = 22)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    /**
      send OrderItem on Hand changes to QB
    */
    public boolean getAllowItemAdjustments() {
        return allowItemAdjustments;
    }
    public void setAllowItemAdjustments(boolean newValue) {
        boolean old = allowItemAdjustments;
        fireBeforePropertyChange(P_AllowItemAdjustments, old, newValue);
        this.allowItemAdjustments = newValue;
        firePropertyChange(P_AllowItemAdjustments, old, this.allowItemAdjustments);
    }
     
    @OAProperty(displayName = "Allow Update Customers", description = "update customers from QB", displayLength = 5, columnLength = 22)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    /**
      update customers from QB
    */
    public boolean getAllowUpdateCustomers() {
        return allowUpdateCustomers;
    }
    public void setAllowUpdateCustomers(boolean newValue) {
        boolean old = allowUpdateCustomers;
        fireBeforePropertyChange(P_AllowUpdateCustomers, old, newValue);
        this.allowUpdateCustomers = newValue;
        firePropertyChange(P_AllowUpdateCustomers, old, this.allowUpdateCustomers);
    }
     
    @OAProperty(displayName = "Allow Disable Customers", description = "ok to disable customers that are not in quickbooks", displayLength = 5, columnLength = 23)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    /**
      ok to disable customers that are not in quickbooks
    */
    public boolean getAllowDisableCustomers() {
        return allowDisableCustomers;
    }
    public void setAllowDisableCustomers(boolean newValue) {
        boolean old = allowDisableCustomers;
        fireBeforePropertyChange(P_AllowDisableCustomers, old, newValue);
        this.allowDisableCustomers = newValue;
        firePropertyChange(P_AllowDisableCustomers, old, this.allowDisableCustomers);
    }
     
    @OAProperty(displayName = "Allow Update Items", description = "update Items from QB", displayLength = 5, columnLength = 18)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    /**
      update Items from QB
    */
    public boolean getAllowUpdateItems() {
        return allowUpdateItems;
    }
    public void setAllowUpdateItems(boolean newValue) {
        boolean old = allowUpdateItems;
        fireBeforePropertyChange(P_AllowUpdateItems, old, newValue);
        this.allowUpdateItems = newValue;
        firePropertyChange(P_AllowUpdateItems, old, this.allowUpdateItems);
    }
     
    @OAProperty(displayName = "Allow Disable Items", description = "ok to disable items that are not in quickbooks", displayLength = 5, columnLength = 19)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    /**
      ok to disable items that are not in quickbooks
    */
    public boolean getAllowDisableItems() {
        return allowDisableItems;
    }
    public void setAllowDisableItems(boolean newValue) {
        boolean old = allowDisableItems;
        fireBeforePropertyChange(P_AllowDisableItems, old, newValue);
        this.allowDisableItems = newValue;
        firePropertyChange(P_AllowDisableItems, old, this.allowDisableItems);
    }
     
    @OAProperty(displayName = "Allow Update Customers Now", displayLength = 5, columnLength = 26)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    public boolean getAllowUpdateCustomersNow() {
        return allowUpdateCustomersNow;
    }
    public void setAllowUpdateCustomersNow(boolean newValue) {
        boolean old = allowUpdateCustomersNow;
        fireBeforePropertyChange(P_AllowUpdateCustomersNow, old, newValue);
        this.allowUpdateCustomersNow = newValue;
        firePropertyChange(P_AllowUpdateCustomersNow, old, this.allowUpdateCustomersNow);
    }
     
    @OAProperty(displayName = "Allow Update Items Now", displayLength = 5, columnLength = 22)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    public boolean getAllowUpdateItemsNow() {
        return allowUpdateItemsNow;
    }
    public void setAllowUpdateItemsNow(boolean newValue) {
        boolean old = allowUpdateItemsNow;
        fireBeforePropertyChange(P_AllowUpdateItemsNow, old, newValue);
        this.allowUpdateItemsNow = newValue;
        firePropertyChange(P_AllowUpdateItemsNow, old, this.allowUpdateItemsNow);
    }
     
    @OAProperty(displayName = "Last Updated", displayLength = 15, isProcessed = true)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getLastUpdated() {
        return lastUpdated;
    }
    public void setLastUpdated(OADateTime newValue) {
        OADateTime old = lastUpdated;
        fireBeforePropertyChange(P_LastUpdated, old, newValue);
        this.lastUpdated = newValue;
        firePropertyChange(P_LastUpdated, old, this.lastUpdated);
    }
     
    @OAProperty(displayName = "Last Get Orders", displayLength = 15)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getLastGetOrders() {
        return lastGetOrders;
    }
    public void setLastGetOrders(OADateTime newValue) {
        OADateTime old = lastGetOrders;
        fireBeforePropertyChange(P_LastGetOrders, old, newValue);
        this.lastGetOrders = newValue;
        firePropertyChange(P_LastGetOrders, old, this.lastGetOrders);
    }
     
    @OAProperty(displayName = "Qbwc Last Called", displayLength = 15, columnLength = 16, isProcessed = true)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getQbwcLastCalled() {
        return qbwcLastCalled;
    }
    public void setQbwcLastCalled(OADateTime newValue) {
        OADateTime old = qbwcLastCalled;
        fireBeforePropertyChange(P_QbwcLastCalled, old, newValue);
        this.qbwcLastCalled = newValue;
        firePropertyChange(P_QbwcLastCalled, old, this.qbwcLastCalled);
    }
     
    @OAProperty(maxLength = 250, displayLength = 20, isProcessed = true)
    public String getConsole() {
        return console;
    }
    public void setConsole(String newValue) {
        String old = console;
        fireBeforePropertyChange(P_Console, old, newValue);
        this.console = newValue;
        firePropertyChange(P_Console, old, this.console);
    }
     
    @OAProperty(displayName = "Warning Console", maxLength = 250, displayLength = 20, isProcessed = true)
    public String getWarningConsole() {
        return warningConsole;
    }
    public void setWarningConsole(String newValue) {
        String old = warningConsole;
        fireBeforePropertyChange(P_WarningConsole, old, newValue);
        this.warningConsole = newValue;
        firePropertyChange(P_WarningConsole, old, this.warningConsole);
    }
     
    @OAProperty(displayName = "Qbwc Console", maxLength = 250, displayLength = 20, isProcessed = true)
    public String getQbwcConsole() {
        return qbwcConsole;
    }
    public void setQbwcConsole(String newValue) {
        String old = qbwcConsole;
        fireBeforePropertyChange(P_QbwcConsole, old, newValue);
        this.qbwcConsole = newValue;
        firePropertyChange(P_QbwcConsole, old, this.qbwcConsole);
    }
     
    @OAProperty(displayName = "Finer Console", maxLength = 250, displayLength = 20)
    public String getFinerConsole() {
        return finerConsole;
    }
    public void setFinerConsole(String newValue) {
        String old = finerConsole;
        fireBeforePropertyChange(P_FinerConsole, old, newValue);
        this.finerConsole = newValue;
        firePropertyChange(P_FinerConsole, old, this.finerConsole);
    }
     
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.started = new OADateTime(timestamp);
        this.enabled = (rs.getShort(3) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_Enabled, true);
        }
        this.currentStep = (int) rs.getInt(4);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_CurrentStep, true);
        }
        timestamp = rs.getTimestamp(5);
        if (timestamp != null) this.tsCurrentStep = new OADateTime(timestamp);
        this.allowSendOrders = (rs.getShort(6) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowSendOrders, true);
        }
        this.allowGetOrders = (rs.getShort(7) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowGetOrders, true);
        }
        this.allowUpdateOrders = (rs.getShort(8) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowUpdateOrders, true);
        }
        this.allowDisableOrders = (rs.getShort(9) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowDisableOrders, true);
        }
        this.allowItemAdjustments = (rs.getShort(10) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowItemAdjustments, true);
        }
        this.allowUpdateCustomers = (rs.getShort(11) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowUpdateCustomers, true);
        }
        this.allowDisableCustomers = (rs.getShort(12) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowDisableCustomers, true);
        }
        this.allowUpdateItems = (rs.getShort(13) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowUpdateItems, true);
        }
        this.allowDisableItems = (rs.getShort(14) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowDisableItems, true);
        }
        this.allowUpdateCustomersNow = (rs.getShort(15) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowUpdateCustomersNow, true);
        }
        this.allowUpdateItemsNow = (rs.getShort(16) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Quickbook.P_AllowUpdateItemsNow, true);
        }
        timestamp = rs.getTimestamp(17);
        if (timestamp != null) this.lastUpdated = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(18);
        if (timestamp != null) this.lastGetOrders = new OADateTime(timestamp);
        timestamp = rs.getTimestamp(19);
        if (timestamp != null) this.qbwcLastCalled = new OADateTime(timestamp);
        if (rs.getMetaData().getColumnCount() != 19) {
            throw new SQLException("invalid number of columns for load method");
        }

        changedFlag = false;
        newFlag = false;
    }
}
 
