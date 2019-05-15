package com.intuit.dev.patterns.ks2kust;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

import com.intuit.dev.patterns.yaml2kpatch.YamlLoader.KeyEdit;
import com.intuit.dev.patterns.yaml2kpatch.YamlLoader.KindNameMap;

public class Config {
    String serviceName;
    Map<String, Object> configMap;

    public static InputStream getDefaultConfig() {
        return Main.class.getResourceAsStream("/config.yaml");
    }

    public Config(File configFile, String serviceName) throws FileNotFoundException {
        this.serviceName = serviceName;
        InputStream configInputStream = (configFile != null) ? new FileInputStream(configFile) : getDefaultConfig();
        configMap = new Yaml().load(configInputStream);
    }

    private static final String INTERPOLATE_PATTERN = Pattern.quote("${service-name}");

    String interpolate(String value) {

        return value.replaceAll(INTERPOLATE_PATTERN, Matcher.quoteReplacement(this.serviceName));
    }

    public KindNameMap getKindNameMap() {
        KindNameMap kindNameMap = new KindNameMap();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> translations = (List<Map<String, Object>>) configMap.get("translations");
        for (Map<String, Object> translation : translations) {
            String kind = (String) translation.get("kind");
            String name = interpolate((String) translation.get("name"));
            String newValue = interpolate((String) translation.get("new-value"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subTranslations = (List<Map<String, Object>>) translation.getOrDefault("translations",
                    null);
            List<KeyEdit> keyEditList = null;
            if (subTranslations != null) {
                keyEditList = new ArrayList<>();
                for (Map<String, Object> subTranslation : subTranslations) {
                    String subPath = (String) subTranslation.get("path");
                    String subExpectedValue = interpolate((String) subTranslation.get("expected-value"));
                    String subNewValue = interpolate((String) subTranslation.get("new-value"));
                    keyEditList.add(new KeyEdit(subPath, subExpectedValue, subNewValue));
                }
            }
            kindNameMap.put(kind, name, newValue, keyEditList);
        }
        return kindNameMap;
    }

    @SuppressWarnings("unchecked")
    public List<String> getBases() {
        return (List<String>) configMap.getOrDefault("bases", Collections.emptyList());
    }

    public String getServiceName() {
        return serviceName;
    }

}
