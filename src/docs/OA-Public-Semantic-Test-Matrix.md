# OA Public Semantic Test Matrix

This audit documents the semantic coverage currently represented by `HubTest` and `OAObjectTest`. It is derived from the current source and test implementations, the original `OA-Unit-Test-Scenario-Matrix.md`, and the OAPOS test model under `com.test.pos.model.oa`.

The counts below are scenario-cell counts for this audit, not a Cartesian product of every method and every possible condition. A cell is included only when source routing, the original matrix, or an implemented/disabled test indicates a distinct public semantic contract.

## 1. Executive Summary

### HubTest

| Metric | Count |
| --- | ---: |
| Production methods reviewed | 248 broad declaration matches / 156 unique source names |
| Production methods represented | 140 represented test groups / 185 test methods |
| Production methods missing or insufficiently represented | 16 |
| Passing tests | 164 |
| Disabled tests | 21 |
| Applicable matrix cells | 109 |
| PASS cells | 63 |
| DISABLED_TODO cells | 21 |
| MISSING cells | 7 |
| PARTIAL cells | 10 |
| QUESTIONABLE cells | 7 |
| N/A cells | 18 |
| INVALID cells | 1 |

Implemented semantic coverage: `63 / 109 = 57.8%`.

Represented semantic coverage: `(63 + 21 + 1) / 109 = 78.0%`.

Assessment: `HubTest` is structurally broad and covers the core in-memory Hub API, AO behavior, master/detail behavior, shared Hubs, and linked Hubs. It is not semantically complete because datasource, serialization, recursive link, trigger, duplicate event, listener mutation, and several overload-specific contracts remain disabled, partial, or missing.

### OAObjectTest

| Metric | Count |
| --- | ---: |
| Production methods reviewed | 156 broad declaration matches / 106 unique source names |
| Production methods represented | 151 represented test groups / 178 test methods |
| Production methods missing or insufficiently represented | 12 |
| Passing tests | 140 |
| Disabled tests | 38 |
| Applicable matrix cells | 114 |
| PASS cells | 59 |
| DISABLED_TODO cells | 38 |
| MISSING cells | 8 |
| PARTIAL cells | 6 |
| QUESTIONABLE cells | 3 |
| N/A cells | 17 |
| INVALID cells | 0 |

Implemented semantic coverage: `59 / 114 = 51.8%`.

Represented semantic coverage: `(59 + 38 + 0) / 114 = 85.1%`.

Assessment: `OAObjectTest` now gives most direct `OAObject` methods a visible location and covers important scalar property, relationship, lifecycle, identity, and reference behavior. It is not semantically complete because event methods, datasource-backed lifecycle operations, unloaded reference semantics, serialization, fkey edge cases, and cascade save/delete remain disabled or shallow.

## 2. Matrix Vocabulary

### Hub Wiring

| Vocabulary | Source/test support | Current status |
| --- | --- | --- |
| `StandaloneHub` | `new Hub<>(Register.class)` and list-style operations | PASS baseline coverage |
| `ObjectOwnedHub` | `new Hub<>(Register.class, store)` and generated many-Hub ownership | PARTIAL; covered through detail/master fixtures, not as a standalone named scenario |
| `MasterHub` | `Hub<Store>` used to create detail Hubs | PASS for AO-driven detail updates |
| `DetailHub` | `Store.Registers` via `getDetailHub(Store.P_Registers)` | PASS for add/insert/remove/clear/sort/AO |
| `SharedHub` | `createSharedHub`, `setSharedHub`, shared membership/AO tests | PASS baseline |
| `SharedHubWithSharedAO` | shared Hub created with shared active object state | PASS through `setAO_withSharedHubTest` |
| `SharedHubWithoutSharedAO` | `createSharedHub(false)`/`setSharedHub(..., false)` style isolation | PARTIAL; explicit AO isolation needs stronger coverage |
| `SharedHubAndDetailHub` | source/shared master with dependent detail Hub | PASS for AO propagation; duplicate-event behavior disabled |
| `LinkHubDirectReference` | link-to object reference selects link-from AO | PASS |
| `LinkHubOnPosition` | integer property maps to link-from position | PASS |
| `LinkHubPropertyToProperty` | compatible scalar properties match link-from object | PASS for match/no-match; duplicate match deferred |
| `LinkHubAndDetailHub` | linked selection requiring master/detail realignment | PASS |
| `SortedHub` | sort/resort/isSorted/cancelSort | PASS baseline; shared/detail sort combinations incomplete |
| `FilteredHub` | `createFilteredHubTest` | PARTIAL; original matrix treats derived Hubs as separate matrices |
| `DatasourceHub` | select/load/save/delete/refresh paths | DISABLED_TODO |
| `RemoteOrSyncHub` | `sendRefresh` | DISABLED_TODO |

### Hub State

| Vocabulary | Source/test support | Current status |
| --- | --- | --- |
| `EmptyHub` | `isEmpty`, clear/removeAll, null AO cases | PASS baseline |
| `NonEmptyHub` | common fixture shape | PASS baseline |
| `HubWithoutAO` | initial Hub state and null AO tests | PASS |
| `HubWithAO` | `setAO`, `setPos`, shared/detail/link tests | PASS |
| `HubAOOutsideHub` | source supports adjust-master; test only covers standalone clear behavior | QUESTIONABLE |
| `LoadedHub` | in-memory generated relationships | PASS baseline |
| `UnloadedHub` | datasource-backed relationship loading | DISABLED_TODO |
| `MoreDataAvailable` | `isMoreData` in-memory false | PARTIAL |
| `EnabledHub` | `getEnabled`/`setEnabled` | PASS baseline |
| `DisabledHub` | `setEnabled(false)` | PASS baseline |
| `SortedState` | `isSorted`, `resort`, `cancelSort` | PASS baseline |
| `UnsortedState` | pre-sort/cancel-sort state | PASS baseline |

### Hub Conditions

| Vocabulary | Source/test support | Current status |
| --- | --- | --- |
| `Null` | `setAO_withNullTest`, null property/remove behavior | PASS |
| `SameActiveObject` | `setAO_withSameActiveObjectTest` | PASS |
| `ObjectNotInHub` | standalone behavior tested; adjust-master branch not fully covered | QUESTIONABLE |
| `DuplicateAdd` | `add_withDuplicateAddTest` | PASS |
| `RepeatedRemove` | `remove_withRepeatedRemoveTest` | PASS |
| `ActiveObjectRemoved` | `remove_withActiveObjectTest` | PASS |
| `NoMatch` | `setLinkHub_withNoMatchTest` | PASS |
| `MultipleMatches` | disabled duplicate linked-property decision | DISABLED_TODO |
| `RecursiveLinkUpdate` | disabled recursive linked-Hub fixture | DISABLED_TODO |
| `ListenerMutation` | disabled event-mutation contract | DISABLED_TODO |
| `DuplicateEventPrevention` | disabled shared/detail event fan-out contract | DISABLED_TODO |
| `InvalidSetupOrder` | rejected shared-as-detail setup | INVALID/PASS |

### OAObject Reference Shape

| Vocabulary | Source/test support | Current status |
| --- | --- | --- |
| `ScalarProperty` | Register/Store scalar properties through `setProperty`/`getProperty` | PASS |
| `OneToOne` | `Store.Address` ↔ `Address.Store` | PASS |
| `OneToMany` | `Store.Registers` ↔ `Register.Store` | PASS |
| `ManyToOne` | `Register.Store` ↔ `Store.Registers` | PASS |
| `ReverseOneLink` | `setHub` plus reverse one-link maintenance | PASS |
| `ReverseManyLink` | one-link setter updates reverse many-Hub | PASS |
| `OwnedReference` | OAPOS metadata exists, delete behavior disabled | DISABLED_TODO |
| `OptionalReference` | null/reference clearing coverage; delete optional behavior disabled | PARTIAL |
| `ExistingParent` | register reassignment between stores | PASS |
| `NullReference` | clearing many-to-one reference | PASS |
| `LoadedReference` | `getObject`, `isLoaded`, `isReferenceNull` for loaded links | PASS/PARTIAL |
| `UnloadedReference` | key-only/datasource fixture missing | DISABLED_TODO |
| `ObjectKeyReference` | fkey/reference key helpers partially covered | PARTIAL/DISABLED_TODO |
| `InitializedManyHub` | generated many-Hub returned after creation | PASS |
| `UninitializedManyHub` | test currently asserts generated Hub is considered loaded before creation | QUESTIONABLE |
| `RecursiveReference` | OAPOS recursive relationships exist in catalog classes, not used by these tests | MISSING |

