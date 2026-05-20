/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.process;


/*qqqqqqqqqqqqq
CODEX

1. OAProcess / isTimedout, isBlockTimedout
     Severity: High
     Bug/risk: Both timeout checks are inverted. isTimedout() returns true while createdTime + maxTime is still in the
     future, and false after the timeout has actually elapsed. isBlockTimedout() has the same issue.
     Production impact: Long-running or blocked processes can be treated as timed out immediately, while genuinely
     expired processes may appear healthy. This breaks cancellation/monitoring decisions.
     Area: src/main/java/com/viaoa/process/OAProcess.java:516, src/main/java/com/viaoa/process/OAProcess.java:527
     Minimal hardening: Change both comparisons to (createdTime + limit) < now, preferably with overflow-safe now -
     createdTime > limit.

9. OAProcess / mutable lifecycle fields
     Severity: Low/Medium
     Bug/risk: Several process-control fields intended for cross-thread observation are not volatile or synchronized:
     pause, bAllowCancel, bBlock, maxBlockTime, maxTime, name, description, status, and steps. Cancellation fields are
     partly volatile, but pause/block/status are not.
     Production impact: Background process control or monitoring threads can see stale pause/block/status values,
     making cancellation or UI/runtime state unreliable under load.
     Area: src/main/java/com/viaoa/process/OAProcess.java:31
     Minimal hardening: Make cross-thread state volatile or synchronize process state mutations/reads. Longer term,
     replace independent booleans with an explicit lifecycle/state object.

*/

/**
 * Tracks the lifecycle of a long-running or asynchronous process and provides
 * state for cancellation, completion, progress steps, timing constraints, and
 * error reporting. <p>
 *
 * OAProcess instances are typically managed by higher-level components that
 * execute tasks in background threads. The class exposes flags and timestamps
 * for cancellation requests, cancellation confirmation, process completion,
 * pause state, and progress-step tracking. Subclasses or callers may override
 * {@link #run()} to execute the actual work associated with the process.
 */
public class OAProcess implements Runnable {

	/**
	 * Flag indicating whether the process is currently in a blocking state.
	 */
    private boolean bBlock;
    
    /**
     * Maximum duration, in milliseconds, that the process is allowed to remain
     * in a blocking state.
     */
    private long maxBlockTime;
    
    
    /**
     * Timestamp, in milliseconds since epoch, marking when this process
     * instance was created.
     */
    private final long createdTime;
    
    /**
     * Indicates whether this process supports being cancelled by a caller.
     */
    private boolean bAllowCancel;
    
    /**
     * Internal flag set when a cancellation has been requested. This flag
     * does not itself confirm cancellation.
     */
    private volatile boolean bRequestCancel;
    
    /**
     * Timestamp of when a cancellation request was made. Zero indicates that
     * no cancellation has been requested.
     */
    private volatile long requestCancelTime;
    
    /**
     * Optional explanation supplied when a cancellation is requested.
     */
    private volatile String requestCancelReason;
    
    /**
     * Flag indicating whether the process has been cancelled. Set when
     * {@link #setWasCancelled(boolean)} is invoked.
     */
    private volatile boolean bWasCancelled;
    
    /**
     * Timestamp recorded when the process is marked as cancelled.
     */
    private volatile long cancelledTime;
    
    /**
     * Optional explanation for why the process was cancelled.
     */
    private volatile String cancelledReason;
    
    
    /**
     * Timestamp recorded when the process completes successfully or otherwise
     * finishes execution.
     */
    private volatile long doneTime;
    
    /**
     * Optional message describing results or information relevant upon
     * process completion.
     */
    private volatile String doneMessage;
    
    
    /**
     * Human-readable identification fields for this process:
     * <ul>
     *   <li>{@code name} — the display name</li>
     *   <li>{@code description} — additional details or purpose</li>
     *   <li>{@code status} — a short status indicator</li>
     * </ul>
     */
    private String name;
    private String description;
    private String status;
    
    /**
     * Estimated time, in milliseconds, that the process may require.
     * This value is optional and set by callers.
     */
    private volatile long estimatedTime;

    /**
     * Optional ordered list of step descriptions representing progress stages
     * within the process.
     */
    private String[] steps;
    
    /**
     * Index of the current progress step within {@link #steps}.
     */
    private volatile int currentStep;
    
    /**
     * Exception captured during process execution, if any.
     */
    private volatile Exception exception;
    
