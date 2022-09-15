package com.ericgha;

/**
 * Some concurrency compromises were made to reduce the interval between
 * tics.  {@code time} should only be modified from one thread.
 * Time will be read from 2 threads so there is the chance for a stale read.
 *
 * In practice resolution we get from non-volatile, non-synchronized access
 * to {@code time} seems worth it.
 */
public class Timer extends Thread {

    private long time;

    public Timer() {
        super();
    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            time++;
        }
    }

    public long getTime() {
        return time;
    }

    public static void main(String[] args) throws InterruptedException {
        Timer timer = new Timer();
        timer.start();
        Thread.sleep( 2 ); // allow timer thread to start
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