### OAObject Lifecycle and State

| Vocabulary | Source/test support | Current status |
| --- | --- | --- |
| `New`, `NotNew` | `getNew`, `isNew`, `setNew` | PASS |
| `Changed`, `Unchanged` | `getChanged`, `isChanged`, `setChanged` | PASS/PARTIAL for relationship cascade variants |
| `Deleted`, `NotDeleted` | `getDeleted`, `isDeleted`, `wasDeleted`, `setDeleted` | PASS |
| `Loaded`, `Unloaded` | loaded in-memory references, unloaded TODOs | PARTIAL |
| `NullProperty`, `NonNullProperty` | `setNull`, `getNull`, `isNull` | PASS baseline |
| `Cached`, `Detached` | object key/unique instance tests | PARTIAL |

## 3. HubTest Source-Method Coverage

| Production method group | Signature/overloads | Matching test methods | Status | Notes |
| --- | --- | --- | --- | --- |
| Constructors | no-arg, object, class, capacity, master/shared variants | `constructorTest`, `constructor_withObjectTest`, `constructor_withObjectClassTest`, `constructor_withCapacityTest`, `constructor_withSharedHubTest`, `constructor_withMasterObjectTest` | PARTIAL | Broad constructor shapes covered; select/detail constructor path is not fully semantic. |
| Properties/runtime | `setProperty`, `getProperty`, `removeProperty`, `getOA`, `toString`, `compareTo`, `clone` | matching tests | PARTIAL | Mostly baseline storage/identity; `toString` and `compareTo` assertions are weak. |
| Size/traversal | `getSize`, `size`, `getCurrentSize`, `getLoadedSize`, `getAt`, `getObjectAt`, `getObject`, `getLast`, `contains`, `indexOf`, collection facade methods | matching tests | COMPLETE | In-memory collection behavior is well represented. |
| AO/position | `getAO`, `getActiveObject`, `setAO` overloads, `setActiveObject` overloads, `setPos`, `getPos`, `resetAO` | matching tests | PARTIAL | Core AO scenarios pass; object-not-in-Hub adjust-master and default position behavior are incomplete. |
| Membership mutation | `add`, `addElement`, indexed add, `insert`, `remove`, `removeAt`, `clear`, `removeAll`, `replace`, `move`, `swap` | matching tests | PARTIAL | Standalone and detail cases mostly pass; collection/detail/shared combinations are not exhaustive. |
| Detail/master | `getDetailHub` overloads, `setMasterHub` overloads, `getMasterHub`, `getMasterObject`, `getMasterClass`, `hasDetailHubs`, `removeDetailHub` | matching tests | PARTIAL | Main relationship shape covered; many overloads collapse into representative tests. |
| Shared Hub | `createSharedHub`, `createShared`, `setSharedHub`, `getSharedHub` | matching tests | PARTIAL | Supported sharing covered; unsupported setup has one invalid test; AO isolation without sharing needs stronger coverage. |
| Linked Hub | `setLinkHub`, `setLinkHubOnPos`, `removeLinkHub`, `getLinkHub`, `getLinkPath`, `setLink` | matching tests | PARTIAL | Direct, position, property match and no-match pass; duplicate-match and recursive-update contracts are TODO. |
| Sorting/selection | `sort`, `resort`, `cancelSort`, `isSorted`, `select`, `selectPassthru`, select where/order methods | matching tests and disabled TODOs | PARTIAL | In-memory sort passes. Datasource selection is disabled. |
| Save/delete/load/refresh | `loadAllData`, `saveAll`, `deleteAll`, `refresh`, `sendRefresh`, `isMoreData` | mixed passing and disabled | TODO_ONLY/PARTIAL | In-memory load/isMoreData only. Datasource/remote behavior deferred. |
| List facade | `containsAll`, `addAll`, `removeAll(Collection)`, `retainAll`, `get`, `set`, iterators, `subList`, `stream`, arrays/list conversion | matching tests | COMPLETE | Public collection facade has meaningful in-memory assertions. |
| Listeners/triggers/events | `addHubListener`, `removeHubListener`, `addListener`, `removeListener`, `addTriggerListener`, event callbacks | matching tests and disabled TODOs | PARTIAL | Listener registration and simple events pass. Trigger/listener mutation/event ordering remain TODO. |
| Rules/permissions | `canAdd`, `getAllowAdd`, `getAllowRemove`, `getVerifyRemove`, `getAllowRemoveAll`, `getCanAddMessage` | matching tests | PARTIAL | Defaults covered; response override/order not covered here. |
| Auto sequence/match | `setAutoSequence`, `resequence`, `setAutoMatch` overloads | passing plus disabled TODOs | PARTIAL | Simple auto-sequence covered; resequence/auto-match fixtures deferred. |
| Serialization/finalization | `readResolve`, `finalize` | disabled TODOs | TODO_ONLY | No stable fixture or invariant. |

## 4. HubTest Semantic Combination Matrix

### `Hub.add(...)`

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | StandaloneHub | Added object becomes a member and size increases. | `addTest` | `Hub<Register>` | PASS | Assertions |  |
| Condition | DuplicateAdd | Duplicate add follows Hub duplicate policy and does not create a second member. | `add_withDuplicateAddTest` | `Hub<Register>` | PASS | Assertions |  |
| Wiring | DetailHub | Adding to a detail Hub assigns the reverse one-link to the current master. | `add_withDetailHubTest` | `Store.Registers` ↔ `Register.Store` | PASS | Assertions + metadata |  |
| Wiring | SharedHub | Adding through a shared Hub mutates shared membership. | `add_withSharedHubTest` | shared `Hub<Register>` | PASS | Assertions |  |
| Condition | ListenerMutation | Supported mutation from inside Hub events is defined. | `add_withListenerMutationTest` | none | DISABLED_TODO | Original matrix | Invariant requires confirmation. |
| Condition | ExistingParent | Adding an object already owned by another master updates prior reverse membership. | none | `Store.Registers` | MISSING | Source reverse-link behavior | Should be added as `add_withDetailHubAndExistingParentTest`. |

### `Hub.insert(...)`

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | StandaloneHub | Insert places object at requested index. | `insertTest`, `add_withIndexTest` | `Hub<Register>` | PASS | Assertions | Public overloads represented. |
| Wiring | DetailHub | Insert into detail Hub assigns reverse master link. | `insert_withDetailHubTest` | `Store.Registers` | PASS | Assertions |  |
| Condition | ExistingParent | Insert of existing child from another master repairs prior parent membership. | none | `Store.Registers` | MISSING | Source detail cleanup | Important reassignment variant. |

### `Hub.remove(...)` and `Hub.removeAt(...)`

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | StandaloneHub | Removed object is no longer a member. | `removeTest` | `Hub<Register>` | PASS | Assertions |  |
| Wiring | DetailHub | Removing from detail Hub clears the reverse one-link for the current master. | `remove_withDetailHubTest` | `Store.Registers` | PASS | Assertions |  |
| Condition | ActiveObjectRemoved | Removing AO updates or clears the active object. | `remove_withActiveObjectTest` | `Hub<Register>` | PASS | Assertions |  |
| Condition | RepeatedRemove | Removing a non-member is stable and returns false. | `remove_withRepeatedRemoveTest` | `Hub<Register>` | PASS | Assertions |  |
| Wiring | StandaloneHub by position | Removing by index removes the object at that position. | `removeAtTest` | `Hub<Register>` | PASS | Assertions |  |
| Condition | ActiveObjectRemoved by position | Removing AO by index follows AO update rules. | none | `Hub<Register>` | MISSING | Original matrix condition | Object remove covers this indirectly; position-specific branch absent. |

### `Hub.clear()` and `Hub.removeAll()`

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | StandaloneHub | All members and AO are cleared. | `clearTest`, `removeAllTest` | `Hub<Register>` | PASS | Assertions |  |
| Wiring | DetailHub | Clearing detail Hub clears reverse one-links. | `clear_withDetailHubTest` | `Store.Registers` | PASS | Assertions |  |
| Wiring | SharedHub | Clearing/removing all through shared Hub updates shared membership. | none | shared `Hub<Register>` | MISSING | Shared data source path | `add/remove` shared cases do not cover clear semantics. |
| Wiring | DetailHub through `removeAll()` | `removeAll()` should follow detail cleanup semantics. | none | `Store.Registers` | MISSING | `removeAll()` delegates to clear | `clear_withDetailHubTest` is indirect but method-specific coverage is absent. |

