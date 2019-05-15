package com.intuit.dev.patterns.ks2kust;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;

public final class ShellUtils {
    private ShellUtils() {
    }

    static List<String> runShellCommand(File baseDir, String command) {
        try {
            final String[] execCommand = new String[] { "sh", "-ec", command };
            System.out.println(command);
    
            ProcessBuilder builder = new ProcessBuilder() //
                    .command(execCommand) //
                    .directory(baseDir) //
                    .redirectOutput(Redirect.PIPE) //
                    .redirectErrorStream(true);
            builder.environment().putAll(System.getenv());
            Process process = builder.start();
            StreamGobbler gobbler = new StreamGobbler(process.getInputStream());
            gobbler.run();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Exit code: " + exitCode + ", on Command: " + command);
            }
            return gobbler.getBuffer();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run '" + command + "'", e);
        }
    }
}
