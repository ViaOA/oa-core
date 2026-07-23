# OA Unit Test Scenario Matrix

This document defines the canonical scenario vocabulary and source-grounded unit-test matrix for OA 4.0 Hub wiring, Hub actions, OAObject references, and reference actions. It is a test-design inventory only; it does not define new behavior and it is not a separate test suite.

Public API tests should prove final observable behavior through the public class under test. Service tests should prove the narrower rule owned by that service. Do not duplicate the full end-to-end scenario and all assertions in every participating service test.

## Scope and Evidence

Production source reviewed for this matrix:

- `com.viaoa.hub.Hub`
- `com.viaoa.hub.*` support classes used by Hub data, events, filtering, sorting, sharing, and linking
- `com.viaoa.object.OAObject`
- `com.viaoa.oa.service.hub.*`
- `com.viaoa.oa.service.object.*`
- `com.viaoa.metadata.*`
- directly involved runtime and internal facade classes where service delegation is visible

Primary source evidence used repeatedly:

- `Hub` constructors and public methods delegate Hub behavior into OA runtime services.
- `HubAOService#setActiveObject(...)` owns AO mutation, detail updates, link updates, shared-Hub propagation, recursion guards, and AO event ordering.
- `HubAddRemoveService#add(...)`, `insert(...)`, `remove(...)`, `clear(...)`, and related helpers own membership mutation, reverse-reference updates, callback checks, and event firing.
- `HubDetailService#setMasterHub(...)`, `getDetailHub(...)`, `updateDetail(...)`, `updateAllDetail(...)`, `updateDetailActiveObject(...)`, and `setPropertyToMasterHub(...)` own master/detail alignment and reverse-reference maintenance.
- `HubLinkService#setLinkHub(...)`, `updateLinkProperty(...)`, `updateLinkedFromHub(...)`, and `getLinkFromHubObjectForLinkToHubObject(...)` own linked-Hub setup and bidirectional AO/link synchronization.
- `OAObjectPropertyService#setProperty(...)`, `setPropertyHubIfNotSet(...)`, `setPropertyCAS(...)`, `getProperty(...)`, and weak-reference helpers own low-level OAObject property/reference storage.
- `OAObjectReflectService#getReferenceHub(...)` and `getReferenceObject(...)` own reference-Hub and single-reference lookup/loading behavior.
- `OALinkInfo` and `OAObjectInfo` describe link cardinality, reverse links, ownership, cascade behavior, auto-create, calculated/transient status, and path metadata.

## Classification Legend

| Classification | Meaning |
| --- | --- |
| REQUIRED | The wiring or reference shape changes the observable contract and needs a direct behavioral test. |
| BASELINE | The standard action test covers this configuration sufficiently. |
| N/A | The wiring/action or reference/action combination does not apply. |
| INDIRECT | Covered through another action or service, but the relationship should be named. |
| DEFERRED | The scenario is relevant, but the expected invariant needs confirmation before implementing a test. |
| INVALID | OA prevents or does not support this combination. |

## Evidence-Level Legend

| Evidence level | Meaning |
| --- | --- |
| EXPLICIT_SOURCE | Direct source branch, guard, assignment, or service call enforces the behavior. |
| JAVADOC | Current Javadoc states the behavior. |
| STRONGLY_INFERRED | Multiple source paths rely on the behavior, but it is not stated as a named contract. |
| EXISTING_TEST_ONLY | Existing tests show the behavior, but source intent was not confirmed. |
| CURRENT_IMPLEMENTATION | The behavior is what the code currently does, but may be an incidental implementation detail. |
| AMBIGUOUS | The intended contract is not clear enough to lock with a required test. |

## Naming Grammar

All future tests should use this grammar:

```text
<productionMethod>Test()
<productionMethod>_with<Wiring>Test()
<productionMethod>_with<State>Test()
<productionMethod>_with<Condition>Test()
<productionMethod>_with<Wiring>And<Condition>Test()
<productionMethod>_with<ReferenceShape>And<Condition>Test()
```

Examples:

```text
setAOTest()
setAO_withDetailHubTest()
setAO_withLinkHubTest()
setAO_withLinkHubAndDetailHubTest()
setAO_withLinkHubAndNoMatchTest()
setReference_withOneToManyTest()
setReference_withExistingParentTest()
setReference_withOneToManyAndNullTest()
```

The method name must begin with the exact production method declared by the class under test. For example, a public Hub test can use `HubTest.setAO_withDetailHubTest()`, but a service test for the same behavior must use `HubAOServiceTest.setActiveObject_withDetailHubTest()` because `HubAOService` declares `setActiveObject(...)`, not `setAO(...)`.

## Canonical Vocabulary

### Wiring

| Canonical test token | OA source terminology and aliases | Definition | Primary or combination |
| --- | --- | --- | --- |
| `StandaloneHub` | plain Hub, no master, no shared Hub, no link Hub | A Hub with independent list data and AO state. | Primary |
| `MasterHub` | master Hub | A Hub used as the source of a master active object for one or more detail Hubs. | Primary |
| `DetailHub` | detail Hub | A Hub whose contents are derived from a master object and `OALinkInfo` path. | Primary |
| `MasterDetailHub` | master/detail pair | A `MasterHub` with a `DetailHub` created by `HubDetailService`. | Combination |
| `SharedHub` | shared Hub, shared data | A Hub whose backing data or active data is shared with another Hub through `HubShareService`. | Primary |
| `LinkHub` | linked Hub, linked-from/linked-to Hub | A pair of Hubs where one Hub AO or property controls the corresponding AO or property on another Hub. | Primary |
| `LinkFromHub` | hubFrom, linked-from Hub | The Hub whose AO is selected by a linked-to object/property. | Role |
| `LinkToHub` | hubTo, linked-to Hub | The Hub whose AO supplies the link property or position value. | Role |
| `LinkHubAndDetailHub` | linked Hub plus master/detail | Linked-Hub behavior where the link-from Hub is also constrained by a master/detail hierarchy. | Combination |
| `SharedHubAndDetailHub` | shared Hub plus detail Hub | Supported combination where shared active/data behavior has dependent detail-Hub effects. | Combination |

### State

| Canonical test token | Definition |
| --- | --- |
| `HubWithAO` | A Hub whose AO is non-null. |
| `HubWithoutAO` | A Hub whose AO is null. |
| `HubAOOutsideHub` | A non-null AO candidate that is not currently in the Hub data but may be resolvable through master adjustment. |
| `LoadedRelationshipHub` | A detail/reference Hub whose relationship has been materialized. |
| `UnloadedRelationshipHub` | A detail/reference Hub whose relationship is not loaded or whose client-side data is not materialized. |
| `ManyHubInitialized` | An OAObject MANY reference Hub already exists in the object's property storage. |
| `ManyHubNotInitialized` | An OAObject MANY reference Hub has not yet been created or loaded. |
| `LoadedReference` | A single-object reference is materialized. |
| `UnloadedReference` | A single-object reference is not materialized, may be represented by key state, or may require datasource lookup. |
| `ObjectKeyReference` | Reference identity is represented by `OAObjectKey` or foreign-key state. |

### Condition

| Canonical test token | Definition |
| --- | --- |
| `Null` | The action supplies or resolves null. |
| `SameActiveObject` | The requested AO is already active. |
| `ObjectNotInHub` | The requested object is not currently contained in the target Hub. |
| `NoMatch` | Linked property-to-property resolution finds no matching object. |
| `MultipleMatches` | Linked property-to-property resolution finds more than one matching object. |
| `ExistingParent` | A child/detail object already belongs to another parent/master. |
| `DuplicateAdd` | The object being added is already present or equivalent under Hub duplicate policy. |
| `RepeatedRemove` | The object being removed is already absent. |
| `ActiveObject` | The object being removed or mutated is the current AO. |
| `CurrentMasterDoesNotOwnDetail` | A detail AO candidate belongs to a different master from the current master AO. |
| `RecursiveLinkUpdate` | Link synchronization can re-enter itself through AO or property changes. |
| `DuplicateEventPrevention` | Shared/detail/link propagation could otherwise fire duplicate events. |
| `EventOrdering` | The test asserts the relative order of property, Hub, detail, and AO events. |
| `ListenerMutation` | A listener mutates Hub or object state during an event callback. |
| `ServerOnlySyncMessage` | Sync or remote-thread state affects event or message propagation. |

### Reference Shapes

