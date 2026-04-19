import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Main <grammar-file> <input-file> [output-dir]");
            System.out.println("Example: java Main input/grammar2.txt input/input_valid.txt output");
            return;
        }

        Path grammarPath = Path.of(args[0]);
        Path inputPath = Path.of(args[1]);
        Path outputDir = args.length >= 3 ? Path.of(args[2]) : Path.of("output");
        Files.createDirectories(outputDir);

        Grammar grammar = Grammar.fromFile(grammarPath);
        grammar.augmentGrammar();

        List<String> inputs = readInputs(inputPath);
        if (inputs.isEmpty()) {
            inputs = List.of("");
        }

        StringBuilder augmentedGrammarText = new StringBuilder();
        augmentedGrammarText.append(grammar.toFormattedString());
        writeText(outputDir.resolve("augmented_grammar.txt"), augmentedGrammarText.toString());

        SLRParser slrParser = new SLRParser(grammar);
        LR1Parser lr1Parser = new LR1Parser(grammar);

        ParserReport slrBuild = slrParser.buildReport();
        ParserReport lr1Build = lr1Parser.buildReport();

        writeText(outputDir.resolve("slr_items.txt"), slrBuild.itemSetsText);
        writeText(outputDir.resolve("slr_parsing_table.txt"), slrBuild.parsingTableText + System.lineSeparator() + slrBuild.conflictText);
        writeText(outputDir.resolve("lr1_items.txt"), lr1Build.itemSetsText);
        writeText(outputDir.resolve("lr1_parsing_table.txt"), lr1Build.parsingTableText + System.lineSeparator() + lr1Build.conflictText);

        StringBuilder slrTrace = new StringBuilder();
        StringBuilder lr1Trace = new StringBuilder();
        StringBuilder parseTrees = new StringBuilder();
        StringBuilder comparison = new StringBuilder();

        comparison.append("Grammar file: ").append(grammarPath).append(System.lineSeparator());
        comparison.append("Original start symbol: ").append(grammar.getOriginalStartSymbol()).append(System.lineSeparator());
        comparison.append("Augmented start symbol: ").append(grammar.getAugmentedStartSymbol()).append(System.lineSeparator());
        comparison.append("SLR states: ").append(slrBuild.stateCount).append(System.lineSeparator());
        comparison.append("LR(1) states: ").append(lr1Build.stateCount).append(System.lineSeparator());
        comparison.append("SLR table entries: ").append(slrBuild.tableEntryCount).append(System.lineSeparator());
        comparison.append("LR(1) table entries: ").append(lr1Build.tableEntryCount).append(System.lineSeparator());
        comparison.append("SLR build time (ms): ").append(nanosToMillis(slrBuild.buildTimeNanos)).append(System.lineSeparator());
        comparison.append("LR(1) build time (ms): ").append(nanosToMillis(lr1Build.buildTimeNanos)).append(System.lineSeparator());
        comparison.append(System.lineSeparator());

        int caseNumber = 1;
        for (String input : inputs) {
            ParserReport slrRun = slrParser.parse(input);
            ParserReport lr1Run = lr1Parser.parse(input);

            slrTrace.append("Input case ").append(caseNumber).append(": ").append(input.isBlank() ? "<empty>" : input).append(System.lineSeparator());
            slrTrace.append(slrRun.traceText).append(System.lineSeparator());
            slrTrace.append("Result: ").append(slrRun.accepted ? "accepted" : "rejected").append(System.lineSeparator());
            slrTrace.append("Conflicts: ").append(slrRun.conflicts.isEmpty() ? "none" : String.join("; ", slrRun.conflicts)).append(System.lineSeparator());
            slrTrace.append(System.lineSeparator());

            lr1Trace.append("Input case ").append(caseNumber).append(": ").append(input.isBlank() ? "<empty>" : input).append(System.lineSeparator());
            lr1Trace.append(lr1Run.traceText).append(System.lineSeparator());
            lr1Trace.append("Result: ").append(lr1Run.accepted ? "accepted" : "rejected").append(System.lineSeparator());
            lr1Trace.append("Conflicts: ").append(lr1Run.conflicts.isEmpty() ? "none" : String.join("; ", lr1Run.conflicts)).append(System.lineSeparator());
            lr1Trace.append(System.lineSeparator());

            parseTrees.append("Input case ").append(caseNumber).append(": ").append(input.isBlank() ? "<empty>" : input).append(System.lineSeparator());
            parseTrees.append("SLR parse tree:").append(System.lineSeparator());
            parseTrees.append(slrRun.treeAsString()).append(System.lineSeparator());
            parseTrees.append("LR(1) parse tree:").append(System.lineSeparator());
            parseTrees.append(lr1Run.treeAsString()).append(System.lineSeparator());
            parseTrees.append(System.lineSeparator());

            comparison.append("Input case ").append(caseNumber).append(": ").append(input.isBlank() ? "<empty>" : input).append(System.lineSeparator());
            comparison.append("  SLR accepted: ").append(slrRun.accepted).append(System.lineSeparator());
            comparison.append("  LR(1) accepted: ").append(lr1Run.accepted).append(System.lineSeparator());
            comparison.append("  SLR parse time (ms): ").append(nanosToMillis(slrRun.parseTimeNanos)).append(System.lineSeparator());
            comparison.append("  LR(1) parse time (ms): ").append(nanosToMillis(lr1Run.parseTimeNanos)).append(System.lineSeparator());
            comparison.append(System.lineSeparator());

            caseNumber++;
        }

        writeText(outputDir.resolve("slr_trace.txt"), slrTrace.toString());
        writeText(outputDir.resolve("lr1_trace.txt"), lr1Trace.toString());
        writeText(outputDir.resolve("comparison.txt"), comparison.toString());
        writeText(outputDir.resolve("parse_trees.txt"), parseTrees.toString());

        System.out.println("Generated outputs in: " + outputDir.toAbsolutePath());
        System.out.println("SLR conflicts: " + slrBuild.conflicts.size());
        System.out.println("LR(1) conflicts: " + lr1Build.conflicts.size());
    }

    private static List<String> readInputs(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String> inputs = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            inputs.add(trimmed);
        }
        return inputs;
    }

    private static void writeText(Path path, String text) throws IOException {
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