### `Hub.setAO(...)` / `Hub.setActiveObject(...)`

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | StandaloneHub | Requested member becomes AO and position matches. | `setAOTest`, `setActiveObjectTest` | `Hub<Register>` | PASS | Assertions |  |
| Condition | Null | AO clears and position becomes `-1`. | `setAO_withNullTest` | `Hub<Register>` | PASS | Assertions |  |
| Condition | SameActiveObject | Reassigning the same AO is stable. | `setAO_withSameActiveObjectTest` | `Hub<Register>` | PASS | Assertions |  |
| State/condition | HubAOOutsideHub / ObjectNotInHub | Outside object should follow public adjust-master semantics and not corrupt membership. | `setAO_withObjectNotInHubTest` | standalone `Hub<Register>` | QUESTIONABLE | Assertions only prove standalone clear behavior | Does not exercise adjust-master/detail branch described by matrix. |
| Wiring | MasterHub | Master AO change updates detail Hub contents. | `setAO_withMasterHubTest` | `Store.Registers` | PASS | Assertions |  |
| Wiring | DetailHub | Setting detail AO realigns master AO. | `setAO_withDetailHubTest` | `Store.Registers` | PASS | Assertions |  |
| Wiring | SharedHub | Shared active object propagates across shared Hubs. | `setAO_withSharedHubTest` | shared `Hub<Register>` | PASS | Assertions |  |
| Wiring | SharedHubAndDetailHub | Shared master AO changes update dependent detail Hub. | `setAO_withSharedHubAndDetailHubTest` | shared `Hub<Store>` + `Store.Registers` | PASS | Assertions | Event fan-out not asserted. |
| Wiring | LinkHubDirectReference | Link-from AO updates link-to object reference. | `setAO_withLinkHubTest` | `Register.Till`/`Till.Register` style fixture | PASS | Assertions |  |
| Wiring | LinkHubAndDetailHub | Linked selection realigns master/detail before final AO. | `setAO_withLinkHubAndDetailHubTest` | `Store.Registers` + `Till.Register` | PASS | Assertions |  |
| Condition | RecursiveLinkUpdate | Recursive linked-Hub update terminates without loops. | `setAO_withLinkHubAndRecursiveLinkUpdateTest` | none | DISABLED_TODO | Original matrix | Fixture and event-count invariant missing. |
| Condition | DuplicateEventPrevention | Shared/detail/link propagation avoids duplicate public events. | `setAO_withSharedHubAndDetailHubAndDuplicateEventPreventionTest` | none | DISABLED_TODO | Original matrix | Exact event fan-out unresolved. |
| Condition | NoMatch | Link-to property with no match clears link-from AO when AO update is triggered. | `setLinkHub_withNoMatchTest` | property-to-property link | PARTIAL | Assertions via setup/property path | Covered under `setLinkHub`, not direct `setAO` action. |

### `Hub.setPos(...)` / `Hub.getPos(...)`

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | StandaloneHub | Position selects object at index and `getPos` reports it. | `setPosTest`, `getPosTest` | `Hub<Register>` | PASS | Assertions |  |
| Wiring | LinkHubOnPosition | Position-linked Hub maps integer property to link-from position. | `setPos_withLinkHubTest`, `setLinkHubOnPosTest` | `Hub<Register>` + integer property object | PASS | Assertions |  |
| Condition | Invalid index/null equivalent | Out-of-range position behavior is explicit. | none | `Hub<Register>` | MISSING | Public API branch | Needed for boundary semantics. |

### `Hub.setSharedHub(...)` / shared creation

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | SharedHub | Shared Hub uses source membership. | `setSharedHubTest`, `createSharedHubTest`, `createSharedTest` | `Hub<Register>` | PASS | Assertions |  |
| State | SharedHubWithoutSharedAO | Membership is shared while AO remains independent. | constructor/shared variants | `Hub<Register>` | PARTIAL | Assertions limited | Needs explicit AO isolation test. |
| Wiring/invalid | InvalidSetupOrder | A Hub that already has `sharedHub` cannot become detail through public master setup. | `setSharedHub_withRejectedMasterHubSetupTest` | `Hub<Register>` | INVALID | Assertion on exception | This is specific setup order only; shared+detail generally exists. |

### `Hub.getDetailHub(...)` / master-detail setup

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | MasterDetailHub | Detail Hub follows current master AO relationship contents. | `getDetailHubTest` | `Store.Registers` | PASS | Assertions | Representative overload coverage only. |
| Condition | CurrentMasterDoesNotOwnDetail | Selecting a detail from another master realigns master AO. | `setAO_withDetailHubTest` | two `Store` objects with registers | PASS | Assertions | Covered through AO scenario. |
| State | UnloadedRelationshipHub | Detail Hub load state is materialized by load path. | `loadAllData_withUnloadedRelationshipHubTest` | none | DISABLED_TODO | Original matrix | Datasource-backed fixture needed. |

### `Hub.setLinkHub(...)` and related link APIs

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | LinkHubDirectReference | Link-to direct object reference selects link-from AO. | `setLinkHubTest` | OAPOS direct one-link | PASS | Assertions |  |
| Wiring | LinkHubOnPosition | Link-to integer property selects link-from position. | `setLinkHubOnPosTest` | `Hub<Register>` and int property | PASS | Assertions |  |
| Wiring | LinkHubPropertyToProperty | Matching scalar property selects link-from object. | `setLinkHub_withPropertyToPropertyTest` | deterministic matching property values | PASS | Assertions |  |
| Condition | NoMatch | No matching scalar value clears link-from AO. | `setLinkHub_withNoMatchTest` | property-to-property link | PASS | Assertions |  |
| Condition | MultipleMatches | Duplicate property matches need intended contract. | `setLinkHub_withMultipleMatchesTest` | none | DISABLED_TODO | Original matrix; current implementation only | Vince decision required. |
| Wiring | LinkHubAndDetailHub | Link-from detail Hub resolves by adjusting master. | `setAO_withLinkHubAndDetailHubTest` | `Store.Registers` + link-to object | PASS | Assertions | Public outcome covered under AO. |
| Condition | RecursiveLinkUpdate | Recursive link update termination. | `setAO_withLinkHubAndRecursiveLinkUpdateTest` | none | DISABLED_TODO | Matrix | Fixture missing. |

### Sorting, Selection, Loading, Save/Delete, and Refresh

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| State | SortedState | Sort by path/comparator reorders membership. | `sortTest`, `sort_withComparatorTest`, `resortTest`, `isSortedTest`, `cancelSortTest` | `Hub<Register>` | PASS | Assertions |  |
| Wiring | DetailHub + SortedHub | Sorting detail Hub preserves reverse master references. | `sort_withDetailHubTest` | `Store.Registers` | PASS | Assertions |  |
| Wiring | DatasourceHub | Select materializes datasource-backed Hub contents. | `selectTest`, `selectPassthruTest` | none | DISABLED_TODO | Matrix | Datasource fixture required. |
| State | UnloadedHub | loadAllData materializes unloaded relationship state. | `loadAllData_withUnloadedRelationshipHubTest` | none | DISABLED_TODO | Matrix | Datasource/reference fixture required. |
| Lifecycle | Save/delete cascade | Hub save/delete follows datasource and cascade rules. | `saveAllTest`, `saveAll_withCascadeRuleTest`, `deleteAllTest` | none | DISABLED_TODO | Matrix | Datasource/ownership fixture required. |
| Remote | RemoteOrSyncHub | Refresh/sendRefresh propagates under sync configuration. | `refreshTest`, `sendRefreshTest` | none | DISABLED_TODO | Matrix | Sync/datasource fixtures required. |

### List Facade, Listeners, and Miscellaneous Hub Methods

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Collection facade | Standard List operations | List facade reflects Hub membership/order. | `containsAllTest`, `addAllTest`, `retainAllTest`, `iteratorTest`, etc. | `Hub<Register>` | PASS | Assertions | Good in-memory coverage beyond original matrix. |
| Event/listener | Basic listener add/remove | Registered listeners observe events; removed listeners do not. | `addHubListenerTest`, `removeHubListenerTest`, aliases | `Hub<Register>` | PASS | Assertions | Event ordering broader than simple add/remove is not covered. |
| Event/listener | Trigger listener path | Dependent path trigger callback fires. | `addTriggerListenerTest`, `addTriggerListener_withBackgroundThreadTest` | none | DISABLED_TODO | Matrix | Trigger fixture and thread contract missing. |
| Derived Hub | FilteredHub | Filtered Hub contains accepted source objects. | `createFilteredHubTest` | `Hub<Register>` | PARTIAL | Assertions | Original matrix says derived Hubs need separate matrix; propagation/AO behavior absent. |
| Serialization/finalize | Serialization/finalization | Runtime state restored or finalization observable contract defined. | `readResolveTest`, `finalizeTest` | none | DISABLED_TODO | Source methods | Finalize may not be stable public invariant. |

