package com.intuit.dev.patterns.ks2kust;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.intuit.dev.patterns.yaml2kpatch.YamlLoader.KeyEdit;
import com.intuit.dev.patterns.yaml2kpatch.YamlLoader.KindNameMap;

public class Ksonnet2KustomizeTest {
    static final List<String> INTUIT_BASES = Arrays
            .asList(new String[] { "https://github.intuit.com/dev-patterns/intuit-kustomize//intuit-service-appd-base" });
    static final KindNameMap NAME_MAP;
    static {
        NAME_MAP = new KindNameMap();
        List<KeyEdit> ingressKeyEditList = new ArrayList<>();
        ingressKeyEditList.add(new KeyEdit("spec.rules[*].http.paths[*].backend.serviceName", "ists", "ists-service"));
        NAME_MAP.put("Ingress", "ists", "ingress", ingressKeyEditList);
        NAME_MAP.put("Service", "ists", "service", null);
        List<KeyEdit> deploymentKeyEditList = new ArrayList<>();
        deploymentKeyEditList.add(new KeyEdit("spec.template.spec.containers[*].name", "ists", "app"));
        NAME_MAP.put("Deployment", "ists", "deployment", deploymentKeyEditList);
        NAME_MAP.put("Ingress", "ists-appd-ingress", "ingress", null);
        NAME_MAP.put("Service", "ists-appd-service", "service", null);
        NAME_MAP.put("Deployment", "ists-appd-deployment", "deployment", null);
    }

    @Test
    public void Ksonnet2Kustomize() {
        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        assertEquals(System.getProperty("user.dir"), sut.getBaseDir());
    }

    @Test
    public void baseDir() {
        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        sut.setBaseDir("someDir");
        assertEquals("someDir", sut.getBaseDir());
    }

    @Test
    public void runShellCommand_success() {
        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        sut.setBaseDir(".");
        List<String> output = ShellUtils.runShellCommand(sut.baseDir, "pwd");
        assertEquals(System.getProperty("user.dir"), output.get(0));
    }

    @Test(expected = RuntimeException.class)
    public void runShellCommand_fail() {
        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        sut.setBaseDir(".");
        List<String> output = ShellUtils.runShellCommand(sut.baseDir, "false");
        assertEquals(System.getProperty("user.dir"), output.get(0));
    }

    @Test
    @Ignore
    public void validateHasKsonnet() {
        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        sut.setBaseDir("/Users/oazmon/git/ists-deployment");
        sut.validateHasKsonnet();
    }

    @Test(expected = RuntimeException.class)
    public void validateHasKsonnet_fail() {
        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        sut.setBaseDir(".");
        sut.validateHasKsonnet();
    }

    @Test
    @Ignore
    public void validateNoKustomize() throws Exception {
        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        sut.setBaseDir("/Users/oazmon/git/ists-deployment");
        sut.validateNoKustomize();
    }

    @Test(expected = RuntimeException.class)
    public void validateNoKustomize_fail() throws Exception {
        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        sut.setBaseDir(".");
        sut.validateNoKustomize();
    }

    private File makeTestDir() throws IOException {
        File tmpDir = File.createTempFile("test-", ".dir");
        tmpDir.delete();
        tmpDir.mkdirs();
        return tmpDir;
    }

    private void deleteAll(File file) throws IOException {
        Files.walk(file.toPath()) //
                .sorted(Comparator.reverseOrder()) //
                .map(Path::toFile) //
                .forEach(File::delete);
    }

