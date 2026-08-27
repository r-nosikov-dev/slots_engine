package com.slotengine.model.feature;

import java.util.List;
import java.util.Optional;

/**
 * Optional mechanics layered on the base evaluation. Missing features are simply off.
 */
public record FeatureSet(
        Optional<FreeSpinsFeature> freeSpins,
        Optional<BuyBonusFeature> buyBonus,
        Optional<AnteFeature> ante,
        Optional<CascadeFeature> cascade,
        Optional<ExpandingWildsFeature> expandingWilds,
        Optional<JackpotFeature> jackpot,
        boolean bothWays
) {

    public FeatureSet {
        freeSpins = freeSpins == null ? Optional.empty() : freeSpins;
        buyBonus = buyBonus == null ? Optional.empty() : buyBonus;
        ante = ante == null ? Optional.empty() : ante;
        cascade = cascade == null ? Optional.empty() : cascade;
        expandingWilds = expandingWilds == null ? Optional.empty() : expandingWilds;
        jackpot = jackpot == null ? Optional.empty() : jackpot;
    }

    public static FeatureSet none() {
        return new FeatureSet(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                false
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public record BuyBonusFeature(long costMultiplier, int guaranteedScatterCount) {
        public BuyBonusFeature {
            if (costMultiplier < 1) {
                throw new IllegalArgumentException("buy-bonus cost multiplier must be >= 1");
            }
            if (guaranteedScatterCount < 1) {
                throw new IllegalArgumentException("guaranteedScatterCount must be >= 1");
            }
        }
    }

    public record AnteFeature(double costMultiplier) {
        public AnteFeature {
            if (costMultiplier < 1.0) {
                throw new IllegalArgumentException("ante cost multiplier must be >= 1.0");
            }
        }
    }

    public record CascadeFeature(boolean enabled, int startMultiplier, int increment, int maxMultiplier) {
        public CascadeFeature {
            if (startMultiplier < 1 || increment < 0 || maxMultiplier < startMultiplier) {
                throw new IllegalArgumentException("invalid cascade multipliers");
            }
        }

        public static CascadeFeature off() {
            return new CascadeFeature(false, 1, 0, 1);
        }

        public static CascadeFeature increasing() {
            return new CascadeFeature(true, 1, 1, 8);
        }
    }

    public record ExpandingWildsFeature(boolean enabled) {
        public static ExpandingWildsFeature off() {
            return new ExpandingWildsFeature(false);
        }

        public static ExpandingWildsFeature on() {
            return new ExpandingWildsFeature(true);
        }
    }

    public record JackpotTier(
            String id,
            long seedCredits,
            long contributionPerMillion,
            double mysteryChancePerSpin
    ) {
        public JackpotTier {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("jackpot id required");
            }
            if (seedCredits < 0 || contributionPerMillion < 0) {
                throw new IllegalArgumentException("jackpot contribution must be >= 0");
            }
            if (mysteryChancePerSpin < 0 || mysteryChancePerSpin > 1) {
                throw new IllegalArgumentException("mysteryChancePerSpin must be in [0, 1]");
            }
        }
    }

    public record JackpotFeature(boolean enabled, List<JackpotTier> tiers) {
        public JackpotFeature {
            tiers = tiers == null ? List.of() : List.copyOf(tiers);
        }

        public static JackpotFeature off() {
            return new JackpotFeature(false, List.of());
        }
    }

    public static final class Builder {
        private FreeSpinsFeature freeSpins;
        private BuyBonusFeature buyBonus;
        private AnteFeature ante;
        private CascadeFeature cascade;
        private ExpandingWildsFeature expandingWilds;
        private JackpotFeature jackpot;
        private boolean bothWays;

        public Builder freeSpins(FreeSpinsFeature feature) {
            this.freeSpins = feature;
            return this;
        }

        public Builder buyBonus(long costMultiplier, int guaranteedScatters) {
            this.buyBonus = new BuyBonusFeature(costMultiplier, guaranteedScatters);
            return this;
        }

        public Builder ante(double costMultiplier) {
            this.ante = new AnteFeature(costMultiplier);
            return this;
        }

        public Builder cascade(CascadeFeature cascade) {
            this.cascade = cascade;
            return this;
        }

        public Builder expandingWilds(boolean enabled) {
            this.expandingWilds = new ExpandingWildsFeature(enabled);
            return this;
        }

        public Builder jackpot(JackpotFeature jackpot) {
            this.jackpot = jackpot;
            return this;
        }

        public Builder bothWays(boolean enabled) {
            this.bothWays = enabled;
            return this;
        }

        public FeatureSet build() {
            return new FeatureSet(
                    Optional.ofNullable(freeSpins),
                    Optional.ofNullable(buyBonus),
                    Optional.ofNullable(ante),
                    Optional.ofNullable(cascade),
                    Optional.ofNullable(expandingWilds),
                    Optional.ofNullable(jackpot),
                    bothWays
            );
        }
    }
}
