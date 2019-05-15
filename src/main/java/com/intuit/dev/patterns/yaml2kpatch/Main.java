package com.intuit.dev.patterns.yaml2kpatch;

import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.intuit.dev.patterns.yaml2kpatch.YamlLoader.KindNameMap;

public class Main {
    private static final DumperOptions YAML_DUMPER_OPTIONS;
    static {
        YAML_DUMPER_OPTIONS = new DumperOptions();
        YAML_DUMPER_OPTIONS.setIndent(2);
        YAML_DUMPER_OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    }

    public static String runPatch(KindNameMap nameMap, String filename1, String filename2) throws IOException {
        Map<String, Map<String, Object>> documentMap1 = new YamlLoader(filename1).mapAllNames(nameMap).load();
        Map<String, Map<String, Object>> documentMap2 = new YamlLoader(filename2).mapAllNames(nameMap).load();

        Map<String, Map<String, Object>> patchMap = //
                new YamlMultiDocumentPatchBuilder(documentMap1). //
                        setKindNameMap(nameMap). //
                        buildFor(documentMap2);
        YamlEditor.removeKeyStartsWith(patchMap, "ksonnet.io/");

        return new Yaml(YAML_DUMPER_OPTIONS).dumpAll(patchMap.values().iterator());
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new Exception("Expected: yaml2patch {original-yaml-file} {new-yaml-file}");
        }

        String result = runPatch(null, args[0], args[1]);
        System.out.println(result);
    }

}
