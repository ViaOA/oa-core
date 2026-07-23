# OA Hub/OAObject Matrix Reconciliation

This document reconciles the regenerated public API tests:

- `com.viaoa.hub.HubTest`
- `com.viaoa.object.OAObjectTest`

against:

- `OA-Unit-Test-Scenario-Matrix.md`
- `OA-Public-Semantic-Test-Matrix.md`

It is an audit artifact only. It does not define new behavior and does not replace service-level test matrices.

## 1. Executive Summary

### HubTest

| Metric | Count |
| --- | ---: |
| Original/source-grounded matrix rows reviewed | 60 |
| Applicable rows | 57 |
| `PASS` | 42 |
| `DISABLED_TODO` | 9 |
| `INVALID` | 1 |
| `N/A` | 3 |
| `PARTIAL` | 3 |
| `QUESTIONABLE` | 2 |
| `MISSING` | 0 |

Accounted-for percentage: `(PASS + DISABLED_TODO + INVALID + N/A) / all reviewed rows = 55 / 60 = 91.7%`.

Implemented percentage: `PASS / applicable non-N/A rows = 42 / 57 = 73.7%`.

Completion gate: `MATRIX_NOT_ACCOUNTED_FOR`, because `PARTIAL` and `QUESTIONABLE` rows remain.

### OAObjectTest

| Metric | Count |
| --- | ---: |
| Original/source-grounded matrix rows reviewed | 47 |
| Applicable rows | 43 |
| `PASS` | 23 |
| `DISABLED_TODO` | 9 |
| `INVALID` | 0 |
| `N/A` | 4 |
| `PARTIAL` | 7 |
| `QUESTIONABLE` | 1 |
| `MISSING` | 3 |

Accounted-for percentage: `(PASS + DISABLED_TODO + INVALID + N/A) / all reviewed rows = 36 / 47 = 76.6%`.

Implemented percentage: `PASS / applicable non-N/A rows = 23 / 43 = 53.5%`.

Completion gate: `MATRIX_NOT_ACCOUNTED_FOR`, because `MISSING`, `PARTIAL`, and `QUESTIONABLE` rows remain.

## 2. Matrix Reconciliation Rules

- Applicability was determined from the original scenario matrix first, then checked against `Hub.java`, `OAObject.java`, current test bodies, and OAPOS metadata.
- A row is `PASS` only when an enabled test body performs the public API action and assertions prove the stated public invariant.
- A disabled test is structural coverage only when it explicitly names the missing fixture, unresolved invariant, infrastructure dependency, or suspected defect.
- `Hub.setActiveObject(Object)` is classified as `N/A - canonical alias coverage` when covered by `Hub.setAO(...)`. Source evidence: `Hub.setActiveObject(Object)`, `Hub.setActiveObject(TYPE)`, `Hub.setAO(Object)`, and `Hub.setAO(TYPE)` all delegate to `oa.internal().hubs().ao().setActiveObject(...)`.
- `Hub.setActiveObject(int)` is classified as `N/A - canonical alias coverage` when covered by `Hub.setPos(...)`. Source evidence: `Hub.setPos(int)` delegates to `setActiveObject(int)`, which delegates to the same AO service.
- `Hub.getActiveObject()` may be covered through `getAO()` where both expose the same active object. Current tests include both `getActiveObjectTest()` and `getAOTest()`.
- Public API tests prove final caller-visible behavior. Service-owned internal rules are named as supporting evidence but are not counted as missing from these two public test classes unless they create distinct public observable behavior.
- OAPOS fixture availability affects whether a row is `PASS`, `DISABLED_TODO`, or `MISSING`; it does not change the underlying matrix classification.
- No broad Cartesian product was generated. A row is included only where the matrix calls it out, source branches differently, or public observable behavior differs.

## 3. Hub Matrix Reconciliation

### Constructors and Basic Hub State

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | `StandaloneHub` | Construction creates an empty typed Hub with stable list state. | `constructorTest`, `constructor_withObjectClassTest` | `Hub<Register>` | `PASS` | Assertions | Valid addition beyond the core matrix. |
| Wiring | `SharedHub` | Constructing from another Hub shares membership as configured. | `constructor_withSharedHubTest` | `Hub<Register>` | `PASS` | Assertions | Supports shared-Hub matrix. |
| Wiring | `ObjectOwnedHub` | Object-owned constructor establishes owned Hub metadata. | `constructor_withMasterObjectTest`, `isOwnedTest` | `Store`/`Register` | `PARTIAL` | Assertions | Tests ownership flag, not full object-owned lifecycle behavior. |
| State | `EmptyHub` | Empty Hubs report zero loaded/current size. | `getCurrentSizeTest`, `getSizeTest`, `sizeTest`, `getLoadedSizeTest` | `Hub<Register>` | `PASS` | Assertions | In-memory state only. |
| State | `EnabledHub` / `DisabledHub` | Enabled flag can be set and read. | `setEnabledTest`, `getEnabledTest` | `Hub<Register>` | `PASS` | Assertions | Simple public state. |

### `Hub.add(...)`

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | `StandaloneHub` | Added object becomes a Hub member and size increases. | `addTest`, `addElementTest`, `addListTest`, `addHubTest` | `Hub<Register>` | `PASS` | Assertions | Covers baseline and convenience add APIs. |
| Wiring | `DetailHub` | Adding to a detail Hub assigns the reverse one-link to the current master. | `add_withDetailHubTest` | `Store.Registers` / `Register.Store` | `PASS` | Assertions + OAPOS metadata | Uses OAPOS master/detail fixture. |
| Wiring | `SharedHub` | Adding through a shared Hub mutates shared backing membership. | `add_withSharedHubTest` | shared `Hub<Register>` | `PASS` | Assertions | Public shared behavior only. |
| Condition | `DuplicateAdd` | Duplicate add preserves the duplicate policy and does not create duplicate membership. | `add_withDuplicateAddTest` | `Hub<Register>` | `PASS` | Assertions | No event-count assertion. |
| Condition | `ExistingParent` | Adding an object already owned by another master should clean up prior reverse membership. | none | two `Store` objects + `Register` | `PARTIAL` | Matrix + source | Existing-parent behavior is covered by `OAObjectTest.setProperty_withExistingParentTest`, but not by Hub detail add. |
| Condition | `ListenerMutation` | Supported listener mutation/reentrancy contract is explicit. | `add_withListenerMutationTest` | none | `DISABLED_TODO` | Disabled reason | Requires Vince decision. |

### `Hub.insert(...)`

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | `StandaloneHub` | Insert places the object at the requested index. | `insertTest` | `Hub<Register>` | `PASS` | Assertions | Baseline order/membership. |
| Wiring | `DetailHub` | Insert into detail Hub assigns reverse one-link to current master. | `insert_withDetailHubTest` | `Store.Registers` / `Register.Store` | `PASS` | Assertions | |
| Condition | `ExistingParent` | Insert should repair prior reverse many-Hub membership for an existing parent. | none | two `Store` objects + `Register` | `PARTIAL` | Matrix + source | Same gap as detail add. |

### `Hub.remove(...)`, `removeAt(...)`, `clear()`, and `removeAll()`

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | `StandaloneHub` | Remove deletes only Hub membership. | `removeTest` | `Hub<Register>` | `PASS` | Assertions | |
| Wiring | `DetailHub` | Removing from detail Hub clears the reverse one-link. | `remove_withDetailHubTest` | `Store.Registers` / `Register.Store` | `PASS` | Assertions | |
| Condition | `ActiveObjectRemoved` | Removing the active object clears or advances AO according to Hub rules. | `remove_withActiveObjectTest` | `Hub<Register>` | `PASS` | Assertions | Object remove path covered. |
| Condition | `RepeatedRemove` | Removing an absent object is a no-op with stable membership. | `remove_withRepeatedRemoveTest` | `Hub<Register>` | `PASS` | Assertions | |
| Wiring | `StandaloneHub` | Removing by index removes the object at that index. | `removeAtTest`, `removePositionTest` | `Hub<Register>` | `PASS` | Assertions | |
| Condition | `ActiveObjectRemoved` | Removing active object by index follows AO transition rules. | none | `Hub<Register>` | `PARTIAL` | Existing object-remove coverage | Position-specific AO branch is not separated. |
| Wiring | `StandaloneHub` | Clear removes all membership and clears AO. | `clearTest` | `Hub<Register>` | `PASS` | Assertions | |
| Wiring | `DetailHub` | Clearing detail Hub clears reverse references. | `clear_withDetailHubTest` | `Store.Registers` | `PASS` | Assertions | |
| Wiring | `SharedHub` | Clearing a shared Hub mutates shared backing data consistently. | none | shared `Hub<Register>` | `PARTIAL` | Matrix + source | Public shared add/remove exists; clear-specific shared row is absent. |
| Wiring | `StandaloneHub` | `removeAll()` follows clear behavior. | `removeAllTest` | `Hub<Register>` | `PASS` | Assertions | `Hub.removeAll()` delegates to `clear()`. |
| Wiring | `DetailHub` | `removeAll()` through a detail Hub follows detail clear cleanup. | none | `Store.Registers` | `PARTIAL` | `clear_withDetailHubTest` | Alias-like behavior is inferred, not method-specific. |

