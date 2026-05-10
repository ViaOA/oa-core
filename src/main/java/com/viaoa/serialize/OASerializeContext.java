package com.viaoa.serialize;

import java.util.IdentityHashMap;
import java.util.Map;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

public class OASerializeContext {
    private final Map<OAObject, Boolean> writtenObjects = new IdentityHashMap<>();

    private boolean includeNulls;
    private boolean includeCalculated;
    private boolean includeTransient;
    private boolean includeReferences = true;
    private boolean writeKeys = true;
    private boolean writeGuid = true;

    private int maxDepth = 20;
    private int depth;

    public boolean hasWritten(OAObject obj) {
        return writtenObjects.containsKey(obj);
    }

    public void markWritten(OAObject obj) {
        if (obj != null) writtenObjects.put(obj, Boolean.TRUE);
    }

    public boolean getIncludeNulls() {
        return includeNulls;
    }

    public void setIncludeNulls(boolean includeNulls) {
        this.includeNulls = includeNulls;
    }

    public boolean getIncludeCalculated() {
        return includeCalculated;
    }

    public void setIncludeCalculated(boolean includeCalculated) {
        this.includeCalculated = includeCalculated;
    }

    public boolean getIncludeTransient() {
        return includeTransient;
    }

    public void setIncludeTransient(boolean includeTransient) {
        this.includeTransient = includeTransient;
    }

    public boolean getIncludeReferences() {
        return includeReferences;
    }

    public void setIncludeReferences(boolean includeReferences) {
        this.includeReferences = includeReferences;
    }

    public boolean getWriteKeys() {
        return writeKeys;
    }

    public void setWriteKeys(boolean writeKeys) {
        this.writeKeys = writeKeys;
    }

    public boolean getWriteGuid() {
        return writeGuid;
    }

    public void setWriteGuid(boolean writeGuid) {
        this.writeGuid = writeGuid;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getDepth() {
        return depth;
    }

    public void pushDepth() {
        depth++;
    }

    public void popDepth() {
        if (depth > 0) depth--;
    }

    public boolean isMaxDepthReached() {
        return maxDepth >= 0 && depth >= maxDepth;
    }
}