## 5. OAObjectTest Source-Method Coverage

| Production method group | Signature/overloads | Matching test methods | Status | Notes |
| --- | --- | --- | --- | --- |
| Version/construction/identity | `getOAVersion`, constructors, `getGuid`, `getObjectKey`, `equals`, `hashCode`, `compareTo` | matching tests | PARTIAL | Runtime GUID/object key covered; version assertion is brittle. |
| Scalar property APIs | `setProperty` primitive/object/formatted overloads, `getProperty`, `getPropertyAsString`, `setNull`, `getNull`, `isNull`, `removeProperty`, `compareAndSwap` | matching tests and one disabled | PARTIAL | Basic scalar behavior strong; same-value, invalid-type, and remove-property are incomplete. |
| Rules/callbacks | valid property, enabled/visible, command, submit/save callbacks | matching tests | PARTIAL | Defaults/callback carrier objects covered; override ordering and negative decisions not covered. |
| Lifecycle flags | new, changed, deleted flags | matching tests | COMPLETE/PARTIAL | Direct flags covered; relationship/cascade changed-state cases limited. |
| Copy APIs | `createCopy`, `copyInto` | matching tests | PARTIAL | Scalar copies covered; reference copy contract missing. |
| Event APIs | protected fire-before/fire/change/local/new-list overloads | disabled TODOs | TODO_ONLY | No deterministic event recorder fixture yet. |
| Hub/reference APIs | protected `getHub` overloads, `setHub`, protected `getObject`, reference null/loaded/key helpers | matching tests and disabled TODOs | PARTIAL | Loaded/in-memory behavior covered; unloaded/key-only incomplete. |
| Save/delete/refresh/load | `save`, `saveAll`, `delete`, `canSave`, `canDelete`, `afterSave`, `afterDelete`, `refresh`, `loadReferences` overloads | mostly disabled or shallow | PARTIAL/TODO_ONLY | Datasource and cascade behavior deferred. |
| Locking | `lock`, `unlock`, `isLocked`, `isPropertyLocked` | matching tests | COMPLETE | In-memory lock behavior covered. |
| Find/search | `find`, `findAll`, `hierFind`, `isUnique`, `getUniqueInstance` | mixed tests | PARTIAL | Basic find covered; unique/datasource/hierarchical behavior incomplete. |
| Remote/server/thread helpers | `isRemoteThread`, `sendMessages`, `callRemote`, `remote`, `isRemoteAvailable`, server-only methods | matching tests | PARTIAL | Mostly local no-server behavior. Sync/remoting not exercised. |
| Fkey helpers | `setFkeyProperty`, protected link-info overload, `getFkeyProperty` overloads | matching tests and disabled | PARTIAL | Basic fkey scalar/reference covered; protected link-info branch disabled. |
| Name/value | `getNameValues` | matching test | PARTIAL | Basic name/value hub exposure only. |
| Debug/finalize | `setDebugMode`, `getDebugMode`, `setFinalizeSave`, `getFinalizeSave` | matching tests | PARTIAL/QUESTIONABLE | Finalize-save behavior appears implementation-specific. |
| FriendAccess | nested FriendAccess methods | matching tests and one disabled | PARTIAL | Internal access paths mostly covered; event firing disabled. |
| Runtime | `getOA` | matching test | COMPLETE | Runtime association covered. |

## 6. OAObjectTest Semantic Combination Matrix

### `OAObject.setProperty(...)`

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Property shape | ScalarProperty | Primitive/object value is stored and observable through generated getter. | `setPropertyBooleanTest`, `setPropertyIntTest`, `setPropertyLongTest`, `setPropertyDoubleTest`, `setPropertyObjectTest` | `Register`, `Store` | PASS | Assertions | Numeric conversion covered for double-to-int. |
| Condition | Formatted value | Formatted assignment updates target value. | `setPropertyFormattedTest` | `Register` | PASS | Assertions |  |
| Reference shape | ManyToOne / OneToMany | Assigning `Register.Store` adds register to `Store.Registers`. | `setProperty_withOneToManyTest` | `Store.Registers` ↔ `Register.Store` | PASS | Assertions |  |
| Reference shape | OneToOne | Assigning one-to-one scalar reference updates reverse scalar side. | `setProperty_withOneToOneTest` | `Store.Address` ↔ `Address.Store` | PASS | Assertions |  |
| Condition | ExistingParent | Reassigning parent removes prior reverse membership and adds new. | `setProperty_withExistingParentTest` | two `Store` objects + `Register` | PASS | Assertions |  |
| Reference shape | NullReference | Clearing many-to-one clears reverse many-Hub. | `setProperty_withNullReferenceTest` | `Store.Registers` | PASS | Assertions |  |
| Condition | SameValue | Reassigning same value does not produce unintended duplicate event/change. | none | `Register` | MISSING | Matrix condition | Event/no-op invariant not represented. |
| Condition | InvalidType | Invalid property type handling is explicit. | none | `Register` scalar property | MISSING | Public API branch | Important failure behavior. |
| Reference shape | RequiredReference + Null | Required reference validation/save behavior on null is explicit. | none | `Register.Store` | MISSING | Metadata says required | Current null reference test covers reverse cleanup, not required validation. |

### Null State and Property Read APIs

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| State | NullProperty | `setNull`, `getNull`, and `isNull` agree. | `setNullTest`, `getNullTest`, `isNullTest` | `Register.Code` | PASS | Assertions |  |
| State | Primitive null | Primitive null state is distinguishable from default primitive value. | `setPrimitiveNullTest` | none | DISABLED_TODO | Source protected method | Needs subclass/helper fixture. |
| Property shape | ScalarProperty read | `getProperty` and `getPropertyAsString` read stored value/format. | `getPropertyTest`, `getPropertyAsString*Test` | `Register` | PASS | Assertions |  |
| Operation | removeProperty | Removing stored property follows OA null/loaded semantics. | `removePropertyTest` | none | DISABLED_TODO | Source method | Likely implementable in-memory. |

### Rules, Visibility, Enabled, and Command Callbacks

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rules | Default valid property change | Normal property change is valid by default. | `isValidPropertyChangeTest`, overload tests | `Register` | PASS | Assertions |  |
| Rules | Callback carrier | Rules methods return `OAObjectCallback` with current response state. | `getIsValidPropertyChangeObjectCallback*Test`, enabled/visible/command callback tests | `Register` | PASS/PARTIAL | Assertions | Carrier exists; override order/negative rules not covered. |
| Rules | Default enabled/visible/command | Public default decision matches current rules engine. | `isEnabled*Test`, `isVisible*Test`, `verifyCommandTest` | `Register` | PASS/PARTIAL | Assertions | Deeper rule stages not covered. |
| Rules | Save/submit callbacks | Callback objects are returned. | `getAllowSubmitTest`, `getVerifySaveObjectCallbackTest` | `Register` | PARTIAL | Assertions only not-null/current decision | No rule override or failure path. |

### Lifecycle, Identity, and Changed State

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Lifecycle | New/NotNew | New flags report and update consistently. | `getNewTest`, `isNewTest`, `setNewTest` | `Register` | PASS | Assertions |  |
| Lifecycle | Deleted/NotDeleted | Deleted flags report and update consistently. | `getDeletedTest`, `wasDeletedTest`, `isDeletedTest`, `setDeletedTest` | `Register` | PASS | Assertions |  |
| Lifecycle | Changed/Unchanged | Changed flags report direct state and can be cleared/set. | `getChangedTest`, `isChangedTest`, `setChangedTest` | `Register` | PASS | Assertions |  |
| Lifecycle | Relationship changed cascade | Include-links and relationship-type variants report expected state. | `getChangedIncludeLinksTest`, `isChangedIncludeLinksTest`, `getChangedRelationshipTypeTest` | `Store.Registers` | PARTIAL | Assertions | Only simple cleared-object cases. |
| Identity | GUID/key/equality/hash/compare | Object identity is based on runtime GUID and object key. | `getGuidTest`, `getObjectKeyTest`, `equalsTest`, `hashCodeTest`, `compareToTest` | `Register` | PASS | Assertions |  |
| Construction | Constructor state | Constructor initializes identity and lifecycle state. | `constructorTest` | `Register` | PARTIAL | Assertions | Combines multiple invariants under constructor. |
| Version | OA version | Version identifies OA 4 runtime. | `getOAVersionTest` | none | QUESTIONABLE | Assertion starts with `4.0.0` | May be too brittle for later patch versions. |