### `Hub.move(...)`, `swap(...)`, `replace(...)`, and Sorting

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | `StandaloneHub` | Move changes order without changing membership. | `moveTest` | `Hub<Register>` | `PASS` | Assertions | |
| Wiring | `StandaloneHub` | Swap exchanges positions without changing membership. | `swapTest` | `Hub<Register>` | `PASS` | Assertions | Valid addition. |
| Wiring | `StandaloneHub` | Replace swaps member at index and preserves size/order around it. | `replaceTest` | `Hub<Register>` | `PASS` | Assertions | Valid addition. |
| Wiring | `SortedHub` | Sort reorders by requested property and preserves membership. | `sortTest`, `resortTest`, `getSortPropertyTest`, `getSortAscTest` | `Hub<Register>` | `PASS` | Assertions | |
| Wiring | `DetailHub` | Sorting detail Hub does not corrupt master/detail references. | `sort_withDetailHubTest` | `Store.Registers` | `PASS` | Assertions | |
| State | `SortedState` / `UnsortedState` | Sort metadata tracks sorted state. | sort metadata tests | `Hub<Register>` | `PASS` | Assertions | |
| Derived | `FilteredHub` | Filtered Hub baseline produces accepted source membership. | `createFilteredHubTest` | `Hub<Register>` | `PARTIAL` | Assertions | Deeper derived-Hub mutation and AO behavior belongs in separate derived-Hub tests. |

### `Hub.setAO(...)`, `setPos(...)`, `getAO()`, and `getPos(...)`

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | `StandaloneHub` | Requested member becomes AO and position matches. | `setAOTest` | `hubRegister` | `PASS` | Assertions | |
| Condition | `Null` | AO clears and position becomes `-1`. | `setAO_withNullTest` | `hubRegister` | `PASS` | Assertions | |
| Condition | `SameActiveObject` | Reassigning same AO leaves AO/position unchanged. | `setAO_withSameActiveObjectTest` | `hubRegister` | `PASS` | Assertions | |
| Condition | `ObjectNotInHub` | Object not currently in Hub follows defined public outside-object behavior. | `setAO_withObjectNotInHubTest` | `hubRegister` | `QUESTIONABLE` | Assertions | Current test proves standalone outside object clears/does not select; it does not exercise adjust-master behavior that made the matrix row critical. |
| Wiring | `MasterHub` | Changing master AO changes detail contents. | `setAO_withMasterHubTest` | `Store.Registers` | `PASS` | Assertions | |
| Wiring | `DetailHub` | Selecting a detail object realigns master AO to the owner. | `setAO_withDetailHubTest` | `Store.Registers` | `PASS` | Assertions | |
| Wiring | `SharedHub` | AO propagates through shared active data. | `setAO_withSharedHubTest` | shared `Hub<Register>` | `PASS` | Assertions | |
| Wiring | `SharedHubAndDetailHub` | Shared AO change updates dependent detail Hub. | `setAO_withSharedHubAndDetailHubTest` | `Store.Registers` | `PASS` | Assertions | Exact event fan-out remains disabled separately. |
| Wiring | `LinkHubDirectReference` | Link-to selection updates link-from AO through direct object reference. | `setAO_withLinkHubTest`, `setLinkHubTest` | `Store` / `Register` | `PASS` | Assertions | |
| Wiring | `LinkHubAndDetailHub` | Linked selection realigns master/detail before final AO. | `setAO_withLinkHubAndDetailHubTest` | `Store` / `Register` / `Till` | `PASS` | Assertions | |
| Condition | `RecursiveLinkUpdate` | Link update terminates without recursive loop. | `setAO_withLinkHubAndRecursiveLinkUpdateTest` | none | `DISABLED_TODO` | Disabled reason | Fixture and event-count invariant require confirmation. |
| Condition | `DuplicateEventPrevention` | Shared/detail propagation does not duplicate AO/detail events. | `setAO_withSharedHubAndDetailHubAndDuplicateEventPreventionTest` | none | `DISABLED_TODO` | Disabled reason | Exact fan-out unresolved. |
| State | `HubWithoutAO` | AO accessors return null/negative position when no AO exists. | `getAOTest`, `getActiveObjectTest`, `getPosTest` | `Hub<Register>` | `PASS` | Assertions | |
| State | `HubWithAO` | AO accessors return selected object and position. | `getAOTest`, `getActiveObjectTest`, `getPos_withObjectTest` | `Hub<Register>` | `PASS` | Assertions | |
| Alias | `setActiveObject(object)` | Same public contract as `setAO(object)`. | `setAO*` group | same | `N/A` | Source delegation | Canonical alias coverage. |
| Alias | `setActiveObject(position)` | Same public contract as position APIs. | `setPosTest`, `setPos_withLinkHubTest` | same | `N/A` | Source delegation | Canonical alias coverage. |
| Alias | `getActiveObject()` | Same active object as `getAO()`. | `getActiveObjectTest`, `getAOTest` | same | `N/A` | Source wrapper | Both are present. |

### Master, Detail, Shared, and Link Setup Methods

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wiring | `MasterDetailHub` | `getDetailHub` binds detail Hub to master Hub and current AO. | `getDetailHubTest` | `Store.Registers` | `PASS` | Assertions | |
| Wiring | `MasterHub` | `setMasterHub` assigns master metadata. | `setMasterHubTest`, `getMasterHubTest`, `getMasterObjectTest`, `getMasterClassTest`, `hasDetailHubsTest`, `removeDetailHubTest` | `Store.Registers` | `PASS` | Assertions | Public metadata paths covered. |
| Wiring | `SharedHub` | `createSharedHub` and `setSharedHub` share membership. | `createSharedHubTest`, `setSharedHubTest`, `getSharedHubTest` | `Hub<Register>` | `PASS` | Assertions | |
| State | `SharedHubWithSharedAO` | Shared AO is propagated when requested. | `createSharedHub_withSharedActiveObjectTest` | `Hub<Register>` | `PASS` | Assertions | |
| State | `SharedHubWithoutSharedAO` | Shared data can keep independent AO when configured. | `setSharedHubTest` | `Hub<Register>` | `PARTIAL` | Assertions | Dedicated no-shared-AO scenario is not separated. |
| Condition | `InvalidSetupOrder` | Shared Hub cannot be made into a detail Hub through rejected setup order. | `setSharedHub_withRejectedMasterHubSetupTest` | `Hub<Store>` / `Hub<Register>` | `INVALID` | `assertThrows` | Specific invalid setup only; not all shared/detail combinations are invalid. |
| Wiring | `LinkHubDirectReference` | `setLinkHub` installs direct reference synchronization. | `setLinkHubTest` | `Store` / `Register` | `PASS` | Assertions | |
| Wiring | `LinkHubOnPosition` | `setLinkHubOnPos` selects by integer position. | `setLinkHubOnPosTest` | `Store` / selector object | `PASS` | Assertions | |
| Wiring | `LinkHubPropertyToProperty` | Property-to-property link selects matching from-Hub object. | `setLinkHub_withPropertyToPropertyTest` | scalar match fixture | `PASS` | Assertions | |
| Condition | `NoMatch` | No property match clears or leaves AO according to source contract. | `setLinkHub_withNoMatchTest` | property-link fixture | `PASS` | Assertions | |
| Condition | `MultipleMatches` | Intended duplicate-match behavior is unresolved. | `setLinkHub_withMultipleMatchesTest` | none | `DISABLED_TODO` | Disabled reason | Current implementation appears first-match, but matrix marks this unresolved. |
| Wiring | `LinkHubAndDetailHub` | Link-from detail Hub setup supports later master adjustment. | `setAO_withLinkHubAndDetailHubTest` | `Store` / `Register` / `Till` | `PARTIAL` | End-to-end assertions | No exact `setLinkHub_withDetailHubTest`; final public behavior is covered by `setAO_withLinkHubAndDetailHubTest`. |
| Action | `removeLinkHub` | Removing link Hub clears link metadata without corrupting Hub state. | `removeLinkHubTest` | link fixture | `PASS` | Assertions | Valid addition beyond original row. |

