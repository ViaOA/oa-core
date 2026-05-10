package com.viaoa.serialize;

public interface OASerializeReader {
    boolean hasNext();

    String nextName();

    Object nextValue();

    void beginObject();
    void endObject();

    void beginHub();
    void endHub();

    boolean isNull();
}