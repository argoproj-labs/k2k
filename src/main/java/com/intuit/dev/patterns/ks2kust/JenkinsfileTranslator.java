package com.intuit.dev.patterns.ks2kust;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

public class JenkinsfileTranslator {
    private static final String[] JENKINSFILE_REPLACE_PATTERNS = {
            // 1 kson_compnt 2 registry, 3 image, 4 tag, 5 env
            "sh \"/usr/local/bin/ks param set \\$\\{(.*)\\} image \\$\\{(.*)\\}/\\$\\{(.*)\\}:\\$\\{(.*)\\} --env \\$(.*)\"",
            "sh \"cd environments/\\$$5; /usr/local/bin/kustomize edit set image \\${$2}/\\${$3}:\\${$4}\"",

            // 1 {leading spaces} 2 appName, 3 env, 4 deploy_repo,
            "([\\s]*)sh \"/argocd app create --name \\$\\{(.*)\\}-\\$\\{(.*)\\} --repo https://\\$\\{(.*)\\} --path . --env \\$\\{(.*)\\} --upsert\"",
            "$1def cluster = clusterMap[$3]\n" + //
                    "$1sh \"/argocd app create --name \\${$2}-\\${$3} --repo https://\\${$4} --path environments/\\${$3}  --dest-server \\${cluster} --dest-namespace \\${$2}-\\${$3} --upsert\"",

            // 1 envName, 2 appName
            "def deployGitOps\\(String (.*), String (.*)\\) \\{", //
            "def deployGitOps(Map clusterMap, String $1, String $2) {",

            // 1 envName, 2 appName
            "deployGitOps\\(([A-Za-z0-9_]+), ([A-Za-z0-9_]+)\\)", //
            "deployGitOps(clusterMap, $1, $2)"

    };
    static File APP_YAML_FILE = new File("app.yaml");

    public static class EnvironmentData {
        private String cluster;
        private String namespace;

        public EnvironmentData(String cluster, String namespace) {
            this.cluster = cluster;
            this.namespace = namespace;
        }

        public String getCluster() {
            return cluster;
        }

        public String getNamespace() {
            return namespace;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((cluster == null) ? 0 : cluster.hashCode());
            result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EnvironmentData other = (EnvironmentData) obj;
            if (cluster == null) {
                if (other.cluster != null)
                    return false;
            } else if (!cluster.equals(other.cluster))
                return false;
            if (namespace == null) {
                if (other.namespace != null)
                    return false;
            } else if (!namespace.equals(other.namespace))
                return false;
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "EnvironmentData [cluster=" + cluster + ", namespace=" + namespace + "]";
        }

    }

    private static Map<String, EnvironmentData> environmentMap = null;

    public static Map<String, EnvironmentData> getEnvironmentMap() throws FileNotFoundException, IOException {
        if (environmentMap == null) {
            environmentMap = new HashMap<>();
            try (FileInputStream appYamlStream = new FileInputStream(APP_YAML_FILE)) {
                Map<String, Object> appYaml = new Yaml().load(appYamlStream);
                @SuppressWarnings("unchecked")
                Map<String, Object> environments = (Map<String, Object>) appYaml.get("environments");
                for (Entry<String, Object> entry : environments.entrySet()) {
                    final String key = entry.getKey();
                    if (key.equals("default")) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> entryMap = (Map<String, Object>) entry.getValue();
                    @SuppressWarnings("unchecked")
                    Map<String, String> destinationMap = (Map<String, String>) entryMap.getOrDefault("destination",
                            Collections.emptyMap());
                    String namespace = destinationMap.get("namespace");
                    String cluster = destinationMap.get("server");
                    environmentMap.put(key, new EnvironmentData(cluster, namespace));
                }
            }
        }
        return environmentMap;
    }

    private static Map<String, String> parameterMap = null;

    private static String trimParameter(String s) {
        return s.trim().replace("\"", "").replace("'", "");
    }

