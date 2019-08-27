package net.avdw.gitassist;

import net.avdw.gitassist.clean.GitAssistClean;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(name = "gitassist",
        description = "The git assistant",
        version = "1.0",
        mixinStandardHelpOptions = true,
        subcommands = {
                HelpCommand.class,
                GitAssistClean.class
        })
public class GitAssist implements Runnable {

    /**
     * Entry point for picocli.
     */
    @Override
    public void run() {
        CommandLine.usage(GitAssist.class, System.out);
    }
}
