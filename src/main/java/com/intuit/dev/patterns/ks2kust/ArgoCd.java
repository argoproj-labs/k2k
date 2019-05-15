package com.intuit.dev.patterns.ks2kust;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import com.intuit.dev.patterns.ks2kust.JenkinsfileTranslator.EnvironmentData;

public class ArgoCd {
    public static enum Action {
        TO_KSONNET,
        TO_KUSTOMIZE
    };
    
    private String jenkinsfilePath;

    public ArgoCd(String jenkinsfilePath) {
        this.jenkinsfilePath = jenkinsfilePath;
    }

    private void runCommand(String[] command) throws IOException {
        System.out.println(Arrays.toString(command));
        ProcessBuilder builder = new ProcessBuilder(command) //
                .inheritIO();
        builder.start();
    }

    public void pushUpdatesNow(Action action, String targetEnvName) throws IOException {
        Map<String, String> parameterMap = JenkinsfileTranslator.getParameterMap(jenkinsfilePath);
        Map<String, EnvironmentData> environmentMap = JenkinsfileTranslator.getEnvironmentMap();
        
        String argocdServer = parameterMap.get("argocd_server");
        if(argocdServer == null) {
            throw new IOException("Unable to locate Argo CD Server destination in Jenkinsfile.");
        }

        // login
        runCommand(new String[] { //
                "/usr/local/bin/argocd", "login", parameterMap.get("argocd_server"), "--sso"//
        });

        int envCount = 0;
        for (Entry<String, EnvironmentData> entry : environmentMap.entrySet()) {
            // update argocd now with kustomize arguments
            String envName = entry.getKey();
            if (targetEnvName != null && !targetEnvName.equals(envName)) {
                continue;
            }
            String cluster = entry.getValue().getCluster();
            String namespace = entry.getValue().getNamespace();

            switch(action) {
            case TO_KUSTOMIZE:
                // tell argocd to switch to Kustomize NOW
                runCommand(new String[] { //
                        "/usr/local/bin/argocd", "app", "create", //
                        "--name", namespace, //
                        "--repo", "https://" + parameterMap.get("deploy_repo"), //
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
                        "--repo", "https://" + parameterMap.get("deploy_repo"), //
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
