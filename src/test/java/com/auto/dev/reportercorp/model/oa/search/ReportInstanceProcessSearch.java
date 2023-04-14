package com.auto.dev.reportercorp.model.oa.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportInfo;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;
import com.auto.dev.reportercorp.model.oa.ThreadInfo;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportInstanceProcessPP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAOne;
import com.viaoa.datasource.OASelect;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAObject;
import com.viaoa.util.OAArray;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAFilter;
import com.viaoa.util.OAString;

@OAClass(useDataSource = false, localOnly = true)
public class ReportInstanceProcessSearch extends OAObject {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(ReportInstanceProcessSearch.class.getName());

	public static final String P_Id = "Id";
	public static final String P_Id2 = "Id2";
	public static final String P_Created = "Created";
	public static final String P_Created2 = "Created2";
	public static final String P_Counter = "Counter";
	public static final String P_Counter2 = "Counter2";
	public static final String P_Completed = "Completed";
	public static final String P_Completed2 = "Completed2";
	public static final String P_FixedJson = "FixedJson";
	public static final String P_FixedJsonUseNull = "FixedJsonUseNull";
	public static final String P_FixedJsonUseNotNull = "FixedJsonUseNotNull";
	public static final String P_PypeError = "PypeError";
	public static final String P_PypeErrorUseNull = "PypeErrorUseNull";
	public static final String P_PypeErrorUseNotNull = "PypeErrorUseNotNull";
	public static final String P_ProcessingError = "ProcessingError";
	public static final String P_ProcessingErrorUseNull = "ProcessingErrorUseNull";
	public static final String P_ProcessingErrorUseNotNull = "ProcessingErrorUseNotNull";
	public static final String P_ReportInfo = "ReportInfo";
	public static final String P_UseReportInfoSearch = "UseReportInfoSearch";
	public static final String P_ThreadInfo = "ThreadInfo";
	public static final String P_UseThreadInfoSearch = "UseThreadInfoSearch";
	public static final String P_MaxResults = "MaxResults";

