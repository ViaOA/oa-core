# com.viaoa.sync.remote

## Purpose

Coordinates client/server live runtime synchronization.

## Architectural Role

Real-time synchronization

## Primary Responsibilities

- Define and preserve the package contracts used by OA runtime services, generated applications, and tests.
- Keep terminology aligned with OA 4.0: OA runtime, OAObject, Hub, Path, ModelUser, SessionUser, and OAObjectRulesService where applicable.

## Package Boundary

Types in this package should be used according to the package role above. Implementation details should not be treated as public contracts unless exposed by public/protected APIs or existing documented behavior.

## Key Types

ClientGetDetail, RemoteClientCallbackInterface, RemoteClientImpl, RemoteClientInterface, RemoteDataSource, RemoteServerImpl, RemoteServerInterface, RemoteSessionImpl, RemoteSessionInterface, RemoteSyncImpl, RemoteSyncInterface

## Dependencies

- com.viaoa
- com.viaoa.callback
- com.viaoa.cascade
- com.viaoa.compare.match
- com.viaoa.concurrent
- com.viaoa.datasource
- com.viaoa.datasource.clientserver
- com.viaoa.filter
- com.viaoa.hub
- com.viaoa.metadata
- com.viaoa.oa
- com.viaoa.oa.sibling
- com.viaoa.object
- com.viaoa.performance
- com.viaoa.remote.multiplexer

## Package-Level Invariants

### INV-PKG-SYNC-REMOTE-001: Remote identities and method contracts are serializable

**Contract**

Remote requests, bind metadata, and callback interfaces must use stable serializable identifiers and arguments.

**Rationale**

Distributed synchronization/replication fails if remote method metadata or object identity changes mid-call.

**Evidence**

Remote* interfaces, RequestInfo/MethodInfo/BindInfo, sync/replication clients

**Test implications**

Serialize remote request metadata and verify target method/object identity survives round trip.

**Confidence**

Medium

### INV-PKG-SYNC-REMOTE-002: Client/server state changes are ordered

**Contract**

Remote sync and replication operations must preserve the intended order of object, Hub, and datasource changes per connection/session.

**Rationale**

Out-of-order operations can corrupt live Hubs and replicated object state.

**Evidence**

OASyncClient/OASyncServer, RemoteServerImpl, OAReplication*

**Test implications**

Apply ordered add/change/delete events and verify clients converge.

**Confidence**

Medium

## Lifecycle and State Rules

Package state should be initialized before runtime use and cleaned up when lifecycle APIs expose cleanup or cancellation. Static or shared state must be treated as runtime-wide unless the API explicitly scopes it by OA instance, object class, Hub, or thread.

## Threading and Concurrency Rules

Unless a type explicitly documents thread safety, callers should treat mutable instances as single-owner or configure-before-publish. Listener, callback, cache, executor, remote, and thread-local types need focused tests for leak-free cleanup.

## Cross-Package Contracts

This package participates in OA runtime contracts through metadata, OAObject, Hub, runtime services, callbacks, paths, datasource, sync, or utility APIs as indicated by its dependencies.

## Required Invariant Tests

- Verify the package-level invariants above with representative model classes and real OA runtime services where practical.
- Include null/boundary behavior, lifecycle cleanup, and cross-package integration paths.

## Open Questions or Unclear Contracts

Some invariants are inferred from current implementation and existing Javadocs. Where confidence is Medium or Low, tests should lock the intended behavior before relying on it as a public architectural guarantee.
