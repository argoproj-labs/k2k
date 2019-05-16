package com.intuit.dev.patterns.ks2kust;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

import com.intuit.dev.patterns.ks2kust.JenkinsfileTranslator.EnvironmentData;

public class ArgoCd {
    public static enum Action {
        TO_KSONNET, TO_KUSTOMIZE
    };

    private String gitRepo;

    public ArgoCd(String gitRepo) throws IOException {
        this.gitRepo = gitRepo;
        if (this.gitRepo == null || gitRepo.isEmpty()) {
            final String[] cmd = new String[] { "sh", "-c",
                    "git remote -v | grep '\\(push\\)' | cut -d'/' -f4-5 | cut -d'.' -f1" };
            Process process = new ProcessBuilder(cmd).start();
            this.gitRepo = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
        }
    }

    private void runCommand(String[] command) throws IOException {
        System.out.println(String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command) //
                .inheritIO();
        builder.start();
    }

    public void pushUpdatesNow(Action action, String targetEnvName) throws IOException {
        Map<String, EnvironmentData> environmentMap = JenkinsfileTranslator.getEnvironmentMap();

        int envCount = 0;
        for (Entry<String, EnvironmentData> entry : environmentMap.entrySet()) {
            // update argocd now with kustomize arguments
            String envName = entry.getKey();
            if (targetEnvName != null && !targetEnvName.equals(envName)) {
                continue;
            }
            String cluster = entry.getValue().getCluster();
            String namespace = entry.getValue().getNamespace();

            switch (action) {
            case TO_KUSTOMIZE:
                // tell argocd to switch to Kustomize NOW
                runCommand(new String[] { //
                        "/usr/local/bin/argocd", "app", "create", //
                        "--name", namespace, //
                        "--repo", "https://" + gitRepo, //
                        "--path", "environments/" + envName, //
                        "--dest-server", cluster, //
                        "--dest-namespace", namespace, //
                        "--upsert" //
                });
                break;
            case TO_KSONNET:
                // tell argocd to switch back to Ksonnet NOW
                runCommand(new String[] { //
                        "/usr/local/bin/argocd", "app", "create", //
                        "--name", namespace, //
                        "--repo", "https://" + gitRepo, //
                        "--path", ".", //
                        "--env", envName, //
                        "--upsert" //
                });
                break;
            }

            // tell argocd to sync with prune
            runCommand(new String[] { //
                    "/usr/local/bin/argocd", "app", "sync", namespace, "--prune" //
            });

            envCount++;
        }
        System.out.println(envCount + " environment(s) updated.");
    }
}
