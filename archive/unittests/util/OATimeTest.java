package com.viaoa.util;

import org.junit.Test;
import static org.junit.Assert.*;

import com.viaoa.OAUnitTest;
import com.viaoa.datetime.OATime;

import test.xice.tsac3.model.oa.*;

public class OATimeTest extends OAUnitTest {

    @Test
    public void test() {
        OATime time = new OATime(20, 0, 0);
        
        long ms;
        OATime tNow = new OATime();
        if (tNow.before(time)) ms = time.betweenMilliSeconds(tNow);
        else {
            ms = tNow.betweenMilliSeconds(time);
            ms = (24 * 60 * 60 * 1000) - ms;
        }
        
        ms = ms / (1000 * 60);

        int xx = 4;
        xx++;
        
    }
    
}


/*


	public static void main(String[] args) {
		// this is the start time - grep "Opened file" *error*.log
		OATime dt = new OATime("19:08:28.024", "HH:mm:ss.SSS");

		// this is the gc timestamp to find
		dt = (OATime) dt.addSeconds(69099);
		dt = (OATime) dt.addMilliSeconds(830);

		System.out.print("==> " + dt.toString("hh:mm:ss.SSS aa"));
		System.out.println(" ==> " + dt.toString("HH:mm:ss.SSS"));
	}


*/