    public static Map<String, String> getParameterMap(String jenkinsfilePath) throws FileNotFoundException, IOException {
        if (parameterMap == null) {
            parameterMap = new HashMap<>();
            try (FileInputStream jenkinsStream = new FileInputStream(jenkinsfilePath)) {
                List<String> lines = IOUtils.readLines(jenkinsStream, Charset.defaultCharset());
                for (String line : lines) {
                    if (line.matches("[\\s]*def[\\s]serviceName[\\s]=.*")) {
                        parameterMap.put("serviceName", trimParameter(line.split("[=]")[1]));
                        continue;
                    }
                    if (line.matches("[\\s]*def[\\s]service_name[\\s]=.*")) {
                        parameterMap.put("serviceName", trimParameter(line.split("[=]")[1]));
                        continue;
                    }
                    if (line.matches("[\\s]*def[\\s]deploy_repo[\\s]=.*")) {
                        parameterMap.put("deploy_repo", trimParameter(line.split("[=]")[1]));
                        continue;
                    }
                    if (line.matches("[\\s]*def[\\s]argocd_server[\\s]*=[\\s]*new URL\\(\"https://(.*)\".*")) {
                        parameterMap.put("argocd_server", trimParameter(line.split("[=(/)]")[4]));
                        continue;
                    }
                    if (line.matches("[\\s]*def[\\s]argocd_server_ppd[\\s]*=.*")) {
                        parameterMap.put("argocd_server", trimParameter(line.split("[=]")[1]));
                        continue;
                    }
                    if (line.matches("[\\s]*serviceName[\\s]=.*")) {
                        parameterMap.put("serviceName", trimParameter(line.split("[=]")[1]));
                        continue;
                    }
                    if (line.matches("[\\s]*service_name[\\s]=.*")) {
                        parameterMap.put("serviceName", trimParameter(line.split("[=]")[1]));
                        continue;
                    }
                    if (line.matches("[\\s]*deploy_repo[\\s]=.*")) {
                        parameterMap.put("deploy_repo", trimParameter(line.split("[=]")[1]));
                        continue;
                    }
                    if (line.matches("[\\s]*argocd_server[\\s]*=[\\s]*new URL\\(\"https://(.*)\".*")) {
                        parameterMap.put("argocd_server", trimParameter(line.split("[=(/)]")[4]));
                        continue;
                    }
                }
            }
        }
        return parameterMap;
    }

    File jenkinsfileFile;

    public JenkinsfileTranslator(String jenkinsfileName) {
        this.jenkinsfileFile = new File(jenkinsfileName);
    }

    public void upgrade() throws IOException {
        System.out.println("Upgrading: " + this.jenkinsfileFile.getAbsolutePath());

        // get clusters
        Map<String, EnvironmentData> environmentMap = getEnvironmentMap();

        // read Jenkins file
        List<String> lines = FileUtils.readLines(jenkinsfileFile, Charset.defaultCharset());

        // find where to add global variables
        int targetix = 0;
        for (int ix = 0; ix < lines.size(); ix++) {
            String line = lines.get(ix);
            if (line.matches("[\\s]*import[\\s].*")) {
                targetix = ix;
            } else if (line.matches("[\\s]*def[\\s].*")) {
                targetix = ix;
            } else if (line.matches("[\\s]*[a-zA-Z][a-zA-Z0-9]*[\\s]*=.*")) {
                targetix = ix;
            }
            // if function done
            else if (line.matches("[\\s]*[a-zA-Z][a-zA-Z0-9]*[\\s]*.*")) {
                break;
            }
            // if pipeline done
            else if (line.matches("[\\s]*\\{.*")) {
                break;
            }
        }

        // add ${env}_cluster for each env
        targetix++;
        lines.add(targetix++, "");
        lines.add(targetix++, "def clusterMap = [:]");
        for (Entry<String, EnvironmentData> e : environmentMap.entrySet()) {
            if (e.getValue().getCluster().matches("\\$\\{paws.*")) {
                lines.add(targetix++, "// clusterMap[\"" + e.getKey() + "\"] = \"" + e.getValue().getCluster() + "\"");
            } else {
                lines.add(targetix++, "clusterMap[\"" + e.getKey() + "\"] = \"" + e.getValue().getCluster() + "\"");
            }
        }

        ListIterator<String> it = lines.listIterator();
        while (it.hasNext()) {
            String line = it.next();

            if (line.matches("[\\s]*kson_compnt[\\s]=[\\s]\"sample\".*")) {
                it.remove();
                continue;
            }

            String updatedLine = line;
            for (int ix = 0; ix + 1 < JENKINSFILE_REPLACE_PATTERNS.length; ix += 2) {
                try {
                    updatedLine = updatedLine.replaceAll(JENKINSFILE_REPLACE_PATTERNS[ix],
                            JENKINSFILE_REPLACE_PATTERNS[ix + 1]);
                } catch (RuntimeException e) {
                    System.out.println("Failed to perform REGEX translation");
                    System.out.println("Match: " + JENKINSFILE_REPLACE_PATTERNS[ix]);
                    System.out.println("Replace: " + JENKINSFILE_REPLACE_PATTERNS[ix +1]);
                    System.out.println("Data: " + updatedLine);
                    throw e;
                }
            }
            if (!line.equals(updatedLine)) {
                // System.out.println("Updated from:\n" + line);
                // System.out.println(" to:\n" + updatedLine);
                it.set(updatedLine);
            }
        }

        FileUtils.writeLines(jenkinsfileFile, lines);
    }

}