### Loading, Datasource, Remote/Sync, Listeners, and Serialization

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| State | `LoadedHub` | In-memory load/traversal preserves membership. | `loadAllDataTest` | `Hub<Register>` | `PASS` | Assertions | Not a datasource load. |
| State | `UnloadedHub` / `UnloadedRelationshipHub` | Loading materializes relationship data and loaded state. | `loadAllData_withUnloadedRelationshipHubTest` | none | `DISABLED_TODO` | Disabled reason | Datasource-backed fixture required. |
| Wiring | `DatasourceHub` | Select, passthrough select, refresh, save, delete use datasource contracts. | `selectTest`, `selectPassthruTest`, `refreshTest`, `saveAllTest`, `saveAll_withCascadeRuleTest`, `deleteAllTest` | none | `DISABLED_TODO` | Disabled reasons | Infrastructure deferred. |
| Wiring | `RemoteOrSyncHub` | Refresh message is sent under sync configuration. | `sendRefreshTest` | none | `DISABLED_TODO` | Disabled reason | Sync/remote fixture required. |
| Condition | `EventOrdering` | Public event callbacks occur in defined order. | `onChangeAOTest`, `onPropertyChangeTest`, `onAddTest`, `onNewListTest`, `onRemoveTest` | `Hub<Register>` | `PARTIAL` | Assertions | Individual callbacks are tested; full relative ordering remains incomplete. |
| Condition | `ListenerMutation` | Listener mutation contract is explicit. | `add_withListenerMutationTest` | none | `DISABLED_TODO` | Disabled reason | Requires contract decision. |
| Serialization | read-resolve | Serialized Hub round trip restores runtime state. | `readResolveTest` | none | `DISABLED_TODO` | Disabled reason | Serialization fixture required. |

## 4. OAObject Matrix Reconciliation

### Constructors, Identity, Lifecycle, Runtime State

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Lifecycle | `New` | New OAObjects expose new state. | `constructorTest`, `isNewTest`, `getNewTest` | `Register` | `PASS` | Assertions | |
| Lifecycle | `NotNew` | New state can be cleared. | `setNewTest` | `Register` | `PASS` | Assertions | |
| Lifecycle | `Changed` / `Unchanged` | Changed flag can be set/read/cleared. | `setChangedTest`, `getChangedTest`, `isChangedTest`, `setChangedFalseTest` | `Register` | `PASS` | Assertions | |
| Lifecycle | `Deleted` / `NotDeleted` | Deleted flag accessors agree. | `setDeletedTest`, `getDeletedTest`, `isDeletedTest`, `wasDeletedTest` | `Register` | `PASS` | Assertions | |
| State | `Cached` | Unique/cache lookup exposes current object identity where available. | `getUniqueInstanceTest`, `isUniqueTest` | `Register` | `PARTIAL` | Assertions + disabled TODO | Full datasource/cache uniqueness is disabled. |
| State | `Detached` | Detached/cached lifecycle boundary is explicit. | none | none | `MISSING` | Matrix minimum | No current test location for detached object semantics. |
| Identity | object key/GUID | Runtime GUID and object key are stable. | `getGuidTest`, `getObjectKeyTest`, `hashCodeTest`, `equalsTest`, `compareToTest` | `Register` | `PASS` | Assertions | |
| Runtime | OA runtime access | OA runtime and debug/static helpers follow public contract. | runtime/debug/version tests | default OA runtime | `PASS` | Assertions | `getOAVersionTest` remains `QUESTIONABLE` if patch text is not a public invariant. |

### `OAObject.setProperty(...)` and Scalar Property APIs

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Reference/property shape | `ScalarProperty` | Scalar values are stored and readable. | `setPropertyBooleanTest`, `setPropertyIntTest`, `setPropertyLongTest`, `setPropertyDoubleTest`, `setPropertyObjectTest`, `getPropertyTest` | `Register`, `Store` | `PASS` | Assertions | |
| Condition | `NumericConversion` | Compatible numeric assignment converts to target value. | `setPropertyNumericConversionTest` | `Register` | `PASS` | Assertions | |
| Condition | `DifferentValue` | New value replaces old value. | scalar set/get and compare-and-swap mismatch tests | `Register` | `PASS` | Assertions | |
| Condition | `SameValue` | Same-value assignment does not create unintended semantic changes/events. | none | `Register.Code` | `MISSING` | Original public semantic matrix | Needs listener/assertion fixture. |
| Condition | `InvalidType` | Invalid assignment fails predictably and preserves prior value. | none | scalar property | `MISSING` | Original public semantic matrix | |
| Condition | `Null` | Null scalar/reference state is represented accurately. | `setNullTest`, `getNullTest`, `isNullTest` | `Register` | `PASS` | Assertions | Primitive-null helper remains disabled. |
| State | `NullProperty` / `NonNullProperty` | Null-state accessors distinguish null from assigned values. | `setNullTest`, `isNullTest`, scalar set tests | `Register` | `PASS` | Assertions | |
| Operation | property removal | Removing a stored property follows OA null/loaded semantics. | `removePropertyTest` | none | `DISABLED_TODO` | Disabled reason | Likely implementable in-memory. |

### OAObject Reference Shapes

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Reference shape | `ManyToOne` / `OneToMany` | Assigning `Register.Store` adds register to `Store.Registers`. | `setProperty_withOneToManyTest` | `Store.Registers` / `Register.Store` | `PASS` | Assertions + metadata | |
| Reference shape | `ReverseOneLink` / `ReverseManyLink` | Reverse sides are kept consistent after one-link assignment. | `setProperty_withOneToManyTest`, `setHub_withReverseOneLinkTest` | `Store.Registers` | `PASS` | Assertions | |
| Reference shape | `ExistingParent` | Reassigning parent removes prior reverse membership and adds new. | `setProperty_withExistingParentTest` | two `Store` objects + `Register` | `PASS` | Assertions | |
| Reference shape | `NullReference` | Clearing reference clears reverse membership. | `setProperty_withNullReferenceTest` | `Store.Registers` | `PASS` | Assertions | |
| Reference shape | `OneToOne` | Assigning one-to-one scalar reference updates reverse scalar side. | `setProperty_withOneToOneTest` | `Store.Address` / `Address.Store` | `PASS` | Assertions + metadata | |
| Reference shape | `OptionalReference` | Optional references can be null without deleting unrelated objects. | `delete_withOptionalReferenceTest` | none | `DISABLED_TODO` | Disabled reason | Delete-specific optional behavior unresolved. |
| Reference shape | `OwnedReference` / `CascadeDelete` | Delete follows ownership/cascade metadata. | `delete_withOwnedReferenceTest` | none | `DISABLED_TODO` | Disabled reason | OAPOS has owned relations, but datasource/delete fixture is not implemented. |
| Reference shape | `CascadeSave` | Save traverses cascade-save references. | `saveCascadeTest`, `saveAllTest` | none | `DISABLED_TODO` | Disabled reason | Datasource fixture required. |
| Reference shape | `RecursiveReference` | Recursive hierarchy operations traverse stable parent/child relationships. | none | `CatalogCategory` | `MISSING` | OAPOS metadata | `hierFindTest` is disabled but not tied to verified recursive fixture. |

