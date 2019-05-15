package com.intuit.dev.patterns.yaml2kpatch;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class YamlMapPatchBuilder {
    private Map<String, Object> original;

    public YamlMapPatchBuilder(Map<String, Object> original) {
        this.original = original;
    }

    public LinkedHashMap<String, Object> buildFor(Map<String, Object> target) {
        LinkedHashMap<String, Object> patchMap = new LinkedHashMap<>();

        for (Entry<String, Object> targetEntry : target.entrySet()) {
            final String key = targetEntry.getKey();
            final Object targetValue = targetEntry.getValue();
            final Object originalValue = original.get(key);
            if (originalValue == null) {
                patchMap.put(key, targetValue);

            } else if (originalValue instanceof Map && targetValue instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> originalMap = (Map<String, Object>) originalValue;
                @SuppressWarnings("unchecked")
                final Map<String, Object> targetMap = (Map<String, Object>) targetValue;
                final Map<String, Object> patchSubMap = new YamlMapPatchBuilder(originalMap).buildFor(targetMap);
                if (!patchSubMap.isEmpty()) {
                    patchMap.put(key, patchSubMap);
                }

            } else if (originalValue instanceof List && targetValue instanceof List) {
                 @SuppressWarnings("unchecked")
                 final List<Object> originalList = (List<Object>) originalValue;
                 @SuppressWarnings("unchecked")
                 final List<Object> targetList = (List<Object>) targetValue;
                 final List<Object> patchSubList = new YamlArrayPatchBuilder(originalList).buildFor(targetList);
                 if(! patchSubList.isEmpty()) {
                     patchMap.put(key, patchSubList);
                 }

            } else if (originalValue instanceof String && targetValue instanceof String) {
                final String originalString = (String) originalValue;
                final String targetString = (String) targetValue;
                if (!originalString.equals(targetString)) {
                    patchMap.put(key, targetString);
                }

            } else if (originalValue instanceof Number && targetValue instanceof Number) {
                final Number originalNumber = (Number) originalValue;
                final Number targetNumber = (Number) targetValue;
                if (!originalNumber.equals(targetNumber)) {
                    patchMap.put(key, targetNumber);
                }

            } else if (originalValue instanceof Boolean && targetValue instanceof Boolean) {
                final Boolean originalBoolean = (Boolean) originalValue;
                final Boolean targetBoolean  = (Boolean) targetValue;
                if (!originalBoolean.equals(targetBoolean )) {
                    patchMap.put(key, targetBoolean);
                }

            } else {
                patchMap.put(key, targetValue);
                System.out.println();
                System.out.println("WARNING: unrecoginzed value type: [original=" + originalValue.getClass() + ", target="
                        + targetValue.getClass() + "]");
                System.out.println();
            }
        }

        return patchMap;
    }

}
