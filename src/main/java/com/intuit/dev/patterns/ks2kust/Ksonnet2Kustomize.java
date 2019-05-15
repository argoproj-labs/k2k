package com.intuit.dev.patterns.ks2kust;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.intuit.dev.patterns.yaml2kpatch.YamlEditor;
import com.intuit.dev.patterns.yaml2kpatch.YamlLoader.KindNameMap;
import com.intuit.dev.patterns.yaml2kpatch.YamlMultiDocumentPatchBuilder;

public class Ksonnet2Kustomize {
    public static final String KSONNET_APP_YAML = "app.yaml";
    public static final String ENVIRONMENTS_DIR_NAME = "environments";
    public static final String APP_BASE_DIR_NAME = "app-base";
    public static final String KUSTOMIZATION_FILE_NAME = "kustomization.yaml";
    static final String KSONNET_MANIFEST_NAME = "ks-manifest.yaml";
    static final String KUSTOMIZE_MANIFEST_NAME = "kustomize-manifest.yaml";
    static final DumperOptions YAML_DUMPER_OPTIONS;
    static {
        YAML_DUMPER_OPTIONS = new DumperOptions();
        YAML_DUMPER_OPTIONS.setIndent(2);
        YAML_DUMPER_OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    }

    public static void cleanKustomize() throws IOException {
        // Test app.yaml exists and is good
        try {
            JenkinsfileTranslator.getEnvironmentMap();
        } catch (IOException e) {
            // ABORT
            throw new IOException("Unable to remove Kustomize after Ksonnet removal.");
        }
        FileUtils.deleteDirectory(new File(APP_BASE_DIR_NAME));
        System.out.println("Deleted app-base directory");
        // rm -f template-k8s/environments/*/*.yaml
        for (File envDir : new File(ENVIRONMENTS_DIR_NAME).listFiles()) {
            if (envDir.isDirectory()) {
                for (File file : envDir.listFiles()) {
                    if (file.getName().endsWith(".yaml")) {
                        FileUtils.deleteQuietly(file);
                        System.out.println("Deleted " + file);
                    }
                }
            }
        }
    }

    public static void cleanKsonnet() throws IOException {
        FileUtils.deleteQuietly(new File(KSONNET_APP_YAML));
        FileUtils.deleteDirectory(new File("components"));
        System.out.println("Deleted components directory");
        FileUtils.deleteDirectory(new File("lib"));
        System.out.println("Deleted lib directory");
        for (File envDir : new File(ENVIRONMENTS_DIR_NAME).listFiles()) {
            if (envDir.isDirectory()) {
                for (File file : envDir.listFiles()) {
                    if (file.getName().endsWith(".jsonnet") || file.getName().endsWith(".libsonnet")) {
                        FileUtils.deleteQuietly(file);
                        System.out.println("Deleted " + file);
                    }
                }
            }
        }

    }

    File baseDir;
    String namePrefix;
    KindNameMap nameMap;
    private boolean noDownTime;

    public Ksonnet2Kustomize() {
        baseDir = new File(".").getAbsoluteFile().getParentFile();
    }

    public Ksonnet2Kustomize setBaseDir(String baseDir) {
        this.baseDir = new File(baseDir);
        return this;
    }

    public String getBaseDir() {
        return this.baseDir.getPath();
    }

