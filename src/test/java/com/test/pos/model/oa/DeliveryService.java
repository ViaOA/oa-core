package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.lang.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "deliveryService",
    pluralName = "DeliveryServices",
    shortName = "dls",
    displayName = "Delivery Service",
    isLookup = true,
    isPreSelect = true,
    displayProperty = "id",
    noPojo = true
)
@OATable(
)
public class DeliveryService extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(DeliveryService.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
     
    public static final String P_OnlineOrderDeliveries = "onlineOrderDeliveries";
    public static final String P_StsDeliveries = "stsDeliveries";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
     
     
    public DeliveryService() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public DeliveryService(int id) {
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

    @OAProperty(lowerName = "name", maxLength = 75, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "Name", maxLength = 75)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }

    @OAMany(
        displayName = "Online Order Deliveries", 
        toClass = OnlineOrderDelivery.class, 
        reverseName = OnlineOrderDelivery.P_DeliveryService, 
        createMethod = false
    )
    private Hub<OnlineOrderDelivery> getOnlineOrderDeliveries() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Sts Deliveries", 
        toClass = StsDelivery.class, 
        reverseName = StsDelivery.P_DeliveryService, 
        createMethod = false
    )
    private Hub<StsDelivery> getStsDeliveries() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