| Canonical test token | OA metadata/source terminology | Definition | Fixture required |
| --- | --- | --- | --- |
| `NoReverseLink` | `OALinkInfo` without reverse link | A reference where setting one side has no model reverse to maintain. | Yes |
| `OneToOne` | link type ONE to reverse ONE | A single object reference with a single reverse reference. | Yes |
| `OneToMany` | one-link on detail, reverse many-Hub on master | Assigning the one-link should update the reverse many-Hub. | Yes |
| `ManyToOne` | many-Hub on master, reverse one-link on detail | Adding/removing many-Hub membership should update the reverse one-link. | Yes |
| `ReverseOneLink` | reverse link type ONE | Reverse side is scalar object property. | Yes |
| `ReverseManyLink` | reverse link type MANY | Reverse side is Hub membership. | Yes |
| `OwnedReference` | private/owned link metadata | Referenced objects are owned by the source object. | Yes |
| `CascadeSaveReference` | cascade save metadata | Save operations traverse the reference. | Yes |
| `CascadeDeleteReference` | cascade delete metadata | Delete operations affect referenced children according to metadata. | Yes |
| `RequiredReference` | required link metadata | Reference cannot be null for validation/save rules. | Yes |
| `OptionalReference` | optional link metadata | Reference can be null. | Yes |
| `CalculatedReference` | calculated link/property metadata | Reference is derived and should not be mutated like stored state. | Deferred |
| `TransientReference` | transient/non-persistent metadata | Reference is runtime-only for datasource persistence. | Deferred |
| `NullReference` | null plus OA primitive/object null state | Reference is explicitly null or absent. | Yes |
| `ExistingReference` | non-null current reference | Reference already points to another object. | Yes |
| `WeakReferenceStorage` | property stored as `WeakReference` to Hub/object | Internal property storage can weakly reference Hub values. | Service fixture |
| `RecursiveReference` | self-referencing relationship | Parent/child relationship uses the same type. | Yes |
| `SequencedManyReference` | sequence property on link | Many-Hub ordering includes sequence metadata. | Deferred |
| `SortedManyReference` | sort/order metadata | Many-Hub relationship has ordering metadata. | Deferred |

## Part 1: Core Hub Wiring Vocabulary

### StandaloneHub

**How it is created**: `new Hub<>(Type.class)` or equivalent constructors in `Hub` without master, shared, or link configuration.

**Participants**: One Hub and zero or more OAObjects.

**Propagation**: Membership and AO changes stay inside the Hub except for object-level reverse-reference changes made by add/remove services.

**Observable side effects**: list membership, AO position, Hub events, object weak-Hub membership.

**Event implications**: `HubAOService#setActiveObject(...)` fires AO change after detail/link updates; add/remove services fire before/after add/remove/new-list events.

**Fixture**: Required. `createStandaloneHubScenario()` should use deterministic `Hub<Register>` or `Hub<Store>` instances.

### MasterDetailHub

**How it is created**: public `Hub#getDetailHub(...)` overloads or `HubDetailService#getDetailHub(...)` / `setMasterHub(...)` paths that supply master object/link metadata.

**Participants**: A master Hub, its AO, a detail Hub, `HubDetail`, and `OALinkInfo` for the master-to-detail relationship.

**Propagation**: Master AO changes replace or realign detail Hub data; detail add/remove can update reverse one-links or many-links through `HubDetailService#setPropertyToMasterHub(...)`.

**Observable side effects**: detail Hub contents, detail AO, master/detail reverse references, new-list events, AO events.

**Fixture**: Required. Use a real OAPOS one-to-many relationship such as `Store` to `Register` when available.

### SharedHub

**How it is created**: public `Hub#setSharedHub(...)` or the `Hub(Hub<TYPE> masterHub)` convenience constructor, which delegates to `HubShareService#setSharedHub(...)`.

**Participants**: Source Hub, shared Hub, shared `HubData`, and optionally shared `HubDataActive`.

**Propagation**: Membership and active data can be shared. `HubAOService` updates shared Hubs that share active data.

**Observable side effects**: shared membership, AO synchronization, dependent detail updates, add-Hub auto-add behavior.

**Fixture**: Required.

### LinkHub

**How it is created**: public `Hub#setLinkHub(...)` overloads, which delegate to `HubLinkService#setLinkHub(...)`.

**Participants**: link-from Hub, link-to Hub, link-to property, optional link-from property, optional position-link flag.

**Propagation**: Link-to AO/property changes resolve the desired link-from object; link-from AO changes update the link-to property where configured.

**Observable side effects**: link-from AO, link-to object property value, master adjustment if the resolved object is not currently contained in the link-from Hub.

**Fixture**: Required. Provide separate fixtures for direct-object, position-link, and property-to-property modes.

### LinkHubAndDetailHub

**How it is created**: A link-from Hub that also has a master/detail constraint, then linking it to a link-to Hub.

**Participants**: link-from Hub, link-to Hub, master Hub, detail Hub, link properties, and master/detail metadata.

**Propagation**: Link resolution finds the desired object; `HubLinkService#updateLinkedFromHub(...)` can call `HubDataService#getPos(..., adjustMaster=true, updateLink=false)` to make the master/detail hierarchy compatible before setting AO.

**Observable side effects**: link-from master AO, link-from AO, detail contents, link-to property, and AO events.

**Fixture**: Required.

### SharedHubAndDetailHub

This is a supported combination when shared active/data behavior has dependent detail-Hub effects. It must not be treated as universally invalid.

**Supported roles**: `HubAOService#setActiveObject(...)` updates shared Hubs and their detail Hubs when active data is shared. `HubDetailService#updateDetail(...)` can temporarily assign a detail Hub to share the actual master object's relationship Hub.

**Invalid role**: A Hub that already has `sharedHub` set cannot itself be assigned a master Hub through `HubDetailService#setMasterHub(thisHub, masterHub, ...)` with non-null `masterHub`. The source guard is `HubDetailService#setMasterHub(...)`, which throws `RuntimeException("sharedHub cant have a master hub")` when `HubDataUnique.getSharedHub() != null` and the requested `masterHub != null`.

**Setup order rejected**: `thisHub.setSharedHub(sourceHub, ...)` followed by service/public detail setup that attempts to make the same `thisHub` a detail Hub with a non-null master.

**Fixture**: Required for supported shared-detail propagation; one INVALID setup test is useful in `HubDetailServiceTest.setMasterHub_withSharedHubTest()`.

## Derived Hub Types and Separate Matrices

These types produce or manage Hubs, but they are not primary dimensions for the core Hub wiring matrix. They should have dedicated matrices/tests owned by their production classes after core Hub behavior is stable.

| Derived type | Production owner | Public entry point | Mutable ordinary Hub actions? | Later dedicated matrix? | Notes |
| --- | --- | --- | --- | --- | --- |
| `FilteredHub` | `com.viaoa.hub.filter.FilteredHub`, `HubFilter`, `CustomHubFilter` | filter constructors/APIs, source Hub changes | Derived membership; direct mutation contract must be confirmed per class. | Yes | Do not classify as core wiring. AO behavior with excluded objects is DEFERRED. |
| `CombinedHub` | `com.viaoa.hub.view.HubCombined` | combined-view construction APIs | Likely view/derived behavior; do not assume full mutable Hub contract. | Yes | Separate from standalone/shared/link wiring. |
| `GroupedHub` | `com.viaoa.hub.view.HubGroupBy`, `OAGroupBy`, `HubGroupByMerger` | group-by construction APIs | Derived/aggregate membership. | Yes | Group rows should test grouping owner classes first. |
| `FlattenedHub` | `com.viaoa.hub.view.HubFlattened` | flattened-view construction APIs | Derived membership. | Yes | Test propagation from source nested Hubs separately. |
| `JoinedHub` | `com.viaoa.hub.view.HubLeftJoin`, `OALeftJoin` | join construction APIs | Derived/join membership. | Yes | Do not put in basic Hub action matrix until owner behavior is mapped. |
| `CalculatedHub` | calculated links, `HubMerger`, server-side calculated metadata | generated getters or metadata-driven reference retrieval | Usually derived from model/calculation rules. | Yes | Confirm with `OALinkInfo#getCalculated()` and generated model behavior before tests. |

## Part 2: Hub Action Matrix

The table below includes source-relevant core Hub rows only. It intentionally avoids a full Cartesian product.

