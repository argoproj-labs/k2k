package com.intuit.dev.patterns.ks2kust;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;

import com.intuit.dev.patterns.ks2kust.ArgoCd.Action;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command( //
        name = "k2k", //
        description = "Converts Ksonnet to Kustomize.", //
        mixinStandardHelpOptions = true, //
        version = "k2k 1.0", //
        subcommands = { //
                K2KTranslate.class, //
                K2KDumpConfig.class, //
                K2KJenkins.class, //
                K2KArgoCd2Kustomize.class, //
                K2KArgoCd2Ksonnet.class, //
                K2KCleanKustomize.class, //
                K2KCleanKsonnet.class //
        } //
)
public class Main implements Callable<Void> {

    @Option(names = { "-d", "--debug" }, description = "Turns on debug logging.")
    static boolean debug = false;

    private static final CommandLine cmd = new CommandLine(Main.class);

    public static void main(String[] args) throws Exception {
        cmd.parseWithHandler(new CommandLine.RunLast(), args);
    }

    @Override
    public Void call() throws Exception {
        System.err.println("Please invoke a subcommand.");
        cmd.usage(System.err);
        return null;
    }
}

@Command( //
        name = "translate", //
        description = "Translates all the Ksonnet files to Kustomize files." //
)
class K2KTranslate implements Callable<Void> {
    @Parameters(index = "0", description = "The name of the service to convert.")
    String serviceName;

    @Option(names = { "-h", "--help" }, usageHelp = true)
    boolean help;

    @Option(names = { "-c",
            "--config" }, description = "provides a none default K2K configuration file for the translation.")
    File configFile;

    @Option(names = { "-n",
            "--nodowntime" }, description = "enables a no downtime conversion, but requires dual ALB and gateway change.")
    boolean noDownTime = false;

    @Override
    public Void call() throws Exception {
        Config config = new Config(configFile, serviceName);
        Ksonnet2Kustomize k2k = new Ksonnet2Kustomize();
        k2k.setNamePrefix(config.getServiceName());
        k2k.setNameMap(config.getKindNameMap());
        k2k.setNoDownTime(noDownTime);
        k2k.validateHasKsonnet();
        k2k.validateNoKustomize();
        k2k.generateKustomize(config.getBases());
        return null;
    }

}

@Command( //
        name = "show-config", //
        description = "Write the default configuration to standard out." //
)
class K2KDumpConfig implements Callable<Void> {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    boolean help;

    @Override
    public Void call() throws Exception {
        IOUtils.copy(Config.getDefaultConfig(), System.out);
        return null;
    }
}

@Command( //
        name = "update-jenkins", //
        description = "Updates Jenkins to tell ArgoCD to use Kustomize files." //
)
class K2KJenkins implements Callable<Void> {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    boolean help;

    @Parameters(index = "0", description = "The path to the Jenkinsfile to convert.")
    String jenkinsfileName;

    @Override
    public Void call() throws Exception {
        new JenkinsfileTranslator(jenkinsfileName).upgrade();
        return null;
    }

}

@Command( //
        name = "argocd2kustomize", //
        description = "Updates ArgoCD to use Kustomize files and to sync with prune." //
)
class K2KArgoCd2Kustomize implements Callable<Void> {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    boolean help;

    @Option(names = { "-g" }, description = "the gitrepo where argocd reads the config. Defaults to the current repo.")
    String gitRepo;

    @Parameters(index = "1", arity = "0..1", description = "The name of the environment to update.")
    String targetEnv = null;

    @Override
    public Void call() throws Exception {
        new ArgoCd(gitRepo).pushUpdatesNow(Action.TO_KUSTOMIZE, targetEnv);
        return null;
    }

}

@Command( //
        name = "argocd2ksonnet", //
        description = "Updates ArgoCD back to use Ksonnet files and to sync with prune." //
)
class K2KArgoCd2Ksonnet implements Callable<Void> {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    boolean help;

    @Option(names = { "-g" }, description = "the gitrepo where argocd reads the config. Defaults to the current repo.")
    String gitRepo;
    
    @Parameters(index = "1", arity = "0..1", description = "The name of the environment to update.")
    String targetEnv = null;

    @Override
    public Void call() throws Exception {
        new ArgoCd(gitRepo).pushUpdatesNow(Action.TO_KSONNET, targetEnv);
        return null;
    }

}

@Command( //
        name = "clean-kustomize", //
        description = "Removes the kustomize files to allow reconversion." //
)
class K2KCleanKustomize implements Callable<Void> {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    boolean help;

    @Override
    public Void call() throws Exception {
        Ksonnet2Kustomize.cleanKustomize();
        return null;
    }

}


@Command( //
        name = "clean-ksonnet", //
        description = "Removes the ksonnet files, once no longer needed." //
)
class K2KCleanKsonnet implements Callable<Void> {
    @Option(names = { "-h", "--help" }, usageHelp = true)
    boolean help;

    @Override
    public Void call() throws Exception {
        Ksonnet2Kustomize.cleanKsonnet();
        return null;
    }

}
