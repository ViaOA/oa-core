package com.viaoa.graph.api.internal;

import com.viaoa.graph.OAGraph;

/*qqqqqqq
CODEX

  #4 — boundary risk
  File/class/method: src/main/java/com/viaoa/graph/OAGraphInternal.java:9
  Exact concern: internal graph API is public and directly castable from OAGraph.
  Why it matters: apps can bypass graph verbs and call object/hub/sync internals, weakening the intended OA 4.0
  facade boundary.
  Minimal fix: mark as unsupported internal API and, later, enforce with module exports or package visibility where
  practical.
  Suggested invariant: GRAPH_INTERNAL_OPS_ARE_NOT_APP_CONTRACT
  Suggested test coverage: architecture test that app-facing packages do not import com.viaoa.graph.api.internal.




*/

public interface OAGraphInternal extends OAGraph {
	
	public ObjectsInternalOps objectsInternal();

	public HubsInternalOps hubsInternal();
    
	public SyncInternalOps syncInternal();

	public ReplicationInternalOps replInternal();
	
	public TriggerInternalOps triggerInternal();
	
}
