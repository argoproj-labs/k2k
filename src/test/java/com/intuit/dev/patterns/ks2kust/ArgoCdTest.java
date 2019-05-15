package com.intuit.dev.patterns.ks2kust;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.intuit.dev.patterns.ks2kust.ArgoCd.Action;

public class ArgoCdTest {

    private File setupResource(String resourceName) throws IOException {
        try (InputStream iStream = JenkinsfileTranslatorTest.class.getResourceAsStream(resourceName)) {
            File resourceFile = File.createTempFile(resourceName, "");
            FileUtils.copyToFile(iStream, resourceFile);
            return resourceFile;
        }
    }

    @Test
    @Ignore
    public void pushUpdatesNow() throws Exception {
        File saveAppYamlFile = JenkinsfileTranslator.APP_YAML_FILE;
        try {
            JenkinsfileTranslator.APP_YAML_FILE = setupResource("/app.yaml");
            ArgoCd argoCd = new ArgoCd("ists");
            argoCd.pushUpdatesNow(Action.TO_KUSTOMIZE, null);
        } finally {
            JenkinsfileTranslator.APP_YAML_FILE = saveAppYamlFile;
        }
    }
}
