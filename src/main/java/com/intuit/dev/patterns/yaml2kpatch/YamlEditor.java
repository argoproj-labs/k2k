package com.intuit.dev.patterns.yaml2kpatch;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class YamlEditor {
    private YamlEditor() {
    }

    public static boolean removeKeyStartsWith(Object yamlObject, String prefix) {
        boolean changed = false;
        if (yamlObject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;
            Iterator<Entry<String, Object>> it = yamlMap.entrySet().iterator();
            while(it.hasNext()) {
//            for (Entry<String, Object> entry : yamlMap.entrySet()) {
                Entry<String, Object> entry = it.next();
                if (entry.getKey().startsWith(prefix)) {
                    it.remove();
                    changed = true;
                } else {
                    changed |= removeKeyStartsWith(entry.getValue(), prefix);
                    if (isEmpty(entry.getValue())) {
                        it.remove();
                    }
                }
            }
        } else if (yamlObject instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> yamlList = (List<Object>) yamlObject;
            Iterator<Object> it = yamlList.iterator();
            while (it.hasNext()) {
                Object entry = it.next();
                changed |= removeKeyStartsWith(entry, prefix);
                if (isEmpty(entry)) {
                    it.remove();
                }
            }
        }
        return changed;
    }

    public static boolean isEmpty(Object yamlObject) {
        if (yamlObject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;
            return yamlMap.isEmpty();
        } else if (yamlObject instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> yamlList = (List<Object>) yamlObject;
            return yamlList.isEmpty();
        } else {
            return false;
        }
    }

}