| Public entry-point class and method | Owning service class and method | Wiring/state/condition | Classification | Primary test class and method | Supporting test class and method | Declarative invariant or question | Evidence level | Risk | Source evidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `Hub.setAO(...)` / `Hub.setActiveObject(...)` | `HubAOService.setActiveObject(...)` | `StandaloneHub` | REQUIRED | `HubTest.setAOTest()` | `HubAOServiceTest.setActiveObjectTest()` | Setting AO selects the requested object in the Hub. | EXPLICIT_SOURCE | High | `Hub` delegates to `HubAOService`; service writes `HubDataActive.activeObject` and fires AO event. |
| `Hub.setAO(...)` / `Hub.setActiveObject(...)` | `HubAOService.setActiveObject(...)` | `HubWithoutAO` + `Null` | REQUIRED | `HubTest.setAO_withNullTest()` | `HubAOServiceTest.setActiveObject_withNullTest()` | Setting AO to null clears the active object and active position. | EXPLICIT_SOURCE | Medium | Invalid/null object path resolves position and calls full AO setter. |
| `Hub.setAO(...)` / `Hub.setActiveObject(...)` | `HubAOService.setActiveObject(...)` | `SameActiveObject` | REQUIRED | `HubTest.setAO_withSameActiveObjectTest()` | `HubAOServiceTest.setActiveObject_withSameActiveObjectTest()` | Setting the same AO again is a no-op unless forced by the service path. | EXPLICIT_SOURCE | Medium | Service returns early when old AO equals new AO and `bForce` is false. |
| `Hub.setAO(...)` / `Hub.setActiveObject(...)` | `HubAOService.setActiveObject(...)`, `HubDataService.getPos(...)` | `HubAOOutsideHub` / `ObjectNotInHub` | REQUIRED | `HubTest.setAO_withObjectNotInHubTest()` | `HubAOServiceTest.setActiveObject_withObjectNotInHubTest()` | Setting AO to an object outside the Hub follows adjust-master behavior and does not corrupt membership. | EXPLICIT_SOURCE | Critical | `HubAOService` resolves position through `callHubDataGetPos(..., adjustMaster, bUpdateLink)` before final AO assignment. |
| `Hub.setAO(...)` / `Hub.setActiveObject(...)` | `HubAOService.setActiveObject(...)`, `HubDetailService.setMasterHubActiveObject(...)` | `DetailHub` | REQUIRED | `HubTest.setAO_withDetailHubTest()` | `HubAOServiceTest.setActiveObject_withDetailHubTest()` | Setting a detail AO preserves or repairs master/detail alignment. | STRONGLY_INFERRED | Critical | Detail reverse-one path can call `setMasterHubActiveObject(...)` to align master AO. |
| `Hub.setAO(...)` / `Hub.setActiveObject(...)` | `HubAOService.setActiveObject(...)`, `HubShareService.setSharedHub(...)` | `SharedHub` | REQUIRED | `HubTest.setAO_withSharedHubTest()` | `HubAOServiceTest.setActiveObject_withSharedHubTest()` | AO changes propagate through shared active data according to sharing configuration. | EXPLICIT_SOURCE | High | `HubAOService` loops shared Hubs with matching `HubDataActive`. |
| `Hub.setAO(...)` / `Hub.setActiveObject(...)` | `HubAOService.setActiveObject(...)`, `HubDetailService.updateDetail(...)` | `SharedHubAndDetailHub` | REQUIRED | `HubTest.setAO_withSharedHubAndDetailHubTest()` | `HubAOServiceTest.setActiveObject_withSharedHubAndDetailHubTest()` | Shared-Hub AO changes update dependent detail Hubs without duplicate detail updates. | STRONGLY_INFERRED | High | `updateDetailHubs(...)` handles shared Hubs separately from direct detail update. |
| `Hub.setAO(...)` / `Hub.setActiveObject(...)` | `HubAOService.setActiveObject(...)`, `HubLinkService.updateLinkProperty(...)` | `LinkHub` | REQUIRED | `HubTest.setAO_withLinkHubTest()` | `HubAOServiceTest.setActiveObject_withLinkHubTest()`, `HubLinkServiceTest.updateLinkPropertyTest()` | Setting link-from AO updates the configured link-to property. | EXPLICIT_SOURCE | Critical | Full AO setter calls `callHubLinkUpdateLinkProperty(...)` when `bUpdateLink` is true. |
| `Hub.setAO(...)` / `Hub.setActiveObject(...)` | `HubLinkService.updateLinkedFromHub(...)`, `HubAOService.setActiveObject(...)`, `HubDetailService.updateDetailActiveObject(...)` | `LinkHubAndDetailHub` | REQUIRED | `HubTest.setAO_withLinkHubAndDetailHubTest()` | `HubLinkServiceTest.updateLinkedFromHub_withDetailHubTest()`, `HubDetailServiceTest.updateDetailActiveObject_withLinkedHubTest()` | A linked AO change realigns the link-from master/detail hierarchy before final public AO result. | EXPLICIT_SOURCE | Critical | `updateLinkedFromHub(...)` resolves object, adjusts master if not contained, then sets AO. Do not duplicate the full end-to-end assertions in each service test. |
| `Hub.setPos(...)` | `HubAOService.setActiveObject(Hub,int,...)` | `StandaloneHub` | REQUIRED | `HubTest.setPosTest()` | `HubAOServiceTest.setActiveObject_withPositionTest()` | Setting position selects the object at that index. | EXPLICIT_SOURCE | High | Public position APIs delegate to the same AO service path. |
| `Hub.setPos(...)` | `HubAOService.setActiveObject(Hub,int,...)`, `HubLinkService.updateLinkProperty(...)` | `LinkHub` | REQUIRED | `HubTest.setPos_withLinkHubTest()` | `HubAOServiceTest.setActiveObject_withLinkHubAndPositionTest()` | Setting position in a linked Hub follows the same link-update contract as setting AO. | STRONGLY_INFERRED | High | position-based AO setter delegates into the full AO path. |
| `Hub.add(...)` | `HubAddRemoveService.add(...)` | `StandaloneHub` | REQUIRED | `HubTest.addTest()` | `HubAddRemoveServiceTest.addTest()` | Adding an object inserts it into the Hub and tracks object membership. | EXPLICIT_SOURCE | High | Add service owns membership and event firing. |
| `Hub.add(...)` | `HubAddRemoveService.add(...)`, `HubDetailService.setPropertyToMasterHub(...)` | `DetailHub` | REQUIRED | `HubTest.add_withDetailHubTest()` | `HubAddRemoveServiceTest.add_withDetailHubTest()`, `HubDetailServiceTest.setPropertyToMasterHubTest()` | Adding to a detail Hub maintains the reverse reference to the current master. | EXPLICIT_SOURCE | Critical | Detail add/remove paths use `setPropertyToMasterHub(...)` to maintain reverse links. |
| `Hub.add(...)` | `HubAddRemoveService.add(...)`, `HubShareService.setSharedHub(...)` | `SharedHub` | REQUIRED | `HubTest.add_withSharedHubTest()` | `HubAddRemoveServiceTest.add_withSharedHubTest()` | Adding through a shared Hub mutates shared backing membership consistently. | STRONGLY_INFERRED | High | Shared Hub data is configured through `HubShareService`; remove path explicitly redirects shared Hub operations. |
| `Hub.add(...)` | `HubAddRemoveService.add(...)` | `DuplicateAdd` | REQUIRED | `HubTest.add_withDuplicateAddTest()` | `HubAddRemoveServiceTest.add_withDuplicateAddTest()` | Duplicate adds preserve the configured Hub duplicate policy. | STRONGLY_INFERRED | Medium | Add service checks Hub data and object identity before mutation. |
| `Hub.insert(...)` | `HubAddRemoveService.insert(...)` | `StandaloneHub` | REQUIRED | `HubTest.insertTest()` | `HubAddRemoveServiceTest.insertTest()` | Inserting places the object at the requested index without corrupting AO. | EXPLICIT_SOURCE | Medium | Insert is a distinct service path from append add. |
| `Hub.insert(...)` | `HubAddRemoveService.insert(...)`, `HubDetailService.setPropertyToMasterHub(...)` | `DetailHub` | REQUIRED | `HubTest.insert_withDetailHubTest()` | `HubAddRemoveServiceTest.insert_withDetailHubTest()` | Inserting into a detail Hub maintains reverse master ownership. | STRONGLY_INFERRED | High | Detail reverse maintenance is common to add/insert paths. |
| `Hub.remove(...)` | `HubAddRemoveService.remove(...)` | `StandaloneHub` | REQUIRED | `HubTest.removeTest()` | `HubAddRemoveServiceTest.removeTest()` | Removing an object deletes only Hub membership and leaves unrelated object state intact. | EXPLICIT_SOURCE | High | remove path checks callbacks, updates data, weak-Hub references, and events. |
| `Hub.remove(...)` | `HubAddRemoveService.remove(...)`, `HubDetailService.setPropertyToMasterHub(...)` | `DetailHub` | REQUIRED | `HubTest.remove_withDetailHubTest()` | `HubAddRemoveServiceTest.remove_withDetailHubTest()`, `HubDetailServiceTest.setPropertyToMasterHub_withNullTest()` | Removing from a detail Hub clears the reverse reference only when it still points to the current master. | EXPLICIT_SOURCE | Critical | `setPropertyToMasterHub(...)` clears one-link only when current master matches. |
| `Hub.remove(...)` | `HubAddRemoveService.remove(...)`, `HubAOService.setActiveObject(...)` | `ActiveObject` | REQUIRED | `HubTest.remove_withActiveObjectTest()` | `HubAddRemoveServiceTest.remove_withActiveObjectTest()` | Removing the active object advances or clears AO according to Hub active-object rules. | STRONGLY_INFERRED | High | remove path updates shared/AO state after local data removal. |
| `Hub.remove(...)` | `HubAddRemoveService.remove(...)` | `RepeatedRemove` | REQUIRED | `HubTest.remove_withRepeatedRemoveTest()` | `HubAddRemoveServiceTest.remove_withRepeatedRemoveTest()` | Removing an object that is not present is a no-op with the documented return behavior. | EXPLICIT_SOURCE | Medium | remove returns null/no-op when object is not contained unless removeAll mode is active. |
| `Hub.removeAt(...)` | `HubAddRemoveService.remove(Hub,int,...)` | `StandaloneHub` | REQUIRED | `HubTest.removeAtTest()` | `HubAddRemoveServiceTest.remove_withPositionTest()` | Removing by index removes the object currently at that index. | EXPLICIT_SOURCE | Medium | service resolves object by index and delegates to object remove. |
| `Hub.clear()` | `HubAddRemoveService.clear(...)` | `StandaloneHub` | REQUIRED | `HubTest.clearTest()` | `HubAddRemoveServiceTest.clearTest()` | Clearing removes all membership and clears AO when configured by the service path. | EXPLICIT_SOURCE | High | `clear(...)` optionally sets AO null and fires removeAll/new-list events. |
| `Hub.removeAll()` | `HubAddRemoveService.clear(...)` through `Hub.clear()` | `StandaloneHub` | REQUIRED | `HubTest.removeAllTest()` | `HubAddRemoveServiceTest.clearTest()` | `Hub.removeAll()` follows the same public behavior as `Hub.clear()` while preserving its own public method coverage. | EXPLICIT_SOURCE | Medium | `Hub.removeAll()` directly calls `this.clear()`. |
| `Hub.clear()` | `HubAddRemoveService.clear(...)`, `HubDetailService.setPropertyToMasterHub(...)` | `DetailHub` | REQUIRED | `HubTest.clear_withDetailHubTest()` | `HubAddRemoveServiceTest.clear_withDetailHubTest()` | Clearing a detail Hub clears reverse references for removed details. | EXPLICIT_SOURCE | Critical | `_clear(...)` iterates removed objects and calls reverse cleanup. |
| `Hub.move(...)` | Hub list/order service path | `StandaloneHub` | REQUIRED | `HubTest.moveTest()` | service-specific move/order test if exposed | Moving an object changes order without changing membership or object references. | STRONGLY_INFERRED | Medium | Hub exposes move semantics distinct from remove/add. |
| `Hub.sort(...)` | `HubSortService.sort(...)` | `StandaloneHub` | REQUIRED | `HubTest.sortTest()` | `HubSortServiceTest.sortTest()` | Sorting reorders membership and preserves AO identity. | STRONGLY_INFERRED | Medium | Hub delegates sort through Hub sort support. |
| `Hub.sort(...)` | `HubSortService.sort(...)` | `DetailHub` | REQUIRED | `HubTest.sort_withDetailHubTest()` | `HubSortServiceTest.sort_withDetailHubTest()` | Sorting a detail Hub does not alter master/detail references. | STRONGLY_INFERRED | Medium | Sorting should not call reverse-reference mutation paths. |
| `Hub.loadAllData()` / select-triggering public traversal | `HubSelectService` | `StandaloneHub` | REQUIRED | `HubTest.loadAllDataTest()` | `HubSelectServiceTest.loadAllDataTest()` | Loading materializes Hub membership without changing unrelated runtime state. | EXPLICIT_SOURCE | High | Hub array/list APIs call `loadAllData()` before traversal. |
| `Hub.loadAllData()` / select-triggering public traversal | `HubSelectService`, detail/object reference services | `UnloadedRelationshipHub` | REQUIRED | `HubTest.loadAllData_withUnloadedRelationshipHubTest()` | `HubSelectServiceTest.loadAllData_withUnloadedRelationshipHubTest()` | Loading a relationship Hub materializes the reference and records loaded state. | STRONGLY_INFERRED | High | Detail and object services distinguish loaded/unloaded relationship state. |
| `Hub.setSharedHub(...)` | `HubShareService.setSharedHub(...)` | `SharedHub` | REQUIRED | `HubTest.setSharedHubTest()` | `HubShareServiceTest.setSharedHubTest()` | A shared Hub uses the source Hub data without duplicating membership. | EXPLICIT_SOURCE | High | Public `Hub#setSharedHub(...)` delegates to `HubShareService#setSharedHub(...)`. |
| public/service detail setup that calls `HubDetailService.setMasterHub(...)` | `HubDetailService.setMasterHub(...)` | `SharedHub` + invalid setup order | INVALID | `HubTest.setSharedHub_withRejectedMasterHubSetupTest()` if public path exists | `HubDetailServiceTest.setMasterHub_withSharedHubTest()` | A Hub that already has `sharedHub` set cannot become a detail Hub with a non-null master Hub through `setMasterHub(...)`. | EXPLICIT_SOURCE | Medium | `HubDetailService#setMasterHub(...)` throws when `HubDataUnique.getSharedHub() != null` and `masterHub != null`. This does not make all shared/detail combinations invalid. |
| `Hub.setLinkHub(...)` | `HubLinkService.setLinkHub(...)` | `LinkHub` | REQUIRED | `HubTest.setLinkHubTest()` | `HubLinkServiceTest.setLinkHubTest()` | Creating a link Hub installs bidirectional AO/property synchronization. | EXPLICIT_SOURCE | Critical | public link-Hub APIs delegate to `HubLinkService#setLinkHub(...)`. |
| `Hub.setLinkHub(...)` | `HubLinkService.setLinkHub(...)`, `HubLinkService.updateLinkedFromHub(...)` | `LinkHubAndDetailHub` | REQUIRED | `HubTest.setLinkHub_withDetailHubTest()` | `HubLinkServiceTest.setLinkHub_withDetailHubTest()` | A link-from detail Hub can resolve linked objects by adjusting master/detail state when necessary. | EXPLICIT_SOURCE | Critical | `updateLinkedFromHub(...)` uses state-based contains check and master adjustment. |
| `Hub.getDetailHub(...)` | `HubDetailService.getDetailHub(...)` | `MasterDetailHub` | REQUIRED | `HubTest.getDetailHubTest()` | `HubDetailServiceTest.getDetailHubTest()` | Creating a detail Hub binds it to the master Hub and current master AO. | EXPLICIT_SOURCE | High | `getDetailHub(...)`, `setMasterHub(...)`, and `updateDetail(...)` establish master/detail data. |
| `Hub.setAO(...)` on master Hub | `HubAOService.setActiveObject(...)`, `HubDetailService.updateAllDetail(...)` | `MasterHub` + `MasterDetailHub` | REQUIRED | `HubTest.setAO_withMasterHubTest()` | `HubAOServiceTest.setActiveObject_withMasterHubTest()` | Changing master AO replaces detail Hub contents with the new master's relationship contents. | EXPLICIT_SOURCE | Critical | AO setter calls `callHubDetailUpdateAllDetail(...)` before AO event. |
| link-to property-change callback path | `HubLinkService.updateLinkedFromHub(...)` | `LinkHub` + direct, position, property modes | REQUIRED | public entry-point test through `HubTest.setLinkHubTest()` or object property test | `HubLinkServiceTest.updateLinkedFromHubTest()` | Changing the link-to property selects the corresponding link-from object for direct, position, and property-to-property modes. | EXPLICIT_SOURCE | Critical | `getLinkFromHubObjectForLinkToHubObject(...)` handles all three modes. |
| link-to property-change callback path | `HubLinkService.updateLinkedFromHub(...)` | `LinkHub` + `NoMatch` | REQUIRED | public entry-point test through `HubTest.setLinkHub_withNoMatchTest()` | `HubLinkServiceTest.updateLinkedFromHub_withNoMatchTest()` | A property-to-property link with no match clears the link-from AO. | EXPLICIT_SOURCE | High | property-to-property branch returns null and `updateLinkedFromHub(...)` clears AO. |
| link-to property-change callback path | `HubLinkService.updateLinkedFromHub(...)` | `LinkHub` + `MultipleMatches` | DEFERRED | public entry-point test deferred | `HubLinkServiceTest.updateLinkedFromHub_withMultipleMatchesTest()` deferred | Current implementation returns the first matching object in Hub iteration order. Intended duplicate-match behavior requires confirmation before this becomes a permanent invariant. | CURRENT_IMPLEMENTATION | Medium | helper loops `for (Object objx : hubFrom)` and returns first `OACompare.isEqual(...)`; no explicit contract was found. |
| public object reference change on object contained by Hub | `OAObjectReferenceService` plus `HubDetailService` | `DetailHub` + `ExistingParent` | REQUIRED | `OAObjectTest.setProperty_withExistingParentTest()` | `HubDetailServiceTest.updateDetailActiveObject_withExistingParentTest()` | Changing a detail object's master reference moves it between the affected master detail Hubs. | STRONGLY_INFERRED | Critical | reverse maintenance is split between object reference services and Hub detail services. |
| listener callback mutation during public Hub event | `HubEventService` and action owner service | `ListenerMutation` | DEFERRED | action-specific public test deferred | service-specific event-depth test deferred | Define supported listener reentrancy and event-depth behavior before locking assertions. | AMBIGUOUS | High | Hub services use locks and event helpers; allowed mutation contract needs confirmation. |
| recursive AO/property link update | `HubLinkService.updateLinkedFromHub(...)`, `HubAOService.setActiveObject(...)` | `LinkHub` + `RecursiveLinkUpdate` | REQUIRED | `HubTest.setAO_withLinkHubAndRecursiveLinkUpdateTest()` | `HubLinkServiceTest.updateLinkedFromHub_withRecursiveLinkUpdateTest()` | Recursive link updates terminate without duplicate AO/property loops. | EXPLICIT_SOURCE | Critical | `updateLinkedFromHub(...)` includes recursive/self-reference guard paths. |