    @Test
    public void writeEnvironmentFiles() throws Exception {
        File testDir = makeTestDir();
        File envDir = new File(testDir, "environments/qal");
        envDir.mkdirs();
        Map<String, Map<String, Object>> testEnvPatchMap = new HashMap<>();
        Map<String, Object> testPatch = new HashMap<>();
        testEnvPatchMap.put("Ingress|v99|mytest|new", testPatch);
        testEnvPatchMap.put("Ingress|v99|mytest", testPatch);
        testPatch.put("kind", "ingress");
        testPatch.put("key", "value");

        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        sut.setBaseDir(testDir.getAbsolutePath());

        sut.writeFiles(envDir, testEnvPatchMap, null);

        assertTrue(new File(testDir, "environments/qal/kustomization.yaml").exists());
        assertEquals(
                "apiVersion: kustomize.config.k8s.io/v1beta1\n" + "kind: Kustomization\n\n" + "bases:\n"
                        + "- ../../app-base\n\n" //
                        + "resources:\n- Ingress-v99-mytest.yaml\n\n" //
                        + "patchesStrategicMerge:\n- Ingress-v99-mytest-patch.yaml\n",
                new String(Files.readAllBytes(new File(testDir, "environments/qal/kustomization.yaml").toPath())));
        assertTrue(new File(testDir, "environments/qal/Ingress-v99-mytest-patch.yaml").exists());
        assertEquals("kind: ingress\nkey: value\n",
                new String(Files.readAllBytes(new File(testDir, "environments/qal/Ingress-v99-mytest-patch.yaml").toPath())));

        deleteAll(testDir);
    }

    @Test
    @Ignore
    public void generateEnvironment() throws Exception {
        File testDir = new File("/Users/oazmon/git/ists-deployment"); // makeTestDir();
        final String ksonnetManifestPath = new File(testDir, "ks-manifest.yaml").getAbsolutePath();
        File envDir = new File(testDir, "environments/qal");

        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        sut.setBaseDir(testDir.getAbsolutePath());
        ShellUtils.runShellCommand(sut.baseDir,
                "/usr/local/bin/ks show default > " + Ksonnet2Kustomize.KSONNET_MANIFEST_NAME);

        sut.generateEnvironment(ksonnetManifestPath, envDir);
    }

    @Test
    public void writeBaseFiles() throws Exception {
        File testDir = makeTestDir();
        System.out.println(testDir);
        File appDir = new File(testDir, "app-base");
        appDir.mkdir();
        Map<String, Map<String, Object>> testPatchMap = new HashMap<>();
        Map<String, Object> testPatch = new HashMap<>();
        testPatchMap.put("Ingress", testPatch);
        testPatch.put("kind", "ingress");
        testPatch.put("key", "value");

        Ksonnet2Kustomize sut = new Ksonnet2Kustomize();
        sut.setBaseDir(testDir.getAbsolutePath());
        sut.writeFiles(appDir, testPatchMap, INTUIT_BASES);

        assertTrue(new File(appDir, "kustomization.yaml").exists());
        assertEquals("apiVersion: kustomize.config.k8s.io/v1beta1\n" + //
                "kind: Kustomization\n" + //
                "\n" + //
                "namePrefix: null-\n" + //
                "\n" + //
                "bases:\n" + //
                "- " + INTUIT_BASES.get(0) + "\n" + //
                "\n" + //
                "resources:\n" + //
                "\n" + //
                "patchesStrategicMerge:\n" + //
                "- Ingress-patch.yaml\n",
                new String(Files.readAllBytes(new File(testDir, "app-base/kustomization.yaml").toPath())));
        assertTrue(new File(testDir, "app-base/ingress-patch.yaml").exists());
        assertEquals("kind: ingress\nkey: value\n",
                new String(Files.readAllBytes(new File(testDir, "app-base/ingress-patch.yaml").toPath())));

        deleteAll(testDir);
    }

    @Test
    @Ignore
    public void generateKustomize() throws Exception {
        File testDir = new File("/Users/oazmon/git/ists-deployment");

        Ksonnet2Kustomize sut = new Ksonnet2Kustomize() //
                .setBaseDir(testDir.getAbsolutePath()) //
                .setNamePrefix("ists") //
                .setNameMap(NAME_MAP);
        sut.generateKustomize(INTUIT_BASES);
    }

}
