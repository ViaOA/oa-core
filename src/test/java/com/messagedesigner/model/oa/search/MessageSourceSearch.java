// Generated by OABuilder
package com.messagedesigner.model.oa.search;

import java.util.logging.Logger;

import com.messagedesigner.model.oa.MessageSource;
import com.messagedesigner.model.oa.MessageType;
import com.messagedesigner.model.oa.propertypath.MessageSourcePP;
import com.messagedesigner.model.oa.search.MessageSourceSearch;
import com.messagedesigner.model.oa.search.MessageTypeSearch;
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
public class MessageSourceSearch extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(MessageSourceSearch.class.getName());
	public static final String P_Created = "Created";
	public static final String P_Source = "Source";
	public static final String P_Source2 = "Source2";
	public static final String P_Id = "Id";
	public static final String P_Id2 = "Id2";
	public static final String P_MessageType = "MessageType";
	public static final String P_UseMessageTypeSearch = "UseMessageTypeSearch";
	public static final String P_MaxResults = "MaxResults";

	protected OADateTime created;
	protected int source;
	protected int source2;
	protected int id;
	protected int id2;
	protected MessageType messageType;
	protected boolean useMessageTypeSearch;
	protected MessageTypeSearch searchMessageType;
	protected int maxResults;

	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
	}

	public int getSource() {
		return source;
	}

	public void setSource(int newValue) {
		int old = source;
		fireBeforePropertyChange(P_Source, old, newValue);
		this.source = newValue;
		firePropertyChange(P_Source, old, this.source);
		firePropertyChange(P_Source + "String");
		firePropertyChange(P_Source + "Enum");
		if (isLoading()) {
			return;
		}
		if (source > source2) {
			setSource2(this.source);
		}
	}

	public int getSource2() {
		return source2;
	}

	public void setSource2(int newValue) {
		int old = source2;
		fireBeforePropertyChange(P_Source2, old, newValue);
		this.source2 = newValue;
		firePropertyChange(P_Source2, old, this.source2);
		firePropertyChange(P_Source + "String");
		firePropertyChange(P_Source + "Enum");
		if (isLoading()) {
			return;
		}
		if (source > source2) {
			setSource(this.source2);
		}
	}

	public String getSourceString() {
		MessageSource.Source source = getSourceEnum();
		if (source == null) {
			return null;
		}
		return source.name();
	}

	public void setSourceString(String val) {
		int x = -1;
		if (OAString.isNotEmpty(val)) {
			MessageSource.Source source = MessageSource.Source.valueOf(val);
			if (source != null) {
				x = source.ordinal();
			}
		}
		if (x < 0) {
			setNull(P_Source);
		} else {
			setSource(x);
		}
	}

	public MessageSource.Source getSourceEnum() {
		if (isNull(P_Source)) {
			return null;
		}
		final int val = getSource();
		if (val < 0 || val >= MessageSource.Source.values().length) {
			return null;
		}
		return MessageSource.Source.values()[val];
	}

	public void setSourceEnum(MessageSource.Source val) {
		if (val == null) {
			setNull(P_Source);
		} else {
			setSource(val.ordinal());
		}
	}

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
	public MessageType getMessageType() {
		if (messageType == null) {
			messageType = (MessageType) getObject(P_MessageType);
		}
		return messageType;
	}

	public void setMessageType(MessageType newValue) {
		MessageType old = this.messageType;
		this.messageType = newValue;
		firePropertyChange(P_MessageType, old, this.messageType);
	}

	public boolean getUseMessageTypeSearch() {
		return useMessageTypeSearch;
	}

	public void setUseMessageTypeSearch(boolean newValue) {
		boolean old = this.useMessageTypeSearch;
		this.useMessageTypeSearch = newValue;
		firePropertyChange(P_UseMessageTypeSearch, old, this.useMessageTypeSearch);
	}

	public MessageTypeSearch getMessageTypeSearch() {
		return this.searchMessageType;
	}

	public void setMessageTypeSearch(MessageTypeSearch newValue) {
		this.searchMessageType = newValue;
	}

	public void reset() {
		setCreated(null);
		setSource(0);
		setNull(P_Source);
		setSource2(0);
		setNull(P_Source2);
		setId(0);
		setNull(P_Id);
		setId2(0);
		setNull(P_Id2);
		setMessageType(null);
		setUseMessageTypeSearch(false);
	}

	public boolean isDataEntered() {
		if (getCreated() != null) {
			return true;
		}
		if (!isNull(P_Source)) {
			return true;
		}
		if (!isNull(P_Id)) {
			return true;
		}
		if (getMessageType() != null) {
			return true;
		}
		if (getUseMessageTypeSearch()) {
			return true;
		}
		return false;
	}

	protected String extraWhere;
	protected Object[] extraWhereParams;
	protected OAFilter<MessageSource> filterExtraWhere;

	public void setExtraWhere(String s, Object... args) {
		this.extraWhere = s;
		this.extraWhereParams = args;
		if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
			OAFilter<MessageSource> f = new OAQueryFilter<MessageSource>(MessageSource.class, s, args);
			setExtraWhereFilter(f);
		}
	}

	public void setExtraWhereFilter(OAFilter<MessageSource> filter) {
		this.filterExtraWhere = filter;
	}

	public OAFilter<MessageSource> getExtraWhereFilter() {
		return this.filterExtraWhere;
	}

	public OASelect<MessageSource> getSelect() {
		final String prefix = "";
		String sql = "";
		String sortOrder = null;
		Object[] args = new Object[0];
		OAFinder finder = null;
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += MessageSource.P_Created + " = ?";
			args = OAArray.add(Object.class, args, this.created);
		}
		if (!isNull(P_Source)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Source2) && source != source2) {
				sql += MessageSource.P_Source + " >= ?";
				args = OAArray.add(Object.class, args, getSource());
				sql += " AND " + MessageSource.P_Source + " <= ?";
				args = OAArray.add(Object.class, args, getSource2());
			} else {
				sql += MessageSource.P_Source + " = ?";
				args = OAArray.add(Object.class, args, getSource());
			}
		}
		if (!isNull(P_Id)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Id2) && id != id2) {
				sql += MessageSource.P_Id + " >= ?";
				args = OAArray.add(Object.class, args, getId());
				sql += " AND " + MessageSource.P_Id + " <= ?";
				args = OAArray.add(Object.class, args, getId2());
			} else {
				sql += MessageSource.P_Id + " = ?";
				args = OAArray.add(Object.class, args, getId());
			}
		}
		if (!useMessageTypeSearch && getMessageType() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += MessageSourcePP.messageTypes().pp + " = ?";
			args = OAArray.add(Object.class, args, getMessageType());
			finder = new OAFinder<MessageType, MessageSource>(getMessageType(), MessageType.P_MessageSource);
		}

		if (OAString.isNotEmpty(extraWhere)) {
			if (sql.length() > 0) {
				sql = "(" + sql + ") AND ";
			}
			sql += extraWhere;
			args = OAArray.add(Object.class, args, extraWhereParams);
		}

		OASelect<MessageSource> select = new OASelect<MessageSource>(MessageSource.class, sql, args, sortOrder);
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
		if (useMessageTypeSearch && getMessageTypeSearch() != null) {
			getMessageTypeSearch().appendSelect(MessageSourcePP.messageTypes().pp, select);
		}
		return select;
	}

	public void appendSelect(final String fromName, final OASelect select) {
		final String prefix = fromName + ".";
		String sql = "";
		Object[] args = new Object[0];
		if (created != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + MessageSource.P_Created + " = ?";
			args = OAArray.add(Object.class, args, this.created);
		}
		if (!isNull(P_Source)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Source2) && source != source2) {
				sql += prefix + MessageSource.P_Source + " >= ?";
				args = OAArray.add(Object.class, args, getSource());
				sql += " AND " + prefix + MessageSource.P_Source + " <= ?";
				args = OAArray.add(Object.class, args, getSource2());
			} else {
				sql += prefix + MessageSource.P_Source + " = ?";
				args = OAArray.add(Object.class, args, getSource());
			}
		}
		if (!isNull(P_Id)) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			if (!isNull(P_Id2) && id != id2) {
				sql += prefix + MessageSource.P_Id + " >= ?";
				args = OAArray.add(Object.class, args, getId());
				sql += " AND " + prefix + MessageSource.P_Id + " <= ?";
				args = OAArray.add(Object.class, args, getId2());
			} else {
				sql += prefix + MessageSource.P_Id + " = ?";
				args = OAArray.add(Object.class, args, getId());
			}
		}
		if (!useMessageTypeSearch && getMessageType() != null) {
			if (sql.length() > 0) {
				sql += " AND ";
			}
			sql += prefix + MessageSourcePP.messageTypes().pp + " = ?";
			args = OAArray.add(Object.class, args, getMessageType());
		}
		if (useMessageTypeSearch && getMessageTypeSearch() != null) {
			getMessageTypeSearch().appendSelect(prefix + MessageSourcePP.messageTypes().pp, select);
		}
		select.add(sql, args);
	}

	private OAFilter<MessageSource> filterDataSourceFilter;

	public OAFilter<MessageSource> getDataSourceFilter() {
		if (filterDataSourceFilter != null) {
			return filterDataSourceFilter;
		}
		filterDataSourceFilter = new OAFilter<MessageSource>() {
			@Override
			public boolean isUsed(MessageSource messageSource) {
				return MessageSourceSearch.this.isUsedForDataSourceFilter(messageSource);
			}
		};
		return filterDataSourceFilter;
	}

	private OAFilter<MessageSource> filterCustomFilter;

	public OAFilter<MessageSource> getCustomFilter() {
		if (filterCustomFilter != null) {
			return filterCustomFilter;
		}
		filterCustomFilter = new OAFilter<MessageSource>() {
			@Override
			public boolean isUsed(MessageSource messageSource) {
				boolean b = MessageSourceSearch.this.isUsedForCustomFilter(messageSource);
				return b;
			}
		};
		return filterCustomFilter;
	}

	public boolean isUsedForDataSourceFilter(MessageSource searchMessageSource) {
		return true;
	}

	public boolean isUsedForCustomFilter(MessageSource searchMessageSource) {
		return true;
	}
}