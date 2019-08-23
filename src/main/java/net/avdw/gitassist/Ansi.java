package net.avdw.gitassist;

// http://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html#16-colors
public final class Ansi {
    private Ansi() {
    }

    public static final String BLACK = "\u001b[30;1m";
    public static final String RED = "\u001b[31;1m";
    public static final String GREEN = "\u001b[32;1m";
    public static final String YELLOW = "\u001b[33m";
    public static final String BLUE = "\u001b[34;1m";
    public static final String MAGENTA = "\u001b[35;1m";
    public static final String CYAN = "\u001b[36m";
    public static final String WHITE = "\u001b[37;1m";
    public static final String RESET = "\u001b[0m";
}