### `OAObject.getObject(...)`, `getHub(...)`, `setHub(...)`, Loaded State, and Object Keys

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| State | `LoadedReference` | Loaded one-link returns materialized object. | `getObject_withLoadedReferenceTest`, `isLoaded_withLoadedReferenceTest` | `Register.Store` | `PASS` | Assertions | |
| State | `UnloadedReference` | Unloaded one-link follows key/load contract without forcing incorrect state. | `getObject_withUnloadedReferenceTest`, `isLoaded_withUnloadedReferenceTest` | none | `DISABLED_TODO` | Disabled reason | Datasource/object-key fixture missing. |
| Reference shape | `ObjectKeyReference` | Key-only reference identity is exposed without requiring loaded object. | `getReferenceObjectKeyTest`, fkey tests | `Register.StoreId` | `PARTIAL` | Assertions | Loaded/fkey baseline exists; key-only/unloaded distinction is not proven. |
| State | `InitializedManyHub` | Existing many-Hub is returned and maintains membership. | `getHub_withManyHubInitializedTest` | `Store.Registers` | `PASS` | Assertions | |
| State | `UninitializedManyHub` / `ManyHubNotInitialized` | First generated many-Hub access follows expected initialization/loaded-state contract. | `getHub_withManyHubNotInitializedTest` | `Store.Registers` | `QUESTIONABLE` | Assertions | Test name conflicts with generated-Hub auto-initialization behavior; invariant needs clarification. |
| Reference shape | `ReverseOneLink` | Assigned Hub additions maintain reverse one-link. | `setHub_withReverseOneLinkTest` | `Store.Registers` | `PASS` | Assertions | |
| Service-only | many-Hub add/remove internals | Generated many-Hub add/remove is service-owned, not a direct OAObject public method. | covered by Hub/OAObject reference tests | `Store.Registers` | `N/A` | Source ownership | Service tests should cover internals later. |

### Copy, Save, Delete, Refresh, Serialization, Sync

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Copy | scalar copy | Scalar copy operations preserve scalar state. | `copyTest`, `copyIntoTest`, `createCopyTest` | `Register` | `PASS` | Assertions | |
| Copy | `CopyWithReferences` | Copy operations follow reference-copy contract. | none | `Store.Registers`, `Store.Address` | `MISSING` | Original public semantic matrix | Needs explicit reference-copy fixture. |
| Datasource | save/cascade save | Save and cascade-save follow metadata. | `saveTest`, `saveCascadeTest`, `saveAllTest` | none | `DISABLED_TODO` | Disabled reasons | Datasource fixture required. |
| Datasource | delete/owned delete | Delete applies owned/cascade-delete behavior and reverse cleanup. | `deleteTest`, `delete_withOwnedReferenceTest`, `delete_withOptionalReferenceTest` | none | `DISABLED_TODO` | Disabled reasons | Verified fixture not implemented. |
| Datasource | refresh | Refresh preserves or reloads references according to datasource contract. | `refreshTest`, `refreshLinkTest`, `refreshLinkPropertyTest`, `refreshMissingLinkPropertyTest` | in-memory only | `PARTIAL` | No-throw/local assertions | Does not prove datasource refresh semantics. |
| Reference loading | load references | Loading references materializes configured relationships and loaded state. | `loadReferencesTest`, overload tests | in-memory only | `PARTIAL` | Assertions/no-throw | Does not prove unloaded materialization. |
| Serialization | read-resolve | Serialized object round trip restores key/reference/runtime state. | `readResolveTest` | none | `DISABLED_TODO` | Disabled reason | Serialization fixture required. |
| Sync | sync apply | Remote/sync mutation application preserves local public state. | none | none | `MISSING` | Matrix | No explicit sync-apply placeholder in OAObjectTest. |
| Datasource | datasource load | Datasource load reconstructs identity and references. | none | none | `MISSING` | Matrix | No explicit datasource-load placeholder in OAObjectTest. |

### Validation, Permissions, Events, Locking, Fkeys, Name/Value, FriendAccess

| Category | Matrix scenario | Expected public invariant | Test method | Fixture | Status | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rules | validation/enabled/visible/command | Callback carrier exposes rule decisions. | validation/rules callback tests | `Register` | `PASS` | Assertions | Rule ordering/override behavior remains service-test work. |
| Event | property events | before/after/local/new-list event contracts are defined. | many `fire*Test` methods | none | `DISABLED_TODO` | Disabled reasons | Event recorder/helper subclass required. |
| Locking | lock/unlock/isLocked | Lock state is observable and reversible. | lock tests | `Register` | `PASS` | Assertions | |
| Fkey | foreign-key helpers | Fkey helper maintains reference identity state. | fkey tests | `Register.StoreId` | `PARTIAL` | Assertions + disabled protected overload | Protected link-info overload still disabled. |
| Name/value | enum/string views | OA name/value pattern exposes string/enum state. | name/value tests | OAPOS name/value model values | `PASS` | Assertions | |
| FriendAccess | internal friend methods | FriendAccess exposes controlled internal state changes. | friendAccess tests | `Register` | `PASS` | Assertions | FriendAccess event firing remains disabled. |
| Service-only | weak-reference storage | Weak-reference storage is service-owned. | none in OAObjectTest | none | `N/A` | Source ownership | Belongs in `OAObjectPropertyServiceTest`. |

## 5. Original Matrix Row Ledger

### Hub Ledger

