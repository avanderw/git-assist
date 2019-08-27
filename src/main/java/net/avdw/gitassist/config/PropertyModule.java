package net.avdw.gitassist.config;

import com.google.inject.AbstractModule;
import net.avdw.gitassist.BashExecutable;
import net.avdw.gitassist.MergedMasterScript;
import net.avdw.gitassist.NotMergedMasterScript;
import net.avdw.gitassist.RemoveBranchScript;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class PropertyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Path.class).annotatedWith(MergedMasterScript.class).toInstance(Paths.get("C:/Users/cp318674/Documents/source-avanderw/gitassist/scripts/merged-master.sh"));
        bind(Path.class).annotatedWith(NotMergedMasterScript.class).toInstance(Paths.get("C:/Users/cp318674/Documents/source-avanderw/gitassist/scripts/not-merged-master.sh"));
        bind(Path.class).annotatedWith(RemoveBranchScript.class).toInstance(Paths.get("C:/Users/cp318674/Documents/source-avanderw/gitassist/scripts/remove-branch.sh"));
        bind(Path.class).annotatedWith(BashExecutable.class).toInstance(Paths.get("C:/Program Files/Git/usr/bin/bash.exe"));
    }
}
