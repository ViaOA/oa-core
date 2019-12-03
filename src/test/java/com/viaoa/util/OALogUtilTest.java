package com.viaoa.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.viaoa.OAUnitTest;

import test.xice.tsac3.model.oa.*;

public class OALogUtilTest extends OAUnitTest {

    @Test
    public void test() {

    }

    public static void main(String[] args) throws Exception {
        OALogUtil.disable();
        Logger log = Logger.getLogger("test");
        
        Handler handler = new Handler() {
            volatile FileHandler handler; // replacement handler when date changes
            volatile long msNextDateChange; // next date change
            long msLast;
            int threadIdLast;
            String sourceLast;
            int cntIgnored;

            long msX;

            @Override
            public synchronized void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    String source = record.getSourceMethodName();
                    int tid = record.getThreadID();
                    long ms = System.currentTimeMillis();

                    if (msX < ms) {
                        sourceLast = null;
                        msX = ms + 3910;
                    }

                    boolean bMatch = (tid == threadIdLast) && (source == null || source.equals(sourceLast));
                    boolean bIgnore = bMatch && (msLast + 1000 > ms);

                    if (!bMatch) {
                        threadIdLast = tid;
                        sourceLast = source;
                    }

                    if (bIgnore) {
                        cntIgnored++;
                        return;
                    }
                    msLast = ms;
                    if (bMatch) {
                        String msg = record.getMessage();
                        record.setMessage(msg += " (note: output throttled)");
                    }
                    System.out.println(record.getMessage());
                }
            }

            @Override
            public void flush() {
                // TODO Auto-generated method stub
            }

            @Override
            public void close() throws SecurityException {
                // TODO Auto-generated method stub
            }
        };
        log.addHandler(handler);
        handler.setLevel(Level.FINE);
        log.setLevel(Level.FINE);
        for (int i = 0;; i++) {
            log.warning("hey" + i);
        }

    }

}