| ID | Original row/description | Target class | Production method | Scenario | Current representation | Status | Reason |
| --- | --- | --- | --- | --- | --- | --- | --- |
| HUB-001 | Set AO selects requested object | `HubTest` | `setAO` | `StandaloneHub` | `setAOTest` | `PASS` | Assertions prove AO and position. |
| HUB-002 | Set AO null clears AO | `HubTest` | `setAO` | `Null` | `setAO_withNullTest` | `PASS` | Assertions prove null AO and `-1` position. |
| HUB-003 | Same AO is stable | `HubTest` | `setAO` | `SameActiveObject` | `setAO_withSameActiveObjectTest` | `PASS` | Assertions prove no change. |
| HUB-004 | Object outside Hub follows adjust-master behavior | `HubTest` | `setAO` | `ObjectNotInHub` | `setAO_withObjectNotInHubTest` | `QUESTIONABLE` | Test covers standalone outside object only, not adjust-master. |
| HUB-005 | Detail AO realigns master | `HubTest` | `setAO` | `DetailHub` | `setAO_withDetailHubTest` | `PASS` | Assertions prove master AO changes to owner. |
| HUB-006 | Shared AO propagates | `HubTest` | `setAO` | `SharedHub` | `setAO_withSharedHubTest` | `PASS` | Assertions prove shared AO behavior. |
| HUB-007 | Shared/detail AO updates dependent detail | `HubTest` | `setAO` | `SharedHubAndDetailHub` | `setAO_withSharedHubAndDetailHubTest` | `PASS` | State outcome proved; event fan-out row separate. |
| HUB-008 | Link Hub direct reference updates AO/property | `HubTest` | `setAO` | `LinkHubDirectReference` | `setAO_withLinkHubTest` | `PASS` | Assertions prove linked state. |
| HUB-009 | Link Hub plus detail realigns master/detail | `HubTest` | `setAO` | `LinkHubAndDetailHub` | `setAO_withLinkHubAndDetailHubTest` | `PASS` | Assertions prove final public graph. |
| HUB-010 | Position selects object | `HubTest` | `setPos` | `StandaloneHub` | `setPosTest` | `PASS` | Assertions prove AO/position. |
| HUB-011 | Position in linked Hub updates link | `HubTest` | `setPos` | `LinkHub` | `setPos_withLinkHubTest` | `PASS` | Assertions prove link result. |
| HUB-012 | Add baseline | `HubTest` | `add` | `StandaloneHub` | `addTest` | `PASS` | Membership assertions. |
| HUB-013 | Add detail reverse link | `HubTest` | `add` | `DetailHub` | `add_withDetailHubTest` | `PASS` | Reverse one-link assertion. |
| HUB-014 | Add through shared Hub | `HubTest` | `add` | `SharedHub` | `add_withSharedHubTest` | `PASS` | Shared membership assertion. |
| HUB-015 | Duplicate add | `HubTest` | `add` | `DuplicateAdd` | `add_withDuplicateAddTest` | `PASS` | Duplicate policy asserted. |
| HUB-016 | Insert baseline | `HubTest` | `insert` | `StandaloneHub` | `insertTest` | `PASS` | Position/order asserted. |
| HUB-017 | Insert detail reverse link | `HubTest` | `insert` | `DetailHub` | `insert_withDetailHubTest` | `PASS` | Reverse one-link asserted. |
| HUB-018 | Remove baseline | `HubTest` | `remove` | `StandaloneHub` | `removeTest` | `PASS` | Membership asserted. |
| HUB-019 | Remove detail clears reverse | `HubTest` | `remove` | `DetailHub` | `remove_withDetailHubTest` | `PASS` | Reverse one-link clear asserted. |
| HUB-020 | Remove active object | `HubTest` | `remove` | `ActiveObjectRemoved` | `remove_withActiveObjectTest` | `PASS` | AO transition asserted. |
| HUB-021 | Repeated remove | `HubTest` | `remove` | `RepeatedRemove` | `remove_withRepeatedRemoveTest` | `PASS` | No-op state asserted. |
| HUB-022 | Remove at index | `HubTest` | `removeAt` | `StandaloneHub` | `removeAtTest` | `PASS` | Indexed removal asserted. |
| HUB-023 | Clear baseline | `HubTest` | `clear` | `StandaloneHub` | `clearTest` | `PASS` | Empty and AO-clear asserted. |
| HUB-024 | RemoveAll baseline | `HubTest` | `removeAll` | `StandaloneHub` | `removeAllTest` | `PASS` | Empty state asserted. |
| HUB-025 | Clear detail cleanup | `HubTest` | `clear` | `DetailHub` | `clear_withDetailHubTest` | `PASS` | Reverse cleanup asserted. |
| HUB-026 | Move baseline | `HubTest` | `move` | `StandaloneHub` | `moveTest` | `PASS` | Order asserted. |
| HUB-027 | Sort baseline | `HubTest` | `sort` | `StandaloneHub` | `sortTest` | `PASS` | Sorted order asserted. |
| HUB-028 | Sort detail | `HubTest` | `sort` | `DetailHub` | `sort_withDetailHubTest` | `PASS` | References preserved. |
| HUB-029 | Load in-memory Hub | `HubTest` | `loadAllData` | `LoadedHub` | `loadAllDataTest` | `PASS` | In-memory state asserted. |
| HUB-030 | Load unloaded relationship Hub | `HubTest` | `loadAllData` | `UnloadedRelationshipHub` | `loadAllData_withUnloadedRelationshipHubTest` | `DISABLED_TODO` | Datasource fixture required. |
| HUB-031 | Set shared Hub | `HubTest` | `setSharedHub` | `SharedHub` | `setSharedHubTest` | `PASS` | Shared state asserted. |
| HUB-032 | Reject shared-as-detail setup | `HubTest` | `setSharedHub` / detail setup | `InvalidSetupOrder` | `setSharedHub_withRejectedMasterHubSetupTest` | `INVALID` | Public path rejects setup. |
| HUB-033 | Set link Hub direct | `HubTest` | `setLinkHub` | `LinkHubDirectReference` | `setLinkHubTest` | `PASS` | Linked behavior asserted. |
| HUB-034 | Set link Hub plus detail | `HubTest` | `setLinkHub` | `LinkHubAndDetailHub` | `setAO_withLinkHubAndDetailHubTest` | `PARTIAL` | End-to-end result covered; setup-method row not separated. |
| HUB-035 | Create detail Hub | `HubTest` | `getDetailHub` | `MasterDetailHub` | `getDetailHubTest` | `PASS` | Detail binding asserted. |
| HUB-036 | Master AO updates detail | `HubTest` | `setAO` | `MasterHub` | `setAO_withMasterHubTest` | `PASS` | Detail contents asserted. |
| HUB-037 | Link property change direct/position/property modes | `HubTest` | `setLinkHub` / `setAO` | `LinkHub` modes | `setLinkHubTest`, `setLinkHubOnPosTest`, `setLinkHub_withPropertyToPropertyTest` | `PASS` | Public outcomes asserted. |
| HUB-038 | Link no match | `HubTest` | `setLinkHub` | `NoMatch` | `setLinkHub_withNoMatchTest` | `PASS` | No-match outcome asserted. |
| HUB-039 | Link multiple matches | `HubTest` | `setLinkHub` | `MultipleMatches` | `setLinkHub_withMultipleMatchesTest` | `DISABLED_TODO` | Contract unresolved. |
| HUB-040 | Existing parent reference change affects detail Hubs | `OAObjectTest` | `setProperty` | `ExistingParent` | `setProperty_withExistingParentTest` | `PASS` | Public object reference row, not Hub API-specific. |
| HUB-041 | Listener mutation during Hub event | `HubTest` | `add` | `ListenerMutation` | `add_withListenerMutationTest` | `DISABLED_TODO` | Contract unresolved. |
| HUB-042 | Recursive link update terminates | `HubTest` | `setAO` | `RecursiveLinkUpdate` | `setAO_withLinkHubAndRecursiveLinkUpdateTest` | `DISABLED_TODO` | Fixture and invariant unresolved. |
| HUB-COND-001 | Null condition | `HubTest` | `setAO` | `Null` | `setAO_withNullTest` | `PASS` | Explicit null assertions. |
| HUB-COND-002 | Same active object condition | `HubTest` | `setAO` | `SameActiveObject` | `setAO_withSameActiveObjectTest` | `PASS` | Explicit same-AO assertions. |
| HUB-COND-003 | Object not in Hub condition | `HubTest` | `setAO` | `ObjectNotInHub` | `setAO_withObjectNotInHubTest` | `QUESTIONABLE` | Critical adjust-master branch not exercised. |
| HUB-COND-004 | No link match condition | `HubTest` | `setLinkHub` | `NoMatch` | `setLinkHub_withNoMatchTest` | `PASS` | Assertions prove public outcome. |
| HUB-COND-005 | Multiple link matches condition | `HubTest` | `setLinkHub` | `MultipleMatches` | `setLinkHub_withMultipleMatchesTest` | `DISABLED_TODO` | Vince decision required. |
| HUB-COND-006 | Existing parent condition | `HubTest` / `OAObjectTest` | detail add/insert and object set | `ExistingParent` | `setProperty_withExistingParentTest` | `PARTIAL` | Object set covered; detail add/insert rows absent. |
| HUB-COND-007 | Duplicate add condition | `HubTest` | `add` | `DuplicateAdd` | `add_withDuplicateAddTest` | `PASS` | Assertions prove no duplicate membership. |
| HUB-COND-008 | Repeated remove condition | `HubTest` | `remove` | `RepeatedRemove` | `remove_withRepeatedRemoveTest` | `PASS` | Assertions prove no-op state. |
| HUB-COND-009 | Active object removed condition | `HubTest` | `remove` | `ActiveObjectRemoved` | `remove_withActiveObjectTest` | `PASS` | Object remove covered; removeAt branch partial in method matrix. |
| HUB-COND-010 | Current master does not own detail | `HubTest` | `setAO` | `CurrentMasterDoesNotOwnDetail` | `setAO_withDetailHubTest` | `PASS` | Master realignment asserted. |
| HUB-COND-011 | Recursive link update condition | `HubTest` | `setAO` | `RecursiveLinkUpdate` | `setAO_withLinkHubAndRecursiveLinkUpdateTest` | `DISABLED_TODO` | Fixture/invariant unresolved. |
| HUB-COND-012 | Duplicate event prevention | `HubTest` | `setAO` | `DuplicateEventPrevention` | `setAO_withSharedHubAndDetailHubAndDuplicateEventPreventionTest` | `DISABLED_TODO` | Event fan-out unresolved. |
| HUB-COND-013 | Event ordering | `HubTest` | event-producing methods | `EventOrdering` | event callback tests | `PARTIAL` | Individual callbacks tested, not full ordering. |
| HUB-COND-014 | Listener mutation | `HubTest` | `add` | `ListenerMutation` | `add_withListenerMutationTest` | `DISABLED_TODO` | Contract unresolved. |
| HUB-COND-015 | Server-only sync message | `HubTest` | `sendRefresh` | `ServerOnlySyncMessage` | `sendRefreshTest` | `DISABLED_TODO` | Sync fixture required. |
| HUB-ALIAS-001 | `setActiveObject(Object)` alias | `HubTest` | `setActiveObject` | alias | `setAO*` group | `N/A` | Canonical alias coverage verified from source. |
| HUB-ALIAS-002 | `setActiveObject(int)` alias | `HubTest` | `setActiveObject` | alias | `setPos*` group | `N/A` | Canonical position coverage verified from source. |
| HUB-ALIAS-003 | `getActiveObject()` alias | `HubTest` | `getActiveObject` | alias | `getActiveObjectTest`, `getAOTest` | `N/A` | Alias and direct tests both exist. |

### OAObject Ledger

