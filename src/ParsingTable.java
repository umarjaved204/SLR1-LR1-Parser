import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParsingTable {
    private final List<String> terminals;
    private final List<String> nonTerminals;
    private final Map<Integer, Map<String, Action>> actionTable = new LinkedHashMap<>();
    private final Map<Integer, Map<String, Integer>> gotoTable = new LinkedHashMap<>();
    private final List<String> conflicts = new ArrayList<>();
    private int entryCount;

    public ParsingTable(Grammar grammar) {
        this.terminals = new ArrayList<>();
        this.terminals.addAll(grammar.getTerminals());
        if (!this.terminals.contains(Grammar.END_MARKER)) {
            this.terminals.add(Grammar.END_MARKER);
        }
        this.nonTerminals = new ArrayList<>();
        for (String symbol : grammar.getNonTerminals()) {
            if (!symbol.equals(grammar.getAugmentedStartSymbol())) {
                this.nonTerminals.add(symbol);
            }
        }
    }

    public void addShift(int state, String terminal, int targetState) {
        putAction(state, terminal, Action.shift(targetState));
    }

    public void addReduce(int state, String terminal, Production production) {
        putAction(state, terminal, Action.reduce(production));
    }

    public void addAccept(int state, String terminal) {
        putAction(state, terminal, Action.accept());
    }

    public void addGoto(int state, String nonTerminal, int targetState) {
        gotoTable.computeIfAbsent(state, key -> new LinkedHashMap<>()).put(nonTerminal, targetState);
        entryCount++;
    }

    public Action getAction(int state, String terminal) {
        Map<String, Action> row = actionTable.get(state);
        if (row == null) {
            return null;
        }
        return row.get(terminal);
    }

    public Integer getGoto(int state, String nonTerminal) {
        Map<String, Integer> row = gotoTable.get(state);
        if (row == null) {
            return null;
        }
        return row.get(nonTerminal);
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public String toFormattedString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%-6s", "State"));
        for (String terminal : terminals) {
            builder.append(String.format("%-18s", terminal));
        }
        for (String nonTerminal : nonTerminals) {
            builder.append(String.format("%-18s", nonTerminal));
        }
        builder.append(System.lineSeparator());

        int stateCount = Math.max(actionTable.keySet().stream().mapToInt(Integer::intValue).max().orElse(0),
                gotoTable.keySet().stream().mapToInt(Integer::intValue).max().orElse(0)) + 1;
        for (int state = 0; state < stateCount; state++) {
            builder.append(String.format("%-6s", "I" + state));
            for (String terminal : terminals) {
                Action action = getAction(state, terminal);
                builder.append(String.format("%-18s", action == null ? "" : action));
            }
            for (String nonTerminal : nonTerminals) {
                Integer target = getGoto(state, nonTerminal);
                builder.append(String.format("%-18s", target == null ? "" : target));
            }
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    public String conflictSummary() {
        if (conflicts.isEmpty()) {
            return "No conflicts detected.\n";
        }
        StringBuilder builder = new StringBuilder();
        for (String conflict : conflicts) {
            builder.append(conflict).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private void putAction(int state, String terminal, Action action) {
        Map<String, Action> row = actionTable.computeIfAbsent(state, key -> new LinkedHashMap<>());
        Action existing = row.get(terminal);
        if (existing == null) {
            row.put(terminal, action);
            entryCount++;
            return;
        }
        if (!existing.equals(action)) {
            conflicts.add("Conflict at ACTION[" + state + ", " + terminal + "]: " + existing + " vs " + action);
        }
    }

    public static final class Action {
        enum Type {
            SHIFT,
            REDUCE,
            ACCEPT
        }

        final Type type;
        final int state;
        final Production production;

        private Action(Type type, int state, Production production) {
            this.type = type;
            this.state = state;
            this.production = production;
        }

        static Action shift(int state) {
            return new Action(Type.SHIFT, state, null);
        }

        static Action reduce(Production production) {
            return new Action(Type.REDUCE, -1, production);
        }

        static Action accept() {
            return new Action(Type.ACCEPT, -1, null);
        }

        @Override
        public String toString() {
            return switch (type) {
                case SHIFT -> "s" + state;
                case REDUCE -> "r(" + production + ")";
                case ACCEPT -> "acc";
            };
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Action)) {
                return false;
            }
            Action that = (Action) other;
            return type == that.type && state == that.state && java.util.Objects.equals(production, that.production);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(type, state, production);
        }
    }
}
