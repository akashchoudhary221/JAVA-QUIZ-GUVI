package com.quizplatform.timer;

/**
 * Quiz countdown timer running on a DEDICATED BACKGROUND THREAD.
 *
 * Demonstrates MULTITHREADING:
 *   - implements Runnable and is started through a new Thread
 *   - Thread.sleep(1000) counts the seconds down
 *   - Thread.interrupt() stops the timer cleanly
 *   - the Swing EDT is never blocked (the GUI registers a listener and
 *     updates labels through SwingUtilities.invokeLater)
 *   - the "volatile" flag is shared between the GUI thread and the timer thread
 */
public class QuizTimer implements Runnable {

    /** Callback interface - implemented by the GUI panel (and by unit tests). */
    public interface QuizTimerListener {
        /** Called once per second with the remaining seconds. */
        void onTick(int secondsRemaining);

        /** Called exactly once when the countdown reaches 00:00 (auto-submit). */
        void onTimeExpired();
    }

    private final int totalSeconds;
    private final QuizTimerListener listener;

    /** Written by the GUI thread, read by the timer thread -> volatile. */
    private volatile boolean running;

    private Thread worker;
    private int secondsRemaining;

    public QuizTimer(int totalSeconds, QuizTimerListener listener) {
        if (totalSeconds <= 0) {
            throw new IllegalArgumentException("Quiz duration must be greater than zero seconds.");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Timer listener must not be null.");
        }
        this.totalSeconds = totalSeconds;
        this.secondsRemaining = totalSeconds;
        this.listener = listener;
    }

    /** Starts the background thread (safe to call only once per attempt). */
    public synchronized void start() {
        if (worker != null && worker.isAlive()) {
            return;
        }
        secondsRemaining = totalSeconds;
        running = true;
        worker = new Thread(this, "QuizTimer-Thread");
        worker.setDaemon(true);   // does not keep the JVM alive on exit
        worker.start();
    }

    /** Stops the countdown and interrupts the sleeping thread. */
    public synchronized void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    /** Formats the remaining time as MM:SS (e.g. 09:59). */
    public String getFormattedTime() {
        return format(secondsRemaining);
    }

    public static String format(int totalSeconds) {
        int minutes = Math.max(totalSeconds, 0) / 60;
        int seconds = Math.max(totalSeconds, 0) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void run() {
        listener.onTick(secondsRemaining);          // initial display, e.g. 10:00
        while (running && secondsRemaining > 0) {
            try {
                Thread.sleep(1000);                 // one tick per second
            } catch (InterruptedException e) {
                // Demonstrates interrupt handling: stop() interrupted us - exit cleanly
                Thread.currentThread().interrupt();
                break;
            }
            if (!running) {
                break;
            }
            secondsRemaining--;
            listener.onTick(secondsRemaining);
        }
        if (running && secondsRemaining <= 0) {
            running = false;
            listener.onTimeExpired();               // auto-submit callback (timer thread!)
        }
    }
}
