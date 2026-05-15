package com.viaoa.graph.api;

/*qqqqqqqqqq
CODEX


#13 — public API surface risk
  File/class/method: src/main/java/com/viaoa/graph/api/ReplOps.java:4, ReplOps; src/main/java/com/viaoa/graph/
  service/OAReplicationService.java:84, start()
  Concern: OAGraph.replication() exposes an empty public contract, while the concrete service has lifecycle methods
  not represented in the API and can start a client with unset/null configuration if called directly.
  Why it matters: graph advertises replication as a first-class API, but there is no usable public contract or safe
  lifecycle guard.
  Minimal fix: either make replication explicitly internal for now, or add guarded public role/start/stop methods to
  ReplOps.
  Invariant: GRAPH_REPLICATION_PUBLIC_CONTRACT_MATCHES_IMPLEMENTATION
  Test coverage: replication start without create fails; master/client role creation and restart behavior are
  documented and tested.


*/

public interface ReplOps {

	
	
}

