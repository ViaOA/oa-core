package com.viaoa.hub.link;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.lang.OAStr;
import com.viaoa.metadata.OALinkInfo;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

public class HubSelectLink<F extends OAObject, T extends OAObject> {

	/** Source Hub, could be a Hub that uses searches. */
	private final Hub<F> hubFrom;
	
	/** List of objects selected from hubFrom that affect HubTo. */
	private final Hub<F> hubSelect;
	
	private final Hub<T> hubTo;
	
	private final String toProperty;
	
	private final boolean bFromAoOnly;
	private final boolean bToAoOnly;
	
//qqqqqqqqqqq todo: not used yet	
	private final boolean bAllowRemove;	
	
	private Type type;
	private HubListener<T> hlHubTo;

	
	
	public HubSelectLink(
		Hub<F> hubFrom, Hub<F> hubSelect, Hub<T> hubTo, 
		String toProperty, 
		boolean bFromAoOnly, boolean bToAoOnly, boolean bAllowRemove) 
	{
		this.hubFrom = hubFrom;
		this.hubSelect = hubSelect;
		this.hubTo = hubTo;
		this.toProperty = toProperty;
		
		this.bFromAoOnly = bFromAoOnly && (hubSelect == null);
		this.bToAoOnly = this.bFromAoOnly && bToAoOnly;
		this.bAllowRemove = bAllowRemove;
		
		setup();
		if (type == null || type == Type.Unknown) {
			throw new RuntimeException("invalid configuration");
		}
	}
	
	public HubSelectLink(Hub<F> hubFrom, Hub<T> hubTo, String toProperty, boolean bFromAoOnly, boolean bToAoOnly, boolean bAllowRemove) {
		this(hubFrom, null, hubTo, toProperty, bFromAoOnly, bToAoOnly, bAllowRemove);
	}
	
	public HubSelectLink(Hub<F> hubFrom, Hub<T> hubTo, boolean bFromAoOnly, boolean bAllowRemove) {
		this(hubFrom, null, hubTo, null, bFromAoOnly, false, bAllowRemove);
	}

	public HubSelectLink(Hub<F> hubFrom, Hub<T> hubTo, String toProperty) {
		this(hubFrom, null, hubTo, toProperty, false, false, false);
	}
	
	public HubSelectLink(Hub<F> hubFrom, Hub<F> hubSelect, Hub<T> hubTo, String toProperty) {
		this(hubFrom, hubSelect, hubTo, toProperty, false, false, false);
	}
	
	public HubSelectLink(Hub<F> hubFrom, Hub<F> hubSelect, Hub<T> hubTo, boolean bAllowRemove) {
		this(hubFrom, hubSelect, hubTo, null, false, false, bAllowRemove);
	}
	
	public void close() {
		if (hlHubTo != null) hubTo.removeHubListener(hlHubTo);
	}

	
	public static enum Type {
		Unknown,
		// hubSelect=null
		hubFromAoUpdatesHubToAoProperty,
		hubFromAoFindOrCreateHubToUsingProperty,
		hubFromAllFindOrCreateHubToUsingProperty, 
		hubFromMatchHubTo, // no property
		hubFromAoAddToHubTo, // no property
        // hubSelect!=null
		hubSelectFindOrCreateHubToUsingProperty,
		hubSelectMatchHubTo,
	}
 
	protected void setup() {
		type = Type.Unknown;
		if (hubFrom == null || hubTo == null) return; // no go

		if (hubFrom == hubSelect || hubFrom == hubTo || hubSelect == hubTo) return;
		final OA oa = OARuntime.oa(hubFrom);
		if (oa.internal().hubs().share().isUsingSameSharedHub(hubFrom, hubTo)) return;
		if (hubSelect != null) {
			if (oa.internal().hubs().share().isUsingSameSharedHub(hubFrom, hubSelect)) return;
			if (oa.internal().hubs().share().isUsingSameSharedHub(hubTo, hubSelect)) return;
		}
		
		if (OAStr.isNotEmpty(toProperty)) {
			OAObjectInfo oiTo = hubTo.getOAObjectInfo(); 
			OALinkInfo li = oiTo.getLinkInfo(toProperty);
			if (li == null) return; // must be linkInfo
			if (li.getType() != OALinkInfo.TYPE_ONE) return;
			if (!li.getToClass().equals(hubFrom.getObjectClass())) return;
		}
		
		boolean bNeedsUpdate = false;
		if (hubSelect == null) {
			if (OAStr.isEmpty(toProperty)) {
				if (!hubFrom.getObjectClass().equals(hubTo.getObjectClass())) return;
				if (bFromAoOnly) type = Type.hubFromAoAddToHubTo;
				else type = Type.hubFromMatchHubTo;
				return; 
			}
			if (bFromAoOnly) {
				if (bToAoOnly) type = Type.hubFromAoUpdatesHubToAoProperty;
				else type = Type.hubFromAoFindOrCreateHubToUsingProperty;
			}
			else type = Type.hubFromAllFindOrCreateHubToUsingProperty;
		}
		else {
			bNeedsUpdate = true;
			if (OAStr.isEmpty(toProperty)) {
				if (!hubFrom.getObjectClass().equals(hubTo.getObjectClass())) return;
				type = Type.hubSelectMatchHubTo;
			}
			else type = Type.hubSelectFindOrCreateHubToUsingProperty;
		}

		if (bNeedsUpdate) {
//qqqqqqqqq todo:  handle add,insert,remove, toProperty change, AO, etc 			
			hlHubTo = new HubListenerAdapter<T>() {
				@Override
				public void afterNewList(HubEvent<T> e) {
					HubSelectLink.this.update();
				}
			};
			hubTo.addHubListener(hlHubTo);
			update();
		}
	}

