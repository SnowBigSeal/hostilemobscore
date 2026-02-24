package com.snowbigdeal.hostilemobscore;

/** Shared game constants. */
public final class Constants {

    private Constants() {}

    // -------------------------------------------------------------------------
    // Time
    // -------------------------------------------------------------------------

    /** Ticks per second in Minecraft. */
    public static final int TICKS_PER_SECOND = 20;

    /** Converts seconds to ticks. */
    public static int seconds(int seconds) {
        return seconds * TICKS_PER_SECOND;
    }

    /** Converts seconds to ticks (float overload). */
    public static int seconds(float seconds) {
        return Math.round(seconds * TICKS_PER_SECOND);
    }
}
