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
import com.viaoa.util.OADate;
import com.cdi.delegate.ModelDelegate;
 
@OAClass(
    shortName = "son",
    displayName = "Sales Order Note",
    displayProperty = "salesOrder",
    sortProperty = "salesOrder"
)
@OATable(
    indexes = {
        @OAIndex(name = "SalesOrderNoteFollowupDate", columns = {@OAIndexColumn(name = "FollowupDate")}),
        @OAIndex(name = "SalesOrderNoteSalesOrder", fkey = true, columns = { @OAIndexColumn(name = "SalesOrderId") }), 
        @OAIndex(name = "SalesOrderNoteUser", fkey = true, columns = { @OAIndexColumn(name = "UserId") })
    }
)
public class SalesOrderNote extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(SalesOrderNote.class.getName());

    public static final String PROPERTY_Id = "Id";
    public static final String P_Id = "Id";
    public static final String PROPERTY_Created = "Created";
    public static final String P_Created = "Created";
    public static final String PROPERTY_Note = "Note";
    public static final String P_Note = "Note";
    public static final String PROPERTY_FollowupDate = "FollowupDate";
    public static final String P_FollowupDate = "FollowupDate";
    public static final String PROPERTY_FollowedUp = "FollowedUp";
    public static final String P_FollowedUp = "FollowedUp";
     
     
    public static final String PROPERTY_ImageStores = "ImageStores";
    public static final String P_ImageStores = "ImageStores";
    public static final String PROPERTY_SalesOrder = "SalesOrder";
    public static final String P_SalesOrder = "SalesOrder";
    public static final String PROPERTY_User = "User";
    public static final String P_User = "User";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String note;
    protected volatile OADate followupDate;
    protected volatile boolean followedUp;
     
    // Links to other objects.
    protected transient Hub<ImageStore> hubImageStores;
    protected volatile transient SalesOrder salesOrder;
    protected volatile transient User user;
     
    public SalesOrderNote() {
        if (!isLoading()) {
            setCreated(new OADateTime());
        }
    }
     
    public SalesOrderNote(int id) {
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
     
    @OAProperty(displayLength = 20, hasCustomCode = true, isHtml = true)
    @OAColumn(sqlType = java.sql.Types.CLOB)
    public String getNote() {
        if (note != null) note = OAString.convertTextToHTML(note, false);
        return note;
    }
    public void setNote(String newValue) {
        String old = note;
        fireBeforePropertyChange(P_Note, old, newValue);
        this.note = newValue;
        firePropertyChange(P_Note, old, this.note);
    }
     
    @OAProperty(displayName = "Followup Date", displayLength = 8, columnName = "Followup")
    @OAColumn(sqlType = java.sql.Types.DATE)
    public OADate getFollowupDate() {
        return followupDate;
    }
    public void setFollowupDate(OADate newValue) {
        OADate old = followupDate;
        fireBeforePropertyChange(P_FollowupDate, old, newValue);
        this.followupDate = newValue;
        firePropertyChange(P_FollowupDate, old, this.followupDate);
    }
     
    @OAProperty(displayName = "Followed Up", displayLength = 5, columnLength = 11)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    public boolean getFollowedUp() {
        return followedUp;
    }
    public void setFollowedUp(boolean newValue) {
        boolean old = followedUp;
        fireBeforePropertyChange(P_FollowedUp, old, newValue);
        this.followedUp = newValue;
        firePropertyChange(P_FollowedUp, old, this.followedUp);
    }
     
    @OAMany(
        displayName = "Image Stores", 
        toClass = ImageStore.class, 
        reverseName = ImageStore.P_SalesOrderNotes
    )
    @OALinkTable(name = "ImageStoreSalesOrderNote", indexName = "ImageStoreSalesOrderNote", columns = {"SalesOrderNoteId"})
    public Hub<ImageStore> getImageStores() {
        if (hubImageStores == null) {
            hubImageStores = (Hub<ImageStore>) getHub(P_ImageStores);
        }
        return hubImageStores;
    }
     
    @OAOne(
        displayName = "Sales Order", 
        reverseName = SalesOrder.P_SalesOrderNotes, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"SalesOrderId"})
    public SalesOrder getSalesOrder() {
        if (salesOrder == null) {
            salesOrder = (SalesOrder) getObject(P_SalesOrder);
        }
        return salesOrder;
    }
    public void setSalesOrder(SalesOrder newValue) {
        SalesOrder old = this.salesOrder;
        fireBeforePropertyChange(P_SalesOrder, old, newValue);
        this.salesOrder = newValue;
        firePropertyChange(P_SalesOrder, old, this.salesOrder);
    }
     
    @OAOne(
        reverseName = User.P_SalesOrderNotes, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        defaultContextPropertyPath = AppUser.P_User
    )
    @OAFkey(columns = {"UserId"})
    public User getUser() {
        if (user == null) {
            user = (User) getObject(P_User);
        }
        return user;
    }
    public void setUser(User newValue) {
        User old = this.user;
        fireBeforePropertyChange(P_User, old, newValue);
        this.user = newValue;
        firePropertyChange(P_User, old, this.user);
    }
     
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.note = rs.getString(3);
        java.sql.Date date;
        date = rs.getDate(4);
        if (date != null) this.followupDate = new OADate(date);
        this.followedUp = (rs.getShort(5) == 1);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, SalesOrderNote.P_FollowedUp, true);
        }
        int salesOrderFkey = rs.getInt(6);
        if (!rs.wasNull() && salesOrderFkey > 0) {
            setProperty(P_SalesOrder, new OAObjectKey(salesOrderFkey));
        }
        int userFkey = rs.getInt(7);
        if (!rs.wasNull() && userFkey > 0) {
            setProperty(P_User, new OAObjectKey(userFkey));
        }
        if (rs.getMetaData().getColumnCount() != 7) {
            throw new SQLException("invalid number of columns for load method");
        }

        changedFlag = false;
        newFlag = false;
    }
}
 
