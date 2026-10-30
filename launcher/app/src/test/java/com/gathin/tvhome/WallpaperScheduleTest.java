package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;

public class WallpaperScheduleTest {
    @Test public void disabledRotationDoesNotScheduleWakeups(){
        assertEquals(-1,WallpaperRotation.nextDelay(1000,2000,0));
        assertEquals(-1,WallpaperRotation.nextDelay(1000,2000,-1));
    }
    @Test public void schedulesExactlyTheRemainingInterval(){
        assertEquals(45000,WallpaperRotation.nextDelay(10000,25000,60000));
        assertEquals(0,WallpaperRotation.nextDelay(10000,70000,60000));
        assertEquals(0,WallpaperRotation.nextDelay(10000,90000,60000));
    }
    @Test public void aClockCorrectionOrMissingTimestampIsImmediatelyDue(){
        assertEquals(0,WallpaperRotation.nextDelay(0,25000,60000));
        assertEquals(0,WallpaperRotation.nextDelay(30000,25000,60000));
    }
}
