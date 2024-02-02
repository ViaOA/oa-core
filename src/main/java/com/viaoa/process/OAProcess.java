/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.process;

/**
 * Similar to a future or promise to track and manage a running process.
 * @author vvia
 */
public class OAProcess implements Runnable {

    private boolean bBlock;
    private long maxBlockTime;
    
    
    private final long createdTime;
    
    private boolean bAllowCancel;
    private volatile boolean bRequestCancel;
    private volatile long requestCancelTime;
    private volatile String requestCancelReason;
    
    private volatile boolean bWasCancelled;
    private volatile long cancelledTime;
    private volatile String cancelledReason;
    
    
    private volatile long doneTime;
    private volatile String doneMessage;
    
    
    private String name, description, status;
    private volatile long estimatedTime;

    private String[] steps;
    private volatile int currentStep;
    
    private volatile Exception exception;
    
    private long maxTime;
    
    private boolean pause;

    /**
     * 
     */
    public OAProcess() {
        createdTime = System.currentTimeMillis();
    }

    public void setAllowCancel(boolean b) {
        this.bAllowCancel = b;
    }
    public boolean getAllowCancel() {
        return bAllowCancel;
    }

    public void setBlock(boolean b) {
        this.bBlock = b;
    }
    public boolean getBlock() {
        return bBlock;
    }
    
    protected void setMaxBlockTime(long x) {
        this.maxBlockTime = x;
    }
    public long getMaxBlockTime() {
        return this.maxBlockTime;
    }
    
    
    public String getName() {
        return name;
    }
    public void setName(String s) {
        this.name = s;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String s) {
        this.description = s;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String s) {
        this.status = s;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public boolean getRequestedToCancel() {
        return this.requestCancelTime != 0;
    }
    public void requestCancel(String reason) {
        this.bRequestCancel = true;
        this.requestCancelTime = System.currentTimeMillis();
        requestCancelReason = reason;
    }
    public String getRequestCancelReason() {
        return requestCancelReason;
    }
    public long getRequestCancelTime() {
        return requestCancelTime;
    }
    

    /**
     * This is used to set WasCancelled=true if requestedToCancel==true
     * @return value of wasCancelled
     */
    public boolean confirmRequestToCancel() {
        if (!getWasCancelled() && getRequestedToCancel()) {
            setWasCancelled(true);
        }
        return getWasCancelled();
    }
    
    
    public void setWasCancelled(boolean b) {
        bWasCancelled = b;
        if (b) this.cancelledTime = System.currentTimeMillis();
    }
    public boolean getWasCancelled() {
        return bWasCancelled;
    }
    
    public String getCancelledReason() {
        return this.cancelledReason;
    }
    public void setCancelledReason(String s) {
        this.cancelledReason = s;
    }
    
    
    public void setDone() {
        doneTime = System.currentTimeMillis();
    }
    public boolean getDone() {
        return (doneTime > 0);
    }
    public long getDoneTime() {
        return doneTime;
    }
    
    public String getDoneMessage() {
        return doneMessage;
    }
    public void setDoneMessage(String s) {
        this.doneMessage = s;
    }
    
    public Exception getException() {
        return exception;
    }
    public void setException(Exception ex) {
        exception = ex;;
    }
    
    
    public String[] getSteps() {
        return steps;
    }
    public void setSteps(String... steps) {
        this.steps = steps;
    }
    public int getTotalSteps() {
        return (steps == null ? 0 : steps.length);
    }
    
    public void setCurrentStep(int x) {
        this.currentStep = x;
    }
    public int getCurrentStep() {
        return this.currentStep;
    }

    protected void setEstimateTime(long x) {
        this.estimatedTime = x;
    }
    public long getEstimateTime() {
        return this.estimatedTime;
    }
    
    public long getMaxTime() {
        return this.maxTime;
    }
    public void setMaxTime(long x) {
        this.maxTime = x;
    }
    
    public boolean isBlockTimedout() {
        long ms = System.currentTimeMillis();
        return ((maxBlockTime + createdTime) > ms);
    }
    
    public boolean isTimedout() {
        if (maxTime < 1) return false;
        long ms = System.currentTimeMillis();
        return ((maxTime + createdTime) > ms);
    }
    
    public void setPause(boolean b) {
        this.pause = b;
    }
    public boolean isPaused() {
        return pause;
    }
    public boolean getPause() {
        return pause;
    }

    @Override
    public void run() {
        // custom code can overwrite this
    }
}

