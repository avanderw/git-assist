package net.avdw.gitassist.clean;

import com.google.inject.Inject;
import net.avdw.gitassist.*;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.text.WordUtils;
import org.pmw.tinylog.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Command(name = "branch",
        description = "Clean stale branches")
public class GitAssistCleanBranch implements Runnable {
    private static final int BASH_PROCESS_ARGS = 3;

    private static final int OLDER_THAN_3_MONTHS = -3;
    private static final int OLDER_THAN_6_MONTHS = -6;
    private static final int OLDER_THAN_NOW = 0;

    @Option(names = "--not-merged")
    private boolean notMerged;

    @Inject
    @MergedMasterScript
    private Path mergedMasterScript;

    @Inject
    @NotMergedMasterScript
    private Path notMergedMasterScript;

    @Inject
    @RemoveBranchScript
    private Path removeRemoteBranchScript;

    @Inject
    @BashExecutable
    private Path bashExecutable;

    /**
     * Entry point for picocli.
     */
    @Override
    public void run() {
        Path branchListOutfile = Paths.get("./branch-list.gitassist");
        Path branchListScript = notMerged ? notMergedMasterScript : mergedMasterScript;
        execute(branchListScript, branchListOutfile.toString());

        List<Item> releases = new ArrayList<>();
        List<Item> features = new ArrayList<>();
        List<Item> bugfixes = new ArrayList<>();
        List<Item> other = new ArrayList<>();
        Frequency releaseFrequency = new Frequency();
        Frequency featureFrequency = new Frequency();
        Frequency bugfixFrequency = new Frequency();
        Frequency otherFrequency = new Frequency();
        try (Scanner scanner = new Scanner(branchListOutfile)) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                Item item = new Item(line);
                if (item.isProtected()) {
                    continue;
                }
                if (item.getAuthor() == null) {
                    Logger.warn(String.format("Unsupported item: %s", line));
                    continue;
                }
                if (item.isRelease()) {
                    releases.add(item);
                    releaseFrequency.addValue(item.getAuthor());
                } else if (item.isFeature()) {
                    features.add(item);
                    featureFrequency.addValue(item.getAuthor());
                } else if (item.isBugFix()) {
                    bugfixes.add(item);
                    bugfixFrequency.addValue(item.getAuthor());
                } else if (item.isSupported) {
                    other.add(item);
                    otherFrequency.addValue(item.getAuthor());
                }
            }
        } catch (IOException e) {
            Logger.error(e);
        }

        cleanBranches(releases, notMerged ? OLDER_THAN_6_MONTHS : OLDER_THAN_3_MONTHS, "RELEASE");
        printFrequency(releaseFrequency, "RELEASE");

        cleanBranches(features, notMerged ? OLDER_THAN_3_MONTHS : OLDER_THAN_NOW, "FEATURE");
        printFrequency(featureFrequency, "FEATURE");

        cleanBranches(bugfixes, notMerged ? OLDER_THAN_3_MONTHS : OLDER_THAN_NOW, "BUGFIX");
        printFrequency(bugfixFrequency, "BUGFIX");

        cleanBranches(other, notMerged ? OLDER_THAN_3_MONTHS : OLDER_THAN_NOW, "OTHER");
        printFrequency(otherFrequency, "OTHER");
    }

    private void cleanBranches(final List<Item> branches, final int protectMonths, final String title) {
        if (!branches.isEmpty()) {
            Console.h1(title);
            branches.stream().sorted(Comparator.comparing(Item::getAuthor)).forEach(Logger::info);
            Calendar c = GregorianCalendar.getInstance();
            c.setTime(new Date());
            c.add(Calendar.MONTH, protectMonths);
            branches.stream().filter(item -> item.isDateBefore(c.getTime()))
                    .forEach(item -> execute(removeRemoteBranchScript, item.getBranchName().replace("origin/", "")));
        }
    }

    private void execute(final Path script, final String... scriptArgs) {
        if (script.toString().endsWith(".sh")) {
            String[] processArgs = new String[BASH_PROCESS_ARGS + scriptArgs.length];
            processArgs[0] = bashExecutable.toString();
            processArgs[1] = "--login";
            processArgs[2] = script.toString();
            System.arraycopy(scriptArgs, 0, processArgs, BASH_PROCESS_ARGS, scriptArgs.length);
            ProcessBuilder pb = new ProcessBuilder(processArgs);
            try {
                pb.inheritIO();
                Process process = pb.start();
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Logger.error(String.format("Unsupported script type: %s", script));
        }
    }

    private void printFrequency(final Frequency frequency, final String title) {
        if (frequency.getUniqueCount() != 0) {
            Console.h1(String.format("Frequency: %s", title));
            Logger.info(String.format("%sRATIO | CNT |%s", Ansi.CYAN, Ansi.RESET));
            frequency.entrySetIterator().forEachRemaining(comparableLongEntry -> {
                if (frequency.getMode().contains(comparableLongEntry.getKey())) {
                    Logger.info(String.format("%s%.3f | %3s | %s%s", Ansi.GREEN,
                            frequency.getPct(comparableLongEntry.getKey()),
                            comparableLongEntry.getValue(),
                            comparableLongEntry.getKey(), Ansi.RESET));
                } else {
                    Logger.info(String.format("%.3f | %3s | %s",
                            frequency.getPct(comparableLongEntry.getKey()),
                            comparableLongEntry.getValue(),
                            comparableLongEntry.getKey()));
                }
            });
            Logger.info(String.format("%sTOTAL   %3s%s", Ansi.CYAN, frequency.getSumFreq(), Ansi.RESET));
        }
    }

    private static class Item {
        private String raw;
        private Date date;
        private String author;
        private String branchName;
        private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        private static final int SAFE_SPLIT_COUNT = 3;
        private boolean isSupported;

        private static Path getUserFullNamePath = Paths.get("C:/Users/cp318674/Documents/source-avanderw/gitassist/scripts/net-user.bat");
        private static Map<String, String> replacements = new HashMap<>();

        static {
            replacements.put("Wallace", "Wallace Van Zyl");
            replacements.put("Pieter", "Pieter Smith");
            replacements.put("Daniel Kotze Work", "Daniel Kotze");
        }

        Item(final String line) {
            this.raw = line;
            String[] split = line.split("\\|");
            if (split.length == SAFE_SPLIT_COUNT) {
                try {
                    date = sdf.parse(split[0]);
                } catch (ParseException e) {
                    Logger.error(String.format("Error parsing date %s", split[0]));
                }
                author = WordUtils.capitalizeFully(split[1]
                        .replace("(Information Technology)", "")
                        .replace("(Business Development)", "")
                        .replace("CAPITECBANK\\", "")
                        .replace(".", " ")
                        .trim());
                branchName = split[2].trim();
                isSupported = true;

                if (author.toLowerCase().startsWith("cp") || author.toLowerCase().startsWith("ct")) {
                    Path outputPath = Paths.get(".").resolve(String.format("%s.gitassist", author));
                    if (!Files.exists(outputPath)) {
                        Logger.debug(String.format("Resolving %s to full name", author));
                        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", getUserFullNamePath.toAbsolutePath().toString(), author);
                        pb.inheritIO();

                        try {
                            Process p = pb.start();
                            p.waitFor();
                        } catch (IOException | InterruptedException e) {
                            Logger.error(e);
                        }
                    }

                    try (Scanner scanner = new Scanner(outputPath)) {
                        while (scanner.hasNext()) {
                            String output = scanner.nextLine();
                            if (output.contains("Full Name")) {
                                author = output
                                        .replace("Full Name", "")
                                        .replace("(Business Development)", "")
                                        .replace("(Information Technology)", "")
                                        .trim();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        Logger.error(e);
                    }
                }

                if (author.contains(",")) {
                    String[] authorSplit = author.split(",");
                    if (authorSplit.length == 2) {
                        author = String.format("%s %s", authorSplit[1].trim(), authorSplit[0].trim());
                    } else {
                        Logger.error(String.format("Unhandled author type %s", author));
                    }
                }

                if (replacements.containsKey(author)) {
                    author = replacements.get(author);
                }
            } else {
                Logger.warn(String.format("Unsupported array: %s", Arrays.toString(split)));
                isSupported = false;
            }
        }

        boolean isRelease() {
            return raw.contains("origin/release");
        }

        boolean isFeature() {
            return raw.contains("origin/feature");
        }

        boolean isBugFix() {
            return raw.contains("origin/bugfix");
        }

        String getAuthor() {
            return author;
        }

        String getType() {
            if (isRelease()) {
                return "RELEASE";
            } else if (isFeature()) {
                return "FEATURE";
            } else if (isBugFix()) {
                return "BUGFIX";
            } else {
                return "OTHER";
            }
        }

        @Override
        public String toString() {
            return String.format("%s%7s%s: %s%s%s %s%s%s %s",
                    Ansi.GREEN, getType(), Ansi.RESET,
                    Ansi.MAGENTA, sdf.format(date), Ansi.RESET,
                    Ansi.YELLOW, author, Ansi.RESET,
                    branchName);
        }

        String getBranchName() {
            return branchName;
        }

        boolean isProtected() {
            return branchName.equals("origin/master") || branchName.equals("origin/develop");
        }

        boolean isDateBefore(final Date thatDate) {
            return this.date.before(thatDate);
        }
    }
}
