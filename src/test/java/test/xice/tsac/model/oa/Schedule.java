// Generated by OABuilder
package test.xice.tsac.model.oa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAIndex;
import com.viaoa.annotation.OAIndexColumn;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.annotation.OATable;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

import test.xice.tsac.delegate.oa.ScheduleDelegate;
import test.xice.tsac.model.oa.filter.ScheduleTodayFilter;

@OAClass(shortName = "sch", displayName = "Schedule", filterClasses = { ScheduleTodayFilter.class })
@OATable(indexes = {
		@OAIndex(name = "ScheduleApplication", columns = { @OAIndexColumn(name = "ApplicationId") }),
		@OAIndex(name = "ScheduleApplicationGroup", columns = { @OAIndexColumn(name = "ApplicationGroupId") })
})
public class Schedule extends OAObject {
	private static final long serialVersionUID = 1L;
	public static final String PROPERTY_Id = "Id";
	public static final String P_Id = "Id";
	public static final String PROPERTY_Name = "Name";
	public static final String P_Name = "Name";
	public static final String PROPERTY_Description = "Description";
	public static final String P_Description = "Description";
	public static final String PROPERTY_StopTime = "StopTime";
	public static final String P_StopTime = "StopTime";
	public static final String PROPERTY_StartTime = "StartTime";
	public static final String P_StartTime = "StartTime";
	public static final String PROPERTY_Sunday = "Sunday";
	public static final String P_Sunday = "Sunday";
	public static final String PROPERTY_Monday = "Monday";
	public static final String P_Monday = "Monday";
	public static final String PROPERTY_Tuesday = "Tuesday";
	public static final String P_Tuesday = "Tuesday";
	public static final String PROPERTY_Wednesday = "Wednesday";
	public static final String P_Wednesday = "Wednesday";
	public static final String PROPERTY_Thursday = "Thursday";
	public static final String P_Thursday = "Thursday";
	public static final String PROPERTY_Friday = "Friday";
	public static final String P_Friday = "Friday";
	public static final String PROPERTY_Saturday = "Saturday";
	public static final String P_Saturday = "Saturday";
	public static final String PROPERTY_ActiveBeginDate = "ActiveBeginDate";
	public static final String P_ActiveBeginDate = "ActiveBeginDate";
	public static final String PROPERTY_ActiveEndDate = "ActiveEndDate";
	public static final String P_ActiveEndDate = "ActiveEndDate";

	public static final String PROPERTY_RealStartTime = "RealStartTime";
	public static final String P_RealStartTime = "RealStartTime";
	public static final String PROPERTY_RealStopTime = "RealStopTime";
	public static final String P_RealStopTime = "RealStopTime";
	public static final String PROPERTY_IsForToday = "IsForToday";
	public static final String P_IsForToday = "IsForToday";

	public static final String PROPERTY_Application = "Application";
	public static final String P_Application = "Application";
	public static final String PROPERTY_ApplicationGroup = "ApplicationGroup";
	public static final String P_ApplicationGroup = "ApplicationGroup";

	protected int id;
	protected String name;
	protected String description;
	protected OATime stopTime;
	protected OATime startTime;
	protected boolean sunday;
	protected boolean monday;
	protected boolean tuesday;
	protected boolean wednesday;
	protected boolean thursday;
	protected boolean friday;
	protected boolean saturday;
	protected OADate activeBeginDate;
	protected OADate activeEndDate;

	// Links to other objects.
	protected transient Application application;
	protected transient ApplicationGroup applicationGroup;

	public Schedule() {
	}

