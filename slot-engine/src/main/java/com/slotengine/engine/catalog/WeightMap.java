package com.slotengine.engine.catalog;

import java.util.LinkedHashMap;
import java.util.Map;

final class WeightMap {

    private WeightMap() {
    }

    static Map<String, Integer> of(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("weight pairs required");
        }
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }
}
