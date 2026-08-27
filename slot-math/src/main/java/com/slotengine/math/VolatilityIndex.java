package com.slotengine.math;

import com.slotengine.model.VolatilityClass;

/**
 * Maps observed standard deviation of (win / bet) per round to a volatility class.
 * Thresholds follow common video-slot studio practice for 5-reel games.
 */
public final class VolatilityIndex {

    private VolatilityIndex() {
    }

    /**
     * Classifies sample stddev of {@code win/bet} per round.
     * LOW &lt; 5, MEDIUM &lt; 10, MEDIUM_HIGH &lt; 15, HIGH &lt; 25, else VERY_HIGH.
     */
    public static VolatilityClass classify(double stddevOfWinOverBet) {
        if (stddevOfWinOverBet < 5.0) {
            return VolatilityClass.LOW;
        }
        if (stddevOfWinOverBet < 10.0) {
            return VolatilityClass.MEDIUM;
        }
        if (stddevOfWinOverBet < 15.0) {
            return VolatilityClass.MEDIUM_HIGH;
        }
        if (stddevOfWinOverBet < 25.0) {
            return VolatilityClass.HIGH;
        }
        return VolatilityClass.VERY_HIGH;
    }

    public static String bucket(double winX) {
        if (winX <= 0) {
            return "0";
        }
        if (winX < 1) {
            return "0-1x";
        }
        if (winX < 5) {
            return "1-5x";
        }
        if (winX < 15) {
            return "5-15x";
        }
        if (winX < 50) {
            return "15-50x";
        }
        if (winX < 100) {
            return "50-100x";
        }
        if (winX < 500) {
            return "100-500x";
        }
        return "500x+";
    }
}