	protected int id;
	protected int id2;
	protected OADateTime created;
	protected OADateTime created2;
	protected int counter;
	protected int counter2;
	protected OADateTime completed;
	protected OADateTime completed2;
	protected boolean fixedJson;
	protected boolean fixedJsonUseNull;
	protected boolean fixedJsonUseNotNull;
	protected boolean pypeError;
	protected boolean pypeErrorUseNull;
	protected boolean pypeErrorUseNotNull;
	protected boolean processingError;
	protected boolean processingErrorUseNull;
	protected boolean processingErrorUseNotNull;
	protected ReportInfo reportInfo;
	protected boolean useReportInfoSearch;
	protected ReportInfoSearch searchReportInfo;
	protected ThreadInfo threadInfo;
	protected boolean useThreadInfoSearch;
	protected ThreadInfoSearch searchThreadInfo;
	protected int maxResults;

	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		int old = id;
		fireBeforePropertyChange(P_Id, old, newValue);
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
		if (isLoading()) {
			return;
		}
		if (id > id2) {
			setId2(this.id);
		}
	}

	public int getId2() {
		return id2;
	}

	public void setId2(int newValue) {
		int old = id2;
		fireBeforePropertyChange(P_Id2, old, newValue);
		this.id2 = newValue;
		firePropertyChange(P_Id2, old, this.id2);
		if (isLoading()) {
			return;
		}
		if (id > id2) {
			setId(this.id2);
		}
	}

	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
		if (isLoading()) {
			return;
		}
		if (created != null) {
			if (created2 == null) {
				setCreated2(this.created.addDays(1));
			} else if (created.compareTo(created2) > 0) {
				setCreated2(this.created.addDays(1));
			}
		}
	}

	public OADateTime getCreated2() {
		return created2;
	}

	public void setCreated2(OADateTime newValue) {
		OADateTime old = created2;
		fireBeforePropertyChange(P_Created2, old, newValue);
		this.created2 = newValue;
		firePropertyChange(P_Created2, old, this.created2);
		if (created != null && created2 != null) {
			if (created.compareTo(created2) > 0) {
				setCreated(this.created2);
			}
		}
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int newValue) {
		int old = counter;
		fireBeforePropertyChange(P_Counter, old, newValue);
		this.counter = newValue;
		firePropertyChange(P_Counter, old, this.counter);
		if (isLoading()) {
			return;
		}
		if (counter > counter2) {
			setCounter2(this.counter);
		}
	}

	public int getCounter2() {
		return counter2;
	}

	public void setCounter2(int newValue) {
		int old = counter2;
		fireBeforePropertyChange(P_Counter2, old, newValue);
		this.counter2 = newValue;
		firePropertyChange(P_Counter2, old, this.counter2);
		if (isLoading()) {
			return;
		}
		if (counter > counter2) {
			setCounter(this.counter2);
		}
	}

	public OADateTime getCompleted() {
		return completed;
	}

	public void setCompleted(OADateTime newValue) {
		OADateTime old = completed;
		fireBeforePropertyChange(P_Completed, old, newValue);
		this.completed = newValue;
		firePropertyChange(P_Completed, old, this.completed);
		if (isLoading()) {
			return;
		}
		if (completed != null) {
			if (completed2 == null) {
				setCompleted2(this.completed.addDays(1));
			} else if (completed.compareTo(completed2) > 0) {
				setCompleted2(this.completed.addDays(1));
			}
		}
	}

	public OADateTime getCompleted2() {
		return completed2;
	}

	public void setCompleted2(OADateTime newValue) {
		OADateTime old = completed2;
		fireBeforePropertyChange(P_Completed2, old, newValue);
		this.completed2 = newValue;
		firePropertyChange(P_Completed2, old, this.completed2);
		if (completed != null && completed2 != null) {
			if (completed.compareTo(completed2) > 0) {
				setCompleted(this.completed2);
			}
		}
	}

	public boolean getFixedJson() {
		return fixedJson;
	}

	public boolean isFixedJson() {
		return getFixedJson();
	}

	public void setFixedJson(boolean newValue) {
		boolean old = fixedJson;
		fireBeforePropertyChange(P_FixedJson, old, newValue);
		this.fixedJson = newValue;
		firePropertyChange(P_FixedJson, old, this.fixedJson);
	}

	public boolean getFixedJsonUseNull() {
		return fixedJsonUseNull;
	}

	public void setFixedJsonUseNull(boolean newValue) {
		boolean old = this.fixedJsonUseNull;
		this.fixedJsonUseNull = newValue;
		firePropertyChange(P_FixedJsonUseNull, old, this.fixedJsonUseNull);
	}

	public boolean getFixedJsonUseNotNull() {
		return fixedJsonUseNotNull;
	}

	public void setFixedJsonUseNotNull(boolean newValue) {
		boolean old = this.fixedJsonUseNotNull;
		this.fixedJsonUseNotNull = newValue;
		firePropertyChange(P_FixedJsonUseNotNull, old, this.fixedJsonUseNotNull);
	}

	public boolean getPypeError() {
		return pypeError;
	}

	public boolean isPypeError() {
		return getPypeError();
	}

	public void setPypeError(boolean newValue) {
		boolean old = pypeError;
		fireBeforePropertyChange(P_PypeError, old, newValue);
		this.pypeError = newValue;
		firePropertyChange(P_PypeError, old, this.pypeError);
	}

	public boolean getPypeErrorUseNull() {
		return pypeErrorUseNull;
	}

	public void setPypeErrorUseNull(boolean newValue) {
		boolean old = this.pypeErrorUseNull;
		this.pypeErrorUseNull = newValue;
		firePropertyChange(P_PypeErrorUseNull, old, this.pypeErrorUseNull);
	}

	public boolean getPypeErrorUseNotNull() {
		return pypeErrorUseNotNull;
	}

	public void setPypeErrorUseNotNull(boolean newValue) {
		boolean old = this.pypeErrorUseNotNull;
		this.pypeErrorUseNotNull = newValue;
		firePropertyChange(P_PypeErrorUseNotNull, old, this.pypeErrorUseNotNull);
	}

	public boolean getProcessingError() {
		return processingError;
	}

	public boolean isProcessingError() {
		return getProcessingError();
	}

	public void setProcessingError(boolean newValue) {
		boolean old = processingError;
		fireBeforePropertyChange(P_ProcessingError, old, newValue);
		this.processingError = newValue;
		firePropertyChange(P_ProcessingError, old, this.processingError);
	}

	public boolean getProcessingErrorUseNull() {
		return processingErrorUseNull;
	}

	public void setProcessingErrorUseNull(boolean newValue) {
		boolean old = this.processingErrorUseNull;
		this.processingErrorUseNull = newValue;
		firePropertyChange(P_ProcessingErrorUseNull, old, this.processingErrorUseNull);
	}

	public boolean getProcessingErrorUseNotNull() {
		return processingErrorUseNotNull;
	}

	public void setProcessingErrorUseNotNull(boolean newValue) {
		boolean old = this.processingErrorUseNotNull;
		this.processingErrorUseNotNull = newValue;
		firePropertyChange(P_ProcessingErrorUseNotNull, old, this.processingErrorUseNotNull);
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int newValue) {
		fireBeforePropertyChange(P_MaxResults, this.maxResults, newValue);
		int old = maxResults;
		this.maxResults = newValue;
		firePropertyChange(P_MaxResults, old, this.maxResults);
	}

	@OAOne
	public ReportInfo getReportInfo() {
		if (reportInfo == null) {
			reportInfo = (ReportInfo) getObject(P_ReportInfo);
		}
		return reportInfo;
	}

	public void setReportInfo(ReportInfo newValue) {
		ReportInfo old = this.reportInfo;
		this.reportInfo = newValue;
		firePropertyChange(P_ReportInfo, old, this.reportInfo);
	}

	public boolean getUseReportInfoSearch() {
		return useReportInfoSearch;
	}

	public void setUseReportInfoSearch(boolean newValue) {
		boolean old = this.useReportInfoSearch;
		this.useReportInfoSearch = newValue;
		firePropertyChange(P_UseReportInfoSearch, old, this.useReportInfoSearch);
	}

	public ReportInfoSearch getReportInfoSearch() {
		return this.searchReportInfo;
	}

	public void setReportInfoSearch(ReportInfoSearch newValue) {
		this.searchReportInfo = newValue;
	}

	@OAOne
	public ThreadInfo getThreadInfo() {
		if (threadInfo == null) {
			threadInfo = (ThreadInfo) getObject(P_ThreadInfo);
		}
		return threadInfo;
	}

	public void setThreadInfo(ThreadInfo newValue) {
		ThreadInfo old = this.threadInfo;
		this.threadInfo = newValue;
		firePropertyChange(P_ThreadInfo, old, this.threadInfo);
	}

	public boolean getUseThreadInfoSearch() {
		return useThreadInfoSearch;
	}

	public void setUseThreadInfoSearch(boolean newValue) {
		boolean old = this.useThreadInfoSearch;
		this.useThreadInfoSearch = newValue;
		firePropertyChange(P_UseThreadInfoSearch, old, this.useThreadInfoSearch);
	}

	public ThreadInfoSearch getThreadInfoSearch() {
		return this.searchThreadInfo;
	}

	public void setThreadInfoSearch(ThreadInfoSearch newValue) {
		this.searchThreadInfo = newValue;
	}

	public void reset() {
		setId(0);
		setNull(P_Id);
		setId2(0);
		setNull(P_Id2);
		setCreated(null);
		setCreated2(null);
		setCounter(0);
		setNull(P_Counter);
		setCounter2(0);
		setNull(P_Counter2);
		setCompleted(null);
		setCompleted2(null);
		setFixedJson(false);
		setNull(P_FixedJson);
		setFixedJsonUseNull(false);
		setFixedJsonUseNotNull(false);
		setPypeError(false);
		setNull(P_PypeError);
		setPypeErrorUseNull(false);
		setPypeErrorUseNotNull(false);
		setProcessingError(false);
		setNull(P_ProcessingError);
		setProcessingErrorUseNull(false);
		setProcessingErrorUseNotNull(false);
		setReportInfo(null);
		setUseReportInfoSearch(false);
		setThreadInfo(null);
		setUseThreadInfoSearch(false);
	}

	public boolean isDataEntered() {
		if (!isNull(P_Id)) {
			return true;
		}
		if (getCreated() != null) {
			return true;
		}
		if (!isNull(P_Counter)) {
			return true;
		}
		if (getCompleted() != null) {
			return true;
		}
		if (!isNull(P_FixedJson)) {
			return true;
		}
		if (getFixedJsonUseNull()) {
			return true;
		}
		if (getFixedJsonUseNotNull()) {
			return true;
		}
		if (!isNull(P_PypeError)) {
			return true;
		}
		if (getPypeErrorUseNull()) {
			return true;
		}
		if (getPypeErrorUseNotNull()) {
			return true;
		}
		if (!isNull(P_ProcessingError)) {
			return true;
		}
		if (getProcessingErrorUseNull()) {
			return true;
		}
		if (getProcessingErrorUseNotNull()) {
			return true;
		}
		if (getReportInfo() != null) {
			return true;
		}
		if (getUseReportInfoSearch()) {
			return true;
		}
		if (getThreadInfo() != null) {
			return true;
		}
		if (getUseThreadInfoSearch()) {
			return true;
		}
		return false;
	}

	protected String extraWhere;
	protected Object[] extraWhereParams;
	protected OAFilter<ReportInstanceProcess> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<ReportInstanceProcess> f = new OAQueryFilter<ReportInstanceProcess>(ReportInstanceProcess.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<ReportInstanceProcess> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<ReportInstanceProcess> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<ReportInstanceProcess> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];
		OAFinder finder = null;
		if (!isNull(P_Id)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Id2) && id != id2) {
				sql += ReportInstanceProcess.P_Id + " >= ?";
				args = OAArray.add(Object.class, args, getId());
				sql += " AND " + ReportInstanceProcess.P_Id + " <= ?";
				args = OAArray.add(Object.class, args, getId2());
			} else {
				sql += ReportInstanceProcess.P_Id + " = ?";
				args = OAArray.add(Object.class, args, getId());
			}
		}
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += ReportInstanceProcess.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + ReportInstanceProcess.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += ReportInstanceProcess.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		if (!isNull(P_Counter)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Counter2) && counter != counter2) {
				sql += ReportInstanceProcess.P_Counter + " >= ?";
				args = OAArray.add(Object.class, args, getCounter());
				sql += " AND " + ReportInstanceProcess.P_Counter + " <= ?";
				args = OAArray.add(Object.class, args, getCounter2());
			} else {
				sql += ReportInstanceProcess.P_Counter + " = ?";
				args = OAArray.add(Object.class, args, getCounter());
			}
		}
		if (completed != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (completed2 != null && !completed.equals(completed2)) {
				sql += ReportInstanceProcess.P_Completed + " >= ?";
				args = OAArray.add(Object.class, args, this.completed);
				sql += " AND " + ReportInstanceProcess.P_Completed + " <= ?";
				args = OAArray.add(Object.class, args, this.completed2);
			} else {
				sql += ReportInstanceProcess.P_Completed + " = ?";
				args = OAArray.add(Object.class, args, this.completed);
			}
		}
		if (fixedJsonUseNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcess.P_FixedJson + " = null";
		} else if (fixedJsonUseNotNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcess.P_FixedJson + " != null";
		}
		if (!isNull(P_FixedJson)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcess.P_FixedJson + " = ?";
			args = OAArray.add(Object.class, args, this.fixedJson);
		}
		if (pypeErrorUseNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcess.P_PypeError + " = null";
		} else if (pypeErrorUseNotNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcess.P_PypeError + " != null";
		}
		if (!isNull(P_PypeError)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcess.P_PypeError + " = ?";
			args = OAArray.add(Object.class, args, this.pypeError);
		}
		if (processingErrorUseNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcess.P_ProcessingError + " = null";
		} else if (processingErrorUseNotNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcess.P_ProcessingError + " != null";
		}
		if (!isNull(P_ProcessingError)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcess.P_ProcessingError + " = ?";
			args = OAArray.add(Object.class, args, this.processingError);
		}
		if (!useReportInfoSearch && getReportInfo() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcessPP.reportInfo().pp + " = ?";
			args = OAArray.add(Object.class, args, getReportInfo());
			finder = new OAFinder<ReportInfo, ReportInstanceProcess>(getReportInfo(), ReportInfo.P_ReportInstanceProcesses);
		}
		if (!useThreadInfoSearch && getThreadInfo() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += ReportInstanceProcessPP.threadInfo().pp + " = ?";
			args = OAArray.add(Object.class, args, getThreadInfo());
			finder = new OAFinder<ThreadInfo, ReportInstanceProcess>(getThreadInfo(), ThreadInfo.P_ReportInstanceProcesses);
		}

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<ReportInstanceProcess> select = new OASelect<ReportInstanceProcess>(ReportInstanceProcess.class, sql, args, sortOrder);
		if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
			select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
		} else {
			select.setFilter(this.getCustomFilter());
		}
		select.setDataSourceFilter(this.getDataSourceFilter());
		select.setFinder(finder);
		if (getMaxResults() > 0) {
			select.setMax(getMaxResults());
		}
		if (useReportInfoSearch && getReportInfoSearch() != null) {
			getReportInfoSearch().appendSelect(ReportInstanceProcessPP.reportInfo().pp, select);
		}
		if (useThreadInfoSearch && getThreadInfoSearch() != null) {
			getThreadInfoSearch().appendSelect(ReportInstanceProcessPP.threadInfo().pp, select);
		}
		return select;
	}

	public void appendSelect(final String fromName, final OASelect select) {
		final String prefix = fromName + ".";
		String sql = "";
		Object[] args = new Object[0];
		if (!isNull(P_Id)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Id2) && id != id2) {
				sql += prefix + ReportInstanceProcess.P_Id + " >= ?";
				args = OAArray.add(Object.class, args, getId());
				sql += " AND " + prefix + ReportInstanceProcess.P_Id + " <= ?";
				args = OAArray.add(Object.class, args, getId2());
			} else {
				sql += prefix + ReportInstanceProcess.P_Id + " = ?";
				args = OAArray.add(Object.class, args, getId());
			}
		}
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (created2 != null && !created.equals(created2)) {
				sql += prefix + ReportInstanceProcess.P_Created + " >= ?";
				args = OAArray.add(Object.class, args, this.created);
				sql += " AND " + prefix + ReportInstanceProcess.P_Created + " <= ?";
				args = OAArray.add(Object.class, args, this.created2);
			} else {
				sql += prefix + ReportInstanceProcess.P_Created + " = ?";
				args = OAArray.add(Object.class, args, this.created);
			}
		}
		if (!isNull(P_Counter)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Counter2) && counter != counter2) {
				sql += prefix + ReportInstanceProcess.P_Counter + " >= ?";
				args = OAArray.add(Object.class, args, getCounter());
				sql += " AND " + prefix + ReportInstanceProcess.P_Counter + " <= ?";
				args = OAArray.add(Object.class, args, getCounter2());
			} else {
				sql += prefix + ReportInstanceProcess.P_Counter + " = ?";
				args = OAArray.add(Object.class, args, getCounter());
			}
		}
		if (completed != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (completed2 != null && !completed.equals(completed2)) {
				sql += prefix + ReportInstanceProcess.P_Completed + " >= ?";
				args = OAArray.add(Object.class, args, this.completed);
				sql += " AND " + prefix + ReportInstanceProcess.P_Completed + " <= ?";
				args = OAArray.add(Object.class, args, this.completed2);
			} else {
				sql += prefix + ReportInstanceProcess.P_Completed + " = ?";
				args = OAArray.add(Object.class, args, this.completed);
			}
		}
		if (fixedJsonUseNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcess.P_FixedJson + " = null";
		} else if (fixedJsonUseNotNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcess.P_FixedJson + " != null";
		}
		if (!isNull(P_FixedJson)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcess.P_FixedJson + " = ?";
			args = OAArray.add(Object.class, args, this.fixedJson);
		}
		if (pypeErrorUseNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcess.P_PypeError + " = null";
		} else if (pypeErrorUseNotNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcess.P_PypeError + " != null";
		}
		if (!isNull(P_PypeError)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcess.P_PypeError + " = ?";
			args = OAArray.add(Object.class, args, this.pypeError);
		}
		if (processingErrorUseNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcess.P_ProcessingError + " = null";
		} else if (processingErrorUseNotNull) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcess.P_ProcessingError + " != null";
		}
		if (!isNull(P_ProcessingError)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcess.P_ProcessingError + " = ?";
			args = OAArray.add(Object.class, args, this.processingError);
		}
		if (!useReportInfoSearch && getReportInfo() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcessPP.reportInfo().pp + " = ?";
			args = OAArray.add(Object.class, args, getReportInfo());
		}
		if (useReportInfoSearch && getReportInfoSearch() != null) {
			getReportInfoSearch().appendSelect(prefix + ReportInstanceProcessPP.reportInfo().pp, select);
		}
		if (!useThreadInfoSearch && getThreadInfo() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + ReportInstanceProcessPP.threadInfo().pp + " = ?";
			args = OAArray.add(Object.class, args, getThreadInfo());
		}
		if (useThreadInfoSearch && getThreadInfoSearch() != null) {
			getThreadInfoSearch().appendSelect(prefix + ReportInstanceProcessPP.threadInfo().pp, select);
		}
		select.add(sql, args);
	}

	private OAFilter<ReportInstanceProcess> filterDataSourceFilter;

	public OAFilter<ReportInstanceProcess> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<ReportInstanceProcess>() {
			@Override
			public boolean isUsed(ReportInstanceProcess reportInstanceProcess) {
				return ReportInstanceProcessSearch.this.isUsedForDataSourceFilter(reportInstanceProcess);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<ReportInstanceProcess> filterCustomFilter;

	public OAFilter<ReportInstanceProcess> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<ReportInstanceProcess>() {
			@Override
			public boolean isUsed(ReportInstanceProcess reportInstanceProcess) {
				boolean b = ReportInstanceProcessSearch.this.isUsedForCustomFilter(reportInstanceProcess);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(ReportInstanceProcess searchReportInstanceProcess) {
		return true;
	}

	public boolean isUsedForCustomFilter(ReportInstanceProcess searchReportInstanceProcess) {
		return true;
	}
}
