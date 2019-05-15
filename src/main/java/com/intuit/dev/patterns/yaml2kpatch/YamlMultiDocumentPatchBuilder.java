package com.intuit.dev.patterns.yaml2kpatch;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.intuit.dev.patterns.yaml2kpatch.YamlLoader.KindNameMap;

public class YamlMultiDocumentPatchBuilder {
    private String originalFilename;
    private KindNameMap nameMap;
    private Map<String, Map<String, Object>> original;

    public YamlMultiDocumentPatchBuilder(Map<String, Map<String, Object>> original) {
        this.original = original;
        this.originalFilename = null;
        this.nameMap = null;
    }

    public YamlMultiDocumentPatchBuilder(String originalFilename) {
        System.out.println("Patch base set to: " + originalFilename);
        this.original = null;
        this.originalFilename = originalFilename;
        this.nameMap = null;
    }

    public YamlMultiDocumentPatchBuilder setKindNameMap(KindNameMap nameMap) {
        this.nameMap = nameMap;
        return this;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> buildFor(Map<String, Map<String, Object>> target) throws IOException {
        if(original == null) {
            original = new YamlLoader(originalFilename).mapAllNames(nameMap).load();
        }

        Map<String, Map<String, Object>> patchMap = new LinkedHashMap<>();

        for (Entry<String, Map<String, Object>> targetEntry : target.entrySet()) {
            final String key = targetEntry.getKey();
            final Map<String, Object> targetValue = targetEntry.getValue();
            final Map<String, Object> originalValue = original.get(key);
            if (originalValue == null) {
                // add to a new resource (adding keys along the way to here)
                patchMap.put(key + "|new", targetValue);
            } else {
                LinkedHashMap<String, Object> patch = new YamlMapPatchBuilder(originalValue).buildFor(targetValue);
                if (!patch.isEmpty()) {
                    LinkedHashMap<String, Object> document = new LinkedHashMap<>();
                    document.put("kind", originalValue.get("kind"));
                    document.put("apiVersion", originalValue.get("apiVersion"));
                    Map<String, Object> originalMetadata = (Map<String, Object>)originalValue.get("metadata");
                    Map<String, Object> metadata = (Map<String, Object>) patch.remove("metadata");
                    if(metadata == null) {
                        metadata = new LinkedHashMap<>();
                    }
                    metadata.put("name", originalMetadata.get("name"));
                    document.put("metadata", metadata);
                    document.putAll(patch);
                    patchMap.put(key, document);
                }
            }
        }

        return patchMap;
    }

    public Map<String, Map<String, Object>> buildFor(String targetFilename) throws IOException {
        System.out.println("Building patch for target: " + targetFilename);
        return this.buildFor(new YamlLoader(targetFilename).mapAllNames(nameMap).load());
    }
}