	public Schedule(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, displayLength = 5)
	@OAId()
	@OAColumn(sqlType = java.sql.Types.INTEGER)
	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		fireBeforePropertyChange(P_Id, this.id, newValue);
		int old = id;
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
	}

	@OAProperty(maxLength = 25, displayLength = 18, columnLength = 12)
	@OAColumn(maxLength = 25)
	public String getName() {
		return name;
	}

	public void setName(String newValue) {
		fireBeforePropertyChange(P_Name, this.name, newValue);
		String old = name;
		this.name = newValue;
		firePropertyChange(P_Name, old, this.name);
	}

	@OAProperty(maxLength = 4000, displayLength = 40)
	@OAColumn(sqlType = java.sql.Types.CLOB)
	public String getDescription() {
		return description;
	}

	public void setDescription(String newValue) {
		fireBeforePropertyChange(P_Description, this.description, newValue);
		String old = description;
		this.description = newValue;
		firePropertyChange(P_Description, old, this.description);
	}

	@OAProperty(displayName = "Stop Time", displayLength = 8)
	@OAColumn(sqlType = java.sql.Types.TIME)
	public OATime getStopTime() {
		return stopTime;
	}

	public void setStopTime(OATime newValue) {
		fireBeforePropertyChange(P_StopTime, this.stopTime, newValue);
		OATime old = stopTime;
		this.stopTime = newValue;
		firePropertyChange(P_StopTime, old, this.stopTime);
	}

	@OAProperty(displayName = "Start Time", displayLength = 8)
	@OAColumn(sqlType = java.sql.Types.TIME)
	public OATime getStartTime() {
		return startTime;
	}

	public void setStartTime(OATime newValue) {
		fireBeforePropertyChange(P_StartTime, this.startTime, newValue);
		OATime old = startTime;
		this.startTime = newValue;
		firePropertyChange(P_StartTime, old, this.startTime);
	}

	@OAProperty(displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getSunday() {
		return sunday;
	}

	public void setSunday(boolean newValue) {
		fireBeforePropertyChange(P_Sunday, this.sunday, newValue);
		boolean old = sunday;
		this.sunday = newValue;
		firePropertyChange(P_Sunday, old, this.sunday);
	}

	@OAProperty(displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getMonday() {
		return monday;
	}

	public void setMonday(boolean newValue) {
		fireBeforePropertyChange(P_Monday, this.monday, newValue);
		boolean old = monday;
		this.monday = newValue;
		firePropertyChange(P_Monday, old, this.monday);
	}

	@OAProperty(displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getTuesday() {
		return tuesday;
	}

	public void setTuesday(boolean newValue) {
		fireBeforePropertyChange(P_Tuesday, this.tuesday, newValue);
		boolean old = tuesday;
		this.tuesday = newValue;
		firePropertyChange(P_Tuesday, old, this.tuesday);
	}

	@OAProperty(displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getWednesday() {
		return wednesday;
	}

	public void setWednesday(boolean newValue) {
		fireBeforePropertyChange(P_Wednesday, this.wednesday, newValue);
		boolean old = wednesday;
		this.wednesday = newValue;
		firePropertyChange(P_Wednesday, old, this.wednesday);
	}

	@OAProperty(displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getThursday() {
		return thursday;
	}

	public void setThursday(boolean newValue) {
		fireBeforePropertyChange(P_Thursday, this.thursday, newValue);
		boolean old = thursday;
		this.thursday = newValue;
		firePropertyChange(P_Thursday, old, this.thursday);
	}

	@OAProperty(displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getFriday() {
		return friday;
	}

	public void setFriday(boolean newValue) {
		fireBeforePropertyChange(P_Friday, this.friday, newValue);
		boolean old = friday;
		this.friday = newValue;
		firePropertyChange(P_Friday, old, this.friday);
	}

	@OAProperty(displayLength = 5)
	@OAColumn(sqlType = java.sql.Types.BOOLEAN)
	public boolean getSaturday() {
		return saturday;
	}

	public void setSaturday(boolean newValue) {
		fireBeforePropertyChange(P_Saturday, this.saturday, newValue);
		boolean old = saturday;
		this.saturday = newValue;
		firePropertyChange(P_Saturday, old, this.saturday);
	}

	@OAProperty(displayName = "Active Begin Date", displayLength = 8, format = "MM/dd")
	@OAColumn(sqlType = java.sql.Types.DATE)
	public OADate getActiveBeginDate() {
		return activeBeginDate;
	}

	public void setActiveBeginDate(OADate newValue) {
		fireBeforePropertyChange(P_ActiveBeginDate, this.activeBeginDate, newValue);
		OADate old = activeBeginDate;
		this.activeBeginDate = newValue;
		firePropertyChange(P_ActiveBeginDate, old, this.activeBeginDate);
	}

	@OAProperty(displayName = "Active End Date", displayLength = 8, format = "MM/dd")
	@OAColumn(sqlType = java.sql.Types.DATE)
	public OADate getActiveEndDate() {
		return activeEndDate;
	}

	public void setActiveEndDate(OADate newValue) {
		fireBeforePropertyChange(P_ActiveEndDate, this.activeEndDate, newValue);
		OADate old = activeEndDate;
		this.activeEndDate = newValue;
		firePropertyChange(P_ActiveEndDate, old, this.activeEndDate);
	}

	@OACalculatedProperty(displayName = "Real Start Time", displayLength = 8, properties = { P_StartTime })
	public OATime getRealStartTime() {
		OATime realStartTime = this.getStartTime();
		if (realStartTime == null) {
			return null;
		}
		realStartTime = (OATime) realStartTime.addHours(ScheduleDelegate.getTimezoneOffset(this));
		return realStartTime;
	}

	@OACalculatedProperty(displayName = "Real Stop Time", displayLength = 8, properties = { P_StopTime })
	public OATime getRealStopTime() {
		OATime realStopTime = this.getStopTime();
		if (realStopTime == null) {
			return null;
		}
		realStopTime = (OATime) realStopTime.addHours(ScheduleDelegate.getTimezoneOffset(this));
		return realStopTime;
	}

	@OACalculatedProperty(displayName = "Is For Today", displayLength = 5, properties = { P_Sunday, P_Monday, P_Tuesday, P_Wednesday,
			P_Thursday, P_Friday, P_Saturday })
	public boolean getIsForToday() {
		OADateTime dt = new OADateTime();
		// need to adjust dt for silo timezone
		dt = dt.addHours(ScheduleDelegate.getTimezoneOffset(this));
		int dayOfWeek = dt.getDayOfWeek();

		if (!ScheduleDelegate.isActive(this, dt)) {
			return false;
		}

		switch (dayOfWeek) {
		case Calendar.SUNDAY:
			if (!getSunday()) {
				return false;
			}
			break;
		case Calendar.MONDAY:
			if (!getMonday()) {
				return false;
			}
			break;
		case Calendar.TUESDAY:
			if (!getTuesday()) {
				return false;
			}
			break;
		case Calendar.WEDNESDAY:
			if (!getWednesday()) {
				return false;
			}
			break;
		case Calendar.THURSDAY:
			if (!getThursday()) {
				return false;
			}
			break;
		case Calendar.FRIDAY:
			if (!getFriday()) {
				return false;
			}
			break;
		case Calendar.SATURDAY:
			if (!getSaturday()) {
				return false;
			}
			break;
		}
		return true;
	}

	@OAOne(reverseName = Application.P_Schedules, allowCreateNew = false)
	@OAFkey(columns = { "ApplicationId" })
	public Application getApplication() {
		if (application == null) {
			application = (Application) getObject(P_Application);
		}
		return application;
	}

	public void setApplication(Application newValue) {
		fireBeforePropertyChange(P_Application, this.application, newValue);
		Application old = this.application;
		this.application = newValue;
		firePropertyChange(P_Application, old, this.application);
	}

	@OAOne(displayName = "Application Group", reverseName = ApplicationGroup.P_Schedules, allowCreateNew = false, allowAddExisting = false)
	@OAFkey(columns = { "ApplicationGroupId" })
	public ApplicationGroup getApplicationGroup() {
		if (applicationGroup == null) {
			applicationGroup = (ApplicationGroup) getObject(P_ApplicationGroup);
		}
		return applicationGroup;
	}

	public void setApplicationGroup(ApplicationGroup newValue) {
		fireBeforePropertyChange(P_ApplicationGroup, this.applicationGroup, newValue);
		ApplicationGroup old = this.applicationGroup;
		this.applicationGroup = newValue;
		firePropertyChange(P_ApplicationGroup, old, this.applicationGroup);
	}

	public void load(ResultSet rs, int id) throws SQLException {
		this.id = id;
		this.name = rs.getString(2);
		this.description = rs.getString(3);
		java.sql.Time time;
		time = rs.getTime(4);
		if (time != null) {
			this.stopTime = new OATime(time);
		}
		time = rs.getTime(5);
		if (time != null) {
			this.startTime = new OATime(time);
		}
		this.sunday = rs.getBoolean(6);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, Schedule.P_Sunday, true);
		}
		this.monday = rs.getBoolean(7);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, Schedule.P_Monday, true);
		}
		this.tuesday = rs.getBoolean(8);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, Schedule.P_Tuesday, true);
		}
		this.wednesday = rs.getBoolean(9);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, Schedule.P_Wednesday, true);
		}
		this.thursday = rs.getBoolean(10);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, Schedule.P_Thursday, true);
		}
		this.friday = rs.getBoolean(11);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, Schedule.P_Friday, true);
		}
		this.saturday = rs.getBoolean(12);
		if (rs.wasNull()) {
			OAObjectInfoDelegate.setPrimitiveNull(this, Schedule.P_Saturday, true);
		}
		java.sql.Date date;
		date = rs.getDate(13);
		if (date != null) {
			this.activeBeginDate = new OADate(date);
		}
		date = rs.getDate(14);
		if (date != null) {
			this.activeEndDate = new OADate(date);
		}
		int applicationFkey = rs.getInt(15);
		if (!rs.wasNull() && applicationFkey > 0) {
			setProperty(P_Application, new OAObjectKey(applicationFkey));
		}
		int applicationGroupFkey = rs.getInt(16);
		if (!rs.wasNull() && applicationGroupFkey > 0) {
			setProperty(P_ApplicationGroup, new OAObjectKey(applicationGroupFkey));
		}
		if (rs.getMetaData().getColumnCount() != 16) {
			throw new SQLException("invalid number of columns for load method");
		}

		changedFlag = false;
		newFlag = false;
	}
}
