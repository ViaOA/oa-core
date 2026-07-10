package com.viaoa.oa.api.internal;

import com.viaoa.oa.api.internal.hubs.HubAOOps;
import com.viaoa.oa.api.internal.hubs.HubAddRemoveOps;
import com.viaoa.oa.api.internal.hubs.HubAutoMatchOps;
import com.viaoa.oa.api.internal.hubs.HubCSOps;
import com.viaoa.oa.api.internal.hubs.HubCombineOps;
import com.viaoa.oa.api.internal.hubs.HubCopyOps;
import com.viaoa.oa.api.internal.hubs.HubDataOps;
import com.viaoa.oa.api.internal.hubs.HubDeleteOps;
import com.viaoa.oa.api.internal.hubs.HubDetailOps;
import com.viaoa.oa.api.internal.hubs.HubEventOps;
import com.viaoa.oa.api.internal.hubs.HubFilterOps;
import com.viaoa.oa.api.internal.hubs.HubFindOps;
import com.viaoa.oa.api.internal.hubs.HubLinkOps;
import com.viaoa.oa.api.internal.hubs.HubMergeOps;
import com.viaoa.oa.api.internal.hubs.HubPropertyOps;
import com.viaoa.oa.api.internal.hubs.HubRootOps;
import com.viaoa.oa.api.internal.hubs.HubSaveOps;
import com.viaoa.oa.api.internal.hubs.HubSelectOps;
import com.viaoa.oa.api.internal.hubs.HubSequenceOps;
import com.viaoa.oa.api.internal.hubs.HubSerializeOps;
import com.viaoa.oa.api.internal.hubs.HubShareOps;
import com.viaoa.oa.api.internal.hubs.HubSizeOps;
import com.viaoa.oa.api.internal.hubs.HubSortOps;
import com.viaoa.oa.api.internal.hubs.HubStatusOps;
import com.viaoa.oa.api.internal.hubs.HubViewOps;

/**
 * Internal Hub operation families exposed through {@code OA.internal().hubs()}.
 */
public interface HubsOps {

	/**
	 * Returns internal Hub add/remove operations.
	 *
	 * @return the add/remove operations facade
	 */
	public HubAddRemoveOps addRemove();

	/**
	 * Returns internal Hub active-object operations.
	 *
	 * @return the active-object operations facade
	 */
	public HubAOOps ao();

	/**
	 * Returns internal Hub auto-match operations.
	 *
	 * @return the auto-match operations facade
	 */
	public HubAutoMatchOps autoMatch();

	/**
	 * Returns internal Hub combine operations.
	 *
	 * @return the combine operations facade
	 */
	public HubCombineOps combine();

	/**
	 * Returns internal Hub copy operations.
	 *
	 * @return the copy operations facade
	 */
	public HubCopyOps copy();

	/**
	 * Returns internal client/server operations for the current object or Hub family.
	 *
	 * @return the client/server operations facade
	 */
	public HubCSOps cs();

	/**
	 * Returns internal Hub data and membership operations.
	 *
	 * @return the data operations facade
	 */
	public HubDataOps data();

	/**
	 * Returns internal delete operations for the current object or Hub family.
	 *
	 * @return the delete operations facade
	 */
	public HubDeleteOps delete();

	/**
	 * Returns internal Hub master/detail operations.
	 *
	 * @return the detail operations facade
	 */
	public HubDetailOps detail();

	/**
	 * Returns internal Hub event and listener operations.
	 *
	 * @return the event operations facade
	 */
	public HubEventOps events();

	/**
	 * Returns internal Hub filter operations.
	 *
	 * @return the filter operations facade
	 */
	public HubFilterOps filter();

	/**
	 * Returns internal find/search operations for the current object or Hub family.
	 *
	 * @return the find operations facade
	 */
	public HubFindOps find();

	/**
	 * Returns internal Hub link operations.
	 *
	 * @return the link operations facade
	 */
	public HubLinkOps link();

	/**
	 * Returns internal Hub merge operations.
	 *
	 * @return the merge operations facade
	 */
	public HubMergeOps merge();

	/**
	 * Returns internal property operations for the current object or Hub family.
	 *
	 * @return the property operations facade
	 */
	public HubPropertyOps property();

	/**
	 * Returns internal Hub root operations.
	 *
	 * @return the root operations facade
	 */
	public HubRootOps root();

	/**
	 * Returns internal save operations for the current object or Hub family.
	 *
	 * @return the save operations facade
	 */
	public HubSaveOps save();

	/**
	 * Returns internal Hub select/query operations.
	 *
	 * @return the select operations facade
	 */
	public HubSelectOps select();

	/**
	 * Returns internal Hub sequence operations.
	 *
	 * @return the sequence operations facade
	 */
	public HubSequenceOps sequence();

	/**
	 * Returns internal serialization operations for the current object or Hub family.
	 *
	 * @return the serialization operations facade
	 */
	public HubSerializeOps serialize();

	/**
	 * Returns internal Hub sharing operations.
	 *
	 * @return the sharing operations facade
	 */
	public HubShareOps share();

	/**
	 * Returns internal Hub size operations.
	 *
	 * @return the size operations facade
	 */
	public HubSizeOps size();

	/**
	 * Returns internal Hub sort operations.
	 *
	 * @return the sort operations facade
	 */
	public HubSortOps sort();

	/**
	 * Returns internal Hub status operations.
	 *
	 * @return the status operations facade
	 */
	public HubStatusOps status();

	/**
	 * Returns internal Hub view operations.
	 *
	 * @return the view operations facade
	 */
	public HubViewOps view();
}
