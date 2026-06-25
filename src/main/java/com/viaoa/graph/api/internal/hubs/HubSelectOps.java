package com.viaoa.graph.api.internal.hubs;

import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.select.OASelect;

public interface HubSelectOps {

	public <T extends OAObject> OASelect<T> getSelect(Hub<T> hub, boolean bCreateIfNull);
	public void loadAllData(Hub<?> hub);
	public void cancelSelect(Hub<?> hub, boolean bRemoveSelect);
	public boolean isMoreData(Hub<?> hub);
	public void setSelectWhere(Hub<?> hub, String whereClause);
	public String getSelectWhere(Hub<?> hub);
	public void setSelectOrder(Hub<?> hub, String orderClause);
	public <T extends OAObject> void setSelectWhereHub(Hub<T> hub, Hub<T> hubSelect);
	public void setSelectWhereHubPropertyPath(Hub<?> hub, String ppFromHub);
	public String getSelectOrder(Hub<?> hub);
	public void select(Hub<?> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderByClause, boolean bAppendFlag);
	public void select(Hub<?> hub, boolean bAppendFlag);
	public <T extends OAObject> void select(Hub<T> hub, OAObject whereObject, String whereClause, Object[] whereParams, String orderBy, boolean bAppendFlag, OAFilter<T> filter);
	public <T extends OAObject> void select(Hub<T> hub, OASelect<T> select);
	public void selectPassthru(Hub<?> hub, String whereClause, String orderClause);
	public <T extends OAObject> OASelect<T> getSelect(Hub<T> hub);
	public void refresh(Hub<?> hub);
	public <T extends OAObject> Hub<T> getSelectWhereHub(Hub<T> hub);
	public String getSelectWhereHubPropertyPath(Hub<?> hub);
	public boolean adoptWhereHub(final Hub<?> thisHub, final String propName, final Hub<?> hubFrom);
}
