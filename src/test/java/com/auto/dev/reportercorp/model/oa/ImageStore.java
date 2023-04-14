package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "imageStore", pluralName = "ImageStores", shortName = "ims", displayName = "Image Store", useDataSource = false, displayProperty = "id", noPojo = true)
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
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public ImageStore(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
	@OAId
	@OAColumn(name = "id", sqlType = java.sql.Types.INTEGER)
	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		int old = id;
		fireBeforePropertyChange(P_Id, old, newValue);
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
	}

	@OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "created", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
	}

	@OAProperty(isBlob = true, uiColumnLength = 5, isImageName = true)
	@OAColumn(name = "bytes", sqlType = java.sql.Types.BLOB)
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

	@OAProperty(displayName = "Orig File Name", maxLength = 250, displayLength = 30, uiColumnLength = 20)
	@OAColumn(name = "orig_file_name", maxLength = 250)
	public String getOrigFileName() {
		return origFileName;
	}

	public void setOrigFileName(String newValue) {
		String old = origFileName;
		fireBeforePropertyChange(P_OrigFileName, old, newValue);
		this.origFileName = newValue;
		firePropertyChange(P_OrigFileName, old, this.origFileName);
	}
}
