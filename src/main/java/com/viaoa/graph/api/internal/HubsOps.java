package com.viaoa.graph.api.internal;

import com.viaoa.graph.api.internal.hubs.HubAOOps;
import com.viaoa.graph.api.internal.hubs.HubAddRemoveOps;
import com.viaoa.graph.api.internal.hubs.HubAutoMatchOps;
import com.viaoa.graph.api.internal.hubs.HubCSOps;
import com.viaoa.graph.api.internal.hubs.HubCombineOps;
import com.viaoa.graph.api.internal.hubs.HubCopyOps;
import com.viaoa.graph.api.internal.hubs.HubDataOps;
import com.viaoa.graph.api.internal.hubs.HubDeleteOps;
import com.viaoa.graph.api.internal.hubs.HubDetailOps;
import com.viaoa.graph.api.internal.hubs.HubEventOps;
import com.viaoa.graph.api.internal.hubs.HubFilterOps;
import com.viaoa.graph.api.internal.hubs.HubFindOps;
import com.viaoa.graph.api.internal.hubs.HubLinkOps;
import com.viaoa.graph.api.internal.hubs.HubMergeOps;
import com.viaoa.graph.api.internal.hubs.HubPropertyOps;
import com.viaoa.graph.api.internal.hubs.HubRootOps;
import com.viaoa.graph.api.internal.hubs.HubSaveOps;
import com.viaoa.graph.api.internal.hubs.HubSelectOps;
import com.viaoa.graph.api.internal.hubs.HubSequenceOps;
import com.viaoa.graph.api.internal.hubs.HubSerializeOps;
import com.viaoa.graph.api.internal.hubs.HubShareOps;
import com.viaoa.graph.api.internal.hubs.HubSizeOps;
import com.viaoa.graph.api.internal.hubs.HubSortOps;
import com.viaoa.graph.api.internal.hubs.HubStatusOps;
import com.viaoa.graph.api.internal.hubs.HubViewOps;

public interface HubsOps {

	public HubAddRemoveOps addRemove();

	public HubAOOps ao();

	public HubAutoMatchOps autoMatch();

	public HubCombineOps combine();

	public HubCopyOps copy();

	public HubCSOps cs();

	public HubDataOps data();

	public HubDeleteOps delete();

	public HubDetailOps detail();

	public HubEventOps events();

	public HubFilterOps filter();

	public HubFindOps find();

	public HubLinkOps link();

	public HubMergeOps merge();

	public HubPropertyOps property();

	public HubRootOps root();

	public HubSaveOps save();

	public HubSelectOps select();

	public HubSequenceOps sequence();

	public HubSerializeOps serialize();

	public HubShareOps share();

	public HubSizeOps size();

	public HubSortOps sort();

	public HubStatusOps status();

	public HubViewOps view();
}
