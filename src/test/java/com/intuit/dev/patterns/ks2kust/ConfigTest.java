package com.intuit.dev.patterns.ks2kust;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import com.intuit.dev.patterns.yaml2kpatch.YamlLoader.KindNameMap;

public class ConfigTest {

    @Test
    public void getDefaultConfig() {
        InputStream iStream = Config.getDefaultConfig();
        assertNotNull(iStream);
        Object o = new Yaml().load(iStream);
        assertNotNull(o);
    }

    @Test
    public void getServiceName() throws Exception {
        Config sut = new Config(null, "my-service");
        assertEquals("my-service", sut.getServiceName());
    }

    @Test
    public void interpolate() throws Exception {
        Config sut = new Config(null, "my-service");
        String result = sut.interpolate("${service-name}${service-name} ${service-name}");
        assertEquals("my-servicemy-service my-service", result);
    }

    @Test
    public void Config_nullConfigFile() throws Exception {
        Map<String, Object> expected = new Yaml().load(Config.getDefaultConfig());
        Config sut = new Config(null, "my-service");
        assertEquals(expected, sut.configMap);
    }

    @Test
    public void Config_emptysConfigFile() throws Exception {
        Map<String, Object> expected = new Yaml().load("");
        File file = File.createTempFile("config-", ".yaml");
        Config sut = new Config(file, "my-service");
        assertEquals(expected, sut.configMap);
    }

    @Test
    public void getKindNameMap() throws Exception {
        Config sut = new Config(null, "my-service");
        KindNameMap map = sut.getKindNameMap();
        assertEquals(6, map.size());
    }

    @Test
    public void getBases() throws Exception {
        Config sut = new Config(null, "my-service");
        List<String> list = sut.getBases();
        assertEquals("[https://github.intuit.com/dev-patterns/intuit-kustomize//intuit-service-appd-base?ref=v0.0.4]", list.toString());
    }

}
