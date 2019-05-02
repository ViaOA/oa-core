package com.viaoa.object;


import com.cdi.model.oa.*;
import com.viaoa.OAUnitTest;
import com.viaoa.context.OAContext;
import com.viaoa.util.OADate;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OAThreadLocalDelegateTest extends OAUnitTest {
    
    @Test
    public void lockTest() {
        reset();
        
        final Object lock = new Object();

        assertFalse(OAThreadLocalDelegate.hasLock());
        assertFalse(OAThreadLocalDelegate.isLocked(lock));
        Object[] locks = OAThreadLocalDelegate.getLocks();
        assertTrue(locks == null || locks.length == 0);
        
        OAThreadLocalDelegate.lock(lock);
        assertTrue(OAThreadLocalDelegate.isLockOwner(lock));
        
        assertTrue(OAThreadLocalDelegate.hasLock());
        assertTrue(OAThreadLocalDelegate.isLocked(lock));
        locks = OAThreadLocalDelegate.getLocks();
        assertTrue(locks != null && locks.length == 1 && locks[0] == lock);
        
        OAThreadLocalDelegate.unlock(lock);
        assertFalse(OAThreadLocalDelegate.hasLock());
        assertFalse(OAThreadLocalDelegate.isLocked(lock));
        assertFalse(OAThreadLocalDelegate.isLockOwner(lock));
        
        for (int i=0; i<5; i++) {
            OAThreadLocalDelegate.lock(lock);
            locks = OAThreadLocalDelegate.getLocks();
            assertTrue(locks != null && locks.length == i+1 && locks[i] == lock);
        }
        for (int i=0; i<5; i++) {
            OAThreadLocalDelegate.unlock(lock);
            locks = OAThreadLocalDelegate.getLocks();
            if (i == 4) {
                assertTrue(locks == null || locks.length == 0);
            }
            else {
                assertTrue(locks != null && locks.length == 5-(i+1) && locks[5-(i+2)] == lock);
            }
        }

        // no locks at this point
        assertFalse(OAThreadLocalDelegate.hasLock());
        assertFalse(OAThreadLocalDelegate.isLocked(lock));
        
        // start another thread to lock it
        Thread t = new Thread() {
            @Override
            public void run() {
                threadLockTest(lock);
            }
        };
        t.start();
        
        try {
            Thread.sleep(1000);
        }
        catch (Exception e) {
        }
        assertTrue(OAThreadLocalDelegate.isLocked(lock));

        for (int i=0; i<5; i++) {
            assertFalse(OAThreadLocalDelegate.hasLock(lock));
        }

        OAThreadLocalDelegate.lock(lock, 1);
        assertTrue(OAThreadLocalDelegate.hasLock());
        assertTrue(OAThreadLocalDelegate.hasLock(lock));
        assertFalse(OAThreadLocalDelegate.isLockOwner(lock));
        OAThreadLocalDelegate.unlock(lock);
        
        OAThreadLocalDelegate.lock(lock, 0);
        
        assertTrue(OAThreadLocalDelegate.hasLock());
        assertTrue(OAThreadLocalDelegate.hasLock(lock));
        assertTrue(OAThreadLocalDelegate.isLockOwner(lock));
        
        OAThreadLocalDelegate.unlock(lock);
        assertFalse(OAThreadLocalDelegate.hasLock(lock));
        assertFalse(OAThreadLocalDelegate.isLocked(lock));
        assertFalse(OAThreadLocalDelegate.hasLock());
        
        // 
        Object[] lockz = new Object[] {new Object(), new Object(), new Object(), new Object(), new Object()};
        for (int i=0; i<lockz.length; i++) {
            OAThreadLocalDelegate.lock(lockz[i]);
        }
        OAThreadLocalDelegate.releaseAllLocks();
        assertFalse(OAThreadLocalDelegate.hasLock(lock));
    }
    private void threadLockTest(final Object lock) {
        assertFalse(OAThreadLocalDelegate.hasLock());
        assertFalse(OAThreadLocalDelegate.isLocked(lock));
        OAThreadLocalDelegate.lock(lock);

        assertTrue(OAThreadLocalDelegate.hasLock());
        assertTrue(OAThreadLocalDelegate.isLocked(lock));
        try {
            Thread.sleep(2000);
        }
        catch (Exception e) {
        }
        finally {
            OAThreadLocalDelegate.unlock(lock);
        }
        // no locks at this point
        assertFalse(OAThreadLocalDelegate.hasLock());
        Object[] locks = OAThreadLocalDelegate.getLocks();
        assertTrue(locks == null || locks.length == 0);
        
    }

    @Test//(timeout=2000)
    public void deadlockTest() {
        final Object lockA = "LockA";
        final Object lockB = "LockB";

        OAThreadLocalDelegate.lock(lockA);
        assertTrue(OAThreadLocalDelegate.hasLock(lockA));

        
        Thread tA = new Thread() {
            @Override
            public void run() {
                OAThreadLocalDelegate.lock(lockB, 0);
                assertTrue(OAThreadLocalDelegate.hasLock(lockB));
                
                OAThreadLocalDelegate.lock(lockA, 0); // deadlock
                // deadlock detection will allow this to continue
                assertTrue(OAThreadLocalDelegate.hasLock(lockA));
                assertTrue(OAThreadLocalDelegate.hasLock(lockB));
                
                OAThreadLocalDelegate.unlock(lockA);
                OAThreadLocalDelegate.unlock(lockB);

                assertFalse(OAThreadLocalDelegate.hasLock(lockA));
                assertFalse(OAThreadLocalDelegate.hasLock(lockB));
            }
        };
        tA.start();
        
        try {
            Thread.sleep(300);
        }
        catch (Exception e) {
        }
        assertTrue(OAThreadLocalDelegate.hasLock(lockA));
        assertEquals(OAThreadLocalDelegate.getDeadlockCount(), 0);
        OAThreadLocalDelegate.lock(lockB, 0);  // deadlock
        assertEquals(OAThreadLocalDelegate.getDeadlockCount(), 1);

        assertTrue(OAThreadLocalDelegate.hasLock(lockA));
        assertTrue(OAThreadLocalDelegate.hasLock(lockB));

        OAThreadLocalDelegate.unlock(lockA);
        OAThreadLocalDelegate.unlock(lockB);

        assertFalse(OAThreadLocalDelegate.hasLock(lockA));
        assertFalse(OAThreadLocalDelegate.hasLock(lockB));
        
        try {
            Thread.sleep(300);
        }
        catch (Exception e) {
        }
        assertFalse(OAThreadLocalDelegate.isLocked(lockA));
        assertFalse(OAThreadLocalDelegate.isLocked(lockB));
    }
    
    @Test
    public void testForContext() {
        boolean b = OAThreadLocalDelegate.setIsAdmin(true);
        assertFalse(b);
        b = OAThreadLocalDelegate.setIsAdmin(false);
        assertTrue(b);
        
        b = OAThreadLocalDelegate.setAlwaysAllowEditProcessed(true);
        assertFalse(b);
        b = OAThreadLocalDelegate.setAlwaysAllowEditProcessed(false);
        assertTrue(b);
    }


    @Test
    public void test5() throws Exception {
        final CountDownLatch cd = new CountDownLatch(1);
        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(true);
        final OAThreadLocal tl = OAThreadLocalDelegate.getOAThreadLocal();
       
        final AtomicBoolean ab = new AtomicBoolean(false);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                OAThreadLocalDelegate.initialize(tl);
                ab.set(OAThreadLocalDelegate.getAlwaysAllowEditProcessed());
                cd.countDown();
            }
        });
        t.start();
        cd.await(5, TimeUnit.SECONDS);
        assertTrue(ab.get());
        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(false);
    }
    
    
    @Test
    public void test6() throws Exception {
        OAThreadLocalDelegate.setContext(null);
        assertNull(OAThreadLocalDelegate.getContext());
        Object context = new Object();
        OAThreadLocalDelegate.setContext(context);
        assertEquals(context, OAThreadLocalDelegate.getContext());
        OAThreadLocalDelegate.setContext(null);
        assertNull(OAThreadLocalDelegate.getContext());
        
        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(false);
        assertFalse(OAThreadLocalDelegate.getAlwaysAllowEditProcessed());
        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(true);
        assertTrue(OAThreadLocalDelegate.getAlwaysAllowEditProcessed());
        
        OAThreadLocalDelegate.setAlwaysAllowEnabled(false);
        assertFalse(OAThreadLocalDelegate.getAlwaysAllowEnabled());
        OAThreadLocalDelegate.setAlwaysAllowEnabled(true);
        assertTrue(OAThreadLocalDelegate.getAlwaysAllowEnabled());

        
        
        OAThreadLocalDelegate.setAlwaysAllowEditProcessed(false);
        OAThreadLocalDelegate.setAlwaysAllowEnabled(false);
        OAContext.removeContext(context);
        OAContext.removeContext();
    }
}