	/**
	 * Called to have hubSelect match hubTo
	 */
	public void update() {
		if (hubSelect == null) return;

		switch (type) {
		case Type.Unknown:
			break;
		case Type.hubFromAoUpdatesHubToAoProperty:
			break;
		case Type.hubFromAoFindOrCreateHubToUsingProperty:
			break;
		case Type.hubFromAllFindOrCreateHubToUsingProperty:
			break;
		case Type.hubFromMatchHubTo:
			break;
		case Type.hubFromAoAddToHubTo:
			break;
		case Type.hubSelectFindOrCreateHubToUsingProperty:
			hubSelect.clear();
			for (OAObject objTo : hubTo) {
				F objx = (F) objTo.getProperty(toProperty);
				hubSelect.add(objx);
			}
			break;
		case Type.hubSelectMatchHubTo:
			hubSelect.clear();
			for (T objTo : hubTo) {
				hubSelect.add((F) objTo);
			}
			break;
		}
	}
	
	
	public void select() {
//qqqqqqqqqq todo: need to verify that changes can be made ... using callback, etc.
		OAObjectInfo oiFrom = hubFrom.getOAObjectInfo(); 
		OAObjectInfo oiTo = hubTo.getOAObjectInfo(); 

		F objFrom;
		T objTo;
		
		OA oa = OARuntime.oa(hubTo);
		
		switch (type) {
		case Type.Unknown:
			break;
		case Type.hubFromAoUpdatesHubToAoProperty:
			objTo = hubTo.getAO();
			objFrom = hubFrom.getAO();
			if (objTo != null) objTo.setProperty(toProperty, objFrom);
			break;
		case Type.hubFromAoFindOrCreateHubToUsingProperty:
			objFrom = hubFrom.getAO();
			if (objFrom == null) break;
			objTo = hubTo.find(toProperty, objFrom);
			if (objTo == null) {
				objTo = oa.create(hubTo.getObjectClass());
				objTo.setProperty(toProperty, objFrom);
				hubTo.add(objTo);
			}
			break;
		case Type.hubFromAllFindOrCreateHubToUsingProperty:
			for (F objFromx : hubFrom) {
				objTo = hubTo.find(toProperty, objFromx);
				if (objTo == null) {
					objTo = oa.create(hubTo.getObjectClass());
					objTo.setProperty(toProperty, objFromx);
					hubTo.add(objTo);
//qqqqqqqqq bAllowRemove					
				}
			}
			break;
		case Type.hubFromMatchHubTo:
			for (F objFromx : hubFrom) {
				hubTo.add( (T) objFromx);
//qqqqqqqqq bAllowRemove					
			}
			break;
		case Type.hubFromAoAddToHubTo:
			objFrom = hubFrom.getAO();
			hubTo.add( (T) objFrom);
			break;
		case Type.hubSelectFindOrCreateHubToUsingProperty:
			
			for (F objFromx : hubSelect) {
				objTo = hubTo.find(toProperty, objFromx);
				if (objTo == null) {
					objTo = oa.create(hubTo.getObjectClass());
					objTo.setProperty(toProperty, objFromx);
					hubTo.add(objTo);
//qqqqqqqqq bAllowRemove					
				}
			}
			break;
		case Type.hubSelectMatchHubTo:
			for (F objFromx : hubSelect) {
				hubTo.add( (T) objFromx);
//qqqqqqqqq bAllowRemove					
			}
			break;
		}
		
	}
	public Type getType() {
		return this.type;
	}
	public Hub<F> getFromHub() {
		return hubFrom;
	}
	public Hub<F> getSelectHub() {
		return hubSelect;
	}
	public Hub<T> getToHub() {
		return hubTo;
	}
	public String getToProperty() {
		return toProperty;
	}
	public boolean getFromAoOnly() {
		return bFromAoOnly;
	}
	public boolean getToAoOnly() {
		return bToAoOnly;
	}
	public boolean getAllowRemove() {
		return bAllowRemove;
	}
	
}





