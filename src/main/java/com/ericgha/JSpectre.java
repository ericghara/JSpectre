package com.ericgha;

import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

public class JSpectre {

    public static final int UNRESTRICTED_SIZE = 10;
    public static final int RESTRICTED_SIZE = 10;
    private static final int[] data = new int[UNRESTRICTED_SIZE + RESTRICTED_SIZE];
    private static final Logger log = Logger.getLogger( JSpectre.class.getName() );
    private static final Blackhole blackhole = new Blackhole( "Today's password is swordfish. I understand instantiating Blackholes directly is dangerous." );

    public static void setRestricted(String s) {
        if (s.length() > RESTRICTED_SIZE) {
            throw new IllegalArgumentException("Input restricted string exceeds available size");
        }
        for (int i = 0; i < s.length(); i++) {
            data[UNRESTRICTED_SIZE + i] = s.charAt(i);
        }
        for (int i = s.length(); i < RESTRICTED_SIZE; i++) {
            data[UNRESTRICTED_SIZE + i] = 0;
        }
        log.info("Set restricted to: "  +
                Arrays.toString(Arrays.copyOfRange(data, UNRESTRICTED_SIZE, UNRESTRICTED_SIZE + RESTRICTED_SIZE) ) );
    }

    public static int getUnrestricted(int i ) {
        if (i < UNRESTRICTED_SIZE) {
            return data[i];
        }
        return 0;
    }

    public static void trainBranchPredictor(int cnt, AccessTime accessTime) {
        for (int i = 0; i < cnt; i++) {
//            accessTime.accessObservable(getUnrestricted( i % UNRESTRICTED_SIZE ) );
            observeSpeculative( i, accessTime );
        }
    }

    private static void observeSpeculative(int i, AccessTime accessTime) {
        accessTime.accessObservable( getUnrestricted( i )  );
    }

    public static AccessTime predictRestricted(int i) {
        final AccessTime accessTime = new AccessTime();
        final int[] observable = accessTime.getObservable();
        RandomGenerator random = new SplittableRandom();
        if (i < UNRESTRICTED_SIZE || i >= RESTRICTED_SIZE + UNRESTRICTED_SIZE) {
            throw new IllegalArgumentException("Index i does not refer to a restricted element");
        }
        for (int j = 0; j < 1000; j++) {
            accessTime.doFlush();
            trainBranchPredictor( 5_000, accessTime );
            observeSpeculative( random.nextBoolean() ? i : 0, accessTime );
            accessTime.scanObservable();
        }
        return accessTime;
    }


    public static void main(String[] args) {
        setRestricted( "ABCDEFGHIJ" );
        AccessTime accessTime = predictRestricted( UNRESTRICTED_SIZE );
        accessTime.report();
    }
}
