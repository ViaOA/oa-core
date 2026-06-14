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
    lowerName = "imageStore",
    pluralName = "ImageStores",
    shortName = "ims",
    displayName = "Image Store",
    displayProperty = "id",
    noPojo = true
)
@OATable(
)
public class ImageStore extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ImageStore.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Bytes = "bytes";
    public static final String P_OrigFileName = "origFileName";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile transient byte[] bytes;
    protected volatile String origFileName;
     
    public ImageStore() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public ImageStore(int id) {
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

    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
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

    @OAProperty(lowerName = "bytes", isBlob = true, uiColumnLength = 5, isImageName = true)
    @OAColumn(name = "Bytes", sqlType = java.sql.Types.BLOB)
    public byte[] getBytes() {
        if (bytes == null) {
            bytes = getBlob(P_Bytes);
        }
        return bytes;
    }
    public void setBytes(byte[] newValue) {
        byte[] old = bytes;
        fireBeforePropertyChange(P_Bytes, old, newValue);
        this.bytes = newValue;
        firePropertyChange(P_Bytes, old, this.bytes);
    }

    @OAProperty(lowerName = "origFileName", displayName = "Orig File Name", maxLength = 250, displayLength = 30, uiColumnLength = 20)
    @OAColumn(name = "OrigFileName", maxLength = 250)
    public String getOrigFileName() {
        return origFileName;
    }
    public void setOrigFileName(String newValue) {
        String old = origFileName;
        fireBeforePropertyChange(P_OrigFileName, old, newValue);
        this.origFileName = newValue;
        firePropertyChange(P_OrigFileName, old, this.origFileName);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.origFileName = rs.getString(3);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
