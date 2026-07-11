# com.viaoa.schedule.OADateChangeController

## Purpose

Registers callbacks that are notified when the calendar date changes.

## Architectural Role

OADateChangeController is a small runtime scheduling utility. It is independent from OA model metadata and rules, but it can be used by OA applications or services that need date-boundary notifications.

## Responsibilities

- Keep weak references to date-change callbacks.
- Start a daemon notifier thread when callbacks are registered.
- Detect transition to a new date.
- Invoke registered callbacks when a new date is observed.

## Collaborators

- `OADate`
- `OADateTime`

## Public Contract

`onChange(Callback)` registers a callback. `Callback#onDateChange()` is called from the notifier thread when the date changes.

## Invariants

### INV-OADATECHANGECONTROLLER-001: Callback registrations are weakly held

**Contract**

Registered callbacks are stored through weak references; callers that require notification must keep their own strong reference to the callback.

**Rationale**

Weak registration avoids permanently retaining application callback objects, but it means registration alone does not define callback lifetime.

**Evidence**

`OADateChangeController#onChange(Callback)` creates `WeakReference<Callback>` entries in the static callback list.

**Test implications**

Verify a strongly held callback can be invoked by direct processing hooks or controlled test seams where practical; document that GC-sensitive behavior should not rely on timing.

**Confidence**

High

### INV-OADATECHANGECONTROLLER-002: Date-change notification runs on a daemon thread

**Contract**

Date-change callbacks must not assume they execute on an application UI thread, request thread, or OA worker thread.

**Rationale**

Callback implementations must handle thread ownership explicitly and avoid mutating UI state directly from the notifier thread.

**Evidence**

`OADateChangeController#onChange(Callback)` creates an `OADateChangeNotifier` daemon thread.

**Test implications**

Verify the created thread properties if a controllable lifecycle hook is added; otherwise test callback contracts through isolated callback implementations.

**Confidence**

Medium

### INV-OADATECHANGECONTROLLER-003: Callback failures must not corrupt callback registration state

**Contract**

Callback execution should not leave the static callback list in a partially updated state.

**Rationale**

Date-change notification is shared process state; one callback should not corrupt registrations for unrelated callbacks.

**Evidence**

`OADateChangeController#process()` snapshots weak references before invoking callbacks and removes cleared references while synchronized on the list.

**Test implications**

Add tests for multiple callbacks, cleared weak references, and exception behavior if the processing loop is made testable.

**Confidence**

Medium

## State Model

State is static and process-wide: a list of weak callback references and a notifier thread reference.

## Threading and Concurrency

Registration synchronizes on the callback list. Callback invocation occurs outside the registration lock using a snapshot.

## Failure and Exception Behavior

The current implementation should be treated conservatively: callback exception isolation and notifier lifecycle should be tested before being relied upon as a strong guarantee.

## Required Invariant Tests

- Null callback registration is a no-op.
- Registration uses weak references.
- Callback invocation is outside the registration lock.
- Notifier lifecycle does not create duplicate notifier threads for repeated registrations.
- One callback throwing should not prevent unrelated callbacks from being considered, if that is the intended contract.

## Evidence in Current Implementation

- Source file: `src/main/java/com/viaoa/schedule/OADateChangeController.java`
- Package: `com.viaoa.schedule`

## Open Questions or Unclear Contracts

The current source contains comments noting risks around notifier-thread lifecycle, weak-only registration, and callback exception isolation. Those should be verified with focused tests before being treated as stable guarantees.
