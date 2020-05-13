/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.object;

import java.io.IOException;
import java.io.ObjectStreamException;
// java1.2
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlTransient;

import com.viaoa.context.OAContext;
import com.viaoa.context.OAUserAccess;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.remote.multiplexer.OARemoteThreadDelegate;
import com.viaoa.sync.OASync;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncDelegate;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAConverter;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAJaxb;
import com.viaoa.util.OALogger;
import com.viaoa.util.OANotExist;
import com.viaoa.util.OAString;

/**
 * OAObject is the Base Class used for Application Data Objects. It is a central class for OA, where all other objects are designed to
 * automatically work with the OAObject class, along with the Hub collection class.
 * <p>
 * OAObjects have built-in functionality to allow it to work with other Classes. This includes other OAObjects, Hub Collections, any
 * datasource/database, JFC component, JSP component, XML, other applications (distributed) and any other Class.
 * <p>
 * &nbsp;&nbsp;&nbsp;<img src="doc-files/ObjectAutomation1.gif" alt=""> <br>
 * Subclasses of OAObject can be created that add properties and methods for building customized software applications. OAObject then
 * supplies the capability for these subclasses to automatically work with any OA Enabled Class.
 * <p>
 * This is a summary of some of the features included in OAObject.
 * <ul>
 * <li>Object Key - property values that makes this object unique.
 * <li>Reference Information - how objects are related to other object. All references use the actual objects and not the key (or foreign
 * key value). References types include one-one, one-many, many-many, recursive self references, owned and un-owned references, and more.
 * <li>Manages reference objects when working with database/datasource.
 * <li>"Moves" objects when changes are made to a reference property.
 * <li>Methods to set and get properties and convert from and to Strings.
 * <li>Store miscellaneous data in name/value pairs, where name is case insensitive.
 * <li>Initialization during creation
 * <li>Null Values - to know if a primitive property value is null
 * <li>Knows which Hub Collections that an object is a member of.
 * <li>Handles events for object, including property changes and calculated properties.
 * <li>Knows if object is "new"
 * <li>Cascading rules. Cancel, Save, Delete can be cascaded to reference objects.
 * <li>Works directly with OADataSource for storing and retrieving objects.
 * <li>Save Method
 * <li>Delete Method
 * <li>Calculated Properties - properties that rely on other properties or objects for their value.
 * <li>Serialization Support - to file/stream, other applications using RMI
 * <li>XML support - reading and writing
 * <li>Locking
 * <li>Client/Server - changes to objects can be automatically updated on other computers.
 * </ul>
 * <p>
 * This is a listing of the types of relationships that an OAObject can have with another OAObject. This information is built into the
 * object information. Relationships between objects are "two-way", meaning that both objects are related to each other.<br>
 * <ul>
 * <li>One-One relationship
 * <li>One-Many relationship
 * <li>Many-Many relationship
 * <li>Recursive - this is where an object can have many children objects of the same class and each of these children can themselves have
 * children, recursively.
 * <li>An Owned relationship is one where the children can not exist without the parent (owner) and all are treated as a single unit.
 * <li>Cascading Rules for save, delete, cancel
 * </ul>
 * <p>
 * Managing Relationships<br>
 * OAObject manages the relationships between objects, and is responsible for retrieving and populating reference objects and for managing
 * changes. An OAObject subclass does not have to have any code to handle retrieving or storing reference objects, OAObject does it
 * completely. If a reference property is changed, then OAObject manages the change so that other objects are updated correctly. <br>
 * For example, if a Department has many Employees, and an Employee has one Department: if an Employee's Department is changed, then the
 * Employee object is removed from the original Department collection and added to the new assigned Department collection. This also works
 * when an Employee is added to a different Departments Employee collection - the Employee's Department property is changed to the newly
 * assigned Department.
 * <p>
 * Working with DataSources<br>
 * OAObjects work directly with OADataSource for initializing properties, saving, deleting. This is all done so that the OAObjects are
 * independent from datasource/database.
 * <p>
 * For more information about this package, see <a href="package-summary.html#package_description">documentation</a>.
 * 
 * @author Vince Via
 * @see Hub for observable collection class that has "linkage" features for automatically managing relationships. see OAHtmlSelect for
 *      datasource independent queries based on object and property paths.
 */
@XmlTransient()
public class OAObject implements java.io.Serializable, Comparable {

	private static final long serialVersionUID = 1L; // internally used by Java Serialization to identify this version of OAObject.

	private static final String oaversion;

	public static String getOAVersion() {
		return oaversion;
	}

	static {
		// oaversion
		String ver = "3.6.2.20200511";
		/*
		try {
		    InputStream resourceAsStream = OAObject.class.getResourceAsStream("/META-INF/maven/com.viaoa/oa/pom.properties");
		    Properties props = new Properties();
		    props.load(resourceAsStream);
		
		    // String g = props.getProperty("groupId");
		    // String a = props.getProperty("artifactId");
		    ver = props.getProperty("version");
		}
		catch (Exception e) {
		}
		*/
		oaversion = ver;
		System.out.println("oa-core version=" + oaversion);
	}

	// system wide to track all changes to OAObject
	public static final Logger OALOG = OALogger.getLogger("OAObject");

	private static final Logger LOG = OALogger.getLogger(OAObject.class);

	protected int guid; // global identifier for this object
	protected volatile OAObjectKey objectKey; // Object identifier, used by Hub/HubController for hashing, etc.
	protected volatile boolean changedFlag = true; // flag to know if this object has been changed
	protected volatile boolean newFlag = true; // flag to know if this object is new (not yet saved).  The object key properties can be changed as long as isNew is true.
	protected byte[] nulls; // keeps track of which primitive type properties that are NULL. Uses bit position, based on OAObjectInfo getPrimitiveProperties() position
	protected volatile boolean deletedFlag;

	// list of Hub Collections that this object is a member of.  
	// OAObject uses these Hubs for sending events.  See: OAObjectHubDelegate
	// elements will be one of the following: 
	//   Hub - if a reference to object needs to be maintained, so that it wont be GCd and can be saved
	//   null - empty slot
	//   WeakReference<Hub> (default) - so that it does not hold the Hub from being GCd
	protected transient volatile WeakReference<Hub<?>>[] weakhubs;

	/**
	 * Link/reference properties that have been loaded. Stores uppercase name of property. Possible values: ONE: OAObjectKey (by calling
	 * setProperty(), the value used will be converted to an OAObjectKey OAObject for the value of the reference MANY: WeakReference to Hub.
	 * The objects in the Hub can be OAObjectKey values that will automatically be retrieved and converted to the correct class of object.
	 */

	/** managed by OAObjectPropertyDelegate.java */
	protected volatile transient Object[] properties; // stores references (oaobj, hub, oaobjkey), or misc property for object.  ex: [0]="Employee" [1]=Emp [2]="Order" [3]=oakey

	/** Cascade rule where no reference objects will be included. */
	public static final int CASCADE_NONE = 0;

	/** Cascade rule where all defined rules for references will be included. This is default for save() and delete(). */
	public static final int CASCADE_LINK_RULES = 1;

	/** Cascade rule where all only the owned references will be included. */
	public static final int CASCADE_OWNED_LINKS = 2;

