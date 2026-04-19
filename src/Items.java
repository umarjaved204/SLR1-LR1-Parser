import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class Items {
    private Items() {
    }

    public static LR0Automaton buildLR0Automaton(Grammar grammar) {
        return new LR0Automaton(grammar);
    }

    public static LR1Automaton buildLR1Automaton(Grammar grammar) {
        return new LR1Automaton(grammar);
    }

    public static String formatLR0ItemSet(int index, Set<LR0Item> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("I").append(index).append(":").append(System.lineSeparator());
        int itemIndex = 1;
        for (LR0Item item : items) {
            builder.append("  ").append(itemIndex++).append(". ").append(item).append(System.lineSeparator());
        }
        return builder.toString();
    }

    public static String formatLR1ItemSet(int index, Set<LR1Item> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("I").append(index).append(":").append(System.lineSeparator());
        int itemIndex = 1;
        for (LR1Item item : items) {
            builder.append("  ").append(itemIndex++).append(". ").append(item).append(System.lineSeparator());
        }
        return builder.toString();
    }
}

final class LR0Item {
    final String lhs;
    final List<String> rhs;
    final int dot;

    LR0Item(String lhs, List<String> rhs, int dot) {
        this.lhs = lhs;
        this.rhs = List.copyOf(rhs);
        this.dot = dot;
    }

    boolean isComplete() {
        return dot >= rhs.size();
    }

    String symbolAfterDot() {
        if (isComplete()) {
            return null;
        }
        return rhs.get(dot);
    }

    LR0Item advanceDot() {
        return new LR0Item(lhs, rhs, dot + 1);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LR0Item)) {
            return false;
        }
        LR0Item that = (LR0Item) other;
        return dot == that.dot && lhs.equals(that.lhs) && rhs.equals(that.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs, dot);
    }

    @Override
    public String toString() {
        return lhs + " -> " + renderWithDot();
    }

    private String renderWithDot() {
        List<String> parts = new ArrayList<>(rhs);
        parts.add(dot, "•");
        if (rhs.isEmpty()) {
            parts.clear();
            parts.add("•");
        }
        return String.join(" ", parts);
    }
}

final class LR1Item {
    final String lhs;
    final List<String> rhs;
    final int dot;
    final String lookahead;

    LR1Item(String lhs, List<String> rhs, int dot, String lookahead) {
        this.lhs = lhs;
        this.rhs = List.copyOf(rhs);
        this.dot = dot;
        this.lookahead = lookahead;
    }

    boolean isComplete() {
        return dot >= rhs.size();
    }

    String symbolAfterDot() {
        if (isComplete()) {
            return null;
        }
        return rhs.get(dot);
    }

    LR1Item advanceDot() {
        return new LR1Item(lhs, rhs, dot + 1, lookahead);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LR1Item)) {
            return false;
        }
        LR1Item that = (LR1Item) other;
        return dot == that.dot && lhs.equals(that.lhs) && rhs.equals(that.rhs) && lookahead.equals(that.lookahead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs, dot, lookahead);
    }

    @Override
    public String toString() {
        List<String> parts = new ArrayList<>(rhs);
        parts.add(dot, "•");
        if (rhs.isEmpty()) {
            parts.clear();
            parts.add("•");
        }
        return "[" + lhs + " -> " + String.join(" ", parts) + ", " + lookahead + "]";
    }
}

final class LR0Automaton {
    final List<Set<LR0Item>> states = new ArrayList<>();
    final Map<Integer, Map<String, Integer>> transitions = new LinkedHashMap<>();

    LR0Automaton(Grammar grammar) {
        build(grammar);
    }

    private void build(Grammar grammar) {
        LinkedHashSet<LR0Item> start = new LinkedHashSet<>();
        Production startProduction = grammar.getProductions().get(0);
        start.add(new LR0Item(startProduction.lhs, startProduction.rhs, 0));
        Set<LR0Item> startClosure = closure(start, grammar);
        states.add(startClosure);

        for (int i = 0; i < states.size(); i++) {
            Set<LR0Item> state = states.get(i);
            LinkedHashSet<String> symbols = new LinkedHashSet<>();
            for (LR0Item item : state) {
                String symbol = item.symbolAfterDot();
                if (symbol != null) {
                    symbols.add(symbol);
                }
            }
            for (String symbol : symbols) {
                Set<LR0Item> gotoState = goTo(state, symbol, grammar);
                if (gotoState.isEmpty()) {
                    continue;
                }
                int existing = indexOfState(gotoState);
                if (existing < 0) {
                    existing = states.size();
                    states.add(gotoState);
                }
                transitions.computeIfAbsent(i, key -> new LinkedHashMap<>()).put(symbol, existing);
            }
        }
    }