    public Ksonnet2Kustomize setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }

    public String getNamePrefix() {
        return this.namePrefix;
    }

    public Ksonnet2Kustomize setNameMap(KindNameMap nameMap) {
        this.nameMap = nameMap;
        return this;
    }

    public KindNameMap getNameMap() {
        return this.nameMap;
    }

    public Ksonnet2Kustomize setNoDownTime(boolean noDownTime) {
        this.noDownTime = noDownTime;
        return this;
    }

    public boolean isNoDownTime() {
        return this.noDownTime;
    }

    public void validateHasKsonnet() {
        if (!new File(baseDir, KSONNET_APP_YAML).canRead()) {
            throw new RuntimeException("Can't find " + KSONNET_APP_YAML + " file in the current directory.");
        }
    }

    public void validateNoKustomize() throws IOException {
        if (Files.walk(baseDir.toPath()).anyMatch(p -> KUSTOMIZATION_FILE_NAME.equals(p.getFileName().toString()))) {
            throw new RuntimeException("Found an existing " + KUSTOMIZATION_FILE_NAME
                    + " in the current directory tree. Unable to begin conversion.");
        }
    }

    public void generateKustomize(List<String> baseUrl) throws Exception {
        final String kustomizeManifestPath = new File(baseDir, KUSTOMIZE_MANIFEST_NAME).getAbsolutePath();
        final String ksonnetManifestPath = new File(baseDir, KSONNET_MANIFEST_NAME).getAbsolutePath();

        try {
            // ks show default (may require setting 'env') to ks-default-manifest.yaml
            ShellUtils.runShellCommand(baseDir, "/usr/local/bin/ks param set sample env 'default' --env=default");
            ShellUtils.runShellCommand(baseDir, "/usr/local/bin/ks show default > " + ksonnetManifestPath);

            // create app-base directory, if needed
            File appBaseDir = new File(baseDir, APP_BASE_DIR_NAME);
            appBaseDir.mkdirs();

            try {
                writeFiles(appBaseDir, Collections.emptyMap(), baseUrl);
                ShellUtils.runShellCommand(baseDir,
                        "cd " + APP_BASE_DIR_NAME + "; /usr/local/bin/kustomize build > " + kustomizeManifestPath);

                // run yaml2patch and add patch to app-base directory
                Map<String, Map<String, Object>> patchMap = new YamlMultiDocumentPatchBuilder(kustomizeManifestPath) //
                        .setKindNameMap(nameMap) //
                        .buildFor(ksonnetManifestPath);
                YamlEditor.removeKeyStartsWith(patchMap, "ksonnet.io/");
                writeFiles(appBaseDir, patchMap, baseUrl);

            } finally {
                // remove the kustomize-default.yaml file
                new File(kustomizeManifestPath).delete();
            }

            // for each environment (other than default) in ks:
            File[] envDirs = new File(baseDir, ENVIRONMENTS_DIR_NAME).listFiles();
            for (File envDir : envDirs) {
                if (envDir.isDirectory() && !"default".equals(envDir.getName())) {
                    generateEnvironment(ksonnetManifestPath, envDir);
                }
            }

        } finally {
            // remove the ks-default file
            new File(ksonnetManifestPath).delete();
        }
    }

    void generateEnvironment(String ksonnetManifestPath, File envDir) throws IOException {
        // ks show {environment} to ks-manifest.yaml
        String envKsonnetManifest = new File(envDir, KSONNET_MANIFEST_NAME).getAbsolutePath();
        String env = envDir.getName();
        try {
            ShellUtils.runShellCommand(baseDir, "/usr/local/bin/ks show " + env + " > " + envKsonnetManifest);

            // run yaml2patch comparing kustomize-default.yaml to environment and add patch to {environment} directory
            Map<String, Map<String, Object>> envPatchMap = new YamlMultiDocumentPatchBuilder(ksonnetManifestPath) //
                    .setKindNameMap(nameMap) //
                    .buildFor(envKsonnetManifest);
            YamlEditor.removeKeyStartsWith(envPatchMap, "ksonnet.io/");
            writeFiles(envDir, envPatchMap, null);

        } finally {
            if (!isNoDownTime()) {
                // remove the kustomize environment manifest yaml file
                new File(envKsonnetManifest).delete();
            }
        }
    }

    void writeFiles(File dir, Map<String, Map<String, Object>> patchMap, List<String> baseUrls) throws IOException {
        // create a kustomize.yaml pointing to app-base
        if (baseUrls != null) {
            System.out.println((patchMap == null ? "Creating" : "Updating") + " base for application at: " + dir);
        } else {
            System.out.println("Building for: " + dir);
        }

        try (FilePrintStream kustomizationWriter = new FilePrintStream( //
                new File(dir, KUSTOMIZATION_FILE_NAME))) {
            kustomizationWriter.println("apiVersion: kustomize.config.k8s.io/v1beta1");
            kustomizationWriter.println("kind: Kustomization");

            // write kustomize.yaml except for resources and patches
            if (baseUrls != null) {
                if (!baseUrls.isEmpty()) {
                    kustomizationWriter.println();
                    kustomizationWriter.println("namePrefix: " + namePrefix + "-");

                    // write patches and update kustomize.yaml
                    kustomizationWriter.println();
                    kustomizationWriter.println("bases:");
                    for (String baseUrl : baseUrls) {
                        kustomizationWriter.println("- " + baseUrl);
                    }
                }
            } else {
                kustomizationWriter.println();
                kustomizationWriter.println("bases:");
                kustomizationWriter.println("- ../../" + APP_BASE_DIR_NAME);
            }

            // write resources and patches, and add to kustomize.yaml
            if (!patchMap.isEmpty()) {
                kustomizationWriter.println();
                kustomizationWriter.println("resources:");
                for (Entry<String, Map<String, Object>> entry : patchMap.entrySet()) {
                    Map<String, Object> patch = entry.getValue();
                    String key = entry.getKey();
                    if (key.endsWith("|new")) {
                        final String filename = key.substring(0, key.length() - 4).replaceAll("[|/]", "-") + ".yaml";
                        try (FilePrintStream envPatchWriter = new FilePrintStream(new File(dir, filename))) {
                            envPatchWriter.print(new Yaml(YAML_DUMPER_OPTIONS).dump(patch));
                        }
                        kustomizationWriter.println("- " + filename);
                    }
                }
                if (baseUrls == null && isNoDownTime()) {
                    kustomizationWriter.println("- ks-manifest.yaml");
                }

                kustomizationWriter.println();
                kustomizationWriter.println("patchesStrategicMerge:");
                for (Entry<String, Map<String, Object>> entry : patchMap.entrySet()) {
                    Map<String, Object> patch = entry.getValue();
                    String key = entry.getKey();
                    if (!key.endsWith("|new")) {
                        final String filename = key.replaceAll("[|/]", "-") + "-patch.yaml";
                        try (FilePrintStream envPatchWriter = new FilePrintStream(new File(dir, filename))) {
                            envPatchWriter.print(new Yaml(YAML_DUMPER_OPTIONS).dump(patch));
                        }
                        kustomizationWriter.println("- " + filename);
                    }
                }
            }

        }
    }

}