### Reference and Hub APIs

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Reference shape | InitializedManyHub | Reading existing many-Hub returns usable Hub. | `getHub_withManyHubInitializedTest` | `Store.Registers` | PASS | Assertions |  |
| Reference shape | UninitializedManyHub | Many-Hub initialization/loaded state is explicit. | `getHub_withManyHubNotInitializedTest` | `Store.Registers` | QUESTIONABLE | Assertions | Test claims pre-getter generated Hub is loaded; vocabulary may need adjustment. |
| Reference shape | ReverseOneLink | Assigning a Hub lets additions maintain reverse one-link. | `setHub_withReverseOneLinkTest` | `Store.Registers` | PASS | Assertions |  |
| Reference shape | LoadedReference | Loaded one-link getter returns current object. | `getObject_withLoadedReferenceTest` | `Register.Store` | PASS | Assertions |  |
| Reference shape | UnloadedReference | Unloaded one-link follows load/key contract. | `getObject_withUnloadedReferenceTest` | none | DISABLED_TODO | Matrix | Datasource/object-key fixture missing. |
| State | LoadedReference | `isLoaded`/`isPropertyLoaded` report loaded relationships/properties. | `isLoaded_withLoadedReferenceTest`, `isPropertyLoadedTest` | `Register.Store`, scalar property | PASS/PARTIAL | Assertions | Scalar loaded state only partly covers relationship loaded state. |
| State | UnloadedReference | Loaded-state accessors report unloaded relationship without forcing load. | `isLoaded_withUnloadedReferenceTest` | none | DISABLED_TODO | Matrix | Fixture missing. |
| Reference state | ObjectKeyReference | Reference key can be read without loaded object. | `getReferenceObjectKeyTest` | `Register.StoreId` | PARTIAL | Assertions | Key-only/unloaded distinction not covered. |
| Reference null | Loaded/null/reference | Null checks distinguish explicit null from loaded object. | `isReferenceNullTest`, `isReferenceObjectNullTest` | `Register.Store` | PARTIAL | Assertions | Object-key/unloaded variants absent. |

### Copy, Save, Delete, Refresh, and Load References

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Copy | Scalars | Copy/createCopy copy scalar properties to distinct object. | `createCopyTest`, `createCopyExcludePropertiesTest`, `copyIntoTest` | `Register` | PASS | Assertions |  |
| Copy | CopyWithReferences | Reference copy contract is explicit. | none | `Store.Registers`, `Store.Address` | MISSING | Matrix | Needed to avoid accidental reference aliasing semantics. |
| Datasource | Save | Save and cascade-save follow metadata. | `saveTest`, `saveCascadeTest`, `saveAllTest` | none | DISABLED_TODO | Matrix | Datasource/cascade fixture required. |
| Datasource | Delete | Delete/owned/cascade/optional reference behavior is explicit. | `deleteTest`, `delete_withOwnedReferenceTest`, `delete_withOptionalReferenceTest` | none | DISABLED_TODO | Matrix | Ownership fixture verified but not used. |
| Datasource | Refresh | Refresh preserves/reloads expected state. | `refreshTest`, `refreshLinkTest` | in-memory only | PARTIAL | `assertDoesNotThrow` style | Datasource result not asserted. |
| Loading | loadReferences | Load reference overloads have observable loaded-state behavior. | `loadReferences*Test` | in-memory | PARTIAL | Mostly no-throw | Unloaded fixture missing. |

### Event, Remote, Fkey, Name/Value, FriendAccess, and Runtime APIs

| Scenario category | Scenario | Expected invariant | Test method | OAPOS fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Event | Property events | before/after/local/new-list event contracts are defined. | many `fire*Test` methods | none | DISABLED_TODO | Source protected methods | Requires event recorder/helper subclass. |
| Remote/sync | No server configured | Remote availability/call behavior is stable locally. | `isRemoteThreadTest`, `callRemoteTest`, `remoteTest`, `isRemoteAvailable*Test` | `Register`, `Hub<Register>` | PASS/PARTIAL | Assertions | No sync server path. |
| Server-only | Local server-only state | Server-only methods restore local state. | `startServerOnlyTest`, `endServerOnlyTest`, `runOnServerOnlyTest` | `Register` | PASS | Assertions |  |
| Fkey | Basic fkey property | Fkey setter/getter map scalar key to relationship. | `setFkeyPropertyTest`, `getFkeyPropertyTest`, `getFkeyPropertyLinkToTest` | `Register.StoreId` | PASS/PARTIAL | Assertions | Protected link-info overload disabled. |
| Name/value | Name values | Name/value Hub is exposed for generated name/value property. | `getNameValuesTest` | generated model name/value property | PARTIAL | Assertions | Invalid value/string view not covered here. |
| FriendAccess | Internal state access | FriendAccess reads/writes internal flags/properties. | friendAccess tests | `Register` | PASS/PARTIAL | Assertions | `firePropertyChange` disabled. |
| Runtime | OA association | Object resolves current OA runtime. | `getOATest` | `Register` | PASS | Assertions |  |

## 7. OAPOS Fixture Mapping

| Semantic shape | OAPOS classes | Property/link | Cardinality | Reverse link | Ownership | Loaded-state suitability | Used by tests |
| --- | --- | --- | --- | --- | --- | --- | --- |
| StandaloneHub | `Register`, `Store` | none | list membership | none | none | loaded/in-memory | Most Hub baseline tests |
| MasterDetailHub / OneToMany / ManyToOne | `Store`, `Register` | `Store.P_Registers` / `Register.P_Store` | `Store` many to `Register`; `Register` one to `Store` | yes | `Store.Registers` has `owner=true`, `cascadeSave=true`, `cascadeDelete=true` | loaded/in-memory; datasource-unloaded not available | Hub detail tests, OAObject reference tests |
| ExistingParent | two `Store` objects + `Register` | `Register.P_Store` | many-to-one reassignment | reverse many-Hub | owned by current store relationship | loaded/in-memory | `setProperty_withExistingParentTest` |
| OneToOne | `Store`, `Address` | `Store.P_Address` / `Address.P_Store` | one-to-one | yes | `Store.Address` auto-create/allow-add constraints; `Address.Store` `isOneAndOnlyOne=true` | loaded/in-memory | `setProperty_withOneToOneTest` |
| Customer address one-to-many | `Customer`, `Address` | `Customer.P_Addresses` / `Address.P_Customer` | one-to-many | yes | cascade save/delete on customer addresses | loaded/in-memory | Available; not primary in current tests |
| Register/Till link | `Register`, `Till`, `Store` | `Register.P_Till`, `Till.P_Register`, `Till.P_Store` | one-to-one/select-from-path shape | yes | required store/register metadata | loaded/in-memory | linked detail tests |
| Owned cascade many | `Register`, `RegisterSession` | `Register.P_RegisterSessions` | one-to-many | yes | `owner=true`, cascade save/delete | loaded/in-memory; datasource required for save/delete | Metadata available; save/delete tests disabled |
| Store safe owned one | `Store`, `StoreSafe` | `Store.P_StoreSafe` | one-to-one | yes | `owner=true`, cascade save/delete | loaded/in-memory; datasource required for lifecycle | Metadata available; not used by current public tests |
| Recursive reference | `CatalogCategory` | parent/child catalog categories | recursive one/many | yes | not used by current fixtures | loaded/in-memory possible | Missing from current tests |
| Object-key/unloaded reference | `Register.StoreId`, fkey metadata | `Register.P_StoreId` / `Register.P_Store` | fkey to one-link | yes | required relationship | requires key-only/datasource setup | Partial fkey tests only |

## 8. Disabled TODO Inventory

### HubTest Disabled TODOs

