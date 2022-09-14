package com.ericgha;

import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

public class JSpectre {

    public static final int UNRESTRICTED_SIZE = 10;
    public static final int RESTRICTED_SIZE = 10;
    private static final int[] data = new int[UNRESTRICTED_SIZE + RESTRICTED_SIZE];
    private static final Logger log = Logger.getLogger( JSpectre.class.getName() );
    private static final RandomGenerator random = new SplittableRandom();

    public static void prepareData(String restrictedCode) {
        if (restrictedCode.length() > RESTRICTED_SIZE) {
            throw new IllegalArgumentException( "Input restricted code exceeds available size" );
        }
        for (int i = 0; i < RESTRICTED_SIZE; i++) {
            data[i] = i;
        }
        for (int i = 0; i < restrictedCode.length(); i++) {
            data[UNRESTRICTED_SIZE + i] = restrictedCode.charAt( i );
        }
        for (int i = restrictedCode.length(); i < RESTRICTED_SIZE; i++) {
            data[UNRESTRICTED_SIZE + i] = 0;
        }
        log.info( "Set restricted to: " +
                Arrays.toString( Arrays.copyOfRange( data, UNRESTRICTED_SIZE, UNRESTRICTED_SIZE + RESTRICTED_SIZE ) ) );
    }

    public static int getUnrestricted(int i) {
        if (i < UNRESTRICTED_SIZE) {
            return data[i];
        }
        return 0;
    }

    public static void trainBranchPredictor(int cnt, AccessTime accessTime) {
        for (int i = 0; i < cnt; i++) {
            observeSpeculative( getUnrestricted( i % UNRESTRICTED_SIZE ), accessTime );
        }
    }

    /**
     * In this attack an <em>attack sequence</em> array is generated with unrestricted indices interspersed with a
     * restricted index.  The sequence is random and the idea is that the branch predictor is trained <em>in situ</em>
     * between restricted array accesses.  The impetus behind randomly programming the attack into the attack sequence
     * is to prevent the JVM from optimizing very predictable restricted calls in {@link JSpectre#predictRestricted}.
     * <br><br>
     * This attack doesn't work.
     *
     * @param trainCnt        avg number of accesses to unrestricted indices for each restricted access
     * @param restrictedIndex restricted index to access
     * @param reps            approximately the number of restrictedIndex accesses (technically median of normal
     *                        distribution)
     * @param accessTime      an access time object to record experimental results
     * @return accessTime object
     */
    public static AccessTime attack(int trainCnt, int restrictedIndex, int reps, AccessTime accessTime) {
        int[] attackSeq = generateAttackSequence( trainCnt, restrictedIndex, reps );
        accessTime.doFlush();
        for (int i = 0; i < attackSeq.length; i++) {
            int curIndex = attackSeq[i];
            observeSpeculative( getUnrestricted( curIndex ), accessTime );
            if (curIndex >= UNRESTRICTED_SIZE) {
                accessTime.scanObservable();
                accessTime.doFlush();
            }
        }
        return accessTime;
    }

    static int[] generateAttackSequence(int trainCnt, int restrictedIndex, int reps) {
        int[] attackSeq = new int[( trainCnt + 1 ) * reps];
        for (int i = 0; i < attackSeq.length; i++) {
            int elem = random.nextInt( 0, trainCnt + 1 );
            if (elem < trainCnt) {
                attackSeq[i] = elem % UNRESTRICTED_SIZE;
            } else {
                attackSeq[i] = restrictedIndex;
            }
        }
        return attackSeq;
    }

    private static void observeSpeculative(int i, AccessTime accessTime) {
        accessTime.accessObservable( getUnrestricted( i ) );
    }

    /**
     * This is one style of attack that trains the branch predictor and then exploits speculative execution.  This
     * currently does not work.
     *
     * @param i restricted index in data array
     * @return AccessTime object for the experiment
     */
    public static AccessTime predictRestricted(int i) {
        final AccessTime accessTime = new AccessTime();
        final int[] observable = accessTime.getObservable();
        RandomGenerator random = new SplittableRandom();
        if (i < UNRESTRICTED_SIZE || i >= RESTRICTED_SIZE + UNRESTRICTED_SIZE) {
            throw new IllegalArgumentException( "Index i does not refer to a restricted element" );
        }
        for (int j = 0; j < 1000; j++) {
            accessTime.doFlush();
            trainBranchPredictor( 1_000, accessTime );
            observeSpeculative( i, accessTime );
            accessTime.scanObservable();
        }
        return accessTime;
    }

    public static void main(String[] args) {
        prepareData( "ABCDEFGHIJ" );
        AccessTime accessTime = attack( 1000, UNRESTRICTED_SIZE, 1000, new AccessTime() );
//        AccessTime accessTime = predictRestricted( UNRESTRICTED_SIZE );
//        neither of these attacks work, for both what we'd expect to see is low times for 0-9 as
//        these elements were used for training the branch predictor, and a low result for 65
//        as 'A' = 65.
        long[] times = accessTime.report();
        long missLow = Integer.MAX_VALUE;
        long missHigh = Integer.MIN_VALUE;
        long hitLow = Integer.MAX_VALUE;
        long hitHigh = Integer.MIN_VALUE;
        for (int i = 0; i < times.length; i++) {
            if (i < UNRESTRICTED_SIZE) {
                hitLow = Math.min( hitLow, times[i] );
                hitHigh = Math.max( hitHigh, times[i] );
            } else {
                missLow = Math.min( missLow, times[i] );
                missHigh = Math.max( missHigh, times[i] );
            }
        }
        // These are accumulated times
        System.out.printf( "Cache Hits (ms): Low: %.3f; High: %.3f.%n", AccessTime.toMs( hitLow ), AccessTime.toMs( hitHigh ) );
        System.out.printf( "Cache Misses (ms): Low: %.3f; High: %.3f%n", AccessTime.toMs( missLow ), AccessTime.toMs( missHigh ) );
        System.out.printf( "Experiment index: %d; Time (ms): %.3f.%n", data[RESTRICTED_SIZE], AccessTime.toMs( times[data[RESTRICTED_SIZE]] ) );
    }
}