| ID | Original row/description | Target class | Production method | Scenario | Current representation | Status | Reason |
| --- | --- | --- | --- | --- | --- | --- | --- |
| OBJ-001 | Set scalar property | `OAObjectTest` | `setProperty` | `ScalarProperty` | scalar set tests | `PASS` | Assertions prove storage/readback. |
| OBJ-002 | Set reference null | `OAObjectTest` | `setProperty` | `NullReference` | `setProperty_withNullReferenceTest` | `PASS` | Reverse membership clear asserted. |
| OBJ-003 | Set one-link updates many-Hub | `OAObjectTest` | `setProperty` | `OneToMany` | `setProperty_withOneToManyTest` | `PASS` | Reverse many-Hub asserted. |
| OBJ-004 | Reassign existing parent | `OAObjectTest` | `setProperty` | `ExistingParent` | `setProperty_withExistingParentTest` | `PASS` | Old/new reverse Hubs asserted. |
| OBJ-005 | Get scalar property | `OAObjectTest` | `getProperty` | `ScalarProperty` | `getPropertyTest` | `PASS` | Readback asserted. |
| OBJ-006 | Get key-only property/reference | `OAObjectTest` | `getProperty` / key helpers | `ObjectKeyReference` | fkey/key tests | `PARTIAL` | Key-only unloaded distinction not proved. |
| OBJ-007 | Set one-to-one reference | `OAObjectTest` | `setProperty` | `OneToOne` | `setProperty_withOneToOneTest` | `PASS` | Reverse scalar side asserted. |
| OBJ-008 | Get loaded reference object | `OAObjectTest` | `getObject` | `LoadedReference` | `getObject_withLoadedReferenceTest` | `PASS` | Loaded reference asserted. |
| OBJ-009 | Get unloaded reference object | `OAObjectTest` | `getObject` | `UnloadedReference` | `getObject_withUnloadedReferenceTest` | `DISABLED_TODO` | Datasource/object-key fixture required. |
| OBJ-010 | Set Hub reverse one-link | `OAObjectTest` | `setHub` | `ReverseOneLink` | `setHub_withReverseOneLinkTest` | `PASS` | Hub add reverse link asserted. |
| OBJ-011 | Get many-Hub not initialized | `OAObjectTest` | `getHub` | `ManyHubNotInitialized` | `getHub_withManyHubNotInitializedTest` | `QUESTIONABLE` | Vocabulary conflicts with generated auto-initialization. |
| OBJ-012 | Get many-Hub initialized | `OAObjectTest` | `getHub` | `ManyHubInitialized` | `getHub_withManyHubInitializedTest` | `PASS` | Existing Hub/membership asserted. |
| OBJ-013 | Service many-Hub add path | service tests | service-owned | `ReverseOneLink` | Hub/OAObject public tests | `N/A` | No direct OAObject public method; service test later. |
| OBJ-014 | Service many-Hub remove path | service tests | service-owned | `ReverseOneLink` | Hub/OAObject public tests | `N/A` | No direct OAObject public method; service test later. |
| OBJ-015 | Set null | `OAObjectTest` | `setNull` | `NullProperty` | `setNullTest` | `PASS` | Null state asserted. |
| OBJ-016 | Is null | `OAObjectTest` | `isNull` | `NullProperty` | `isNullTest` | `PASS` | Null state asserted. |
| OBJ-017 | Reference-null methods | `OAObjectTest` | reference-null accessors | loaded/unloaded/key | `isReferenceNullTest`, `isReferenceObjectNullTest` | `PARTIAL` | Loaded/null covered; unloaded/key-only not proved. |
| OBJ-018 | Loaded reference state | `OAObjectTest` | `isLoaded`, `isPropertyLoaded` | `LoadedReference` | `isLoaded_withLoadedReferenceTest` | `PASS` | Loaded one-link asserted. |
| OBJ-019 | Unloaded reference state | `OAObjectTest` | `isLoaded` | `UnloadedReference` | `isLoaded_withUnloadedReferenceTest` | `DISABLED_TODO` | Fixture missing. |
| OBJ-020 | Reference object key | `OAObjectTest` | `getReferenceObjectKey` | `ObjectKeyReference` | `getReferenceObjectKeyTest` | `PARTIAL` | Loaded/fkey baseline only. |
| OBJ-021 | Foreign-key helpers | `OAObjectTest` | fkey helpers | foreign-key reference | fkey tests + disabled overload | `PARTIAL` | Protected link-info branch disabled. |
| OBJ-022 | Copy with references | `OAObjectTest` | `copy/createCopy/copyInto` | `CopyWithReferences` | none | `MISSING` | Scalar copy only. |
| OBJ-023 | Cascade save | `OAObjectTest` | `save` | `CascadeSave` | `saveTest`, `saveCascadeTest`, `saveAllTest` | `DISABLED_TODO` | Datasource fixture required. |
| OBJ-024 | Owned/cascade delete | `OAObjectTest` | `delete` | `OwnedReference` / `CascadeDelete` | `deleteTest`, `delete_withOwnedReferenceTest` | `DISABLED_TODO` | Delete fixture required. |
| OBJ-025 | Optional reference delete | `OAObjectTest` | `delete` | `OptionalReference` | `delete_withOptionalReferenceTest` | `DISABLED_TODO` | Optional reference contract unresolved. |
| OBJ-026 | Refresh loaded reference | `OAObjectTest` | `refresh` | `LoadedReference` | refresh tests | `PARTIAL` | In-memory/no-throw only. |
| OBJ-027 | Load references | `OAObjectTest` | `loadReferences` | `UnloadedReference` | loadReferences tests | `PARTIAL` | Unloaded materialization not proved. |
| OBJ-028 | Serialization | `OAObjectTest` | `readResolve` | serialization | `readResolveTest` | `DISABLED_TODO` | Serialization fixture required. |
| OBJ-029 | Sync apply | `OAObjectTest` | sync-related public state | sync | none | `MISSING` | No explicit placeholder. |
| OBJ-030 | Datasource load | `OAObjectTest` | datasource-backed load | datasource | none | `MISSING` | No explicit placeholder. |
| OBJ-031 | Service CAS reference key | service tests | `setPropertyCAS` | reference key | public compareAndSwap scalar tests | `N/A` | Service-owned reference-key row. |
| OBJ-032 | Service weak-reference storage | service tests | weak storage | weak reference | none | `N/A` | Service-owned row. |
| OBJ-DIM-001 | Same-value assignment | `OAObjectTest` | `setProperty` | `SameValue` | none | `MISSING` | Original public semantic matrix gap. |
| OBJ-DIM-002 | Invalid type assignment | `OAObjectTest` | `setProperty` | `InvalidType` | none | `MISSING` | Original public semantic matrix gap. |
| OBJ-DIM-003 | Numeric conversion | `OAObjectTest` | `setProperty` | `NumericConversion` | `setPropertyNumericConversionTest` | `PASS` | Assertions prove conversion. |
| OBJ-DIM-004 | Recursive reference | `OAObjectTest` | hierarchy/reference traversal | `RecursiveReference` | `hierFindTest` disabled but not fixture-specific | `MISSING` | OAPOS fixture exists but not implemented. |
| OBJ-DIM-005 | Detached state | `OAObjectTest` | lifecycle/cache | `Detached` | none | `MISSING` | No explicit detached invariant. |

## 6. Tests Beyond the Original Matrix

| Test area | Classification | Reason |
| --- | --- | --- |
| Hub collection facade methods: arrays, lists, getAt, elementAt, contains, indexOf | `VALID_ADDITION` | These are public Hub APIs and provide useful public semantic coverage. |
| Hub dynamic properties, refresh flag, changed flag, enabled flag, addHub, null-on-remove | `VALID_ADDITION` | Public methods declared by `Hub`; tests are source-mapped. |
| Hub listener convenience methods | `VALID_ADDITION` | Public event registration APIs; deeper event ordering remains partial/disabled. |
| Hub derived filtered baseline | `VALID_ADDITION` | Useful public creation baseline, but deeper derived behavior belongs in dedicated derived-Hub tests. |
| Hub `finalizeTest` | `QUESTIONABLE` | Disabled because finalization is not a stable public invariant. |
| OAObject version/debug/runtime helpers | `VALID_ADDITION` | Public/static OAObject APIs. |
| OAObject FriendAccess tests | `VALID_ADDITION` | Directly declared nested API; event-firing branch remains disabled. |
| OAObject local remote/server-only checks | `VALID_ADDITION` | Public methods expose local runtime behavior; sync apply remains missing. |
| OAObject finalize-save behavior | `QUESTIONABLE` | Could freeze current implementation unless Vince confirms invariant. |

