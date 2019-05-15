package com.intuit.dev.patterns.ks2kust;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.intuit.dev.patterns.ks2kust.JenkinsfileTranslator.EnvironmentData;

public class JenkinsfileTranslatorTest {

    private File setupResource(String resourceName) throws IOException {
        try (InputStream iStream = JenkinsfileTranslatorTest.class.getResourceAsStream(resourceName)) {
            File resourceFile = File.createTempFile(resourceName, "");
            FileUtils.copyToFile(iStream, resourceFile);
            return resourceFile;
        }
    }

    @Test
    public void getParameterMap() throws Exception {
        File tmpFile = File.createTempFile("Jenkinsfile.", "");
        try {
            FileUtils.write(tmpFile,
                    "def argocd_server = new URL(\"https://msaas.argocd.tools-k8s-prd.a.intuit.com\").getHost() + \":443\"",
                    Charset.defaultCharset());
            Map<String, String> result = JenkinsfileTranslator.getParameterMap(tmpFile.getAbsolutePath());
            assertEquals("msaas.argocd.tools-k8s-prd.a.intuit.com", result.get("argocd_server"));
        } finally {
            tmpFile.delete();
        }
    }

    @Test
    public void getEnvironmentMap() throws IOException {
        File saveAppYamlFile = JenkinsfileTranslator.APP_YAML_FILE;
        try {
            JenkinsfileTranslator.APP_YAML_FILE = setupResource("/app.yaml");

            Map<String, EnvironmentData> result = JenkinsfileTranslator.getEnvironmentMap();
            assertEquals(3, result.size());
            assertEquals(
                    new EnvironmentData("https://api-tools-sgmnt-prod-clus-7a3fcq-1154882921.us-west-2.elb.amazonaws.com",
                            "dev-patterns-ists-usw2-prd-prd"),
                    result.get("prd"));
            assertEquals(
                    new EnvironmentData("https://api-tools-sgmnt-ppd-usw2--s8maps-2116249654.us-west-2.elb.amazonaws.com",
                            "dev-patterns-ists-usw2-ppd-e2e"),
                    result.get("e2e"));
            assertEquals(
                    new EnvironmentData("https://api-tools-sgmnt-ppd-usw2--s8maps-2116249654.us-west-2.elb.amazonaws.com",
                            "dev-patterns-ists-usw2-ppd-qal"),
                    result.get("qal"));
        } finally {
            JenkinsfileTranslator.APP_YAML_FILE = saveAppYamlFile;
        }
    }

    @Test
    public void upgrade() throws Exception {
        File saveAppYamlFile = JenkinsfileTranslator.APP_YAML_FILE;
        File original = null;
        File expected = null;
        try {
            JenkinsfileTranslator.APP_YAML_FILE = setupResource("/app.yaml");
            original = setupResource("/JenkinsfileV1.before.txt");
            expected = setupResource("/JenkinsfileV1.after.txt");

            JenkinsfileTranslator sut = new JenkinsfileTranslator(original.getAbsolutePath());
            sut.upgrade();

            String[] expectedLines = (String[]) FileUtils.readLines(expected, Charset.defaultCharset())
                    .toArray(new String[0]);
            String[] originalLines = (String[]) FileUtils.readLines(original, Charset.defaultCharset())
                    .toArray(new String[0]);
            int ixMax = (int) Math.min(originalLines.length, expectedLines.length);
            for (int ix = 0; ix < ixMax; ix++) {
                assertEquals("Line " + (ix + 1), expectedLines[ix], originalLines[ix]);
                System.out.println("Line " + (ix + 1) + ":" + expectedLines[ix]);
            }
            assertArrayEquals(expectedLines, originalLines);
        } finally {
            JenkinsfileTranslator.APP_YAML_FILE = saveAppYamlFile;
            if (original != null) {
                original.delete();
            }
            if (expected != null) {
                original.delete();
            }
        }
    }

}
