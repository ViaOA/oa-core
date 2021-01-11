package com.viaoa.hub;

import java.lang.reflect.Method;

import com.viaoa.object.OAObject;
import com.viaoa.util.OAPropertyPath;

/**
 * Uses a HubMerger to get objects, and then adds the found objects to a groupBy object Hub property.
 * <p>
 * Example:<br>
 * mergerPP: [Orders]:open().orderItems.item <br>
 * groupByPP: [Orders].customer <br>
 * groupByProperty (in customer): Hub<Item> hubOpenOrderItems (calculated)<br>
 * <p> 
 * Example:<br>
 * mergerPP: [Orders]:open().orderItems <br>
 * groupByPP: [Orders].orderItems.item <br>
 * groupByProperty (in item): Hub<OrderItem> hubOpenOrderItems (calculated)<br>
 * @author vvia
 * @param <F> from object for the hub merger
 * @param <T> to object for the hub merger
 */
public class HubGroupByMerger<F extends OAObject, T extends OAObject> {

	private String mergerPropertyPath;
	private String groupByPropertyPath;
	private String groupByProperty;

	private int cntAbove; // number of data.parent to go from mergePropertyPath to then use groupByPropertyPath

	private HubMerger<F, T> hubMerger;

	/**
	 * @param hubRoot
	 * @param mergerPropertyPath  PP from Root to merger objects
	 * @param groupByPropertyPath PP from hubRoot objects to the object where there is a calc Hub<T> 
	 * for storing the found merger objects.  
	 * *NOTE: this PP must start from same root as mergerPropertyPath.
	 *                            <T>
	 * @param groupByProperty     name of property Hub<T> in groupByPP for storing the found merger objects.
	 */
	public HubGroupByMerger(Hub<F> hubRoot, String mergerPropertyPath, String groupByPropertyPath, String groupByProperty) {
		this.mergerPropertyPath = mergerPropertyPath;
		this.groupByPropertyPath = groupByPropertyPath;
		this.groupByProperty = groupByProperty;

        final OAPropertyPath ppMergerPropertyPath = new OAPropertyPath(hubRoot.getObjectClass(), mergerPropertyPath);
        Method[] msMergerPropertyPath = ppMergerPropertyPath.getMethods();
		
		final OAPropertyPath ppGroupByPropertyPath = new OAPropertyPath(hubRoot.getObjectClass(), groupByPropertyPath);
		Method[] msGroupByPropertyPath = ppGroupByPropertyPath.getMethods();

		int cnt = 0;
		for (; cnt < msGroupByPropertyPath.length && cnt < msMergerPropertyPath.length; cnt++) {
			if (!msGroupByPropertyPath[cnt].equals(msMergerPropertyPath[cnt])) {
				break;
			}
		}

		// find how much of the the groupBy PP is same as merger 
		this.cntAbove = msMergerPropertyPath.length - (cnt + 1);
		
		final int groupByPropertyPathStartPos = cnt;

		hubMerger = new HubMerger(hubRoot, null, mergerPropertyPath, false, null, true, false, false) {
			@Override
			protected void onAddToCombined(Data data, OAObject obj) {
			    if (cntAbove >= 0) {
    				for (int i = 0; data != null && i < cntAbove; i++) {
    					data = data.parent;
    				}
                    if (data == null) obj = null;
                    else {
                        obj = data.parentObject;
                    }
			    }
                OAObject objx = (OAObject) ppGroupByPropertyPath.getValue(obj, groupByPropertyPathStartPos);
                if (objx != null) {
                    Hub hub = (Hub) objx.getProperty(groupByProperty);
                    hub.add(obj);
                }
			}

			@Override
			protected void onRemoveFromCombined(Data data, OAObject obj) {
				for (int i = 0; data != null && i < cntAbove; i++) {
					data = data.parent;
				}
				if (data != null) {
					OAObject objx = (OAObject) ppGroupByPropertyPath.getValue(data.parentObject);
					Hub hub = (Hub) objx.getProperty(groupByProperty);
					hub.remove(obj);
				}
			}
		};

		hubMerger.setServerSideOnly(true);
	}
}