## 7. Missing Matrix Rows

| Priority | Class | Production method | Scenario | Required test name | Suggested OAPOS fixture | Invariant |
| --- | --- | --- | --- | --- | --- | --- |
| P0 | `HubTest` | `setAO` | `ObjectNotInHub` with master adjustment | `setAO_withDetailHubAndObjectNotInHubTest` | two-store detail fixture | Outside detail object either realigns master or is rejected/cleared according to source. |
| P1 | `HubTest` | `add` | `DetailHubAndExistingParent` | `add_withDetailHubAndExistingParentTest` | two `Store` objects, one `Register` | Adding to new detail Hub removes prior reverse membership. |
| P1 | `HubTest` | `insert` | `DetailHubAndExistingParent` | `insert_withDetailHubAndExistingParentTest` | two `Store` objects, one `Register` | Insert follows add's parent-repair contract. |
| P2 | `HubTest` | `removeAt` | `ActiveObjectRemoved` | `removeAt_withActiveObjectTest` | `Hub<Register>` | Position removal follows AO transition rules. |
| P1 | `HubTest` | `clear` | `SharedHub` | `clear_withSharedHubTest` | shared `Hub<Register>` | Clearing a shared Hub mutates shared backing membership consistently. |
| P2 | `HubTest` | `removeAll` | `DetailHub` | `removeAll_withDetailHubTest` | `Store.Registers` | Alias method follows detail cleanup behavior. |
| P2 | `HubTest` | `setPos` | invalid index | `setPos_withInvalidIndexTest` | `Hub<Register>` | Boundary behavior is explicit and state remains stable. |
| P1 | `OAObjectTest` | `setProperty` | `SameValue` | `setProperty_withSameValueTest` | `Register.Code` + listener | Same-value assignment does not create unintended semantic change/events. |
| P1 | `OAObjectTest` | `setProperty` | `InvalidType` | `setProperty_withInvalidTypeTest` | scalar property | Invalid assignment fails predictably and preserves prior value. |
| P1 | `OAObjectTest` | `copy/createCopy/copyInto` | `CopyWithReferences` | `createCopy_withReferencesTest`, `copyInto_withReferencesTest` | `Store.Registers`, `Store.Address` | Reference copy contract is explicit. |
| P2 | `OAObjectTest` | hierarchy/reference traversal | `RecursiveReference` | `hierFind_withRecursiveReferenceTest` | `CatalogCategory` parent/children | Recursive traversal follows metadata safely. |
| P3 | `OAObjectTest` | sync apply | sync/remoting | sync-specific TODO test | sync fixture | Applied remote mutation preserves local public state. |
| P3 | `OAObjectTest` | datasource load | datasource | datasource-specific TODO test | deterministic datasource | Load reconstructs identity and references. |

## 8. Partial and Questionable Rows

| Test method | Claimed invariant | Actual assertions | Issue | Status | Recommended correction |
| --- | --- | --- | --- | --- | --- |
| `setAO_withObjectNotInHubTest` | Object outside Hub follows object-not-in-Hub behavior. | Standalone Hub does not select outside object. | Does not exercise adjust-master branch. | `QUESTIONABLE` | Add `setAO_withDetailHubAndObjectNotInHubTest`. |
| `setLinkHub` + detail row | Link-from detail Hub can adjust master/detail during link setup. | End-to-end linked detail selection is asserted through `setAO_withLinkHubAndDetailHubTest`. | No exact setup-method row. | `PARTIAL` | Add `setLinkHub_withDetailHubTest` or document canonical public row. |
| `ExistingParent` Hub condition | Existing-parent cleanup works for detail add/insert. | Object reference reassignment is asserted in `OAObjectTest`. | Hub detail add/insert variants absent. | `PARTIAL` | Add detail add/insert existing-parent tests. |
| `EventOrdering` Hub condition | Hub event order is defined. | Individual listener callbacks are asserted. | Relative event ordering is not asserted. | `PARTIAL` | Add event recorder after contract confirmation. |
| `getHub_withManyHubNotInitializedTest` | Many-Hub not initialized behavior. | Generated getter auto-initialization behavior is asserted. | Scenario token likely incorrect. | `QUESTIONABLE` | Rename/clarify generated many-Hub initialization invariant. |
| `getReferenceObjectKeyTest` and fkey tests | ObjectKeyReference is fully covered. | Loaded/fkey state is asserted. | Key-only/unloaded reference is not proved. | `PARTIAL` | Add key-only fixture. |
| `refresh*Test` | Refresh semantics are covered. | Mostly local/no-throw or in-memory assertions. | Datasource refresh semantics are not proved. | `PARTIAL` | Use datasource fixture or narrow invariant. |
| `loadReferences*Test` | Reference loading is covered. | Loaded/no-throw behavior only. | Unloaded materialization is not proved. | `PARTIAL` | Pair with unloaded reference fixture. |
| `setFkeyProperty*Test` | Fkey helpers are covered. | Public/simple fkey state asserted. | Protected link-info overload disabled. | `PARTIAL` | Implement link-info fixture. |

## 9. Disabled TODO Reconciliation

### HubTest Disabled TODOs

| Test method | Production method | Scenario | Reason disabled | Original row represented | Can complete in memory? | Needs |
| --- | --- | --- | --- | --- | --- | --- |
| `loadAllData_withUnloadedRelationshipHubTest` | `loadAllData` | `UnloadedRelationshipHub` | datasource-backed fixture required | HUB-030 | No | datasource/key fixture |
| `saveAllTest` | `saveAll` | datasource save | datasource fixture required | datasource row | No | datasource |
| `saveAll_withCascadeRuleTest` | `saveAll(int)` | cascade save | datasource fixture required | datasource row | No | datasource/cascade |
| `deleteAllTest` | `deleteAll` | cascade delete | ownership/cascade fixture required | datasource row | No | datasource/delete |
| `setAO_withLinkHubAndRecursiveLinkUpdateTest` | `setAO` | `RecursiveLinkUpdate` | recursive link fixture and invariant need confirmation | HUB-042 | Possibly | fixture + Vince decision |
| `setRootHubTest` | `setRootHub` | root Hub | public contract unclear | beyond matrix | Possibly | Vince decision |
| `getRootHubTest` | `getRootHub` | root Hub | public contract unclear | beyond matrix | Possibly | Vince decision |
| `onBeforeRefreshTest` | listener API | refresh event | deterministic refresh event fixture required | event row | No | datasource/refresh fixture |
| `add_withListenerMutationTest` | `add` | `ListenerMutation` | contract requires confirmation | HUB-041 | Yes after decision | Vince decision |
| `setAO_withSharedHubAndDetailHubAndDuplicateEventPreventionTest` | `setAO` | `DuplicateEventPrevention` | event fan-out requires confirmation | HUB-COND-012 | Yes after decision | event recorder + decision |
| `resequenceTest` | `resequence` | auto sequence | deterministic sequence fixture required | beyond matrix | Possibly | sequence fixture |
| `setAutoMatchTest` | `setAutoMatch` | auto match | fixture and propagation direction unclear | beyond matrix | Possibly | fixture + source decision |
| `selectTest` | `select` | `DatasourceHub` | datasource select fixture required | datasource row | No | datasource |
| `setLinkHub_withMultipleMatchesTest` | `setLinkHub` | `MultipleMatches` | duplicate-match behavior unresolved | HUB-039 | Yes after decision | Vince decision |
| `sendRefreshTest` | `sendRefresh` | `RemoteOrSyncHub` | sync/remote fixture required | HUB-COND-015 | No | sync fixture |
| `refreshTest` | `refresh` | datasource refresh | datasource fixture required | datasource row | No | datasource |
| `readResolveTest` | `readResolve` | serialization | serialization fixture required | serialization row | No | serialization fixture |
| `finalizeTest` | `finalize` | finalization | not stable public invariant | beyond matrix | No | likely exclude/decision |
| `addTriggerListenerTest` | `addTriggerListener` | trigger listener | trigger fixture required | beyond matrix | Possibly | trigger fixture |
| `addTriggerListener_withBackgroundThreadTest` | `addTriggerListener` | background trigger | background-thread contract unclear | beyond matrix | No | threading decision |
| `selectPassthruTest` | `selectPassthru` | datasource select | datasource fixture required | datasource row | No | datasource |

### OAObjectTest Disabled TODOs