| Test method | Production method | Scenario | Reason disabled | Matrix status | Missing fixture/decision | Recommended resolution |
| --- | --- | --- | --- | --- | --- | --- |
| `loadAllData_withUnloadedRelationshipHubTest` | `loadAllData` | UnloadedRelationshipHub | deterministic datasource-backed unloaded relationship fixture required | DISABLED_TODO | Datasource fixture | Build key-only relationship fixture. |
| `saveAllTest` | `saveAll` | Datasource save | save behavior requires deterministic datasource fixture | DISABLED_TODO | Datasource fixture | Use in-memory datasource or test datasource harness. |
| `saveAll_withCascadeRuleTest` | `saveAll(int)` | Cascade save | save behavior requires deterministic datasource fixture | DISABLED_TODO | Datasource/cascade fixture | Use owned OAPOS relation. |
| `deleteAllTest` | `deleteAll` | Cascade delete | verified ownership/cascade fixture required | DISABLED_TODO | Datasource/delete fixture | Use `Register.RegisterSessions` or `Store.StoreSafe`. |
| `setAO_withLinkHubAndRecursiveLinkUpdateTest` | `setAO` | RecursiveLinkUpdate | recursive linked-Hub fixture and event-count invariant require confirmation | DISABLED_TODO | Fixture + invariant decision | Decide supported recursive link shape. |
| `setRootHubTest` | `setRootHub` | RootHub | root-Hub public contract unclear for plain Hubs | DISABLED_TODO | Vince/source decision | Define public invariant or exclude. |
| `getRootHubTest` | `getRootHub` | RootHub | root-Hub public contract unclear for plain Hubs | DISABLED_TODO | Vince/source decision | Define public invariant or exclude. |
| `onBeforeRefreshTest` | `onBeforeRefresh` | Refresh event | deterministic refresh event fixture required | DISABLED_TODO | Refresh fixture | Pair with datasource refresh. |
| `add_withListenerMutationTest` | `add` | ListenerMutation | listener mutation contract requires confirmation | DISABLED_TODO | Vince decision | Define allowed mutations during events. |
| `setAO_withSharedHubAndDetailHubAndDuplicateEventPreventionTest` | `setAO` | DuplicateEventPrevention | exact shared/detail event fan-out requires confirmation | DISABLED_TODO | Event recorder + Vince decision | Decide event order/fan-out. |
| `resequenceTest` | `resequence` | AutoSequence | deterministic non-unique integer sequence fixture required | DISABLED_TODO | Sequence fixture | Use non-unique int property. |
| `setAutoMatchTest` | `setAutoMatch` | AutoMatch | auto-match fixture and propagation direction require confirmation | DISABLED_TODO | Fixture + source decision | Verify source path. |
| `selectTest` | `select` | DatasourceHub | deterministic datasource select fixture required | DISABLED_TODO | Datasource fixture | Implement with known data. |
| `setLinkHub_withMultipleMatchesTest` | `setLinkHub` | MultipleMatches | duplicate match behavior unresolved | DISABLED_TODO | Vince decision | Decide first-match/error/undefined. |
| `sendRefreshTest` | `sendRefresh` | RemoteOrSyncHub | sync/remote fixture required | DISABLED_TODO | Sync fixture | Defer to sync tests. |
| `refreshTest` | `refresh` | Datasource refresh | datasource-backed refresh fixture required | DISABLED_TODO | Datasource fixture | Define refreshed membership assertions. |
| `readResolveTest` | `readResolve` | Serialization | serialization fixture required | DISABLED_TODO | Serialization fixture | Add round-trip test. |
| `finalizeTest` | `finalize` | Finalization | finalization side effects not stable public invariant | DISABLED_TODO | Decision | Prefer exclude unless public contract exists. |
| `addTriggerListenerTest` | `addTriggerListener` | TriggerListener | trigger fixture and callback path require confirmation | DISABLED_TODO | Trigger path fixture | Use dependent path from OAPOS. |
| `addTriggerListener_withBackgroundThreadTest` | `addTriggerListener` | Background trigger | background-thread contract requires confirmation | DISABLED_TODO | Threading decision | Define sync/async observable. |
| `selectPassthruTest` | `selectPassthru` | Datasource select | datasource fixture required | DISABLED_TODO | Datasource fixture | Add with raw where/order fixture. |

### OAObjectTest Disabled TODOs

| Test method group | Production method | Scenario | Reason disabled | Matrix status | Missing fixture/decision | Recommended resolution |
| --- | --- | --- | --- | --- | --- | --- |
| `readResolveTest` | `readResolve` | Serialization | invariant needs definition | DISABLED_TODO | Serialization fixture | Add object round-trip with key/reference state. |
| `setPrimitiveNullTest` | `setPrimitiveNull` | Primitive null | invariant needs definition | DISABLED_TODO | Subclass/helper access | Implement through test subclass or generated int property. |
| `removePropertyTest` | `removeProperty` | Property removal | invariant needs definition | DISABLED_TODO | In-memory fixture | Likely implementable now. |
| `fireBeforePropertyChange*Test` | protected fire-before overloads | Event ordering | invariant needs definition | DISABLED_TODO | Event recorder/helper subclass | Define before-event assertions. |
| `firePropertyChange*Test` | protected fire overloads | Event ordering | invariant needs definition | DISABLED_TODO | Event recorder/helper subclass | Define after-event assertions. |
| `fireLocalPropertyChange*Test` | protected local-fire overloads | Local events | invariant needs definition | DISABLED_TODO | Event recorder/helper subclass | Define local-only behavior. |
| `fireNewListTest` | `fireNewList` | New list event | invariant needs definition | DISABLED_TODO | Hub listener fixture | Define event contract. |
| `getHubSortOrder*Test`, `getHubMatchHubTest` | protected getHub overloads | Sort/match Hub | invariant needs definition | DISABLED_TODO | Sequence/sort/match fixtures | Use generated sorted/sequence metadata if available. |
| `getObject_withUnloadedReferenceTest` | `getObject` | UnloadedReference | datasource/key-only fixture missing | DISABLED_TODO | Datasource/object-key fixture | Build unloaded reference scenario. |
| `getBlobTest` | `getBlob` | Blob reference | invariant needs definition | DISABLED_TODO | Blob fixture | Use model blob property if available. |
| `save*Test` | `save`, `saveAll` | Datasource/cascade save | datasource/cascade fixture missing | DISABLED_TODO | Datasource fixture | Use owned OAPOS relations. |
| `delete*Test` | `delete` | Owned/optional delete | delete fixture missing | DISABLED_TODO | Datasource/delete fixture | Verify cascade and non-owned references. |
| `isUniqueTest` | `isUnique` | Unique datasource check | invariant needs definition | DISABLED_TODO | Datasource/cache fixture | Decide in-memory vs datasource contract. |
| `isLoaded_withUnloadedReferenceTest` | `isLoaded` | UnloadedReference | fixture missing | DISABLED_TODO | Key-only/unloaded relation | Pair with `getObject` test. |
| `hierFindTest` | `hierFind` | Hierarchical search | invariant needs definition | DISABLED_TODO | Hierarchy fixture | CatalogCategory recursive fixture likely suitable. |
| `setFkeyPropertyLinkInfoTest` | protected fkey setter | ObjectKeyReference | invariant needs definition | DISABLED_TODO | OALinkInfo/fkey fixture | Use `Register.StoreId` metadata. |
| `friendAccessFirePropertyChangeTest` | FriendAccess fire | Event | invariant needs definition | DISABLED_TODO | Event recorder | Pair with event tests. |

## 9. Missing Scenario Inventory