## Part 3: Hub Conditions and Edge Scenarios

| Canonical condition | Applies to actions | Applies to wiring/state/reference shape | Canonical method suffix | Classification | Evidence level | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| `Null` | `setAO`, `setPos`, `setLinkHub`, `updateLinkedFromHub`, `remove`, reference setters | most Hub and reference types | `_withNullTest` | REQUIRED | EXPLICIT_SOURCE | AO null handling is explicit in `HubAOService`; link resolution returns null when link-to object/property is null. |
| `SameActiveObject` | `setAO`, `setPos` | `HubWithAO` | `_withSameActiveObjectTest` | REQUIRED | EXPLICIT_SOURCE | `HubAOService` returns early when old AO equals new AO and not forced. |
| `ObjectNotInHub` | `setAO`, link resolution | standalone/detail/link | `_withObjectNotInHubTest` | REQUIRED | EXPLICIT_SOURCE | `HubDataService#getPos(..., adjustMaster, updateLink)` can adjust master or reject. |
| `NoMatch` | `updateLinkedFromHub` | property-to-property `LinkHub` | `_withNoMatchTest` | REQUIRED | EXPLICIT_SOURCE | Property-to-property no match clears link-from AO. |
| `MultipleMatches` | `updateLinkedFromHub` | property-to-property `LinkHub` | `_withMultipleMatchesTest` | DEFERRED | CURRENT_IMPLEMENTATION | Current code returns first match; intended duplicate-match behavior needs Vince's decision. |
| `ExistingParent` | reference set/clear, detail add/remove | `OneToMany`, `ManyToOne`, `DetailHub` | `_withExistingParentTest` | REQUIRED | STRONGLY_INFERRED | Tests should verify prior reverse membership cleanup. |
| `DuplicateAdd` | `add`, `insert` | standalone/detail/shared | `_withDuplicateAddTest` | REQUIRED | STRONGLY_INFERRED | Duplicate policy affects membership and events. |
| `RepeatedRemove` | `remove`, `removeAt` | standalone/detail/shared | `_withRepeatedRemoveTest` | REQUIRED | EXPLICIT_SOURCE | remove returns no-op/null when object is not in Hub. |
| `ActiveObject` | `remove`, `removeAt`, `clear` | `HubWithAO` | `_withActiveObjectTest` | REQUIRED | STRONGLY_INFERRED | remove path updates AO and shared AO state. |
| `CurrentMasterDoesNotOwnDetail` | `setAO`, `setPos` | `DetailHub` | `_withCurrentMasterDoesNotOwnDetailTest` | REQUIRED | STRONGLY_INFERRED | Detail AO can require master adjustment. |
| `RecursiveLinkUpdate` | `setAO`, `updateLinkedFromHub`, `updateLinkProperty` | recursive/self `LinkHub` | `_withRecursiveLinkUpdateTest` | REQUIRED | EXPLICIT_SOURCE | Link service contains recursion guards. |
| `DuplicateEventPrevention` | add/remove/AO/detail update | shared/detail/link combinations | `_withDuplicateEventPreventionTest` | REQUIRED | STRONGLY_INFERRED | Shared and recursive paths can produce duplicate events if guards fail. |
| `EventOrdering` | add/remove/AO/detail/link | all primary wiring | `_withEventOrderingTest` | REQUIRED | STRONGLY_INFERRED | Services intentionally update dependent state before/after specific events. |
| `ListenerMutation` | add/remove/AO/new-list actions | event-producing Hubs | `_withListenerMutationTest` | DEFERRED | AMBIGUOUS | Need explicit event-depth contract before implementation. |
| `ServerOnlySyncMessage` | add/remove/clear/property change | sync-backed Hubs/objects | `_withServerOnlySyncMessageTest` | DEFERRED | AMBIGUOUS | Sync behavior should be isolated after local Hub invariants are stable. |

