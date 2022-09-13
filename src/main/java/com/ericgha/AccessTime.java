package com.ericgha;

import org.openjdk.jmh.infra.Blackhole;

import java.util.Comparator;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AccessTime {

    public static final int OBSERVABLE_SIZE = 256;
    private static final int FLUSH_SIZE = 1_000_000;
    private static final int OBSERVABLE_OFFSET = 4096;
    private static final Logger log = Logger.getLogger(AccessTime.class.getName() );


    private RandomGenerator random = new SplittableRandom();
    private final int[] flush = new int[FLUSH_SIZE];
    private final int[] observable = new int[OBSERVABLE_OFFSET * (OBSERVABLE_SIZE +1)];
    private final long[] accessTimes = new long[OBSERVABLE_SIZE];

    public void doFlush() {
        for (int i = 0; i < FLUSH_SIZE; i++) {
            flush[i] = 1;
            if (i > OBSERVABLE_OFFSET) {
                int j = random.nextInt( 0, i - OBSERVABLE_OFFSET );
                flush[j] = 1;
            }
        }
    }

    long timeAccess(int i) {
        int index = (i + 1) * OBSERVABLE_OFFSET;
        long then = System.nanoTime();
        observable[index] += 88;
        return System.nanoTime() - then;
    }

    void scanObservable() {
        for (int i = 0; i < OBSERVABLE_SIZE; i++) {
            accessTimes[i] += timeAccess( i );
        }
    }

    public void accessObservable(int i) {
        observable[OBSERVABLE_OFFSET * (i + 1)] += 1;
    }

    public int[] getObservable() {
        return observable;
    }

    public long[] report() {
        for (int i = 0; i < OBSERVABLE_SIZE; i++) {
            System.out.printf("%d: %d ns.%n", i, accessTimes[i]);
        }
        System.out.print("Lowest three: ");
        IntStream.range(0, OBSERVABLE_SIZE).boxed()
                .map( i -> new int[] {(int) accessTimes[i] ,i} )
                .sorted( Comparator.comparingInt( tupA -> tupA[0] ) )
                .limit(3)
                .forEach(tup -> System.out.printf(" %d: %dns,", tup[1], tup[0]) );
        System.out.print(".\n");

        return accessTimes;
    }

    public static void main(String[] args) {
        AccessTime accessTime = new AccessTime();
        Set<Integer> toAccess = Set.of(13);
        for (int i = 0; i < 700; i++) {
            accessTime.doFlush();
            toAccess.forEach( accessTime::accessObservable );
            accessTime.scanObservable();
        }
        long[] times = accessTime.report();
        long noiseLow = Integer.MAX_VALUE;
        long noiseHigh = Integer.MIN_VALUE;
        long signalLow = Integer.MAX_VALUE;
        long signalHigh = Integer.MIN_VALUE;
        for (int i = 0; i < times.length; i++) {
            if (toAccess.contains(i) ) {
                signalLow = Math.min( signalLow, times[i] );
                signalHigh = Math.max( signalHigh, times[i] );
            }
            else {
                noiseLow = Math.min( noiseLow, times[i] );
                noiseHigh = Math.max( noiseHigh, times[i] );
            }
        }
        System.out.printf("Signal: ( Low: %d, High: %d )%n", signalLow, signalHigh);
        System.out.printf("Noise: ( Low: %d, High: %d )%n", noiseLow, noiseHigh);
    }
}