| Test method/group | Production method | Scenario | Reason disabled | Original row represented | Can complete in memory? | Needs |
| --- | --- | --- | --- | --- | --- | --- |
| `readResolveTest` | `readResolve` | serialization | invariant needs definition | OBJ-028 | No | serialization fixture |
| `setPrimitiveNullTest` | `setPrimitiveNull` | primitive null | invariant needs definition | null-state row | Yes | subclass/helper or generated int fixture |
| `removePropertyTest` | `removeProperty` | property removal | invariant needs definition | null/property row | Yes | scalar fixture |
| `fireBeforePropertyChange*Test` | protected event methods | event ordering | invariant needs definition | event row | Yes | event recorder/helper subclass |
| `firePropertyChange*Test` | protected event methods | event ordering | invariant needs definition | event row | Yes | event recorder/helper subclass |
| `fireLocalPropertyChange*Test` | protected local events | local events | invariant needs definition | event row | Yes | event recorder/helper subclass |
| `fireNewListTest` | `fireNewList` | new-list event | invariant needs definition | event row | Yes | Hub listener fixture |
| `getHubSortOrder*Test`, `getHubMatchHubTest` | protected getHub overloads | sort/match Hub | invariant needs definition | derived/sort row | Possibly | sorted/sequence metadata fixture |
| `getObject_withUnloadedReferenceTest` | `getObject` | `UnloadedReference` | datasource/key-only fixture missing | OBJ-009 | No | unloaded reference fixture |
| `getBlobTest` | `getBlob` | blob reference | invariant needs definition | beyond matrix | Possibly | blob property fixture |
| `saveTest`, `saveCascadeTest`, `saveAllTest` | `save`, `saveAll` | datasource/cascade save | datasource/cascade fixture missing | OBJ-023 | No | datasource fixture |
| `deleteTest`, `delete_withOwnedReferenceTest`, `delete_withOptionalReferenceTest` | `delete` | owned/optional delete | delete fixture missing/contract unclear | OBJ-024, OBJ-025 | No | datasource/delete fixture |
| `isUniqueTest` | `isUnique` | unique datasource check | invariant needs definition | cache/datasource row | No | datasource/cache fixture |
| `isLoaded_withUnloadedReferenceTest` | `isLoaded` | `UnloadedReference` | fixture missing | OBJ-019 | No | key-only/unloaded fixture |
| `hierFindTest` | `hierFind` | hierarchical/recursive search | invariant needs definition | recursive row | Yes | `CatalogCategory` fixture |
| `setFkeyPropertyLinkInfoTest` | protected fkey setter | `ObjectKeyReference` | invariant needs definition | OBJ-021 | Yes | `OALinkInfo`/fkey fixture |
| `friendAccessFirePropertyChangeTest` | FriendAccess fire | event | invariant needs definition | event row | Yes | event recorder |

Disabled tests that are likely implementable in-memory now: `removePropertyTest`, `setPrimitiveNullTest`, event helper tests with a focused subclass, `hierFindTest`, and `setFkeyPropertyLinkInfoTest`.

## 10. OAPOS Fixture Coverage

| Semantic shape | OAPOS relationship | Used by test methods | Missing scenarios | Suitability |
| --- | --- | --- | --- | --- |
| `OneToMany` / `ManyToOne` / `MasterDetailHub` | `Store.Registers` ↔ `Register.Store` | Hub master/detail tests, OAObject setProperty tests | detail add/insert existing-parent | Strong. `Store.P_Registers` is `@OAMany(reverseName = Register.P_Store)` and `Register.P_Store` is `@OAOne(reverseName = Store.P_Registers, fkeys = P_StoreId)`. |
| one-to-one | `Store.Address` ↔ `Address.Store` | `setProperty_withOneToOneTest` | none currently | Strong. `Store.P_Address` and `Address.P_Store` provide scalar reverse-reference fixture. |
| linked direct/reference/detail | `Register.Till`, `Till.Register`, `Till.Store`, `Store.Tills` | linked-Hub tests | recursive linked-Hub fixture | Good for direct and linked-detail scenarios. |
| fkey/object-key | `Register.StoreId` ↔ `Register.Store` | fkey and reference-key tests | key-only/unloaded object-key | Partial. Fkey property exists; unloaded datasource/key-only fixture missing. |
| owned/cascade many | `Register.RegisterSessions` ↔ `RegisterSession.Register`, `Store.Registers` and other OAPOS owned Hubs | disabled save/delete tests | cascade save/delete implementation | Suitable after datasource/delete fixture is built. |
| recursive reference | `CatalogCategory.ParentCatalogCategory` ↔ `CatalogCategory.CatalogCategories` | none currently | recursive/hierarchical tests | Suitable; generated recursive metadata exists. |
| optional reference | OAPOS has many optional one-links | disabled optional delete test | exact optional delete invariant | Needs selected fixture and delete contract decision. |
| unloaded relationship | any fkey-backed one-link with datasource fixture | disabled unloaded tests | all unloaded-reference rows | Not suitable until deterministic datasource/key-only fixture exists. |

## 11. Completion Gate

### HubTest

Result: `MATRIX_NOT_ACCOUNTED_FOR`.

Reasons:

- `QUESTIONABLE`: `setAO_withObjectNotInHubTest` does not prove the adjust-master branch.
- `PARTIAL`: `setLinkHub` plus detail setup is only represented by the later `setAO_withLinkHubAndDetailHubTest`.
- `PARTIAL`: detail add/insert existing-parent rows and event ordering are not fully represented.
- `DISABLED_TODO` rows are structurally represented but not implemented.

### OAObjectTest

Result: `MATRIX_NOT_ACCOUNTED_FOR`.

Reasons:

- `MISSING`: same-value property assignment, invalid-type property assignment, reference copy, sync apply, datasource load, detached state, and recursive reference behavior.
- `QUESTIONABLE`: many-Hub not-initialized test vocabulary does not match generated auto-initialization behavior.
- `PARTIAL`: object-key, fkey, refresh, and loadReferences rows do not prove full key-only/unloaded/datasource behavior.
- `DISABLED_TODO` rows are structurally represented but not implemented.

## 12. Exact Next Actions

### To Reach `MATRIX_FULLY_ACCOUNTED_FOR`

1. Add `HubTest.setAO_withDetailHubAndObjectNotInHubTest` or reclassify the existing standalone outside-object row in the matrix.
2. Add disabled or passing Hub placeholders for detail add/insert existing-parent, shared clear, detail removeAll, removeAt active-object, and setPos invalid-index rows.
3. Add disabled or passing OAObject placeholders for same-value assignment, invalid-type assignment, reference copy, recursive reference, detached state, sync apply, and datasource load.
4. Clarify and rename the `ManyHubNotInitialized` row so it matches generated OAPOS many-Hub auto-initialization behavior.
5. Decide whether `setLinkHub_withDetailHubTest` should exist as a setup-specific public test or whether `setAO_withLinkHubAndDetailHubTest` is the canonical public coverage row.

### To Reach `MATRIX_FULLY_IMPLEMENTED`

1. Implement the in-memory missing/partial rows first: same-value and invalid-type `setProperty`, detail add/insert existing-parent, shared clear, removeAt active-object, reference copy, recursive `CatalogCategory`, and fkey link-info.
2. Add focused event-recorder fixtures for Hub event ordering, duplicate event prevention, listener mutation, and OAObject fire-event methods after Vince confirms the exact contract.
3. Build deterministic datasource/key-only fixtures for unloaded references, datasource load, select/loadAllData/refresh, save, delete, cascade, and uniqueness.
4. Build serialization round-trip fixtures for Hub and OAObject read-resolve behavior.
5. Build sync/remoting fixtures only after local public Hub/OAObject semantics are complete.

## Console Summary

```text
HubTest:
    matrix rows reviewed: 60
    PASS: 42
    DISABLED_TODO: 9
    INVALID: 1
    N/A: 3
    PARTIAL: 3
    QUESTIONABLE: 2
    MISSING: 0
    completion gate: MATRIX_NOT_ACCOUNTED_FOR

OAObjectTest:
    matrix rows reviewed: 47
    PASS: 23
    DISABLED_TODO: 9
    INVALID: 0
    N/A: 4
    PARTIAL: 7
    QUESTIONABLE: 1
    MISSING: 3
    completion gate: MATRIX_NOT_ACCOUNTED_FOR

Overall:
    all original rows represented: yes
    matrix fully accounted for: no
    matrix fully implemented: no
```
