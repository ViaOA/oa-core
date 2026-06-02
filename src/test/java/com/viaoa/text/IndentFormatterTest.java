package com.viaoa.text;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

/** Internal source-mirrored tests for IndentFormatter. */
public class IndentFormatterTest {
    @Test
    public void formatTest() {
        IndentFormatter formatter = new IndentFormatter();
        // normal log message
        LogRecord record = new LogRecord(Level.INFO, "hello");
        String text = formatter.format(record);
        assertNotNull(text);
        assertTrue(text.contains("hello"));
        // entry marker increases indentation path safely
        LogRecord entry = new LogRecord(Level.FINEST, "ENTRY");
        entry.setSourceClassName("TestClass");
        entry.setSourceMethodName("testMethod");
        assertNotNull(formatter.format(entry));
        // return marker decreases indentation path safely
        LogRecord exit = new LogRecord(Level.FINEST, "RETURN");
        exit.setSourceClassName("TestClass");
        exit.setSourceMethodName("testMethod");
        assertNotNull(formatter.format(exit));
        // throwable is included safely
        LogRecord err = new LogRecord(Level.SEVERE, "bad");
        err.setThrown(new RuntimeException("boom"));
        assertNotNull(formatter.format(err));
    }
}
