package com.slotengine.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record Symbol(
        String id,
        String displayName,
        SymbolKind kind,
        Set<String> substitutes,
        Set<String> cannotReplace,
        int wildMultiplier,
        int tier
) {

    public Symbol {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(kind, "kind");
        substitutes = Set.copyOf(substitutes == null ? Set.of() : substitutes);
        cannotReplace = Set.copyOf(cannotReplace == null ? Set.of() : cannotReplace);
        if (id.isBlank()) {
            throw new IllegalArgumentException("symbol id must not be blank");
        }
        if (wildMultiplier < 1) {
            throw new IllegalArgumentException("wildMultiplier must be >= 1");
        }
    }

    public static Symbol regular(String id) {
        return regular(id, id, 1);
    }

    public static Symbol regular(String id, String displayName, int tier) {
        return new Symbol(id, displayName, SymbolKind.REGULAR, Set.of(), Set.of(), 1, tier);
    }

    public static Symbol wild(String id) {
        return wild(id, 1);
    }

    public static Symbol wild(String id, int multiplier) {
        return new Symbol(id, id, SymbolKind.WILD, Set.of("*"), Set.of(), multiplier, 0);
    }

    public static Symbol scatter(String id) {
        return new Symbol(id, id, SymbolKind.SCATTER, Set.of(), Set.of(), 1, 0);
    }

    public boolean isWild() {
        return kind == SymbolKind.WILD;
    }

    public boolean isScatter() {
        return kind == SymbolKind.SCATTER;
    }

    public boolean isSpecial() {
        return kind != SymbolKind.REGULAR && kind != SymbolKind.WILD;
    }

    /**
     * Whether this wild can stand in for {@code target}. Regular/scatter symbols return false.
     * A substitutes set of {@code *} means "all non-special symbols except cannotReplace".
     */
    public boolean substitutesFor(Symbol target) {
        if (!isWild() || target == null) {
            return false;
        }
        if (target.isSpecial() || target.isWild()) {
            return false;
        }
        if (cannotReplace.contains(target.id())) {
            return false;
        }
        return substitutes.contains("*") || substitutes.contains(target.id());
    }

    public Symbol withCannotReplace(Set<String> ids) {
        Set<String> merged = new LinkedHashSet<>(cannotReplace);
        merged.addAll(ids);
        return new Symbol(id, displayName, kind, substitutes, merged, wildMultiplier, tier);
    }
}