## Part 4: OAObject Reference Action Matrix

The public methods listed here must be verified against the exact methods declared by `OAObject` or generated model methods that delegate into `OAObject`. Service tests use the service method names, not public convenience names.

| Public entry-point class and method | Owning service class and method | Reference shape/state/condition | Classification | Primary test class and method | Supporting test class and method | Declarative invariant or question | Evidence level | Risk | Source evidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `OAObject.setProperty(...)` or generated setter | `OAObjectPropertyService.setProperty(...)` | scalar property | REQUIRED | `OAObjectTest.setPropertyTest()` | `OAObjectPropertyServiceTest.setPropertyTest()` | Setting a scalar property updates the stored property and fires the property-change contract. | EXPLICIT_SOURCE | High | property service stores values in OAObject property storage. |
| `OAObject.setProperty(...)` or generated one-link setter | object reference service plus `OAObjectPropertyService.setProperty(...)` | `NullReference` + `Null` | REQUIRED | `OAObjectTest.setProperty_withNullReferenceTest()` | `OAObjectPropertyServiceTest.setProperty_withNullReferenceTest()` | Setting a reference property to null clears the object reference and null state consistently. | STRONGLY_INFERRED | High | property service stores/removes property; reference services maintain reverse side. |
| generated one-link setter / `OAObject.setProperty(...)` | `OAObjectReferenceService` and `HubDetailService.setPropertyToMasterHub(...)` | `OneToMany` | REQUIRED | `OAObjectTest.setProperty_withOneToManyTest()` | `OAObjectReferenceServiceTest.setReference_withOneToManyTest()` | Assigning the one-link adds the object to the reverse many-Hub. | STRONGLY_INFERRED | Critical | Hub detail mutation uses `setPropertyToMasterHub(...)`; object reference mutation must preserve the same reverse-link invariant. |
| generated one-link setter / `OAObject.setProperty(...)` | `OAObjectReferenceService` | `ExistingParent` | REQUIRED | `OAObjectTest.setProperty_withExistingParentTest()` | `OAObjectReferenceServiceTest.setReference_withExistingParentTest()` | Reassigning a one-link removes the object from the prior reverse many-Hub and adds it to the new one. | STRONGLY_INFERRED | Critical | stale reverse membership would corrupt ownership. |
| `OAObject.getProperty(...)` | `OAObjectPropertyService.getProperty(...)` | scalar property | REQUIRED | `OAObjectTest.getPropertyTest()` | `OAObjectPropertyServiceTest.getPropertyTest()` | Reading a stored property returns the current OAObject property value. | EXPLICIT_SOURCE | Medium | `getProperty(...)` reads from `OAObjectProperty[]`. |
| `OAObject.getProperty(...)` / reference key accessor | `OAObjectPropertyService.getProperty(...)`, key/reference services | `ObjectKeyReference` | REQUIRED | `OAObjectTest.getProperty_withObjectKeyReferenceTest()` | `OAObjectPropertyServiceTest.getProperty_withObjectKeyReferenceTest()` | A reference represented by an object key is distinguishable from an explicit null reference. | EXPLICIT_SOURCE | High | `setPropertyCAS(...)` explicitly compares `OAObjectKey` and object keys. |
| generated one-link setter / `OAObject.setProperty(...)` | `OAObjectReferenceService` | `OneToOne` | REQUIRED | `OAObjectTest.setProperty_withOneToOneTest()` | `OAObjectReferenceServiceTest.setReference_withOneToOneTest()` | Assigning a one-to-one reference updates both scalar sides exactly once. | STRONGLY_INFERRED | Critical | reverse one-link maintenance is service-owned. |
| generated one-link getter / protected `OAObject.getObject(...)` | `OAObjectReflectService.getReferenceObject(...)` | `LoadedReference` | REQUIRED | `OAObjectTest.getObject_withLoadedReferenceTest()` | `OAObjectReflectServiceTest.getReferenceObject_withLoadedReferenceTest()` | Reading a loaded reference returns the current referenced object without reloading. | EXPLICIT_SOURCE | Medium | `OAObject#getObject(...)` delegates to `OAObjectReflectService#getReferenceObject(...)`. |
| generated one-link getter / protected `OAObject.getObject(...)` | `OAObjectReflectService.getReferenceObject(...)` | `UnloadedReference` | REQUIRED | `OAObjectTest.getObject_withUnloadedReferenceTest()` | `OAObjectReflectServiceTest.getReferenceObject_withUnloadedReferenceTest()` | Reading an unloaded reference follows the configured load/key-resolution contract. | STRONGLY_INFERRED | High | reflect service checks loaded state and may invoke datasource/reference lookup. |
| `OAObject.setHub(...)` | `OAObjectPropertyService.setProperty(...)`, `HubDetailService` | `ReverseOneLink` | REQUIRED | `OAObjectTest.setHub_withReverseOneLinkTest()` | `OAObjectPropertyServiceTest.setProperty_withHubTest()` | Assigning a many-Hub allows additions to maintain the reverse one-link. | EXPLICIT_SOURCE | Critical | property service calls `callHubSetMasterObject(hub, oaObj, name)` for Hub values without a master. |
| protected/generated many-link getter | `OAObjectReflectService.getReferenceHub(...)` | `ManyHubNotInitialized` | REQUIRED | `OAObjectTest.getHub_withManyHubNotInitializedTest()` | `OAObjectReflectServiceTest.getReferenceHub_withManyHubNotInitializedTest()` | Reading a many reference creates or loads a Hub with correct master metadata. | EXPLICIT_SOURCE | High | `getReferenceHub(...)` creates/loads/caches Hub and sets autoMatch/sequence/sort as metadata requires. |
| protected/generated many-link getter | `OAObjectReflectService.getReferenceHub(...)` | `ManyHubInitialized` | REQUIRED | `OAObjectTest.getHub_withManyHubInitializedTest()` | `OAObjectReflectServiceTest.getReferenceHub_withManyHubInitializedTest()` | Reading an initialized many-Hub returns usable Hub state without rebuilding unrelated state. | EXPLICIT_SOURCE | High | reflect service detects existing Hub and applies sort/sequence/autoMatch setup as needed. |
| generated many-Hub add path or public Hub add | `OAObjectHubService.addHub(...)`, `HubAddRemoveService.add(...)` | `ReverseOneLink` | REQUIRED | `OAObjectTest.addHubTest()` if public/protected method is declared and reachable through model | `OAObjectHubServiceTest.addHubTest()` | Adding an object to a named many reference maintains the reverse one-link. | EXPLICIT_SOURCE | High | `OAObjectHubService#addHub(...)` tracks Hub membership for OAObjects. |
| generated many-Hub remove path or public Hub remove | `OAObjectHubService.removeHub(...)`, `HubAddRemoveService.remove(...)` | `ReverseOneLink` | REQUIRED | `OAObjectTest.removeHubTest()` if public/protected method is declared and reachable through model | `OAObjectHubServiceTest.removeHubTest()` | Removing from a named many reference clears the reverse one-link only for the current parent. | EXPLICIT_SOURCE | High | Hub remove/detail paths use guarded reverse cleanup. |
| `OAObject.setNull(...)` | `OAObjectPropertyService` / state service | scalar and reference | REQUIRED | `OAObjectTest.setNullTest()` | `OAObjectPropertyServiceTest.setNullTest()` if service method exists | Setting OA null state makes null-state accessors agree without changing unrelated properties. | STRONGLY_INFERRED | High | OA supports primitive/object null state independent of Java default value. |
| `OAObject.isNull(...)` | object property/state services | primitive/null state | REQUIRED | `OAObjectTest.isNullTest()` | service-specific null-state test | Null-state queries distinguish default primitive values from explicit non-null values. | STRONGLY_INFERRED | High | name/value int properties use OA null state in generated model patterns. |
| `OAObject.isReferenceNull(...)` / `isReferenceObjectNull(...)` | object reference/reflect services | `LoadedReference`, `UnloadedReference`, `ObjectKeyReference` | REQUIRED | `OAObjectTest.isReferenceNullTest()` | reference/reflect service tests | Reference-null checks distinguish unloaded, key-only, explicit null, and loaded object states. | STRONGLY_INFERRED | High | OA exposes separate reference-null methods for this distinction. |
| `OAObject.isLoaded(...)` / `isPropertyLoaded(...)` | object property/reflect services | `LoadedReference` | REQUIRED | `OAObjectTest.isLoaded_withLoadedReferenceTest()` | service-specific loaded-state test | Loaded-state accessors report materialized relationship state. | STRONGLY_INFERRED | Medium | loaded state drives client/detail behavior. |
| `OAObject.isLoaded(...)` / `isPropertyLoaded(...)` | object property/reflect services | `UnloadedReference` | REQUIRED | `OAObjectTest.isLoaded_withUnloadedReferenceTest()` | service-specific loaded-state test | Loaded-state accessors report an unloaded relationship without forcing mutation. | STRONGLY_INFERRED | High | Hub/detail services branch on loaded relationship state. |
| `OAObject.getReferenceObjectKey(...)` | object key/reference services | `ObjectKeyReference` | REQUIRED | `OAObjectTest.getReferenceObjectKeyTest()` | key/reference service test | Object-key references expose stable identity without requiring a loaded object. | EXPLICIT_SOURCE | High | `OAObject#getReferenceObjectKey(String)` is declared directly and reference services use object-key state. |
| foreign-key helper methods if declared on object/model | object property/reference services | foreign-key reference | REQUIRED | method-specific `OAObjectTest.<method>Test()` | service-specific fkey tests | Foreign-key property assignment preserves reference identity semantics. | STRONGLY_INFERRED | High | source includes fkey helpers for reference identity; exact public method names must be taken from `OAObject.java`. |
| `OAObject.copy(...)` / `copyInto(...)` / `createCopy(...)` | object copy service | references | REQUIRED | `OAObjectTest.copy_withReferencesTest()` | copy service test if present | Copying preserves scalar values while following the configured reference-copy contract. | STRONGLY_INFERRED | Medium | copy services are public OAObject behavior and must not corrupt references. |
| `OAObject.save(...)` | `OAObjectSaveService` | `CascadeSaveReference` | REQUIRED | `OAObjectTest.save_withCascadeSaveReferenceTest()` | `OAObjectSaveServiceTest.save_withCascadeSaveReferenceTest()` | Saving an object traverses cascade-save references according to metadata. | STRONGLY_INFERRED | High | save service and metadata own cascade-save behavior. |
| `OAObject.delete(...)` | `OAObjectDeleteService` | `OwnedReference`, `CascadeDeleteReference` | REQUIRED | `OAObjectTest.delete_withOwnedReferenceTest()` | `OAObjectDeleteServiceTest.delete_withOwnedReferenceTest()` | Deleting an owner applies owned/cascade-delete behavior and cleans reverse references. | STRONGLY_INFERRED | Critical | delete service interacts with ownership and Hub reverse cleanup. |
| `OAObject.delete(...)` | `OAObjectDeleteService` | `OptionalReference` | REQUIRED | `OAObjectTest.delete_withOptionalReferenceTest()` | `OAObjectDeleteServiceTest.delete_withOptionalReferenceTest()` | Deleting an object with optional references does not delete unrelated non-owned objects. | STRONGLY_INFERRED | High | ownership metadata must separate delete cascade from association cleanup. |
| `OAObject.refresh(...)` if declared | refresh/datasource services | `LoadedReference` | DEFERRED | `OAObjectTest.refresh_withLoadedReferenceTest()` | refresh/datasource service test | Define whether refresh preserves, reloads, or clears loaded references. | AMBIGUOUS | Medium | needs datasource fixture contract. |
| reference loading public/model path | object load/reference services | `UnloadedReference` | REQUIRED | public method-specific load reference test | service-specific load reference test | Loading references materializes configured relationships and updates loaded state. | STRONGLY_INFERRED | High | load/reference services are central to lazy relationship behavior. |
| serialization write/read | serialize services | `ObjectKeyReference`, `LoadedReference` | REQUIRED | serialization-focused public test | `OAObjectSerializeServiceTest.serialization_withReferencesTest()` | Serialization preserves object identity, keys, scalar state, and relationship representation. | STRONGLY_INFERRED | Medium | OAObject supports serialization across runtime boundaries. |
| sync apply | sync services | references and Hubs | DEFERRED | sync public entry test deferred | sync service test deferred | Define local observable state after remote reference or Hub mutation is applied. | AMBIGUOUS | High | sync paths can suppress or replay local events. |
| datasource load | datasource/cache services | `ObjectKeyReference`, `UnloadedReference` | REQUIRED | datasource public entry test | datasource/cache service tests | Datasource load reconstructs identity and reference-key state without duplicate objects. | STRONGLY_INFERRED | High | cache and datasource services define canonical identity. |
| service-only `OAObjectPropertyService.setPropertyCAS(...)` | `OAObjectPropertyService.setPropertyCAS(...)` | reference key | REQUIRED | N/A public OAObject test unless exposed by public method | `OAObjectPropertyServiceTest.setPropertyCAS_withReferenceKeyTest()` | Compare-and-swap treats equivalent object keys as matching current reference identity. | EXPLICIT_SOURCE | Medium | service explicitly compares `OAObjectKey` and OAObject keys. |
| service-only weak-reference storage | `OAObjectPropertyService.setPropertyWeakRef(...)` | `WeakReferenceStorage` | REQUIRED | N/A public OAObject test unless exposed by public method | `OAObjectPropertyServiceTest.setPropertyWeakRefTest()` | Weak-reference conversion preserves readable property value while allowing weak storage. | EXPLICIT_SOURCE | Medium | service converts property storage between strong and weak references. |

