package com.phicomm.smarthome.command.util;

import java.util.Map;

public class MapUtil {
    public static void filterMap(Map mapFilter, Map mapOri) {
        for (Object key : mapOri.keySet()) {
            Object value2 = mapOri.get(key);

            if (value2 instanceof Map) {
                if (mapFilter.containsKey(key)) {
                    Object value1 = mapFilter.get(key);
                    if (value1 instanceof Map) {
                        Map m1 = (Map) value1;
                        Map m2 = (Map) value2;
                        filterMap(m1, m2);
                    } else {
                        filterInner(mapFilter, key, value2);
                    }
                } else {
                    filterInner(mapFilter, key, value2);
                }
            } else {
                filterInner(mapFilter, key, value2);
            }
        }
    }

    private static void filterInner(Map map, Object key, Object value) {
        if (map.containsKey(key)) {
            map.put(key, value);
        }
    }

    public static void mergeMap(Map map1, Map map2) {
        for (Object key : map2.keySet()) {
            Object value2 = map2.get(key);
            if (value2 instanceof Map) {
                if (map1.containsKey(key)) {
                    Object value1 = map1.get(key);
                    if (value1 instanceof Map) {
                        Map m1 = (Map) value1;
                        Map m2 = (Map) value2;
                        mergeMap(m1, m2);
                    } else {
                        mergeInner(map1, key, value2);
                    }
                } else {
                    mergeInner(map1, key, value2);
                }
            } else {
                mergeInner(map1, key, value2);
            }
        }
    }

    private static void mergeInner(Map map, Object key, Object value) {
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }
}
