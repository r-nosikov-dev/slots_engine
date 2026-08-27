package com.slotengine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Designer targets. The engine does not enforce RTP at runtime — the simulator
 * measures it from the reel strips and paytable.
 */
public record MathSpec(
        BigDecimal targetRtp,
        VolatilityClass volatility,
        int maxWinMultiplier,
        Integer hitFrequencyPercent,
        String notes
) {

    public MathSpec {
        Objects.requireNonNull(targetRtp, "targetRtp");
        Objects.requireNonNull(volatility, "volatility");
        if (targetRtp.compareTo(BigDecimal.ZERO) <= 0 || targetRtp.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("target RTP must be in (0, 1], got " + targetRtp);
        }
        targetRtp = targetRtp.setScale(4, RoundingMode.HALF_UP);
        if (maxWinMultiplier < 1) {
            throw new IllegalArgumentException("maxWinMultiplier must be >= 1");
        }
        if (notes == null) {
            notes = "";
        }
    }

    public static MathSpec of(double rtp, VolatilityClass volatility, int maxWin) {
        return new MathSpec(BigDecimal.valueOf(rtp), volatility, maxWin, null, "");
    }
}
