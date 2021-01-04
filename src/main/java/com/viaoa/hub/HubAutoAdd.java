package com.viaoa.hub;

/**
 * Same as HubAutoMatch, except that this will not remove objects.
 * 
 * @author vvia
 * @param <TYPE>
 * @param <PROPTYPE>
 */
public class HubAutoAdd<TYPE, PROPTYPE> extends HubAutoMatch<TYPE, PROPTYPE> {

	public HubAutoAdd(Hub<TYPE> hub, String property, Hub<PROPTYPE> hubMaster, boolean bManuallyCalled) {
		super(hub, property, hubMaster, bManuallyCalled);
	}

	public HubAutoAdd(Hub<TYPE> hub, String property, Hub<PROPTYPE> hubMaster) {
		super(hub, property, hubMaster, false);
	}

	@Override
	public boolean okToRemove(Object obj, Object propertyValue) {
		return false;
	}
}
