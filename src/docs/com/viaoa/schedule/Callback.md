# com.viaoa.schedule.OADateChangeController.Callback

## Purpose

Receives notification from `OADateChangeController` when the calendar date changes.

## Architectural Role

Callback is a nested listener contract for date-change notification. It is a small extension point for code that needs to react to date-boundary changes.

## Responsibilities

- Implement `onDateChange()` with application-specific date-change work.
- Avoid assuming execution on a UI, request, or OA service thread.
- Keep a strong reference to the callback object if notification must be guaranteed.

## Collaborators

- `OADateChangeController`

## Public Contract

`onDateChange()` is invoked by the controller's notifier thread when a new date is detected.

## Invariants

### INV-OADATECHANGECALLBACK-001: Callback execution is asynchronous to registration

**Contract**

`onDateChange()` must be prepared to run later on the controller's notifier thread, not during registration.

**Rationale**

Date changes are time-driven and the controller creates a background daemon notifier.

**Evidence**

`OADateChangeController#onChange(Callback)` registers weak references and starts an `OADateChangeNotifier` thread that calls `process()`.

**Test implications**

Callback tests should avoid UI-thread assumptions and should verify thread-sensitive work is marshalled by the callback implementation when needed.

**Confidence**

Medium

### INV-OADATECHANGECALLBACK-002: Registration does not define callback lifetime

**Contract**

Because callbacks are weakly referenced, application code must retain a strong reference for as long as notification is required.

**Rationale**

Weak registration prevents leaks but allows callbacks to disappear before a date change.

**Evidence**

`OADateChangeController#onChange(Callback)` stores callbacks as `WeakReference<Callback>`.

**Test implications**

Document and test strong-reference usage where practical; avoid GC-timing-dependent assertions.

**Confidence**

High

## Threading and Concurrency

Callback implementations own any required synchronization or thread marshalling for the work performed by `onDateChange()`.

## Failure and Exception Behavior

Exception behavior should be verified before treating callback isolation as guaranteed.

## Required Invariant Tests

- Strongly referenced callback receives notification in controlled processing tests.
- Weak-only callback behavior is documented without relying on nondeterministic GC.
- Callback exception behavior is explicitly tested once the controller contract is finalized.

## Evidence in Current Implementation

- Source file: `src/main/java/com/viaoa/schedule/OADateChangeController.java`
- Nested type: `OADateChangeController.Callback`

## Open Questions or Unclear Contracts

The current implementation should be reviewed for whether one callback exception should stop the notifier loop or be isolated from other callbacks.
