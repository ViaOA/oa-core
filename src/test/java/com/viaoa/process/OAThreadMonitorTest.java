package com.viaoa.process;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

class OAThreadMonitorTest {

    @Test
    void checkThreadDumpCapturesCurrentThreads() throws Exception {
        OAThreadMonitor monitor = new OAThreadMonitor();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream()));

            monitor.checkThreadDump();

            assertFalse(monitor.hmThreadInfo.isEmpty());
        }
        finally {
            System.setOut(original);
        }
    }
}
