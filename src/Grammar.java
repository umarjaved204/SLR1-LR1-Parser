import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Grammar {
    public static final String EPSILON = "epsilon";
    public static final String END_MARKER = "$";

    private final List<Production> productions = new ArrayList<>();
    private final LinkedHashSet<String> nonTerminals = new LinkedHashSet<>();
    private final LinkedHashSet<String> terminals = new LinkedHashSet<>();
    private final LinkedHashMap<String, List<Production>> byLhs = new LinkedHashMap<>();

    private final String originalStartSymbol;
    private String startSymbol;
    private String augmentedStartSymbol;
    private boolean augmented;

    private Map<String, Set<String>> firstSets;
    private Map<String, Set<String>> followSets;

    private Grammar(String startSymbol) {
        this.originalStartSymbol = startSymbol;
        this.startSymbol = startSymbol;
    }

    public static Grammar fromFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<RawProduction> rawProductions = new ArrayList<>();
        String firstLhs = null;

        for (String line : lines) {
            String trimmed = stripComment(line).trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int arrowIndex = trimmed.indexOf("->");
            if (arrowIndex < 0) {
                throw new IOException("Invalid grammar line: " + line);
            }
            String lhs = trimmed.substring(0, arrowIndex).trim();
            String rhsPart = trimmed.substring(arrowIndex + 2).trim();
            if (lhs.isEmpty()) {
                throw new IOException("Missing left-hand side: " + line);
            }
            if (firstLhs == null) {
                firstLhs = lhs;
            }
            String[] alternatives = rhsPart.split("\\|");
            for (String alternative : alternatives) {
                List<String> rhs = tokenizeRhs(alternative.trim());
                rawProductions.add(new RawProduction(lhs, rhs));
            }
        }

        if (firstLhs == null) {
            throw new IOException("Grammar file is empty.");
        }

        Grammar grammar = new Grammar(firstLhs);
        for (RawProduction raw : rawProductions) {
            grammar.addProduction(raw.lhs, raw.rhs);
        }
        grammar.refreshSymbolSets();
        return grammar;
    }

    public Grammar augmentGrammar() {
        if (augmented) {
            return this;
        }
        String candidate = makeUniqueAugmentedStart(originalStartSymbol);
        augmentedStartSymbol = candidate;
        productions.add(0, new Production(candidate, Collections.singletonList(originalStartSymbol)));
        nonTerminals.add(candidate);
        byLhs.computeIfAbsent(candidate, key -> new ArrayList<>()).add(0, productions.get(0));
        startSymbol = candidate;
        augmented = true;
        refreshSymbolSets();
        firstSets = null;
        followSets = null;
        return this;
    }

    public String getOriginalStartSymbol() {
        return originalStartSymbol;
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    public String getAugmentedStartSymbol() {
        return augmentedStartSymbol;
    }

    public List<Production> getProductions() {
        return Collections.unmodifiableList(productions);
    }

    public List<Production> getProductionsFor(String nonTerminal) {
        return byLhs.getOrDefault(nonTerminal, Collections.emptyList());
    }

    public Set<String> getNonTerminals() {
        return Collections.unmodifiableSet(nonTerminals);
    }

    public Set<String> getTerminals() {
        return Collections.unmodifiableSet(terminals);
    }

    public boolean isNonTerminal(String symbol) {
        return nonTerminals.contains(symbol);
    }

    public boolean isTerminal(String symbol) {
        return terminals.contains(symbol) || END_MARKER.equals(symbol);
    }

    public Map<String, Set<String>> getFirstSets() {
        if (firstSets == null) {
            computeFirstSets();
        }
        return firstSets;
    }

    public Map<String, Set<String>> getFollowSets() {
        if (followSets == null) {
            computeFollowSets();
        }
        return followSets;
    }

    public Set<String> firstOfSequence(List<String> sequence) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (sequence.isEmpty()) {
            result.add(EPSILON);
            return result;
        }

        boolean allNullable = true;
        for (String symbol : sequence) {
            Set<String> first = getFirstSets().getOrDefault(symbol, Collections.singleton(symbol));
            for (String token : first) {
                if (!EPSILON.equals(token)) {
                    result.add(token);
                }
            }
            if (!first.contains(EPSILON)) {
                allNullable = false;
                break;
            }
        }
        if (allNullable) {
            result.add(EPSILON);
        }
        return result;
    }

    public Set<String> firstOfSequenceWithLookahead(List<String> sequence, String lookahead) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (sequence.isEmpty()) {
            result.add(lookahead);
            return result;
        }

        boolean allNullable = true;
        for (String symbol : sequence) {
            Set<String> first = getFirstSets().getOrDefault(symbol, Collections.singleton(symbol));
            for (String token : first) {
                if (!EPSILON.equals(token)) {
                    result.add(token);
                }
            }
            if (!first.contains(EPSILON)) {
                allNullable = false;
                break;
            }
        }
        if (allNullable) {
            result.add(lookahead);
        }
        return result;
    }

    public String toFormattedString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<Production>> entry : byLhs.entrySet()) {
            builder.append(entry.getKey()).append(" -> ");
            List<Production> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    builder.append(" | ");
                }
                builder.append(list.get(i).rhsAsString());
            }
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    public String productionsWithNumbers() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < productions.size(); i++) {
            builder.append(String.format("[%d] %s%n", i, productions.get(i)));
        }
        return builder.toString();
    }

    private void addProduction(String lhs, List<String> rhs) {
        Production production = new Production(lhs, rhs);
        productions.add(production);
        byLhs.computeIfAbsent(lhs, key -> new ArrayList<>()).add(production);
        nonTerminals.add(lhs);
    }

    private void refreshSymbolSets() {
        nonTerminals.clear();
        terminals.clear();
        byLhs.clear();
        for (Production production : productions) {
            nonTerminals.add(production.lhs);
            byLhs.computeIfAbsent(production.lhs, key -> new ArrayList<>()).add(production);
        }
        for (Production production : productions) {
            for (String symbol : production.rhs) {
                if (!nonTerminals.contains(symbol) && !EPSILON.equals(symbol)) {
                    terminals.add(symbol);
                }
            }
        }
    }

    private void computeFirstSets() {
        Map<String, Set<String>> first = new LinkedHashMap<>();
        for (String terminal : terminals) {
            first.put(terminal, new LinkedHashSet<>(Collections.singleton(terminal)));
        }
        first.put(END_MARKER, new LinkedHashSet<>(Collections.singleton(END_MARKER)));
        for (String nonTerminal : nonTerminals) {
            first.putIfAbsent(nonTerminal, new LinkedHashSet<>());
        }

        boolean changed;
        do {
            changed = false;
            for (Production production : productions) {
                Set<String> firstLhs = first.computeIfAbsent(production.lhs, key -> new LinkedHashSet<>());
                if (production.rhs.isEmpty()) {
                    changed |= firstLhs.add(EPSILON);
                    continue;
                }
                boolean nullablePrefix = true;
                for (String symbol : production.rhs) {
                    Set<String> firstSymbol = first.computeIfAbsent(symbol, key -> {
                        LinkedHashSet<String> singleton = new LinkedHashSet<>();
                        singleton.add(key);
                        return singleton;
                    });
                    for (String token : firstSymbol) {
                        if (!EPSILON.equals(token)) {
                            changed |= firstLhs.add(token);
                        }
                    }
                    if (!firstSymbol.contains(EPSILON)) {
                        nullablePrefix = false;
                        break;
                    }
                }
                if (nullablePrefix) {
                    changed |= firstLhs.add(EPSILON);
                }
            }
        } while (changed);

        firstSets = first;
    }

    private void computeFollowSets() {
        Map<String, Set<String>> follow = new LinkedHashMap<>();
        for (String nonTerminal : nonTerminals) {
            follow.put(nonTerminal, new LinkedHashSet<>());
        }
        follow.get(startSymbol).add(END_MARKER);

        boolean changed;
        do {
            changed = false;
            for (Production production : productions) {
                List<String> rhs = production.rhs;
                for (int i = 0; i < rhs.size(); i++) {
                    String symbol = rhs.get(i);
                    if (!nonTerminals.contains(symbol)) {
                        continue;
                    }
                    List<String> suffix = rhs.subList(i + 1, rhs.size());
                    Set<String> firstSuffix = firstOfSequence(suffix);
                    Set<String> followSymbol = follow.get(symbol);
                    for (String token : firstSuffix) {
                        if (!EPSILON.equals(token)) {
                            changed |= followSymbol.add(token);
                        }
                    }
                    if (suffix.isEmpty() || firstSuffix.contains(EPSILON)) {
                        changed |= followSymbol.addAll(follow.get(production.lhs));
                    }
                }
            }
        } while (changed);

        followSets = follow;
    }

    private String makeUniqueAugmentedStart(String base) {
        String candidate = base + "Prime";
        while (nonTerminals.contains(candidate) || terminals.contains(candidate)) {
            candidate = candidate + "Prime";
        }
        return candidate;
    }

    private static List<String> tokenizeRhs(String rhsPart) {
        if (rhsPart.isEmpty() || rhsPart.equals(EPSILON) || rhsPart.equals("@")) {
            return new ArrayList<>();
        }
        String[] tokens = rhsPart.trim().split("\\s+");
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isBlank()) {
                result.add(token.trim());
            }
        }
        if (result.size() == 1 && (result.get(0).equals(EPSILON) || result.get(0).equals("@"))) {
            return new ArrayList<>();
        }
        return result;
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        if (hash >= 0) {
            return line.substring(0, hash);
        }
        return line;
    }

    private static final class RawProduction {
        private final String lhs;
        private final List<String> rhs;

        private RawProduction(String lhs, List<String> rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }
}

final class Production {
    final String lhs;
    final List<String> rhs;

    Production(String lhs, List<String> rhs) {
        this.lhs = lhs;
        this.rhs = List.copyOf(rhs);
    }

    String rhsAsString() {
        if (rhs.isEmpty()) {
            return Grammar.EPSILON;
        }
        return String.join(" ", rhs);
    }

    @Override
    public String toString() {
        return lhs + " -> " + rhsAsString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Production)) {
            return false;
        }
        Production that = (Production) other;
        return lhs.equals(that.lhs) && rhs.equals(that.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs);
    }
}