## Part 5: Deterministic Fixtures

Fixtures should use real generated `com.test.pos` model objects wherever possible. Values must be deterministic and each fixture should be isolated, closeable, and able to clean OA runtime/cache/thread-local state.

| Fixture | Model classes | Object counts | Initial AO | Initial references | Loaded state | Baseline invariants | Cleanup |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `createStandaloneHubScenario()` | `Register` or `Store` | 1 Hub, 3 objects | first object | none required | loaded | order, contains, AO index | clear Hub, close OA runtime |
| `createMasterDetailHubScenario()` | `Store`, `Register` | 2 stores, 3 registers | store1 | store1 has 2 registers, store2 has 1 | loaded | reverse one-link and many-Hub agree | remove refs, clear Hubs, close runtime |
| `createDetailHubNoMasterScenario()` | `Store`, `Register` | 1 master Hub, empty detail Hub | null | stores/registers exist but no AO | loaded | detail Hub empty when master AO null | clear Hubs |
| `createLinkHubDirectScenario()` | direct-reference POS relationship or focused test model | 1 from Hub, 1 to Hub, 3 from objects, 2 to objects | to AO set | to object has direct OAObject reference | loaded | from AO equals to object's link property | clear link metadata/Hubs |
| `createLinkHubPositionScenario()` | name/value object and value Hub | 1 from Hub, 1 to Hub | to AO set | to int property indexes from Hub | loaded | from AO equals `fromHub.getAt(intValue)` | clear Hubs |
| `createLinkHubPropertyScenario()` | two classes with compatible scalar properties | 1 from Hub, 1 to Hub | to AO set | property values match | loaded | no-match and duplicate-match variants explicit | clear Hubs |
| `createLinkedMasterDetailHubScenario()` | `Store`, `Register`, plus link-to object | master Hub, detail/link-from Hub, link-to Hub | master AO initially mismatched | link target references detail owned by different master | loaded | link update adjusts master before AO set | clear links/Hubs |
| `createSharedHubScenario()` | `Register` | source Hub, shared Hub, 3 objects | source AO first | none | loaded | shared membership and AO semantics match source contract | clear shared configuration and Hubs |
| `createSharedHubAndDetailHubScenario()` | `Store`, `Register` | source/shared master Hub plus detail Hub | source AO first | shared active/data affects detail | loaded | supported shared/detail propagation | clear shared/detail links |
| `createRejectedSharedDetailSetupScenario()` | `Store`, `Register` | shared Hub and master Hub | N/A | shared Hub already configured before master setup | loaded | `setMasterHub` rejects the setup | clear Hubs |
| `createOneToManyReferenceScenario()` | `Store`, `Register` | 1 store, 2 registers | N/A | registers point to store | loaded | store/register reverse links agree | clear refs/Hubs |
| `createExistingParentReferenceScenario()` | `Store`, `Register` | 2 stores, 1 register | N/A | register initially belongs to store1 | loaded | reassignment moves reverse membership | clear refs/Hubs |
| `createUnloadedReferenceScenario()` | classes with datasource-backed or key-only refs | at least 2 objects | N/A | reference represented by key/unloaded state | unloaded | null/reference/key/loaded states are distinct | clear cache/datasource |
| `createOwnedDeleteScenario()` | owner and owned child from POS model | 1 owner, 2 children | N/A | owned/private relationship | loaded | delete behavior follows ownership metadata | close runtime/cache cleanup |
| `createRecursiveReferenceScenario()` | recursive POS class if available | parent, child, grandchild | parent | child parent references | loaded | recursive traversal and reverse membership agree | clear recursive refs |
| `createEventRecorderScenario()` | any mutable Hub/object pair | fixture plus listener recorder | configurable | configurable | loaded | event order and counts are captured without mutation | remove listeners in close |