    /**
     * Maximum allowed runtime, in milliseconds, before the process is considered
     * to have timed out. A value less than 1 disables timeout checking.
     */
    private long maxTime;
    
    /**
     * Indicates whether the process is currently paused.
     */
    private boolean pause;

    /**
     * Creates a new process instance and records its creation timestamp.
     */
    public OAProcess() {
        createdTime = System.currentTimeMillis();
    }

    /**
     * Enables or disables the ability to request cancellation of this process.
     *
     * @param b true to allow cancellation, false to disallow
     */
    public void setAllowCancel(boolean b) {
        this.bAllowCancel = b;
    }

    /**
     * Indicates whether cancellation requests are permitted for this process.
     *
     * @return true if cancellation is allowed
     */
    public boolean getAllowCancel() {
        return bAllowCancel;
    }

    /**
     * Sets whether the process is in a blocking state.
     *
     * @param b true to enable blocking
     */
    public void setBlock(boolean b) {
        this.bBlock = b;
    }

    /**
     * Returns whether the process is currently flagged as blocking.
     *
     * @return true if blocking
     */
    public boolean getBlock() {
        return bBlock;
    }
    
    /**
     * Sets the maximum allowable time, in milliseconds, for the process to
     * remain blocked.
     *
     * @param x block timeout value in milliseconds
     */
    protected void setMaxBlockTime(long x) {
        this.maxBlockTime = x;
    }

    /**
     * Sets the maximum allowable time, in milliseconds, for the process to
     * remain blocked.
     *
     * @param x block timeout value in milliseconds
     */
    public long getMaxBlockTime() {
        return this.maxBlockTime;
    }
    
    /**
     * Returns the name assigned to this process.
     *
     * @return process name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name for this process.
     *
     * @param s descriptive name
     */
    public void setName(String s) {
        this.name = s;
    }
    
    /**
     * Returns the description of this process.
     *
     * @return process description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the description of this process.
     *
     * @param s descriptive text
     */
    public void setDescription(String s) {
        this.description = s;
    }

    /**
     * Returns the current status value for this process.
     *
     * @return status string
     */
    public String getStatus() {
        return status;
    }

    /**
     * Updates the status indicator for this process.
     *
     * @param s status text
     */
    public void setStatus(String s) {
        this.status = s;
    }
    
    /**
     * Returns the timestamp marking when this process instance was created.
     *
     * @return creation time in milliseconds since epoch
     */
    public long getCreatedTime() {
        return createdTime;
    }
    
    /**
     * Indicates whether a cancellation request has been issued for this process.
     * This is based on whether {@link #requestCancelTime} is non-zero.
     *
     * @return true if cancellation has been requested
     */
    public boolean getRequestedToCancel() {
        return this.requestCancelTime != 0;
    }

    /**
     * Requests that this process be cancelled. Records the timestamp and reason
     * for the cancellation request.
     *
     * @param reason explanation for the cancellation request
     */
    public void requestCancel(String reason) {
        this.bRequestCancel = true;
        this.requestCancelTime = System.currentTimeMillis();
        requestCancelReason = reason;
    }
    
    /**
     * Returns the reason provided when cancellation was requested.
     *
     * @return cancellation request reason, or null if none
     */
    public String getRequestCancelReason() {
        return requestCancelReason;
    }
    
    /**
     * Returns the timestamp of the cancellation request.
     *
     * @return time the request was made, or 0 if none
     */
    public long getRequestCancelTime() {
        return requestCancelTime;
    }
    

    /**
     * Confirms a previously issued cancellation request. If a request exists
     * and the process has not yet been marked as cancelled, this method marks
     * the process as cancelled and returns true.
     *
     * @return true if the process is now confirmed as cancelled
     */
    public boolean confirmRequestToCancel() {
        if (!getWasCancelled() && getRequestedToCancel()) {
            setWasCancelled(true);
        }
        return getWasCancelled();
    }
    
    
    /**
     * Sets the cancelled state for this process. When set to true, records the
     * cancellation timestamp.
     *
     * @param b true to mark the process as cancelled
     */
    public void setWasCancelled(boolean b) {
        bWasCancelled = b;
        if (b) this.cancelledTime = System.currentTimeMillis();
    }
    
    /**
     * Indicates whether this process has been marked as cancelled.
     *
     * @return true if cancelled
     */
    public boolean getWasCancelled() {
        return bWasCancelled;
    }
    
