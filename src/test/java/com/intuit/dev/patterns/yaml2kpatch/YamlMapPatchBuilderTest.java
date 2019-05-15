package com.intuit.dev.patterns.yaml2kpatch;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.intuit.dev.patterns.yaml2kpatch.YamlLoader.KeyEdit;
import com.intuit.dev.patterns.yaml2kpatch.YamlLoader.KindNameMap;

public class YamlMapPatchBuilderTest {
    private static final String KSONNET_MANIFEST = MainTest.class.getClassLoader()
            .getResource("ksonet-default-manifest.yaml").getPath();
    private static final String KUSTOMIZE_MANIFEST = MainTest.class.getClassLoader()
            .getResource("kustomize-default-manifest.yaml").getPath();
    static final KindNameMap NAME_MAP;
    static {
        NAME_MAP = new KindNameMap();
        NAME_MAP.put("Ingress", "ists", "ingress", null);
        NAME_MAP.put("Service", "ists", "service", null);
        List<KeyEdit> deploymentKeyEditList = new ArrayList<>();
        deploymentKeyEditList.add(new KeyEdit("spec.template.spec.containers[*].name", "ists", "app"));
        NAME_MAP.put("Deployment", "ists", "deployment", deploymentKeyEditList);
        NAME_MAP.put("Ingress", "appd-ingress", "ingress", null);
        NAME_MAP.put("Service", "appd-service", "service", null);
        NAME_MAP.put("Deployment", "appd-deployment", "deployment", null);
    }
    private static final String EXPECTED = //
            "" + //
                    "kind: Service\n" + //
                    "apiVersion: v1\n" + //
                    "metadata:\n" + //
                    "  name: service\n" + //
                    "spec:\n" + //
                    "  selector:\n" + //
                    "    app: ists\n" + //
                    "---\n" + //
                    "kind: Deployment\n" + //
                    "apiVersion: apps/v1beta2\n" + //
                    "metadata:\n" + //
                    "  name: deployment\n" + //
                    "spec:\n" + //
                    "  replicas: 1\n" + //
                    "  selector:\n" + //
                    "    matchLabels:\n" + //
                    "      app: ists\n" + //
                    "  template:\n" + //
                    "    metadata:\n" + //
                    "      annotations:\n" + //
                    "        iam.amazonaws.com/role: k8s-default\n" + //
                    "      labels:\n" + //
                    "        app: ists\n" + //
                    "    spec:\n" + //
                    "      containers:\n" + //
                    "      - name: app\n" + //
                    "        env:\n" + //
                    "        - name: APP_NAME\n" + //
                    "          value: ists\n" + //
                    "        - name: L1\n" + //
                    "          value: dev\n" + //
                    "        - name: L2\n" + //
                    "          value: patterns\n" + //
                    "        - name: APPDYNAMICS_CONTROLLER_HOST_NAME\n" + //
                    "          value: intuit-sbg-dev.saas.appdynamics.com\n" + //
                    "        - name: APPDYNAMICS_AGENT_ACCOUNT_NAME\n" + //
                    "          value: intuit-sbg-dev\n" + //
                    "        livenessProbe:\n" + //
                    "          failureThreshold: 2\n" + //
                    "          initialDelaySeconds: 60\n" + //
                    "        readinessProbe:\n" + //
                    "          initialDelaySeconds: 45\n" + //
                    "          successThreshold: 1\n" + //
                    "        volumeMounts:\n" + //
                    "        - mountPath: /etc/k8s-secrets\n" + //
                    "          name: k8s-secret-volume\n" + //
                    "          readOnly: true\n" + //
                    "      initContainers:\n" + //
                    "      - name: segment-app-init\n" + //
                    "        env:\n" + //
                    "        - name: APP_NAME\n" + //
                    "          value: ists\n" + //
                    "        - name: APPDYNAMICS_AGENT_ACCOUNT_NAME\n" + //
                    "          value: intuit-sbg-dev\n" + //
                    "        - name: SEGMENT_CLUSTER_ROLE_ARN\n" + //
                    "          value: arn:aws:iam::795188202216:role/shared.tools-sgmnt-ppd-usw2.cluster.k8s.local\n" + //
                    "        - name: SEGMENT_IDPS_APPLIANCE\n" + //
                    "          value: bu-segments-pre-production-8vwhtx.pd.idps.a.intuit.com\n" + //
                    "        - name: SEGMENT_IDPS_POLICY_ID\n" + //
                    "          value: p-w2x5u0hcwbt0\n" + //
                    "        image: docker.artifactory.a.intuit.com/dev/containers/segment-app-init/service/segment-app-init:master-29-dc47b83\n"
                    + //
                    "      volumes:\n" + //
                    "      - name: k8s-secret-volume\n" + //
                    "        secret:\n" + //
                    "          secretName: intuit-application-id\n" + //
                    "---\n" + //
                    "kind: Ingress\n" + //
                    "apiVersion: extensions/v1beta1\n" + //
                    "metadata:\n" + //
                    "  annotations:\n" + //
                    "    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-west-2:795188202216:certificate/d8f8bbc9-4880-4744-94ff-166e36144735\n"
                    + //
                    "    alb.ingress.kubernetes.io/security-groups: iks-intuit-cidr-ingress-tcp-443, iks-intuit-api-gw-ingress-preprod-tcp-443,\n"
                    + //
                    "      iks-intuit-app-alb-custom-ingress\n" + //
                    "  labels:\n" + //
                    "    app: ists\n" + //
                    "  name: ingress\n" + //
                    "spec:\n" + //
                    "  rules:\n" + //
                    "  - host: 'dev-patterns-TODO: env-name-ists.tools-sgmnt-ppd.a.intuit.com'\n" + //
                    "    http:\n" + //
                    "      paths:\n" + //
                    "      - backend:\n" + //
                    "          serviceName: ists\n" + //
                    "          servicePort: 443\n" + //
                    "        path: /\n" + //
                    "";

    @Test
    public void YamlMapPatchBuilder() throws Exception {
        String actual = Main.runPatch(NAME_MAP, KUSTOMIZE_MANIFEST, KSONNET_MANIFEST);
        assertEquals(EXPECTED, actual);
    }
}
