package com.ericgha;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * <b>Don't use. {@link System#nanoTime()} is far better</b>
 * <p>
 * This was a proof of concept to see if it was possible to create a timer with greater resolution than {@link System#nanoTime() };
 * <p>
 * Some concurrency compromises were made to reduce the interval between tics.  {@code time} should only be modified
 * from one thread. Time will be read from 2 threads, so there is the chance for a stale read.
 * <p>
 * In practice resolution we get from non-volatile, non-synchronized access to {@code time} seems worth it.
 */
public class Timer extends Thread {

    private static Logger log = Logger.getLogger( Timer.class.getName() );
    private long time;


    public Timer() {
        super("Timer: " + UUID.randomUUID());
    }

    @Override
    public void run() {
        log.info( "Started at: " + time + " tics" );
        while (!this.isInterrupted()) {
            time++;
        }
        log.info( "Stopped at: " + time + " tics" );
    }

    public long getTime() {
        return time;
    }

    public static void main(String[] args) throws InterruptedException {
        Timer timer = new Timer();
        timer.start();
        Thread.sleep( 100 ); // allow timer thread to start
        int x = 0;
        int y = -1;
        long[] times = new long[100];
        for (int i = 0; i < times.length; i++) {
            // busy work;
            x ^= -1;
            y ^= x;
            times[i] = timer.getTime();
        }
        timer.interrupt();
        for (int i = 1; i < times.length; i++) {
            long start = times[i - 1];
            long stop = times[i];
            System.out.printf( "run %d: %d tics%n", i, stop - start );
        }
        timer.join( 500 );
    }
}
