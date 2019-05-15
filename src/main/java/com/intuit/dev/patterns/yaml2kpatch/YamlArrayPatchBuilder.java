package com.intuit.dev.patterns.yaml2kpatch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlArrayPatchBuilder {
    private List<Object> original;

    public YamlArrayPatchBuilder(List<Object> original) {
        this.original = original;
    }

    public List<Object> buildFor(List<Object> target) {
        List<Object> patchMap = new ArrayList<>();

        for (Object targetValue : target) {
            final Object originalValue = getOriginal(targetValue);
            if (originalValue == null) {
                // add to patch (adding keys along the way to here)
                patchMap.add(targetValue);

            } else if (originalValue instanceof Map && targetValue instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<String, Object> originalMap = (Map<String, Object>) originalValue;
                @SuppressWarnings("unchecked")
                final Map<String, Object> targetMap = (Map<String, Object>) targetValue;
                Map<String, Object> patchSubMap = new YamlMapPatchBuilder(originalMap).buildFor(targetMap);
                if (patchSubMap.isEmpty()) {
                    continue;
                }
                Object nameValue = targetMap.get("name");
                if (nameValue != null) {
                    LinkedHashMap<String, Object> tmpMap = new LinkedHashMap<>();
                    tmpMap.put("name", nameValue);
                    tmpMap.putAll(patchSubMap);
                    patchSubMap = tmpMap;
                }
                patchMap.add(patchSubMap);

            } else if (originalValue instanceof List && targetValue instanceof List) {
                @SuppressWarnings("unchecked")
                final List<Object> originalList = (List<Object>) originalValue;
                @SuppressWarnings("unchecked")
                final List<Object> targetList = (List<Object>) targetValue;
                final List<Object> patchSubList = new YamlArrayPatchBuilder(originalList).buildFor(targetList);
                if (!patchSubList.isEmpty()) {
                    patchMap.add(patchSubList);
                }

            } else if (originalValue instanceof String && targetValue instanceof String) {
                final String originalString = (String) originalValue;
                final String targetString = (String) targetValue;
                if (!originalString.equals(targetString)) {
                    patchMap.add(targetString);
                }

            } else if (originalValue instanceof Number && targetValue instanceof Number) {
                final Number originalNumber = (Number) originalValue;
                final Number targetNumber = (Number) targetValue;
                if (!originalNumber.equals(targetNumber)) {
                    patchMap.add(targetNumber);
                }

            } else if (originalValue instanceof Boolean && targetValue instanceof Boolean) {
                final Boolean originalBoolean = (Boolean) originalValue;
                final Boolean targetBoolean = (Boolean) targetValue;
                if (!originalBoolean.equals(targetBoolean)) {
                    patchMap.add(targetBoolean);
                }

            } else {
                System.out.println();
                System.out.println("WARNING: unrecoginzed value type: [original=" + originalValue.getClass() + ", target="
                        + targetValue.getClass() + "]");
                System.out.println();
            }
        }

        return patchMap;
    }

    private Object getOriginal(Object targetValue) {
        if (targetValue instanceof Map) {
            for (Object originalValue : original) {
                if (!(originalValue instanceof Map)) {
                    continue;
                }
                Map<?, ?> originalMap = (Map<?, ?>) originalValue;
                Map<?, ?> targetMap = (Map<?, ?>) targetValue;
                if (originalMap.keySet().equals(targetMap.keySet())) {
                    boolean keyFound = false;
                    for (String key : new String[] { "name", "host" }) {
                        String name = (String) targetMap.get(key);
                        if (name != null) {
                            keyFound = true;
                            if (name.equals(originalMap.get("name"))) {
                                return originalValue;
                            }
                        }
                    }
                    if (!keyFound && originalMap.equals(targetMap)) {
                        return originalValue;
                    }
                }
            }
        } else if (targetValue instanceof List) {
            for (Object originalValue : original) {
                if (originalValue instanceof List) {
                    return originalValue;
                }
            }
        } else {
            for (Object originalValue : original) {
                if (originalValue.equals(targetValue)) {
                    return originalValue;
                }
            }
        }

        return null;
    }
}
