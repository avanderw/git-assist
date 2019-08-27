package net.avdw.gitassist.clean;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "clean",
        description = "Clean the git repository",
        version = "1.0",
        mixinStandardHelpOptions = true,
        subcommands = {
                CommandLine.HelpCommand.class,
                GitAssistCleanBranch.class
        })
public class GitAssistClean implements Runnable {

    /**
     * Entry point for picocli.
     */
    @Override
    public void run() {
        CommandLine.usage(GitAssistClean.class, System.out);
    }
}
