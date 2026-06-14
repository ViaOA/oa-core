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
    lowerName = "reward",
    pluralName = "Rewards",
    shortName = "rwr",
    displayName = "Reward",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "RewardRewardType", fkey = true, columns = { @OAIndexColumn(name = "RewardTypeId") })
    }
)
public class Reward extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Reward.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_RewardType = "rewardType";
    public static final String P_RewardTypeId = "rewardTypeId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected volatile transient RewardType rewardType;
     
    public Reward() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Reward(int id) {
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

    @OAOne(
        displayName = "Reward Type", 
        reverseName = RewardType.P_Rewards, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_RewardTypeId, toProperty = RewardType.P_Id)}
    )
    public RewardType getRewardType() {
        if (rewardType == null) {
            rewardType = (RewardType) getObject(P_RewardType);
        }
        return rewardType;
    }
    public void setRewardType(RewardType newValue) {
        RewardType old = this.rewardType;
        fireBeforePropertyChange(P_RewardType, old, newValue);
        this.rewardType = newValue;
        firePropertyChange(P_RewardType, old, this.rewardType);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "RewardTypeId")
    public Integer getRewardTypeId() {
        return (Integer) getFkeyProperty(P_RewardTypeId);
    }
    public void setRewardTypeId(Integer newValue) {
        this.rewardType = null;
        setFkeyProperty(P_RewardTypeId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int rewardTypeFkey = rs.getInt(3);
        setFkeyProperty(P_RewardType, rs.wasNull() ? null : rewardTypeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
