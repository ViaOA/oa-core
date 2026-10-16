package com.viaoa.hub.view;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.viaoa.hub.Hub;
import com.viaoa.hub.HubListener;
import com.viaoa.object.OAObject;

/**
 * Shares a destination Hub with whichever registered source Hub most recently
 * changes to a non-null active object (AO), then copies that AO to the
 * destination.
 *
 * <p>For each qualifying source event, this helper calls:
 * <pre>{@code
 * hubMain.setSharedHub(sourceHub, false);
 * hubMain.setActiveObject(sourceHub.getAO());
 * }</pre>
 *
 * <p>The destination shares the source's contents but retains independent AO
 * state. Changing the destination AO does not change the source AO through
 * this helper. Source AO changes to {@code null} are ignored.
 *
 * <p>Registration does not immediately change the destination. A source must
 * subsequently fire an AO-change event with a non-null AO. Programmatic AO
 * changes are handled in the same way as changes initiated by a user.
 *
 * <p>This allows several navigation Hubs, such as search results, a pick list,
 * and a tree selection, to supply one stable Hub for subsequent operations.
 * This helper does not execute selection commands or commit a HubSelectLink.
 *
 * <pre>{@code
 * HubShareOnAOChange<ItemCategory> sharing =
 *     new HubShareOnAOChange<>(
 *         hubSingle, hubResults, hubSearchFrom, hubTree);
 * }</pre>
 *
 * <p>Additional sources can be registered using {@link #add(Hub)}.
 * Null sources, duplicate sources, and the destination itself are ignored.
 * The destination supplied to the constructor must be non-null.
 *
 * <p>Registered listeners remain attached to their source Hubs; this class
 * currently provides no method to unregister them.
 *
 * @param <T> the OAObject type contained in the participating Hubs
 */
public class HubShareOnAOChange<T extends OAObject> implements Closeable {

	private Hub<T> hubMain;
	
	public HubShareOnAOChange(Hub<T> hubMain, Hub<T> ...hubs) {
		this.hubMain = hubMain;
		if (hubs != null) {
			for (Hub<T> h : hubs) {
				add(h);
			}
		}
	}

	private Map<Hub, HubListener> hmOnChangeListener = new HashMap<>();
	
	public void add(Hub<T> h) {
		if (h == null || h == hubMain) return;
		if (hmOnChangeListener.containsKey(h)) return;
		HubListener hl = h.onChangeAO(onEvent -> {
			Hub<T> hx = onEvent.getHub();
			T t = hx.getAO();
			if (t == null) return;
			hubMain.setSharedHub(hx, false);
			hubMain.setActiveObject(t);
		});
		hmOnChangeListener.put(h, hl);
	}
	
	public void remove(Hub<T> h) {
	    if (h == null) return;
	    HubListener<T> hl = hmOnChangeListener.remove(h);
	    if (hl != null) h.removeListener(hl);
	}

	public void close() {
	    for (Hub<T> h : hmOnChangeListener.keySet().toArray(new Hub[0])) {
	        remove(h);
	    }
	}
	
}
