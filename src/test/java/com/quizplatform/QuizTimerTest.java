package com.quizplatform;

import com.quizplatform.timer.QuizTimer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of the quiz timer THREAD (no GUI, no database required).
 * Demonstrates: Runnable, Thread.sleep, interrupt(), listener callbacks
 * and the automatic expiry used for auto-submission.
 */
class QuizTimerTest {

    @Test
    @DisplayName("Timer counts down every second and fires onTimeExpired() at 00:00")
    void timerCountsDownAndExpires() throws Exception {
        AtomicInteger lastTick = new AtomicInteger(-1);
        AtomicInteger tickCount = new AtomicInteger(0);
        CountDownLatch expired = new CountDownLatch(1);

        QuizTimer timer = new QuizTimer(2, new QuizTimer.QuizTimerListener() {
            @Override
            public void onTick(int secondsRemaining) {
                lastTick.set(secondsRemaining);
                tickCount.incrementAndGet();
            }

            @Override
            public void onTimeExpired() {
                expired.countDown();
            }
        });

        timer.start();
        assertTrue(timer.isRunning());

        boolean finished = expired.await(6, TimeUnit.SECONDS);
        assertTrue(finished, "timer must reach 00:00 within the expected time");
        assertEquals(0, lastTick.get(), "last tick should be 0 seconds remaining");
        assertTrue(tickCount.get() >= 2, "onTick must be called once per second");
        assertFalse(timer.isRunning());
    }

    @Test
    @DisplayName("stop() interrupts the thread and prevents the expiry callback")
    void stopPreventsExpiry() throws Exception {
        AtomicInteger expiredCount = new AtomicInteger(0);

        QuizTimer timer = new QuizTimer(30, new QuizTimer.QuizTimerListener() {
            @Override
            public void onTick(int secondsRemaining) {
                // display would be updated here in the GUI (via invokeLater)
            }

            @Override
            public void onTimeExpired() {
                expiredCount.incrementAndGet();
            }
        });

        timer.start();
        Thread.sleep(1300);         // long enough for at least one 1-second tick
        timer.stop();               // Demonstrates interrupt() based cancellation
        Thread.sleep(600);          // no further ticks may happen after stop()

        assertFalse(timer.isRunning());
        assertEquals(0, expiredCount.get(), "expiry must not fire after stop()");
        assertTrue(timer.getSecondsRemaining() < 30, "countdown must have advanced");
    }

    @Test
    @DisplayName("Timer formats the remaining time as MM:SS")
    void formattingWorks() {
        assertEquals("10:00", QuizTimer.format(600));
        assertEquals("09:59", QuizTimer.format(599));
        assertEquals("00:00", QuizTimer.format(0));
        assertEquals("00:07", QuizTimer.format(7));
    }

    @Test
    @DisplayName("Invalid durations are rejected with IllegalArgumentException")
    void invalidDurationIsRejected() {
        QuizTimer.QuizTimerListener listener = new QuizTimer.QuizTimerListener() {
            @Override
            public void onTick(int secondsRemaining) {
            }

            @Override
            public void onTimeExpired() {
            }
        };
        assertThrows(IllegalArgumentException.class, () -> new QuizTimer(0, listener));
        assertThrows(IllegalArgumentException.class, () -> new QuizTimer(60, null));
    }
}