    private Set<LR0Item> closure(Set<LR0Item> items, Grammar grammar) {
        LinkedHashSet<LR0Item> closure = new LinkedHashSet<>(items);
        boolean changed;
        do {
            changed = false;
            List<LR0Item> snapshot = new ArrayList<>(closure);
            for (LR0Item item : snapshot) {
                String nextSymbol = item.symbolAfterDot();
                if (nextSymbol == null || !grammar.isNonTerminal(nextSymbol)) {
                    continue;
                }
                for (Production production : grammar.getProductionsFor(nextSymbol)) {
                    LR0Item candidate = new LR0Item(production.lhs, production.rhs, 0);
                    if (closure.add(candidate)) {
                        changed = true;
                    }
                }
            }
        } while (changed);
        return closure;
    }

    private Set<LR0Item> goTo(Set<LR0Item> state, String symbol, Grammar grammar) {
        LinkedHashSet<LR0Item> moved = new LinkedHashSet<>();
        for (LR0Item item : state) {
            if (symbol.equals(item.symbolAfterDot())) {
                moved.add(item.advanceDot());
            }
        }
        if (moved.isEmpty()) {
            return moved;
        }
        return closure(moved, grammar);
    }

    private int indexOfState(Set<LR0Item> target) {
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i).equals(target)) {
                return i;
            }
        }
        return -1;
    }
}

final class LR1Automaton {
    final List<Set<LR1Item>> states = new ArrayList<>();
    final Map<Integer, Map<String, Integer>> transitions = new LinkedHashMap<>();

    LR1Automaton(Grammar grammar) {
        build(grammar);
    }

    private void build(Grammar grammar) {
        LinkedHashSet<LR1Item> start = new LinkedHashSet<>();
        Production startProduction = grammar.getProductions().get(0);
        start.add(new LR1Item(startProduction.lhs, startProduction.rhs, 0, Grammar.END_MARKER));
        Set<LR1Item> startClosure = closure(start, grammar);
        states.add(startClosure);

        for (int i = 0; i < states.size(); i++) {
            Set<LR1Item> state = states.get(i);
            LinkedHashSet<String> symbols = new LinkedHashSet<>();
            for (LR1Item item : state) {
                String symbol = item.symbolAfterDot();
                if (symbol != null) {
                    symbols.add(symbol);
                }
            }
            for (String symbol : symbols) {
                Set<LR1Item> gotoState = goTo(state, symbol, grammar);
                if (gotoState.isEmpty()) {
                    continue;
                }
                int existing = indexOfState(gotoState);
                if (existing < 0) {
                    existing = states.size();
                    states.add(gotoState);
                }
                transitions.computeIfAbsent(i, key -> new LinkedHashMap<>()).put(symbol, existing);
            }
        }
    }

    private Set<LR1Item> closure(Set<LR1Item> items, Grammar grammar) {
        LinkedHashSet<LR1Item> closure = new LinkedHashSet<>(items);
        boolean changed;
        do {
            changed = false;
            List<LR1Item> snapshot = new ArrayList<>(closure);
            for (LR1Item item : snapshot) {
                String nextSymbol = item.symbolAfterDot();
                if (nextSymbol == null || !grammar.isNonTerminal(nextSymbol)) {
                    continue;
                }
                List<String> beta = item.rhs.subList(item.dot + 1, item.rhs.size());
                Set<String> lookaheads = grammar.firstOfSequenceWithLookahead(beta, item.lookahead);
                for (Production production : grammar.getProductionsFor(nextSymbol)) {
                    for (String lookahead : lookaheads) {
                        LR1Item candidate = new LR1Item(production.lhs, production.rhs, 0, lookahead);
                        if (closure.add(candidate)) {
                            changed = true;
                        }
                    }
                }
            }
        } while (changed);
        return closure;
    }

    private Set<LR1Item> goTo(Set<LR1Item> state, String symbol, Grammar grammar) {
        LinkedHashSet<LR1Item> moved = new LinkedHashSet<>();
        for (LR1Item item : state) {
            if (symbol.equals(item.symbolAfterDot())) {
                moved.add(item.advanceDot());
            }
        }
        if (moved.isEmpty()) {
            return moved;
        }
        return closure(moved, grammar);
    }

    private int indexOfState(Set<LR1Item> target) {
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i).equals(target)) {
                return i;
            }
        }
        return -1;
    }
}