	/** Cascade rule where all reference objects are followed, even if cascade rule is false. */
	public static final int CASCADE_ALL_LINKS = 4;

	public static volatile int cntNew;
	public static volatile int cntFinal;

	/**
	 * Creates new OAObject and calls OAObjectDelegate.initialize()
	 * 
	 * @see OAObjectDelegate#initialize
	 */
	public OAObject() {
		OAObjectDelegate.initialize(this);

		cntNew++;
		if (cntNew % 500 == 0) {
			//System.out.println(cntNew+") new OAObject.guid="+guid+" "+this.getClass().getSimpleName());
		}

		// 20141209 removed, since it was creating dup oaObjKeys, one when putting in cache, then clearing it, and then
		//    creating another the next time that OAObj calls for it.
		// 20141127 Note: call oaObject.toString(), until the object is loaded, since it will create an objectKey with Id=0
		//if (objectKey != null) objectKey = null; // in case it was generated before the Id was loaded.
	}

	/**
	 * Read OAObject data. Note: This method must stay "private" or it will never be called. It does not need to be subclassed because any
	 * object that is a subclass should have its own readObject() method. ObjectInputStream.readObject() calls the readObject() for each
	 * class, superClass, and subClass individually.
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		OAObjectSerializeDelegate._readObject(this, in);
	}

	/* 
	 * This is called by serialization to check if object already exists in the Cache.
	 * @see OAObjectSerializeDelegate#_readResolve
	 */
	protected Object readResolve() throws ObjectStreamException {
		Object obj = OAObjectSerializeDelegate._readResolve(this);
		return obj;
	}