    /**
     * Returns the reason recorded when the process was marked as cancelled.
     *
     * @return cancellation reason, or null if none
     */
    public String getCancelledReason() {
        return this.cancelledReason;
    }

    /**
     * Sets a descriptive reason explaining why the process was cancelled.
     *
     * @param s cancellation explanation
     */
    public void setCancelledReason(String s) {
        this.cancelledReason = s;
    }
    
    
    /**
     * Marks the process as completed by recording the current timestamp.
     */
    public void setDone() {
        doneTime = System.currentTimeMillis();
    }

    /**
     * Indicates whether the process has been marked as completed.
     *
     * @return true if completion time has been recorded
     */
    public boolean getDone() {
        return (doneTime > 0);
    }

    /**
     * Returns the timestamp that was recorded when the process completed.
     *
     * @return completion time in milliseconds since epoch
     */
    public long getDoneTime() {
        return doneTime;
    }
    
    /**
     * Returns the optional message associated with process completion.
     *
     * @return completion message, or null if none set
     */
    public String getDoneMessage() {
        return doneMessage;
    }

    /**
     * Sets an informational message describing the result of the process.
     *
     * @param s completion message text
     */
    public void setDoneMessage(String s) {
        this.doneMessage = s;
    }
    
    /**
     * Returns the exception captured during process execution, if any.
     *
     * @return exception instance or null
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Records an exception that occurred during process execution.
     *
     * @param ex exception to store
     */
    public void setException(Exception ex) {
        exception = ex;;
    }
    
    
    /**
     * Returns the list of step descriptions associated with this process.
     *
     * @return array of step names, or null if not defined
     */
    public String[] getSteps() {
        return steps;
    }

    /**
     * Defines an ordered list of descriptive steps for tracking process
     * progress.
     *
     * @param steps variable-length list of step descriptions
     */
    public void setSteps(String... steps) {
        this.steps = steps;
    }
    
    /**
     * Returns the number of steps defined for this process.
     *
     * @return total number of steps, or zero if no steps are defined
     */
    public int getTotalSteps() {
        return (steps == null ? 0 : steps.length);
    }
    
    /**
     * Updates the index of the current progress step.
     *
     * @param x step index to assign
     */
    public void setCurrentStep(int x) {
        this.currentStep = x;
    }

    /**
     * Returns the index of the current progress step.
     *
     * @return zero-based step index
     */
    public int getCurrentStep() {
        return this.currentStep;
    }

    /**
     * Sets the estimated runtime for this process in milliseconds.
     *
     * @param x estimated duration
     */
    protected void setEstimateTime(long x) {
        this.estimatedTime = x;
    }

    /**
     * Returns the estimated runtime for this process.
     *
     * @return estimated duration in milliseconds
     */
    public long getEstimateTime() {
        return this.estimatedTime;
    }
    
    /**
     * Returns the maximum allowed execution time for this process.
     *
     * @return timeout value in milliseconds
     */
    public long getMaxTime() {
        return this.maxTime;
    }

    /**
     * Sets the maximum allowed execution time for this process.
     *
     * @param x timeout value in milliseconds
     */
    public void setMaxTime(long x) {
        this.maxTime = x;
    }
    
    /**
     * Determines whether the allowed block duration has been exceeded.
     *
     * @return true if (createdTime + maxBlockTime) is earlier than now
     */
    public boolean isBlockTimedout() {
        long ms = System.currentTimeMillis();
        return ((maxBlockTime + createdTime) > ms);
    }
    
    /**
     * Determines whether the process has exceeded its maximum allowed
     * execution time. Disabled when {@code maxTime < 1}.
     *
     * @return true if the process has timed out
     */
    public boolean isTimedout() {
        if (maxTime < 1) return false;
        long ms = System.currentTimeMillis();
        return ((maxTime + createdTime) > ms);
    }
    
    /**
     * Sets whether this process is paused.
     *
     * @param b true to pause the process, false to resume
     */
    public void setPause(boolean b) {
        this.pause = b;
    }
 
    /**
     * Indicates whether the process is currently paused.
     *
     * @return true if paused
     */
    public boolean isPaused() {
        return pause;
    }
    
    /**
     * Alias for {@link #isPaused()}. Returns whether the process is paused.
     *
     * @return true if paused
     */
    public boolean getPause() {
        return pause;
    }

    /**
     * Executable entry point for subclasses to implement process logic.
     * Default implementation is empty and intended to be overridden.
     */
    @Override
    public void run() {
        // custom code can overwrite this
    }
}