`AutoCloseable` is appropriate for the fixture result type. A future fixture layer should expose named objects and Hubs directly so tests do not depend on positions except in position-link tests.

## Part 6: Ownership and Test Placement

| Scenario/action | Primary test location | Optional supporting service test | Intentional overlap | What must not be duplicated |
| --- | --- | --- | --- | --- |
| `Hub.setAO(...)` with `StandaloneHub` | `HubTest.setAOTest()` | `HubAOServiceTest.setActiveObjectTest()` | Public test proves final AO/position/events; service test proves direct active-data rule. | Do not duplicate every public event assertion in service tests. |
| `Hub.setAO(...)` with `LinkHubAndDetailHub` | `HubTest.setAO_withLinkHubAndDetailHubTest()` | `HubLinkServiceTest.updateLinkedFromHub_withDetailHubTest()`, `HubDetailServiceTest.updateDetailActiveObject_withLinkedHubTest()` | Public test proves end-to-end result; service tests isolate link resolution and detail realignment. | Do not duplicate the full link+detail+event scenario in all three classes. |
| `Hub.getDetailHub(...)` | `HubTest.getDetailHubTest()` | `HubDetailServiceTest.getDetailHubTest()` | Public test proves usable detail Hub; service test proves metadata/master setup. | Do not retest every Hub add/remove behavior here. |
| `Hub.setSharedHub(...)` | `HubTest.setSharedHubTest()` | `HubShareServiceTest.setSharedHubTest()` | Public test proves caller-visible sharing; service test proves internal data/active sharing choices. | Do not include linked-Hub assertions unless the row is explicitly shared+link. |
| `Hub.add/remove/clear(...)` | `HubTest` method matching public method | `HubAddRemoveServiceTest` method matching service method | Public test proves membership and reverse-link outcome; service test proves callbacks and event ordering. | Do not duplicate full master/detail/link matrix in every add/remove test. |
| `OAObject.setProperty(...)` or generated setter | `OAObjectTest.setProperty...` | `OAObjectPropertyServiceTest.setProperty...`, `OAObjectReferenceServiceTest...` | Public test proves object/reverse-link result; service test proves storage or reverse-maintenance rule. | Do not convert generated model setter behavior into unrelated production-class tests. |
| `OAObject.setHub(...)` | `OAObjectTest.setHub...` | `OAObjectPropertyServiceTest.setProperty_withHub...` | Public test proves assigned Hub behavior; service test proves master metadata side effect. | Do not duplicate Hub add/remove service tests. |
| Save/delete cascade and ownership | `OAObjectTest.save...` / `OAObjectTest.delete...` if public method is under review | `OAObjectSaveServiceTest`, `OAObjectDeleteServiceTest` | Public test proves entry-point outcome; service test proves cascade traversal rule. | Do not test datasource reconstruction in delete tests unless needed. |
| Metadata link shape | `OALinkInfoTest`, `OAObjectInfoTest` | N/A | Metadata tests prove classification used by services. | Do not perform object mutation scenario tests in metadata classes. |
| Sync/remoting propagation | sync public/service tests | Hub/OAObject service tests only as local-state support | Sync tests prove remote/thread-local effects. | Do not mix sync assertions into base Hub/OAObject tests. |

## Part 7: Canonical Name Table

| Concept | Canonical test token | Avoid aliases |
| --- | --- | --- |
| basic independent Hub | `StandaloneHub` | `PlainHub`, `SimpleHub` |
| detail Hub only | `DetailHub` | `Detail`, `ChildHub` |
| master/detail pair | `MasterDetailHub` | `MasterDetail`, `MasterHubDetailHub` |
| shared Hub | `SharedHub` | `Shared`, `ShareHub` |
| linked Hub pair | `LinkHub` | `LinkedHub`, `LinkedPair` |
| link-from role | `LinkFromHub` | `FromHub`, `LinkedFrom` |
| link-to role | `LinkToHub` | `ToHub`, `LinkedTo` |
| linked Hub plus detail Hub | `LinkHubAndDetailHub` | `LinkedDetailHub`, `LinkedMasterDetail` |
| shared Hub plus detail Hub | `SharedHubAndDetailHub` | `SharedDetail` |
| no active object | `Null` or `HubWithoutAO` | `NoAO`, `AOIsNull` |
| same active object | `SameActiveObject` | `SameAO` in method names |
| object not contained | `ObjectNotInHub` | `ExternalObject` |
| no property match | `NoMatch` | `MissingMatch` |
| duplicate matching candidates | `MultipleMatches` | `DuplicateMatches` |
| active object removed | `ActiveObject` when action implies removal | `RemovedAO` |
| current master mismatch | `CurrentMasterDoesNotOwnDetail` | `WrongMaster` |
| unloaded relationship | `UnloadedReference` or `UnloadedRelationshipHub` | `LazyReference` unless source uses it |
| one-to-many relationship | `OneToMany` | `ParentChild` |
| many-to-one relationship | `ManyToOne` | `ChildParent` |
| prior parent | `ExistingParent` | `OldParent`, `PreviousMaster` |
| key-only reference | `ObjectKeyReference` | `FkeyOnly`, `KeyReference` |

## Part 8: Priority Plan