	/*
	 *  Used to serialize and object.  
	 *  @see OAObjectSerializeDelegate#_writeObject to see how objects can be custom written by selecting the properties that will be sent 
	 *  in the object graph.
	 */
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		OAObjectSerializeDelegate._writeObject(this, stream);
	}

	/* calls setProperty()
	    @see #setProperty(String, Object, String)
	*/
	public void setProperty(String propName, boolean value) {
		OAObjectReflectDelegate.setProperty(this, propName, value ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE, null);
	}

	/**
	 * calls setProperty()
	 * 
	 * @see #setProperty(String, Object, String)
	 */
	public void setProperty(String propName, int value) {
		OAObjectReflectDelegate.setProperty(this, propName, new Integer(value), null);
	}

	/* calls setProperty()
	    @see #setProperty(String, Object, String)
	*/
	public void setProperty(String propName, long value) {
		OAObjectReflectDelegate.setProperty(this, propName, new Long(value), null);
	}

	/* calls setProperty()
	    @see #setProperty(String, Object, String)
	*/
	public void setProperty(String propName, double value) {
		OAObjectReflectDelegate.setProperty(this, propName, new Double(value), null);
	}

	/* calls setProperty()
	    @see #setProperty(String, Object, String)
	*/
	public void setProperty(String propName, Object value) {
		OAObjectReflectDelegate.setProperty(this, propName, value, null);
	}

	public void setNull(String propName) {
		OAObjectReflectDelegate.setProperty(this, propName, null, null);
	}

	/**
	 * Generic way for setting any property and storing name/value pairs.<br>
	 * If propertyName is a valid property, then the setX method will be called, where X is the name of property.<br>
	 * If propertyName is a valid property and obj is a String, obj will be converted to the correct obj that the method needs. Ex: if
	 * property method is for an OADate and obj = "05/04/65", then obj will be converted to a date.<br>
	 * If propertyName is a valid property that is an OAObject reference, then obj can be the ObjectKey value instead of the object
	 * itself.<br>
	 * if value is a String and property is a reference property, then value will be converted to match objectId type.<br>
	 * <br>
	 * Ex: if employee.setProperty("Dept", 12), where Dept is a reference object, 12 will be used to find correct Dept obj<br>
	 * Ex: if employee.setProperty("Dept", "12"), "12" will be converted to int 12<br>
	 * Ex: employee.setSalary("32,500"), where salary property is an int. "32,500" will be converted to (int) 32500<br>
	 * Ex: employee.setHireDate("05/24/1988"), where HireDate property is an OADate and will be converted<br>
	 * <br>
	 * If property is a Hub, then the following is done:
	 * <ol>
	 * <li>if hub has already loaded by calling getHub(propName), then value is converted to object and then added into Hub.
	 * <li>if hub has not been loaded by calling getHub(propName), but has been created:<br>
	 * a: if value is an OAObject, then getHub(propName) is called and value is added to Hub.<br>
	 * b: Hub for class OAObjectKey is created and value is converted to OAObjectKey and then added. When getHub(propName) is called, then
	 * all of the ObjectKeys will be converted to objects.
	 * </ol>
	 * 
	 * @param propName
	 *            name of property
	 * @param value
	 *            new value
	 * @param fmt
	 *            format to convert from
	 * @see OAObjectReflectDelegate#setProperty
	 */
	public void setProperty(String propName, Object value, String fmt) {
		OAObjectReflectDelegate.setProperty(this, propName, value, fmt);
	}

	/**
	 * Generic way for getting any property or value. This will first look for get"PropName" method in this object (including superclass
	 * OAObject) Note: this supports property paths. For example: "dept.manager.lastname" from an Employee.class Note: if the property is of
	 * a primitive type, it can return null.
	 * 
	 * @param propName
	 *            can be a property path. If a Hub property is in the path and is not the last property, then the ActiveObject will be used.
	 */
	public Object getProperty(String propName) {
		return OAObjectReflectDelegate.getProperty(this, propName);
	}

	/**
	 * Generic way for getting any property or value as a String value.
	 */
	public String getPropertyAsString(String propName) {
		return getPropertyAsString(propName, null);
	}

	public String getPropertyAsString(String propName, boolean bUseDefaultFormatting) {
		Object obj = getProperty(propName);
		return OAConverter.toString(obj, bUseDefaultFormatting);
	}

	/**
	 * Generic way for getting any property or value as a String value.
	 * 
	 * @return if value is null then "", else formatted value using OAConverter.toString(value,fmt)
	 * @param propName
	 * @param fmt
	 */
	public String getPropertyAsString(String propName, String fmt) {
		Object obj = getProperty(propName);
		if (obj == null) {
			return ""; // note: if null is sent to OAConvert.toString(...), it wont know the correct class to use to - since obj=null
		}
		return OAConverter.toString(obj, fmt);
	}

	/*
	    Generic way for getting any property or value as a String value.
	    @return if value is null then nullValue, else formatted value using OAConverter.toString(value,fmt)
	*/
	public String getPropertyAsString(String propName, String fmt, String nullValue) {
		Object obj = getProperty(propName);
		if (obj == null) {
			return nullValue;
		}
		return OAConverter.toString(obj, fmt);
	}

	/**
	 * removing property. If this property caused isChanged() to be true, then isChanged() will be false.
	 * 
	 * @param name
	 *            of property to remove. (case insensitive)
	 */
	public void removeProperty(String name) {
		OAObjectPropertyDelegate.removeProperty(this, name, true);
	}

	/**
	 * allows other components to interact with OAObject property, by call
	 * 
	 * @see OAObjectEditQueryDelegate#getVerifyPropertyChange(int, OAObject, String, Object, Object)
	 */
	public boolean isValidPropertyChange(String propertyName, Object oldValue, Object newValue) {
		return OAObjectEditQueryDelegate.getVerifyPropertyChange(	OAObjectEditQuery.CHECK_CallbackMethod, this, propertyName, oldValue,
																	newValue);
	}

	public OAObjectEditQuery getIsValidPropertyChangeEditQuery(String propertyName, Object oldValue, Object newValue) {
		OAObjectEditQuery eq = OAObjectEditQueryDelegate.getVerifyPropertyChangeEditQuery(	OAObjectEditQuery.CHECK_CallbackMethod, this,
																							propertyName, oldValue, newValue);
		return eq;
	}

	public boolean isEnabled(String propertyName) {
		return OAObjectEditQueryDelegate.getAllowEnabled(OAObjectEditQuery.CHECK_ALL, null, this, propertyName);
	}

	public OAObjectEditQuery getIsEnabledEditQuery(String propertyName, Object oldValue, Object newValue) {
		OAObjectEditQuery eq = OAObjectEditQueryDelegate.getAllowEnabledEditQuery(OAObjectEditQuery.CHECK_ALL, null, this, propertyName);
		return eq;
	}

	public boolean isEnabled() {
		return OAObjectEditQueryDelegate.getAllowEnabled(OAObjectEditQuery.CHECK_ALL, null, this, null);
	}

	public OAObjectEditQuery getIsEnabledEditQuery() {
		OAObjectEditQuery eq = OAObjectEditQueryDelegate.getAllowEnabledEditQuery(OAObjectEditQuery.CHECK_ALL, null, this, null);
		return eq;
	}

	public boolean isVisible(String propertyName) {
		return OAObjectEditQueryDelegate.getAllowVisible(null, this, propertyName);
	}

	public OAObjectEditQuery getIsVisibleEditQuery(String propertyName) {
		OAObjectEditQuery eq = OAObjectEditQueryDelegate.getAllowVisibleEditQuery(null, this, propertyName);
		return eq;
	}

	public boolean isVisible() {
		return OAObjectEditQueryDelegate.getAllowVisible(null, this, null);
	}

	public OAObjectEditQuery getIsVisibleEditQuery() {
		OAObjectEditQuery eq = OAObjectEditQueryDelegate.getAllowVisibleEditQuery(null, this, null);
		return eq;
	}

	public boolean verifyCommand(String methodName) {
		OAObjectEditQuery eq = OAObjectEditQueryDelegate.getVerifyCommandEditQuery(this, methodName, OAObjectEditQuery.CHECK_ALL);
		return eq.getAllowed();
	}

	public OAObjectEditQuery getVerifyCommand(String methodName) {
		OAObjectEditQuery eq = OAObjectEditQueryDelegate.getVerifyCommandEditQuery(this, methodName, OAObjectEditQuery.CHECK_ALL);
		return eq;
	}

	/**
	 * Flag to know if object is new and has not been saved.
	 */
	public boolean getNew() {
		return newFlag;
	}

	public boolean isNew() {
		return newFlag;
	}

	@XmlTransient
	public void setNew(boolean b) {
		OAObjectDelegate.setNew(this, b);
	}

	/**
	 * Flag to know if object was deleted.
	 */
	public boolean getDeleted() {
		return deletedFlag;
	}

	public boolean wasDeleted() {
		return deletedFlag;
	}

	public boolean isDeleted() {
		return deletedFlag;
	}

	@XmlTransient
	public void setDeleted(boolean tf) {
		OAObjectDeleteDelegate.setDeleted(this, tf);
	}

	/**
	 * OAObjects are equal if:
	 * <ul>
	 * <li>the objects are the same address.
	 * <li>the objects are the same class and the values of the propertyIds are equal. If both objects isNew() and either one has a its
	 * propertyId.isNull(), then they will never be equal.
	 * <li>if the object being compared to is equal to the objectId property of this object.
	 * </ul>
	 * 
	 * @param obj
	 *            object to compare to, object or objects[] to compare this object's objectId(s) with or OAObjectKey to compare with this
	 *            object's objectId
	 */
	public final boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}

		//20141125 if obj is oaObj, then need to make sure that they are same class 
		if (obj instanceof OAObject) {
			if (!obj.getClass().equals(this.getClass())) {
				return false;
			}
		}

		return OAObjectKeyDelegate.getKey(this).equals(obj);
	}

	//20140128 add hashCode
	@Override
	public int hashCode() {
		return OAObjectKeyDelegate.getKey(this).hashCode();
	}

	public int compareTo(Object obj) {
		if (obj == null) {
			return 1;
		}
		if (obj == this) {
			return 0;
		}
		if (!obj.getClass().equals(this.getClass())) {
			return -1;
		}
		return OAObjectKeyDelegate.getKey(this).compareTo(obj);
	}

	/**
	 * Returns true if this object is new or any changes have been made to this object or any objects in Links that are CASCADE=true
	 */
	public boolean getChanged() {
		return getChanged(CASCADE_NONE);
	}

	public boolean isChanged() {
		return getChanged(CASCADE_NONE);
	}

	public boolean getChanged(boolean bIncludeLinks) {
		return getChanged(bIncludeLinks ? CASCADE_LINK_RULES : CASCADE_NONE);
	}

	public boolean isChanged(boolean bIncludeLinks) {
		return getChanged(bIncludeLinks ? CASCADE_LINK_RULES : CASCADE_NONE);
	}

	/**
	 * Returns true if this object is new or any changes have been made to this object or any objects in Links that are TYPE=MANY and
	 * CASCADE=true that match the relationshipType parameter.
	 */
	public boolean getChanged(int relationshipType) {
		return OAObjectDelegate.getChanged(this, relationshipType);
	}

	/**
	 * Flag to know if object has been changed.
	 * <p>
	 * This is automatically set to true whenever firePropertyChange. It is set to false when save() is called.
	 * 
	 * @param tf
	 *            if false then all original values of changed properties will be removed.
	 */
	@XmlTransient
	public void setChanged(boolean tf) {
		if (changedFlag != tf) {
			boolean bOld = changedFlag;

			OAObjectEventDelegate.fireBeforePropertyChange(	this, OAObjectDelegate.WORD_Changed,
															bOld ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE,
															tf ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE,
															tf, // local only  20150530 was: "false", now only sending if changed=false 
															false);
			changedFlag = tf;
			OAObjectEventDelegate.firePropertyChange(	this, OAObjectDelegate.WORD_Changed,
														bOld ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE,
														changedFlag ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE, false, false);

			if (changedFlag) {
				// 20190307
				if (!isRemoteThread()) {
					OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(this.getClass());
					OAPropertyInfo pi = oi.getTimestampProperty();
					if (pi != null) {
						this.setProperty(pi.getName(), new OADateTime());
					}
				}

				OAObjectPropertyDelegate.setReferenceable(this, true);

				// 20180520 notify owner
				WeakReference<Hub<?>>[] refs = OAObjectHubDelegate.getHubReferencesNoCopy(this);
				if (refs != null) {
					for (WeakReference wr : refs) {
						if (wr == null) {
							continue;
						}
						Hub hx = (Hub) wr.get();
						if (hx == null) {
							continue;
						}

						OAObject obj = hx.getMasterObject();
						if (obj != null) {
							OALinkInfo li = HubDetailDelegate.getLinkInfoFromMasterHubToDetail(hx);
							if (li != null && (li.getCascadeSave() || li.getOwner())) {
								obj.setChanged(true);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Copies the properties and some of the links from a source object (this) to a new object. For links of type One, all of the links are
	 * used, the same ref object from the source object is used. For links of type Many, only the owned links are used, and clones of the
	 * objects are created in the Hub of the new object. Note: this is done on the server.
	 * 
	 * @see OAObjectReflectDelegate#copyInto(OAObject, OAObject, String[], OACopyCallback)
	 */
	public OAObject createCopy() {
		return OAObjectReflectDelegate.createCopy(this, null);
	}

	public Object createCopy(String[] excludePropertyNames) {
		return OAObjectReflectDelegate.createCopy(this, excludePropertyNames);
	}

	/* 
	    @see OAObjectReflectDelegate#copyInto(OAObject, OAObject, String[], OACopyCallback)
	*/
	public void copyInto(OAObject toObject) {
		OAObjectReflectDelegate.copyInto(this, toObject, (String[]) null, null);
	}

	/**
	 * Option to have finalized objects automatically saved to datasource. Default is false.
	 * 
	 * @param b
	 *            to set value
	 */
	public static void setFinalizeSave(boolean b) {
		OAObjectDelegate.bFinalizeSave = b;
	}

	public static boolean getFinalizeSave() {
		return OAObjectDelegate.bFinalizeSave;
	}

	/**
	 * Removes object from HubController and calls super.finalize().
	 */
	protected void finalize() throws Throwable {
		OAObjectDelegate.finalizeObject(this);
		super.finalize();
		cntFinal++;
		//if (cntFinal % 500 == 0) System.out.println(cntFinal+") finalize OAObject.guid="+guid+" "+this.getClass().getSimpleName());
	}

	/**
	 * True if this object is in process of being loaded.
	 */
	public boolean isLoading() {
		return OAThreadLocalDelegate.isLoading();
	}

	/**
	 * Used to manage property changes. Sends a "beforePropertyChange()" to all listeners of the Hubs that this object is a member of. <br>
	 * 
	 * @param propertyName
	 *            is not case sensitive
	 */
	protected void fireBeforePropertyChange(String propertyName, Object oldObj, Object newObj, boolean bLocalOnly) {
		OAObjectEventDelegate.fireBeforePropertyChange(this, propertyName, oldObj, newObj, bLocalOnly, true);
	}

	protected void fireBeforePropertyChange(String propertyName, Object oldObj, Object newObj) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(this.getClass());
		OAObjectEventDelegate.fireBeforePropertyChange(this, propertyName, oldObj, newObj, oi.getLocalOnly(), true);
	}

	/* @see #fireBeforePropertyChange(String, Object, Object, boolean, boolean) firePropertyChange */
	protected void fireBeforePropertyChange(String property, boolean oldObj, boolean newObj) {
		fireBeforePropertyChange(	property, oldObj ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE,
									newObj ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE);
	}

	/* @see #fireBeforePropertyChange(String, Object, Object, boolean, boolean) firePropertyChange */
	protected void fireBeforePropertyChange(String property, int oldObj, int newObj) {
		fireBeforePropertyChange(property, new Integer(oldObj), new Integer(newObj));
	}

	/* @see #fireBeforePropertyChange(String, Object, Object, boolean, boolean) firePropertyChange */
	protected void fireBeforePropertyChange(String property, long oldObj, long newObj) {
		fireBeforePropertyChange(property, new Long(oldObj), new Long(newObj));
	}

	/* @see #fireBeforePropertyChange(String, Object, Object, boolean, boolean) firePropertyChange */
	protected void fireBeforePropertyChange(String property, double oldObj, double newObj) {
		fireBeforePropertyChange(property, new Double(oldObj), new Double(newObj));
	}

	protected void firePropertyChange(String propertyName, Object oldObj, Object newObj, boolean bLocalOnly) {
		OAObjectEventDelegate.firePropertyChange(this, propertyName, oldObj, newObj, bLocalOnly, true);
	}

	protected void firePropertyChange(String propertyName, Object oldObj, Object newObj) {
		OAObjectEventDelegate.firePropertyChange(this, propertyName, oldObj, newObj, false, true);
	}

	protected void firePropertyChange(String propertyName) {
		OAObjectEventDelegate.firePropertyChange(this, propertyName, null, null, false, true, true);
	}

	/* @see #firePropertyChange(String, Object, Object, boolean, boolean) firePropertyChange */
	protected void firePropertyChange(String property, boolean oldObj, boolean newObj) {
		firePropertyChange(	property, oldObj ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE,
							newObj ? OAObjectDelegate.TRUE : OAObjectDelegate.FALSE);
	}

	/* @see #firePropertyChange(String, Object, Object, boolean, boolean) firePropertyChange */
	protected void firePropertyChange(String property, int oldObj, int newObj) {
		firePropertyChange(property, new Integer(oldObj), new Integer(newObj));
	}

	/* @see #firePropertyChange(String, Object, Object, boolean, boolean) firePropertyChange */
	protected void firePropertyChange(String property, long oldObj, long newObj) {
		firePropertyChange(property, new Long(oldObj), new Long(newObj));
	}

	/* @see #firePropertyChange(String, Object, Object, boolean, boolean) firePropertyChange */
	protected void firePropertyChange(String property, double oldObj, double newObj) {
		firePropertyChange(property, new Double(oldObj), new Double(newObj));
	}

	/*
	    Version of firePropertyChange that will not send to OAServer.
	    @see #firePropertyChange(String, Object, Object, boolean, boolean) firePropertyChange
	*/
	protected void fireLocalPropertyChange(String property, Object oldObj, Object newObj) {
		firePropertyChange(property, oldObj, newObj, true);
	}

	protected void fireLocalPropertyChange(String property) {
		OAObjectEventDelegate.firePropertyChange(this, property, null, null, true, true, true);
	}

	/*
	    Version of firePropertyChange that will not send to OAServer.
	    @see #firePropertyChange(String, Object, Object, boolean, boolean) firePropertyChange
	*/
	protected void fireLocalPropertyChange(String property, int oldObj, int newObj) {
		firePropertyChange(property, new Integer(oldObj), new Integer(newObj), true);
	}

	/*
	    Retrieves reference property that is for a Hub Collection.
	    @see #getHub(String, String)
	*/
	protected Hub getHub(String linkPropertyName) {
		return OAObjectReflectDelegate.getReferenceHub(this, linkPropertyName, null, false, null);
	}

	public void setHub(String linkPropertyName, Hub hub) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(this);
		OALinkInfo linkInfo = OAObjectInfoDelegate.getLinkInfo(oi, linkPropertyName);

		if (OAObjectInfoDelegate.cacheHub(linkInfo, hub)) {
			OAObjectPropertyDelegate.setProperty(this, linkPropertyName, new WeakReference(hub));
		} else {
			OAObjectPropertyDelegate.setProperty(this, linkPropertyName, hub);
		}
	}

	/**
	 * DataSource independent method to retrieve a reference property that is a Hub Collection.
	 * 
	 * @param linkPropertyName
	 *            name of property to retrieve. (case insensitive)
	 */
	protected Hub getHub(String linkPropertyName, String sortOrder) {
		return OAObjectReflectDelegate.getReferenceHub(this, linkPropertyName, sortOrder, false, null);
	}

	/**
	 * @param bSequence
	 *            if true, then a setAutoSequence will be called on the hub
	 */
	protected Hub getHub(String linkPropertyName, String sortOrder, boolean bSequence) {
		return OAObjectReflectDelegate.getReferenceHub(this, linkPropertyName, sortOrder, bSequence, null);
	}

	/**
	 * @param bSequence
	 *            if true, then a setAutoSequence will be called on the hub
	 */
	protected Hub getHub(String linkPropertyName, String sortOrder, boolean bSequence, Hub hubMatch) {
		return OAObjectReflectDelegate.getReferenceHub(this, linkPropertyName, sortOrder, bSequence, null);
	}

	protected Hub getHub(String linkPropertyName, String sortOrder, Hub hubMatch) {
		return OAObjectReflectDelegate.getReferenceHub(this, linkPropertyName, sortOrder, false, hubMatch);
	}

	protected Hub getHub(String linkPropertyName, Hub hubMatch) {
		return OAObjectReflectDelegate.getReferenceHub(this, linkPropertyName, null, false, hubMatch);
	}

	/**
	 * DataSource independent method to retrieve a reference property.
	 * <p>
	 * If reference object is not already loaded, then OADataSource will be used to retreive object.
	 */
	protected Object getObject(String linkPropertyName) {
		Object obj = OAObjectReflectDelegate.getReferenceObject(this, linkPropertyName);
		return obj;
	}

	public boolean isReferenceObjectNull(String name) {
		boolean b = OAObjectReflectDelegate.isReferenceObjectNullOrEmpty(this, name);
		return b;
	}

	/**
	 * DataSource independent method to retrieve a blob/byte[] property.
	 * <p>
	 * If reference object is not already loaded, then OADataSource will be used to retrieve object.
	 */
	protected byte[] getBlob(String linkPropertyName) {
		return OAObjectReflectDelegate.getReferenceBlob(this, linkPropertyName);
	}

	/**
	 * This is used to save object to OADataSource and flag object as being saved and no longer new.
	 * <p>
	 * It does the following:
	 * <ol>
	 * <li>calls canSave() to verify that object and its LinkInfo objects that TYPE=MANY and CASCADE=true can be saved. This will call this
	 * objects beforeSave() method. It will then call hubBeforeSave() for all listeners .
	 * <li>calls this Objects "onSave()" method. The default onSave() will call OADataSource to save.
	 * <li>calls listeners hubAfterSave()
	 * <li>calls "cascadeSave()" to save all Links with TYPE=ONE and CASCADE=true
	 * </ol>
	 * 
	 * @see #isChanged
	 */
	public void save() {
		boolean b3 = OAThreadLocalDelegate.setAdmin(true);
		try {
			this.save(CASCADE_LINK_RULES);
		} finally {
			OAThreadLocalDelegate.setAdmin(b3);
		}
	}

	/**
	 * @param iCascadeRule
	 *            OR combination of CASCADE, ALL, FORCE, NOCHECK
	 * @see #save()
	 */
	public void save(int iCascadeRule) {
		if (!canSave()) {
			if (iCascadeRule == CASCADE_NONE) {
				throw new RuntimeException("can Save returned false for " + getClass().getSimpleName());
			}
			return; // else allow it to keep going
		}

		OAObjectSaveDelegate.save(this, iCascadeRule); // this will save on server if using OAClient
	}

	public boolean canSave() {
		boolean flag = OAObjectEditQueryDelegate.getAllowSave(this, OAObjectEditQuery.CHECK_ALL);
		return flag;
	}

	/**
	 * Cascade save all links.
	 */
	public void saveAll() {
		OAObjectSaveDelegate.save(this, OAObject.CASCADE_ALL_LINKS);
	}

	/**
	 * called after an object is saved
	 */
	public void afterSave() {
	}

	/**
	 * Remove this object from all hubs and deletes object from OADataSource. Calls canDelete first. see Hub#deleted
	 */
	public void delete() {
		// verify with editQuery
		if (!OARemoteThreadDelegate.isRemoteThread()) {
			OAObjectEditQuery em = OAObjectEditQueryDelegate.getVerifyDeleteEditQuery(null, this, OAObjectEditQuery.CHECK_CallbackMethod);
			if (!em.getAllowed()) {
				String s = em.getResponse();
				if (OAString.isEmpty(s)) {
					s = "edit query returned false for delete, object=" + this;
				}
				throw new RuntimeException(s, em.getThrowable());
			}
		}
		OAObjectDeleteDelegate.delete(this);
	}

	public boolean canDelete() {
		boolean b = OAObjectEditQueryDelegate.getAllowDelete(null, this);
		return b;
	}

	/**
	 * called after an object is deleted.
	 */
	public void afterDelete() {
	}

	/**
	 * Creates a lock on this object. see OALock#lock(Object,Object,Object)
	 */
	public void lock() {
		OAObjectLockDelegate.lock(this);
	}

	/**
	 * Unlocks this object.
	 */
	public void unlock() {
		OAObjectLockDelegate.unlock(this);
	}

	/**
	 * Checks to see if object is locked.
	 */
	public boolean isLocked() {
		return OAObjectLockDelegate.isLocked(this);
	}

	/**
	 * Using a propertyPath from this object, find the first matching object.
	 * <p>
	 * Example: find a SectionItem from a SectionItem<br>
	 * SectionItem si = (SectionItem) secItem.find("section.templateRow.template.templateRows.sections.sectionItems.item", item);
	 * 
	 * @see #findAll(String,Object)
	 */
	public Object find(String propertyPath, Object value) {
		Object[] objs = OAObjectDelegate.find(this, propertyPath, value, false);
		if (objs != null && objs.length > 0) {
			return objs[0];
		}
		return null;
	}

	/**
	 * Using a propertyPath from this object, find all of the matching objects.
	 * <p>
	 * Example: find a SectionItem from a SectionItem<br>
	 * SectionItem si = (SectionItem) secItem.find("section.templateRow.template.templateRows.sections.sectionItems.item", item);
	 * 
	 * @see OAObject#find(String,Object)
	 */
	public Object[] findAll(String propertyPath, Object value) {
		return OAObjectDelegate.find(this, propertyPath, value, true);
	}

	//todo: needs to work for all properties, not just primitives ??
	public boolean isNull(String prop) {
		boolean b = OAObjectReflectDelegate.getPrimitiveNull(this, prop);
		return b;
	}

	/**
	 * This is used so that code will only be ran on the server. If the current thread is an OAClientThread, then it will still send
	 * messages to other clients.
	 */
	public boolean isServer() {
		return OASyncDelegate.isServer(getClass());
	}

	public boolean isClient() {
		return !OASyncDelegate.isServer(getClass());
	}

	/*
	public boolean beginServerOnly() {
	    return OASync.beginServerOnly(getClass().getPackage());
	}
	public boolean endServerOnly() {
	    return OASync.endServerOnly(getClass().getPackage());
	}
	*/

	/**
	 * All OASync messages will be processed by an OARemoteThread.
	 */
	public boolean isRemoteThread() {
		return OARemoteThreadDelegate.isRemoteThread();
	}

	/**
	 * This is used to send out OASync messages, even if the currentThread is a OARemoteThread.
	 */
	public boolean sendMessages(boolean b) {
		return OARemoteThreadDelegate.sendMessages(b);
	}

	public boolean sendMessages() {
		return OARemoteThreadDelegate.sendMessages(true);
	}

	/**
	 * Called after an object has been loaded from a datasource.
	 */
	public void afterLoad() {
		OAObjectEmptyHubDelegate.initialize(this);
		OAObjectEventDelegate.fireAfterLoadEvent(this);
		OAObjectCacheDelegate.fireAfterLoadEvent(this);
	}

	public OAObjectKey getObjectKey() {
		return OAObjectKeyDelegate.getKey(this);
	}

	public int getGuid() {
		return guid;
	}

	// 20130630
	/**
	 * Used to determine if an object should be added to a reference/master hub when one of it's OAObject properties is set. If false, then
	 * the object will not be added to masterHubs until this is called with "true" or when oaObj is saved.
	 * 
	 * @param b
	 *            (default is true)
	 */
	public void setAutoAdd(boolean b) {
		OAObjectDelegate.setAutoAdd(this, b);
	}

	@XmlTransient
	public boolean getAutoAdd() {
		return OAObjectDelegate.getAutoAdd(this);
	}

	public boolean isEmpty(Object obj) {
		return OAString.isEmpty(obj);
	}

	public boolean isHubLoaded(String name) {
		Object objx = OAObjectPropertyDelegate.getProperty(this, name, true, true);
		if (objx == OANotExist.instance) {
			return false;
		}
		if (objx == null) {
			return true;
		}
		if (objx instanceof WeakReference) {
			if (((WeakReference) objx).get() == null) {
				return false;
			}
		}
		return true;
	}

	public void loadReferences(boolean bIncludeCalc) {
		OAObjectReflectDelegate.loadAllReferences(this, bIncludeCalc);
	}

	public void loadReferences(boolean bOne, boolean bMany, boolean bIncludeCalc) {
		OAObjectReflectDelegate.loadAllReferences(this, bOne, bMany, bIncludeCalc);
	}

	public void loadReferences(int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc) {
		OAObjectReflectDelegate.loadAllReferences(this, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc);
	}

	public void loadReferences(int maxLevelsToLoad, int additionalOwnedLevelsToLoad, boolean bIncludeCalc, int maxRefsToLoad) {
		int x = OAObjectReflectDelegate.loadAllReferences(this, maxLevelsToLoad, additionalOwnedLevelsToLoad, bIncludeCalc, maxRefsToLoad);
	}

	// 20160506
	/*
	 * 
	 * @param params
	 * @return
	 */
	public static Object callRemote(Hub hub, Object... args) {
		if (hub == null) {
			return null;
		}

		StackTraceElement[] sts = Thread.currentThread().getStackTrace();
		String mname = sts[2].getMethodName();

		final Class clazz = hub.getObjectClass();

		if (OASyncDelegate.isServer(clazz) || OARemoteThreadDelegate.isRemoteThread()) {
			throw new RuntimeException("method " + mname + ", isRemoable=false, thread=" + Thread.currentThread());
		}

		OASyncClient sc = OASync.getSyncClient();
		if (sc == null) {
			throw new RuntimeException("method " + mname + ", OASyncClient=null, thread=" + Thread.currentThread());
		}

		RemoteServerInterface rs;
		try {
			rs = sc.getRemoteServer();
		} catch (Exception e) {
			throw new RuntimeException("method " + mname + ", OASyncClient=null, thread=" + Thread.currentThread(), e);
		}

		if (rs == null) {
			throw new RuntimeException("method " + mname + ", RemoteServerInterface=null, thread=" + Thread.currentThread());
		}

		Object result = rs.runRemoteMethod(hub, mname, args);
		return result;
	}

	public Object remote(Object... args) {
		StackTraceElement[] sts = Thread.currentThread().getStackTrace();
		String mname = sts[2].getMethodName();

		if (!isRemoteAvailable()) {
			throw new RuntimeException("method " + mname + ", isRemoable=false, thread=" + Thread.currentThread());
		}

		OASyncClient sc = OASync.getSyncClient();
		if (sc == null) {
			throw new RuntimeException("method " + mname + ", OASyncClient=null, thread=" + Thread.currentThread());
		}

		//qqqqqqqqqqqqqqqqqqqqqqqqqqq 20190918
		if (OAObjectCSDelegate.isInNewObjectCache(this)) {
			OAObjectCSDelegate.addToServerSideCache(this);
		}

		RemoteServerInterface rs;
		try {
			rs = sc.getRemoteServer();
		} catch (Exception e) {
			throw new RuntimeException("method " + mname + ", OASyncClient=null, thread=" + Thread.currentThread(), e);
		}

		if (rs == null) {
			throw new RuntimeException("method " + mname + ", RemoteServerInterface=null, thread=" + Thread.currentThread());
		}

		Object val = rs.runRemoteMethod(getClass(), OAObjectKeyDelegate.getKey(this), mname, args);

		return val;
	}

	// 20180629
	public boolean isUnique(String property, Object value) {
		OAObject obj = OAObjectUniqueDelegate.getUnique(getClass(), property, value, false);
		return (obj != null);
	}

	// 20180629
	public static OAObject getUniqueInstance(final Class<? extends OAObject> clazz, final String propertyName, final Object uniqueKey,
			final boolean bAutoCreate) {
		OAObject obj = OAObjectUniqueDelegate.getUnique(clazz, propertyName, uniqueKey, bAutoCreate);
		return obj;
	}

	/**
	 * returns true if this is a oaclient and is not a remoteThread.
	 */
	public boolean isRemoteAvailable() {
		if (OARemoteThreadDelegate.isRemoteThread()) {
			return false;
		}
		return isClient();
	}

	public static boolean isRemoteAvailable(Hub hub) {
		if (hub == null) {
			return false;
		}
		if (OARemoteThreadDelegate.isRemoteThread()) {
			return false;
		}
		final Class clazz = hub.getObjectClass();
		if (OASyncDelegate.isServer(clazz)) {
			return false;
		}
		return true;
	}

	public boolean isLoaded(String prop) {
		return OAObjectPropertyDelegate.isPropertyLoaded(this, prop);
	}

	public boolean isPropertyLoaded(String prop) {
		return OAObjectPropertyDelegate.isPropertyLoaded(this, prop);
	}

	public boolean isReferenceNull(String prop) {
		return OAObjectPropertyDelegate.isReferenceNull(this, prop);
	}

	public Object hierFind(String propertyName, String heirarchyPropertyPath) {
		OAHierFinder hf = new OAHierFinder<OAObject>(propertyName, heirarchyPropertyPath);
		Object objx = hf.findFirstNotEmpty(this);
		return objx;
	}

	/**
	 * Makes sure that the following code is only ran on the server. Any oasync changes will be sent to clients.
	 * 
	 * @see #endServerOnly()
	 */
	public boolean beginServerOnly() {
		if (isLoading()) {
			return false;
		}
		if (!OASyncDelegate.isServer(getClass())) {
			return false;
		}
		OARemoteThreadDelegate.sendMessages(true);
		return true;
	}

	public boolean startServerOnly() {
		return beginServerOnly();
	}

	public void endServerOnly() {
		if (isLoading()) {
			return;
		}
		if (!OASyncDelegate.isServer(getClass())) {
			return;
		}
		if (OARemoteThreadDelegate.isRemoteThreadSendingMessages()) {
			OARemoteThreadDelegate.sendMessages(false);
		}
	}

	// runOnServerOnly(() -> parseXML());
	public void runOnServerOnly(Runnable r) {
		if (r == null || !startServerOnly()) {
			return;
		}
		try {
			beginServerOnly();
			r.run();
		} finally {
			endServerOnly();
		}
	}

	private static boolean DebugMode = false;

	public static void setDebugMode(boolean b) {
		LOG.config("DebugMode set to " + b);
		DebugMode = b;
	}

	public static boolean getDebugMode() {
		return DebugMode;
	}

	public boolean isPropertyLocked(String prop) {
		boolean b = OAObjectPropertyDelegate.isPropertyLocked(this, prop);
		return b;
	}

	/*
	 *  20191018 JAXB support using OAJaxb to allow OAObjects & Hubs to be serialzied as Json/Xml.  
	 *  Support for sending reference objects as the following:
	 *      1: Ids only
	 *      2: ref to the object already in the (json/xml) output
	 *      3: full object
	 *  OAJax allows controlling how references are handling, including the use of adding propertyPaths.  
	 */

	/**
	 * These Jaxb methods are used to work with OAJaxb (JAXB xml processing). Only one of them will return a value, depending on what is
	 * needed for the XML. Otherwise, a null is sent so that it wont be included in the xml gen. The xsd will have elements for object,
	 * objectRef and objectId, but only one will be included in the produced XML
	 * 
	 * @XmlElement(name="vendor") public Vendor getJaxbVendor() { return (Vendor) super.getJaxb(P_Vendor); }
	 * @XmlElement(name="vendorRef") @XmlIDREF public Vendor getJaxbVendorRef() { return (Vendor) super.getJaxbRef(P_Vendor); }
	 * @XmlElement(name="vendorId") public String getJaxbVendorId() { return super.getJaxbId(P_Vendor); }
	 */

	/**
	 * Called by reference objects to find the next object from OAJaxb
	 * 
	 * @param clazz
	 * @return
	 */
	public static OAObject jaxbCreateInstance(Class clazz) {
		OAJaxb jaxb = OAThreadLocalDelegate.getOAJaxb();
		if (jaxb == null) {
			return null;
		}
		OAObject obj = jaxb.getNextUnmarshalObject(clazz);
		return obj;
	}

	/**
	 * Uses OAJaxb to determine if property can be updated. Used by setJaxb[PropertyName](..)
	 */
	public boolean getJaxbAllowPropertyChange(String propertyName, Object oldValue, Object newValue) {
		OAJaxb jaxb = OAThreadLocalDelegate.getOAJaxb();
		if (jaxb == null) {
			return true;
		}
		boolean b = jaxb.getAllowJaxbPropertyUpdate(this, propertyName);

		if (b) {
			OAObjectEditQuery eq = OAObjectEditQueryDelegate.getVerifyPropertyChangeEditQuery(	OAObjectEditQuery.CHECK_ALL, this,
																								propertyName, oldValue, newValue);
			b = eq.isAllowed();
		}
		return b;
	}

	public boolean getJaxbAllowSecurePropertyChange(String propertyName, Object oldValue, Object newValue) {
		boolean b = getJaxbAllowPropertyChange(propertyName, oldValue, newValue);
		if (b) {
			OAUserAccess ua = OAContext.getContextUserAccess();
			if (ua == null) {
				b = false;
			} else {
				b = false; //qqqqqqqqqqqq dont allow directly changing secure (ex: passwords) data through JAXB
			}
		}
		return b;
	}

	public boolean getJaxbShouldInclude(String propertyName) {
		OAJaxb jaxb = OAThreadLocalDelegate.getOAJaxb();
		boolean b = true;
		if (jaxb != null) {
			b = jaxb.getShouldInclude(this, propertyName);
		}
		if (b) {
			OAObjectEditQuery eq = OAObjectEditQueryDelegate.getAllowVisibleEditQuery(null, this, propertyName);
			b = eq.isAllowed();
		}
		return b;
	}

	/**
	 * Includes this.guid in output if OAJaxb.includeGuids=true
	 */
	public Integer getJaxbGuid() {
		OAJaxb jaxb = OAThreadLocalDelegate.getOAJaxb();
		if (jaxb == null) {
			return null;
		}
		if (!jaxb.getIncludeGuids()) {
			return null;
		}
		return getGuid();
	}

	public OAObject getJaxbObject(String propertyName) {
		OAJaxb jaxb = OAThreadLocalDelegate.getOAJaxb();
		if (jaxb != null) {
			OAJaxb.SendRefType type = jaxb.getSendRefType(this, propertyName);
			if (type != OAJaxb.SendRefType.object) {
				return null;
			}
			if (!getJaxbShouldInclude(propertyName)) {
				return null;
			}
		}
		return (OAObject) getObject(propertyName);
	}

	public OAObject getJaxbRefObject(String propertyName) {
		OAJaxb jaxb = OAThreadLocalDelegate.getOAJaxb();
		if (jaxb != null) {
			OAJaxb.SendRefType type = jaxb.getSendRefType(this, propertyName);
			if (type != OAJaxb.SendRefType.ref) {
				return null;
			}
		}
		return (OAObject) getObject(propertyName);
	}

	/***
	 * qqqqqqqqqq public int getJaxbId(String propertyName) { // was: public String getJaxbId(String propertyName) { // jaxb required xmlId
	 * to be a String, but Moxy does not OAJaxb jaxb = OAThreadLocalDelegate.getOAJaxb(); if (jaxb != null) { //OAJaxb.SendRefType type =
	 * jaxb.getSendRefType(this, propertyName); //if (type != OAJaxb.SendRefType.id) return 0; //was: if (type != OAJaxb.SendRefType.id)
	 * return null; } Object objx = OAObjectPropertyDelegate.getProperty(this, propertyName, true, true); if (objx instanceof OANotExist)
	 * return 0; //was: if (objx instanceof OANotExist) return null; if (objx instanceof OAObject) objx = ((OAObject) objx).getObjectKey();
	 * if (objx instanceof OAObjectKey) { Object[] objs = ((OAObjectKey) objx).getObjectIds(); if (objs == null || objs.length != 1) return
	 * 0; //if (objs == null || objs.length != 1) return null; return (int) objs[0]; //was: return objs[0]+""; } return OAConv.toInt(objx);
	 * //was: return objx+""; } public void setJaxbId(String propertyName, int id) { //was: public void setJaxbId(String propertyName,
	 * String id) { setProperty(propertyName, new OAObjectKey(id)); //was: int idx = Integer.valueOf(id); //was: setProperty(propertyName,
	 * new OAObjectKey(idx)); }
	 **/

	public String getJaxbId(String propertyName) {
		OAJaxb jaxb = OAThreadLocalDelegate.getOAJaxb();
		if (jaxb != null) {
			OAJaxb.SendRefType type = jaxb.getSendRefType(this, propertyName);
			if (type != OAJaxb.SendRefType.id) {
				// one of the other (jaxb*) methods for this property is being used                
				return null;
			}
		}
		Object objx = OAObjectPropertyDelegate.getProperty(this, propertyName, true, true);
		if (objx instanceof OANotExist) {
			return null;
		}
		if (objx instanceof OAObject) {
			objx = ((OAObject) objx).getObjectKey();
		}
		if (objx instanceof OAObjectKey) {
			Object[] objs = ((OAObjectKey) objx).getObjectIds();
			if (objs == null || objs.length != 1) {
				return null;
			}
			return objs[0] + "";
		}
		return objx + "";
	}

	public void setJaxbId(String propertyName, String id) {
		if (OAString.isEmpty(propertyName) || OAString.isEmpty(id)) {
			return;
		}

		if (!getJaxbAllowPropertyChange(propertyName, null, id)) {
			return;
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(getClass());
		OALinkInfo li = oi.getLinkInfo(propertyName);
		if (li == null) {
			return;
		}

		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (pi.getId()) {
				Object idx = OAConv.convert(pi.getClassType(), id);
				OAObjectKey objKey = new OAObjectKey(idx);

				/* 
				 * this.setProperty will correctly set the property:
				 * If isLoading=true, then it will only need to store the key
				 * else it will get the ref object and call the setter method.
				 */
				this.setProperty(propertyName, objKey);
				break;
			}
		}
	}

	public List getJaxbHub(String propertyName) {
		List list = getJaxbHubX(propertyName, null, false, null, false);
		return list;
	}

	public List getJaxbHub(String propertyName, String sortOrder) {
		return getJaxbHubX(propertyName, sortOrder, false, null, false);
	}

	public List getJaxbHub(String propertyName, String sortOrder, boolean bSequence, Hub hubMatch) {
		return getJaxbHubX(propertyName, sortOrder, bSequence, hubMatch, false);
	}

	public List getJaxbRefHub(String propertyName) {
		return getJaxbHubX(propertyName, null, false, null, true);
	}

	public List getJaxbRefHub(String propertyName, String sortOrder) {
		return getJaxbHubX(propertyName, sortOrder, false, null, true);
	}

	public List getJaxbRefHub(String propertyName, String sortOrder, boolean bSequence, Hub hubMatch) {
		return getJaxbHubX(propertyName, sortOrder, bSequence, hubMatch, true);
	}

	/**
	 * Same as getHub, that is used by Jaxb. To return an OAObject.hub property (ex: Dept.getEmps), Dept will have 3 methods: 1:
	 * getEmps() @XmlTransient that returns the real Hub 2: getJaxbEmps() that is used by jaxb to return list (or hub) of all emps in
	 * Dept.emps hub that have not been included in the xml output. 3: getJaxbRefEmps() that is used to return list (or hub) of
	 * references @XmlIDRef of objects that have already been included.
	 * 
	 * @param linkPropertyName
	 * @param sortOrder
	 * @param bSequence
	 * @param hubMatch
	 * @param bRefsOnly
	 *            if this is to only return hub objects that need to be XmlIDRef only
	 * @return
	 */
	protected List getJaxbHubX(final String linkPropertyName, final String sortOrder, final boolean bSequence, final Hub hubMatch,
			final boolean bRefsOnly) {
		if (!getJaxbShouldInclude(linkPropertyName)) {
			return null;
		}

		OAJaxb jaxb = OAThreadLocalDelegate.getOAJaxb();

		Hub hub = null;
		if (jaxb != null) {

			OAJaxb.SendRefType type = jaxb.getSendRefType(this, linkPropertyName);
			if (type == OAJaxb.SendRefType.notNeeded) {
				return null; // not requested in property paths
			}

			if (!jaxb.shouldIncludeProperty(this, linkPropertyName, true)) {
				return null;
			}

			if (jaxb.isMarshelling()) {
				ArrayList<OAObject> lstRefsOnly = jaxb.getRefsOnlyList(linkPropertyName);
				if (bRefsOnly) {
					if (lstRefsOnly != null) {
						if (lstRefsOnly.size() == 0) {
							return null;
						}
						return lstRefsOnly;
					}
				}

				hub = getHub(linkPropertyName, sortOrder, bSequence, hubMatch);
				int cnt = 0;
				int cntRef = 0;
				for (Object obj : hub) {
					if (!jaxb.getUseReferences() && !jaxb.isInStack((OAObject) obj)) {
						cnt++;
					} else if (jaxb.isAlreadyIncluded((OAObject) obj)) {
						cntRef++;
					} else if (jaxb.willBeIncludedLater((OAObject) obj)) {
						cntRef++;
					} else {
						cnt++;
					}
					if (cnt > 0 && cntRef > 0) {
						break;
					}
				}

				if (cnt > 0 && cntRef > 0) {
					List list = new ArrayList();
					if (!bRefsOnly) {
						lstRefsOnly = new ArrayList<OAObject>(); // hold for next call
						jaxb.setRefsOnlyList(linkPropertyName, lstRefsOnly);
					}

					for (Object obj : hub) {
						if (!jaxb.getUseReferences()) {
							if (jaxb.isInStack((OAObject) obj)) {
								// must use ref anyway, to avoid circular reference exception
								if (bRefsOnly) {
									list.add(obj);
								} else {
									lstRefsOnly.add((OAObject) obj);
								}
							} else {
								if (!bRefsOnly) {
									list.add(obj);
								}
							}
						} else if (jaxb.isAlreadyIncluded((OAObject) obj)) {
							if (bRefsOnly) {
								list.add(obj);
							} else {
								lstRefsOnly.add((OAObject) obj);
							}
						} else if (jaxb.willBeIncludedLater((OAObject) obj)) {
							if (bRefsOnly) {
								list.add(obj);
							} else {
								lstRefsOnly.add((OAObject) obj);
							}
						} else if (!bRefsOnly) {
							list.add(obj);
						}
					}
					return list;
				}
				if (bRefsOnly) {
					if (cntRef == 0) {
						return null;
					}
				} else {
					if (cnt == 0 && cntRef > 0) {
						return null; // all refs
					}
					if (lstRefsOnly == null) {
						lstRefsOnly = new ArrayList(); // no refs
						jaxb.setRefsOnlyList(linkPropertyName, lstRefsOnly);
					}
				}
			}
		}
		if (hub == null) {
			hub = getHub(linkPropertyName, sortOrder, bSequence, hubMatch);
		}
		return hub;
	}

	public boolean isSubmitted() {
		return _isSubmitted(0);
	}

	public boolean _isSubmitted(int cnt) {
		if (cnt > 10) {
			String s = "recursive > 10, will return true and continue";
			LOG.log(Level.WARNING, "recursive, obj=" + this, new Exception(s));
			return true;
		}
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(this.getClass());
		OAPropertyInfo pi = oi.getSubmitProperty();
		if (pi == null) {
			// check owner (recursive)
			OALinkInfo[] lis = oi.getOwnedLinkInfos();
			if (lis != null) {
				for (OALinkInfo li : lis) {
					Object objx = li.getValue(this);
					if (objx instanceof OAObject) {
						boolean b = ((OAObject) objx)._isSubmitted(cnt + 1);
						if (!b) {
							return false;
						}
					}
				}
			}
			return true;
		}
		Object objx = this.getProperty(pi.getName());
		if (objx == null) {
			return false;
		}
		boolean b = OAConv.toBoolean(objx);
		return b;
	}
}