| Class | Production method | Missing scenario | Why applicable | Recommended test name | Suggested fixture | Priority |
| --- | --- | --- | --- | --- | --- | --- |
| `HubTest` | `add` | ExistingParent in detail Hub | Detail add should repair prior reverse membership. | `add_withDetailHubAndExistingParentTest` | two `Store` objects, one `Register` | P1 |
| `HubTest` | `insert` | ExistingParent in detail Hub | Insert is distinct add path. | `insert_withDetailHubAndExistingParentTest` | two `Store` objects, one `Register` | P1 |
| `HubTest` | `removeAt` | ActiveObjectRemoved | Position remove path can differ from object remove. | `removeAt_withActiveObjectTest` | `Hub<Register>` | P2 |
| `HubTest` | `clear` | SharedHub | Shared clear can route through shared data. | `clear_withSharedHubTest` | shared `Hub<Register>` | P1 |
| `HubTest` | `removeAll` | DetailHub | Method-specific alias should prove detail cleanup. | `removeAll_withDetailHubTest` | `Store.Registers` | P2 |
| `HubTest` | `setPos` | Invalid index | Boundary behavior is public API. | `setPos_withInvalidIndexTest` | `Hub<Register>` | P2 |
| `HubTest` | `setSharedHub` | SharedHubWithoutSharedAO | AO isolation is a distinct shared state. | `setSharedHub_withSharedHubWithoutSharedAOTest` | shared `Hub<Register>` | P1 |
| `OAObjectTest` | `setProperty` | SameValue | Duplicate event/no-op semantics are important. | `setProperty_withSameValueTest` | `Register.Code` + listener | P1 |
| `OAObjectTest` | `setProperty` | InvalidType | Failure behavior is part of public contract. | `setProperty_withInvalidTypeTest` | `Register.Id` or boolean property | P1 |
| `OAObjectTest` | `setProperty` | RequiredReferenceAndNull | `Register.Store` is required; validation/save semantics unclear. | `setProperty_withRequiredReferenceAndNullTest` | `Register.Store` | P1 |
| `OAObjectTest` | `createCopy` | CopyWithReferences | Reference copy contract not covered. | `createCopy_withReferencesTest` | `Store.Registers`, `Store.Address` | P1 |
| `OAObjectTest` | `copyInto` | CopyWithReferences | Reference copy contract not covered. | `copyInto_withReferencesTest` | `Store.Registers`, `Store.Address` | P1 |
| `OAObjectTest` | `hierFind` | RecursiveReference | OAPOS has recursive catalog category model. | `hierFind_withRecursiveReferenceTest` | `CatalogCategory` parent/children | P2 |
| `OAObjectTest` | loaded/reference helpers | ObjectKeyReference | Key-only/unloaded state not covered. | `getReferenceObjectKey_withUnloadedReferenceTest` | fkey-only `Register.StoreId` fixture | P1 |

## 10. Partial and Questionable Test Inventory

| Test method | Claimed invariant | Actual assertions | Issue | Status | Recommended correction |
| --- | --- | --- | --- | --- | --- |
| `setAO_withObjectNotInHubTest` | Outside AO follows object-not-in-Hub behavior. | Proves standalone Hub clears/does not select outside object. | Does not exercise adjust-master branch that made this matrix row critical. | QUESTIONABLE | Add detail/master outside-object fixture or rename invariant to standalone-only. |
| `createFilteredHubTest` | Filtered Hub contains accepted source objects. | Checks initial filtered contents. | Does not cover derived-Hub mutation, AO, source-change propagation, or read-only rules. | PARTIAL | Move deeper derived-Hub work to filtered-Hub matrix/test class. |
| `toStringTest` | toString identifies object class. | Checks string contains `Register`. | Weak but acceptable baseline; not a semantic invariant beyond diagnosis. | PARTIAL | Add size/AO/detail safeguards only if source documents them. |
| `compareToTest` | compareTo orders Hubs. | Checks self zero and distinct nonzero. | Does not prove stable ordering contract. | PARTIAL | Define whether compareTo has any public order guarantee. |
| `resetAOTest` | resetAO updates AO from default position. | Basic default behavior only. | Default-pos branch and no-default branch are not both fully covered. | QUESTIONABLE | Add explicit default-position scenario. |
| `loadAllDataTest` | In-memory Hub load is stable. | Mostly no membership changes. | Does not cover source-level load/select behavior. | PARTIAL | Keep as baseline; use disabled datasource TODO for real load behavior. |
| `setUniquePropertyTest` | Unique property rejects duplicates. | Asserts broad runtime exception. | Exception type/message not precise; state atomicity not fully asserted. | QUESTIONABLE | Verify Hub state after failed duplicate add. |
| `getOAVersionTest` | Version identifies OA 4 runtime. | Asserts prefix `4.0.0`. | Overly specific to patch version. | QUESTIONABLE | Use stable OA 4 major/minor contract if intended. |
| `getHub_withManyHubNotInitializedTest` | Many-Hub not initialized behavior. | Asserts generated Hub is considered loaded before creation. | Name/vocabulary conflicts with actual generated behavior. | QUESTIONABLE | Clarify whether generated many-Hub access auto-initializes and rename scenario. |
| `refreshTest` / `refreshLinkTest` | Refresh behavior. | No-throw/local effects only. | Does not prove datasource refresh semantics. | PARTIAL | Defer datasource refresh or add explicit in-memory invariant. |
| `loadReferences*Test` | Reference loading behavior. | Mostly no-throw/in-memory. | Does not prove unloaded materialization or loaded-state updates. | PARTIAL | Use key-only/datasource fixture. |
| `setFinalizeSaveTest` | finalize-save behavior is disabled in current runtime. | Asserts setter does not enable behavior. | May freeze current implementation rather than intended public contract. | QUESTIONABLE | Confirm intended invariant or disable. |
| `getAllowSubmitTest`, `getVerifySaveObjectCallbackTest` | Callback returned. | Mostly not-null/current response. | Does not cover rule order/overrides. | PARTIAL | Add focused rules-engine fixture later. |

## 11. Original Matrix Reconciliation

### Original REQUIRED rows implemented

- `Hub.setAO` with `StandaloneHub`, `Null`, `SameActiveObject`, `DetailHub`, `MasterHub`, `SharedHub`, `SharedHubAndDetailHub`, `LinkHubDirectReference`, and `LinkHubAndDetailHub` are represented by passing tests.
- `Hub.add`, `insert`, `remove`, `clear`, `move`, `sort`, and `getDetailHub` baseline in-memory rows are represented by passing tests.
- `Hub.setLinkHub` direct, position, property-to-property, and no-match modes are represented by passing tests.
- `OAObject.setProperty` scalar, many-to-one/one-to-many, one-to-one, existing-parent, and null-reference rows are represented by passing tests.
- `OAObject.setHub` reverse-one-link behavior is represented by a passing test.
- `OAObject` identity, lifecycle flags, null-state, loaded-reference baseline, fkey baseline, lock, and runtime access have passing tests.

### Original REQUIRED rows disabled

- Hub datasource load/select/save/delete/refresh and sync refresh rows.
- Hub recursive linked-Hub update, duplicate event prevention, listener mutation, trigger listener, auto-match, resequence, serialization, and finalization rows.
- OAObject event methods, serialization, primitive-null helper, removeProperty, unloaded reference/object-key load, datasource save/delete/cascade, sort/match many-Hub overloads, blob, unique datasource, hierarchical find, protected fkey branch, and FriendAccess event firing.

### Original REQUIRED rows missing

- Hub detail add/insert with existing parent.
- Hub shared clear/removeAll semantics.
- Hub `setPos` invalid index/boundary behavior.
- OAObject `setProperty` same-value, invalid-type, and required-reference-null semantics.
- OAObject copy/createCopy/copyInto with references.
- OAObject recursive reference/hierarchy behavior using OAPOS recursive model.
- OAObject key-only/unloaded object-key distinction beyond simple fkey baseline.

### Original DEFERRED rows represented

- Linked-Hub duplicate property matches are represented by disabled `setLinkHub_withMultipleMatchesTest`.
- Shared/detail duplicate event fan-out is represented by disabled `setAO_withSharedHubAndDetailHubAndDuplicateEventPreventionTest`.
- Listener mutation is represented by disabled `add_withListenerMutationTest`.
- Sync/remoting refresh is represented by disabled `sendRefreshTest`.

### Original INVALID rows tested

- A specific shared-Hub invalid setup order is represented by `setSharedHub_withRejectedMasterHubSetupTest`: a Hub with `sharedHub` configured is rejected when the public path attempts to make that same Hub a detail Hub with a non-null master. This does not classify all shared/detail combinations as invalid.

### Tests added beyond original matrix

- Many Java `List` facade methods on `Hub` have explicit in-memory tests.
- Hub dynamic property storage, addHub, enable/disable, unique property, refresh flag, rules defaults, and listener aliases are represented beyond the core matrix.
- OAObject debug mode, finalize-save, remote/server-only local behavior, name/value exposure, and FriendAccess internals have test locations beyond the original public matrix.

### Original rows no longer applicable

- Service-only `OAObjectHubService.addHub/removeHub` rows do not belong in `OAObjectTest` unless reachable through public/protected `OAObject` methods. Public behavior is represented through generated many-Hub access and `Hub` mutation.
- Derived Hub forms such as `FilteredHub`, combined/grouped/flattened/joined/calculated Hubs are separate matrices and should not be counted as required core Hub wiring rows for `HubTest` beyond the public creation baseline.

### Conflicts between original matrix and current tests