| Priority | Test group | First production class | First methods | Minimum high-value coverage | Fixture dependencies | Open questions |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Basic standalone Hub behavior | `Hub` | `add`, `insert`, `remove`, `clear`, `removeAll`, `setAO`, `setPos` | membership, order, AO, events | `StandaloneHub` | duplicate-add policy details |
| 2 | Hub active object internals | `HubAOService` | `setActiveObject` overloads | null, same AO, object not in Hub, force, event order | `StandaloneHub`, `MasterDetailHub` | exception atomicity if detail/link update throws |
| 3 | Master/detail Hub behavior | `HubDetailService` | `setMasterHub`, `getDetailHub`, `updateDetail`, `setPropertyToMasterHub` | master AO changes detail contents; detail add/remove maintains reverse links | `MasterDetailHub` | exact AO defaulting after new-list |
| 4 | Linked-Hub behavior | `HubLinkService` | `getLinkFromHubObjectForLinkToHubObject`, `updateLinkedFromHub`, `updateLinkProperty` | direct object, position link, property-to-property, no match | three link fixtures | duplicate-match behavior needs decision |
| 5 | Linked Hub plus master/detail | `HubLinkService` | `updateLinkedFromHub` | resolved object not currently contained adjusts master before AO set | `LinkedMasterDetailHub` | failed master adjustment result |
| 6 | Shared Hub behavior | `Hub` / `HubShareService` | `setSharedHub`, AO/membership operations | shared data, shared AO, detail propagation, rejected shared-as-detail setup | `SharedHub`, `SharedHubAndDetailHub` | exact event fan-out across shared Hubs |
| 7 | OAObject one-to-one references | `OAObject` / `OAObjectReferenceService` | `setProperty`, generated setter/getter, protected `getObject` where appropriate | both scalar sides updated; clear removes reverse side | one-to-one fixture | model class availability |
| 8 | OAObject one-to-many references | `OAObject`, `HubAddRemoveService` | `setProperty`, `setHub`, generated many-Hub getter | one-link and reverse many-Hub agree | `OneToManyReference` | event ordering between object and Hub events |
| 9 | Reassignment and clearing | `OAObjectReferenceService` | set/clear reference methods | old parent cleanup and new parent add | `ExistingParentReference` | required-reference validation timing |
| 10 | Events and duplicate-event prevention | event services plus Hub/OAObject entry points | add/remove/AO/property methods | event count and ordering | `EventRecorderScenario` | listener mutation support level |
| 11 | Loaded/unloaded references | object reference/load services | `isLoaded`, `isReferenceNull`, `getReferenceHub`, `getReferenceObject` | null/key/loaded states distinct | `UnloadedReference` | datasource fixture boundary |
| 12 | Delete and ownership | `OAObjectDeleteService` | `delete` | owned/cascade behavior and reverse cleanup | `OwnedDeleteScenario` | exact cascade-delete metadata per POS model |
| 13 | Serialization and datasource reconstruction | serialization/datasource services | read/write/load methods | identity, object keys, loaded refs | serialization/datasource fixture | whether to use in-memory datasource or generated datasource |
| 14 | Sync and remoting propagation | sync services | sync apply/send methods | remote mutation results and event suppression | sync fixture | thread-local contract under remote thread |

Recommended first five test groups to implement:

1. `HubTest` for standalone add/remove/AO basics.
2. `HubAOServiceTest` for AO null, same AO, detail Hub, and link Hub cases using `setActiveObject...` method names.
3. `HubDetailServiceTest` for master/detail contents and reverse reference maintenance.
4. `HubLinkServiceTest` for direct, position, property-to-property, no-match, and linked-master-detail resolution.
5. `OAObjectTest` for one-to-many set, clear, and reassign reference behavior.

## Part 9: Source Evidence and Ambiguity

### Explicitly enforced by source

- `Hub#setAO(...)` and `Hub#setActiveObject(...)` delegate to `HubAOService#setActiveObject(...)`.
- `HubAOService#setActiveObject(...)` updates detail Hubs and link properties around AO assignment and fires the AO event after dependent state is updated.
- `Hub#removeAll()` directly calls `Hub#clear()`, while service-level coverage belongs under `HubAddRemoveService#clear(...)`.
- `HubDetailService#setMasterHub(...)` rejects the specific setup where `thisHub` already has `sharedHub` and a non-null master Hub is assigned.
- `HubDetailService#updateDetail(...)` rebuilds detail Hub data from the current master AO and link metadata.
- `HubDetailService#setPropertyToMasterHub(...)` maintains reverse one-link or many-Hub state when detail membership changes.
- `HubLinkService#getLinkFromHubObjectForLinkToHubObject(...)` resolves linked-from AO using direct object reference, integer position, or property-to-property matching.
- `HubLinkService#updateLinkedFromHub(...)` performs master adjustment based on whether the resolved object is currently contained in the link-from Hub.
- `OAObject#setHub(...)` stores Hub references through `OAObjectPropertyService#setProperty(...)`.
- `OAObject#getObject(...)` delegates to `OAObjectReflectService#getReferenceObject(...)`.
- `OAObjectPropertyService#setProperty(...)` assigns Hub master metadata when a Hub is stored as an OAObject property and has no master object.
- `OAObjectPropertyService#setPropertyCAS(...)` has special comparison behavior for `OAObjectKey` and OAObject key equivalence.

### Documented or strongly implied behavior

- Hub events are part of the public observable contract for add/remove/AO/new-list operations.
- `OALinkInfo` metadata drives reverse-link, ownership, cascade, calculated, and cardinality behavior.
- Loaded/unloaded relationship state is distinct from Java null and from OA null state.
- OA primitive name/value properties require null-state tests because zero can be a valid persisted value.

### Current implementation only

- Property-to-property linked-Hub duplicate matches currently return the first matching object in Hub iteration order. This is not yet treated as a permanent REQUIRED invariant because no explicit contract was found.

### Ambiguous or deferred behavior

| Area | Question | Recommended classification | Evidence level |
| --- | --- | --- | --- |
| Filtered Hub AO | Can AO be an object excluded by the filter, and should public `contains` decide membership? | DEFERRED | AMBIGUOUS |
| AO assignment exception atomicity | If detail or link update throws after AO state is written, what state must remain observable? | DEFERRED | AMBIGUOUS |
| Listener mutation during events | Which Hub mutations are supported from inside before/after events? | DEFERRED | AMBIGUOUS |
| Shared-Hub event fan-out | Which shared Hubs should receive AO/list events and in what order? | DEFERRED until event recorder fixtures exist | AMBIGUOUS |
| Failed master adjustment in linked detail Hub | If `callHubDataGetPos(..., adjustMaster=true, updateLink=false)` cannot make the resolved object visible, should AO be null or object? | DEFERRED | AMBIGUOUS |
| Client-side unloaded reference cleanup | Detail reverse cleanup has branches for client/unloaded Hub state; exact test fixture boundary needs confirmation. | DEFERRED | AMBIGUOUS |
| Sync/remoting event suppression | Local event expectations under remote-thread state require dedicated sync fixtures. | DEFERRED | AMBIGUOUS |
| Cascade delete ownership | Exact owned/cascade rules should be verified from model metadata before writing delete tests. | DEFERRED | STRONGLY_INFERRED |
| Duplicate linked-Hub property matches | Should first-match selection be an intended contract, an error, or undefined behavior? | DEFERRED | CURRENT_IMPLEMENTATION |

## Final Report

| Item | Count |
| --- | ---: |
| Core Hub wiring configurations identified | 10 |
| Hub state tokens identified | 10 |
| Hub/reference condition tokens identified | 15 |
| Derived Hub types moved to separate-matrix section | 6 |
| Hub actions analyzed | 18 |
| Hub action/wiring or condition cells classified | 39 |
| OAObject reference shapes identified | 19 |
| OAObject reference actions analyzed | 18 |
| Reference action/shape cells classified | 28 |
| Deterministic fixtures recommended | 16 |

Highest-risk untested combinations:

1. `HubTest.setAO_withLinkHubAndDetailHubTest()` plus `HubLinkServiceTest.updateLinkedFromHub_withDetailHubTest()` for linked object resolution that requires master adjustment.
2. `HubLinkServiceTest.updateLinkedFromHub_withLinkedFromObjectNotInHubTest()` for state-based master adjustment using containment.
3. `OAObjectTest.setProperty_withExistingParentTest()` for stale reverse many-Hub cleanup.
4. `HubTest.remove_withDetailHubTest()` and `HubTest.clear_withDetailHubTest()` for guarded reverse-reference cleanup.
5. `HubTest.setAO_withSharedHubAndDetailHubTest()` for shared AO/detail propagation without duplicate events.
6. `OAObjectTest.delete_withOwnedReferenceTest()` for cascade/ownership behavior.
7. `OAObjectTest.isReferenceNullTest()` for loaded, unloaded, explicit-null, and key-only reference distinctions.

Recommended first five test groups:

1. Basic standalone Hub membership and AO behavior.
2. Master/detail Hub alignment and detail reverse references.
3. Linked-Hub direct, position, and property-to-property resolution.
4. Linked-Hub plus master/detail adjustment.
5. OAObject one-to-many set, clear, and reassign behavior.

This matrix is intentionally finite. Additional cells should be added only when source analysis shows that wiring, metadata, or runtime state changes the observable contract.
