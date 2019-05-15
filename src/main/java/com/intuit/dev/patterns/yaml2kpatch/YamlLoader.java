package com.intuit.dev.patterns.yaml2kpatch;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

public class YamlLoader {
    public static final class KindNameMapKey {

        private String kind;
        private String name;

        public KindNameMapKey(String kind, String name) {
            this.kind = kind;
            this.name = name;
        }

        public String getKind() {
            return kind;
        }

        public String getName() {
            return name;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((kind == null) ? 0 : kind.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            KindNameMapKey other = (KindNameMapKey) obj;
            if (kind == null) {
                if (other.kind != null)
                    return false;
            } else if (!kind.equals(other.kind))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "KindNameMapKey [kind=" + kind + ", name=" + name + "]";
        }

    }

    public static final class KeyEdit {
        private String key;
        private String expectedValue;
        private String newValue;

        public KeyEdit(String key, String expectedValue, String newValue) {
            super();
            this.key = key;
            this.expectedValue = expectedValue;
            this.newValue = newValue;
        }

        public String getKey() {
            return key;
        }

        public String getExpectedValue() {
            return expectedValue;
        }

        public String getNewValue() {
            return newValue;
        }
    }

    public static final class KindNameMapValue {
        private String targetName;
        private List<KeyEdit> keyEditList;

        public KindNameMapValue(String targetName, List<KeyEdit> keyEditList) {
            this.targetName = targetName;
            this.keyEditList = keyEditList;
        }

        public KindNameMapValue(String targetName, KeyEdit... keyEdits) {
            this.targetName = targetName;
            this.keyEditList = Arrays.asList(keyEdits);
        }

        public String getTargetName() {
            return targetName;
        }

        public List<KeyEdit> getKeyEditList() {
            return keyEditList;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "KindNameMapValue [targetName=" + targetName + ", keyEditList=" + keyEditList + "]";
        }
    }

    public static final class KindNameMap extends HashMap<KindNameMapKey, KindNameMapValue> {
        private static final long serialVersionUID = 1L;

        public KindNameMap() {
            super();
        }

        public KindNameMap(Map<? extends KindNameMapKey, ? extends KindNameMapValue> m) {
            super(m);
        }

        public KindNameMap put(String kind, String name, String newValue, List<KeyEdit> keyEditList) {
            KindNameMapKey k = new KindNameMapKey(kind, name);
            KindNameMapValue v = new KindNameMapValue(newValue, keyEditList);
            put(k, v);
            return this;
        }
    }

    static final DumperOptions YAML_DUMPER_OPTIONS = new DumperOptions();
    static final Representer YAML_CUSTOM_REPRESENTER = new Representer();
    static {
        YAML_CUSTOM_REPRESENTER.setPropertyUtils(new CustomPropertyUtils());
    }

    public static Yaml getYaml() {
        return new Yaml(YAML_CUSTOM_REPRESENTER, YAML_DUMPER_OPTIONS);
    }

    private String filename;
    private KindNameMap nameMap;

    public YamlLoader(String filename) {
        this.filename = filename;
        this.nameMap = new KindNameMap();
    }

    public YamlLoader mapName(String kind, String name, String targetName, List<KeyEdit> keyEditList) {
        nameMap.put(kind, name, targetName, keyEditList);
        return this;
    }

    public YamlLoader mapAllNames(KindNameMap nameMap) {
        if (nameMap != null) {
            this.nameMap.putAll(nameMap);
        }
        return this;
    }

    public Map<String, Map<String, Object>> load() throws IOException {
        try (InputStream iStream = new FileInputStream(filename)) {

            Map<String, Map<String, Object>> documentMap = new LinkedHashMap<String, Map<String, Object>>();
            Iterator<Object> it = getYaml().loadAll(iStream).iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (!(o instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) o;
                documentMap.put(makeDocumentKey(map), map);
            }
            return documentMap;
        }
    }

    String makeDocumentKey(Map<String, Object> map) {
        String apiVersion = (String) map.getOrDefault("apiVersion", "");
        String kind = (String) map.getOrDefault("kind", "");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) map.getOrDefault("metadata", null);
        String name = (String) metadata.getOrDefault("name", "");
        KindNameMapValue mappedValue = nameMap.get(new KindNameMapKey(kind, name));
        if (mappedValue != null) {
            if (mappedValue.getTargetName() != null) {
                System.out.println("Mapping kind: " + kind + " name: " + name + " to: " + mappedValue.getTargetName());
                metadata.put("name", mappedValue.getTargetName());
                name = mappedValue.getTargetName();
            }
            if (mappedValue.getKeyEditList() != null) {
                for (KeyEdit keyEdit : mappedValue.getKeyEditList()) {
                    performKeyEdit(map, keyEdit);
                }
            }
        }
        return kind + "|" + apiVersion + "|" + name;
    }

    void performKeyEdit(Map<String, Object> map, KeyEdit keyEdit) {
        Iterator<String> it = Arrays.asList(keyEdit.getKey().split("[.]")).iterator();
        performKeyEdit(map, it, keyEdit.expectedValue, keyEdit.newValue);
    }

    @SuppressWarnings("unchecked")
    private void performKeyEdit(Map<String, Object> subMap, Iterator<String> it, String expectedValue, String newValue) {
        if (!it.hasNext()) {
            return;
        }
        String part = it.next();
        if (!part.endsWith("[*]")) {
            Object entry = subMap.get(part);
            if (entry == null) {
            } else if (it.hasNext()) {
                if (entry instanceof Map) {
                    performKeyEdit((Map<String, Object>) entry, it, expectedValue, newValue);
                } else {
                    // Report error
                }
            } else {
                if (entry.toString().equals(expectedValue)) {
                    subMap.put(part, newValue);
                } else {
                    // Report Error
                }
            }
        } else /* part.endsWith("[*]") */ {
            part = part.substring(0, part.length() - 3);
            Object o = subMap.get(part);
            if (o instanceof List) {
                for (Object item : (List<?>) o) {
                    if (item instanceof Map) {
                        performKeyEdit((Map<String, Object>) item, it, expectedValue, newValue);
                    }
                }
            } else {
                // Report Error
            }
        }

    }
}