| Conflict | Recommendation |
| --- | --- |
| `setAO_withObjectNotInHubTest` does not exercise the adjust-master behavior that the matrix calls out. | Add a real detail/master outside-object scenario or downgrade that test to standalone baseline. |
| `getHub_withManyHubNotInitializedTest` uses vocabulary that conflicts with generated-Hub loaded behavior. | Clarify matrix vocabulary for generated many-Hub auto-initialization. |
| Several no-throw tests are counted as source-method locations but not complete invariants. | Keep locations, add real assertions or mark partial in future tracking. |
| Current tests include public API rows for derived filtered Hub behavior while original matrix says derived Hubs need separate matrices. | Leave baseline public creation test, but move deeper derived semantics to separate document/test plan. |
| `getOAVersionTest` appears to freeze patch-level version text. | Vince should decide whether patch version is a public invariant. |

## 12. Completion Assessment

### HubTest

Classification: `STRUCTURALLY_COMPLETE_BUT_SEMANTICALLY_INCOMPLETE`.

Reasons:

- Most direct public Hub methods have a visible test location.
- Core in-memory membership, AO, master/detail, shared, and linked-Hub public behavior has meaningful passing tests.
- Important matrix rows are still disabled for datasource, serialization, sync, recursive links, duplicate event prevention, listener mutation, trigger listeners, and auto-match/resequence.
- Several overload groups are represented by one baseline test rather than overload-specific behavior.
- Some tests are partial or questionable because they assert a weaker invariant than the display name or matrix row implies.

### OAObjectTest

Classification: `STRUCTURALLY_COMPLETE_BUT_SEMANTICALLY_INCOMPLETE`.

Reasons:

- Most direct `OAObject` methods, including protected methods and FriendAccess, have a visible test location.
- Scalar property, core reference-shape, lifecycle, identity, lock, fkey, and runtime behavior has meaningful passing coverage.
- Event contracts, unloaded reference behavior, object-key-only semantics, datasource save/delete/refresh, serialization, cascade behavior, and recursive/hierarchical references remain disabled or missing.
- Several tests are currently assertion-light and prove only local no-throw behavior rather than the full matrix invariant.

## 13. Recommended Next Actions

### P0 - Missing Foundational Invariants

| Target | Production method | Scenario | Current status | Recommended test name | Fixture | Invariant |
| --- | --- | --- | --- | --- | --- | --- |
| `HubTest` | `setAO` | ObjectNotInHub with adjust-master | QUESTIONABLE | `setAO_withDetailHubAndObjectNotInHubTest` | two-store detail fixture | Outside detail object realigns master or clears AO according to source contract. |
| `OAObjectTest` | `setProperty` | SameValue | MISSING | `setProperty_withSameValueTest` | `Register.Code` + listener | Same-value assignment does not create unintended semantic change/events. |
| `OAObjectTest` | `setProperty` | InvalidType | MISSING | `setProperty_withInvalidTypeTest` | scalar property | Invalid assignment fails predictably and preserves old value. |

### P1 - Matrix Combination Gaps

| Target | Production method | Scenario | Current status | Recommended test name | Fixture | Invariant |
| --- | --- | --- | --- | --- | --- | --- |
| `HubTest` | `add` | DetailHub + ExistingParent | MISSING | `add_withDetailHubAndExistingParentTest` | two `Store` objects + `Register` | Adding child to new detail Hub removes it from prior reverse many-Hub. |
| `HubTest` | `insert` | DetailHub + ExistingParent | MISSING | `insert_withDetailHubAndExistingParentTest` | two `Store` objects + `Register` | Insert follows the same reverse-parent repair rule as add. |
| `HubTest` | `clear` | SharedHub | MISSING | `clear_withSharedHubTest` | shared `Hub<Register>` | Clear mutates shared backing data consistently. |
| `OAObjectTest` | `createCopy` | CopyWithReferences | MISSING | `createCopy_withReferencesTest` | `Store.Registers`, `Store.Address` | Copy follows intended reference copy contract without corrupting source links. |
| `OAObjectTest` | `copyInto` | CopyWithReferences | MISSING | `copyInto_withReferencesTest` | `Store.Registers`, `Store.Address` | Copy-into follows intended reference copy contract. |

### P2 - Weak or Partial Assertions

| Target | Production method | Scenario | Current status | Recommended test name | Fixture | Invariant |
| --- | --- | --- | --- | --- | --- | --- |
| `HubTest` | `setUniqueProperty` | DuplicateAdd | QUESTIONABLE | keep current name, strengthen assertions | `Hub<Register>` | Failed duplicate add leaves size, order, and original object unchanged. |
| `HubTest` | `createFilteredHub` | FilteredHub | PARTIAL | future derived-Hub matrix | `Hub<Register>` | Source changes and AO rules for filtered Hubs are explicit. |
| `OAObjectTest` | `loadReferences` | LoadedReference | PARTIAL | strengthen overload tests | loaded `Store.Registers` | loadReferences updates loaded state or preserves loaded state as defined. |
| `OAObjectTest` | `refresh` | LoadedReference | PARTIAL | `refresh_withLoadedReferenceTest` | datasource fixture | Refresh behavior has observable state assertions. |

### P3 - Disabled Tests That Can Likely Be Completed In-Memory

| Target | Production method | Scenario | Current status | Recommended test name | Fixture | Invariant |
| --- | --- | --- | --- | --- | --- | --- |
| `OAObjectTest` | `removeProperty` | ScalarProperty | DISABLED_TODO | `removePropertyTest` | `Register.Code` | Removing a dynamic/scalar property restores expected null/loaded state. |
| `OAObjectTest` | `setPrimitiveNull` | Primitive null | DISABLED_TODO | `setPrimitiveNullTest` | test subclass or generated int property | Primitive null state distinguishes default value from explicit value. |
| `HubTest` | `setSharedHub` | SharedHubWithoutSharedAO | PARTIAL | `setSharedHub_withSharedHubWithoutSharedAOTest` | `Hub<Register>` | Shared membership can keep independent AO when configured. |
| `HubTest` | `removeAt` | ActiveObjectRemoved | MISSING | `removeAt_withActiveObjectTest` | `Hub<Register>` | Position removal of AO follows AO transition rules. |

### P4 - Datasource/Sync/Serialization Fixture Work

| Target | Production method | Scenario | Current status | Recommended test name | Fixture | Invariant |
| --- | --- | --- | --- | --- | --- | --- |
| `HubTest` | `select`, `selectPassthru`, `loadAllData`, `refresh` | DatasourceHub | DISABLED_TODO | existing TODOs | deterministic test datasource | Select/load/refresh materialize expected objects and loaded state. |
| `HubTest` | `sendRefresh` | RemoteOrSyncHub | DISABLED_TODO | existing TODO | sync fixture | Refresh notification is sent under sync configuration. |
| `OAObjectTest` | `save`, `delete`, `refresh`, `isUnique` | Datasource/cascade | DISABLED_TODO | existing TODOs | deterministic test datasource | Save/delete/refresh/unique semantics are observable and isolated. |
| both | `readResolve` | Serialization | DISABLED_TODO | existing TODOs | serialization round-trip | Runtime/object identity state is restored after deserialization. |

### P5 - Original Matrix/Document Corrections

| Area | Recommendation |
| --- | --- |
| `HubAOOutsideHub` | Clarify standalone outside-object behavior versus detail/master adjust-master behavior. |
| `ManyHubNotInitialized` | Rename or split generated auto-created Hub state from truly unloaded datasource relationship state. |
| `FilteredHub` | Keep baseline `Hub.createFilteredHub` test but move deeper behavior to a derived-Hub matrix. |
| Version invariants | Decide whether `getOAVersion` should assert major/minor only or exact patch text. |
| Event contracts | Vince should decide which event order/fan-out behaviors are public invariants before implementing tests. |

## Console Summary

```text
HubTest:
    methods reviewed: 248 broad declarations / 156 unique source names
    methods represented: 140 represented test groups / 185 test methods
    applicable matrix cells: 109
    PASS: 63
    DISABLED_TODO: 21
    MISSING: 7
    PARTIAL: 10
    QUESTIONABLE: 7
    completion classification: STRUCTURALLY_COMPLETE_BUT_SEMANTICALLY_INCOMPLETE

OAObjectTest:
    methods reviewed: 156 broad declarations / 106 unique source names
    methods represented: 151 represented test groups / 178 test methods
    applicable matrix cells: 114
    PASS: 59
    DISABLED_TODO: 38
    MISSING: 8
    PARTIAL: 6
    QUESTIONABLE: 3
    completion classification: STRUCTURALLY_COMPLETE_BUT_SEMANTICALLY_INCOMPLETE
```
