package com.viaoa.oa.api.internal.hubs;

import java.util.Comparator;

import com.viaoa.hub.Hub;
import com.viaoa.hub.sort.HubSortListener;

public interface HubSortOps {

 	public HubSortListener getSortListener(Hub<?> hub);
	public void sort(Hub<?> hub, String propertyPaths, boolean bAscending, Comparator<?> comp);
	public boolean isSorted(Hub<?> hub);
	public void cancelSort(Hub<?> hub);
	public void sort(Hub<?> hub);
	public void resort(Hub<?> hub);

}
