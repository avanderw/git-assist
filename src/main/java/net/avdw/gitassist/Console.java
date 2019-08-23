package net.avdw.gitassist;

import org.pmw.tinylog.Logger;

public final class Console {
    private Console() {
    }

    public static void h1(final String text) {
        System.out.println();
        Logger.info(String.format("%s== %s ==%s", Ansi.CYAN, text, Ansi.RESET));
    }
}
