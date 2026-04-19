import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LR1Parser {
    private final Grammar grammar;

    public LR1Parser(Grammar grammar) {
        this.grammar = grammar;
    }

    public ParserReport buildReport() {
        long start = System.nanoTime();
        LR1Automaton automaton = Items.buildLR1Automaton(grammar);
        ParsingTable table = buildParsingTable(automaton);
        long end = System.nanoTime();

        ParserReport report = new ParserReport();
        report.grammarText = grammar.toFormattedString();
        report.augmentedGrammarText = grammar.toFormattedString();
        report.itemSetsText = formatAutomaton(automaton);
        report.parsingTableText = table.toFormattedString();
        report.conflicts.addAll(table.getConflicts());
        report.conflictText = table.conflictSummary();
        report.stateCount = automaton.states.size();
        report.tableEntryCount = table.getEntryCount();
        report.buildTimeNanos = end - start;
        return report;
    }

    public ParserReport parse(String input) {
        long buildStart = System.nanoTime();
        LR1Automaton automaton = Items.buildLR1Automaton(grammar);
        ParsingTable table = buildParsingTable(automaton);
        long buildEnd = System.nanoTime();

        List<String> tokens = tokenize(input);
        Stack stack = new Stack();
        TreeNode rootNode = null;
        StringBuilder trace = new StringBuilder();
        trace.append(String.format("%-6s %-35s %-25s %-18s%n", "Step", "Stack", "Input", "Action"));

        int step = 1;
        int index = 0;
        boolean accepted = false;
        while (true) {
            int state = stack.topState();
            String lookahead = index < tokens.size() ? tokens.get(index) : Grammar.END_MARKER;
            ParsingTable.Action action = table.getAction(state, lookahead);
            String stackSnapshot = stack.snapshot();
            String remainingInput = joinRemaining(tokens, index);
            String actionText;

            if (action == null) {
                actionText = "error";
                trace.append(String.format("%-6d %-35s %-25s %-18s%n", step, stackSnapshot, remainingInput, actionText));
                break;
            }

            switch (action.type) {
                case SHIFT -> {
                    TreeNode terminalNode = new TreeNode(lookahead);
                    stack.shift(lookahead, action.state, terminalNode);
                    index++;
                    actionText = "shift " + action.state;
                }
                case REDUCE -> {
                    Production production = action.production;
                    List<TreeNode> children = stack.popNodes(production.rhs.size());
                    reverse(children);
                    TreeNode parent = new TreeNode(production.lhs, children);
                    int gotoState = table.getGoto(stack.topState(), production.lhs);
                    stack.pushNonTerminal(production.lhs, gotoState, parent);
                    rootNode = parent;
                    actionText = "reduce " + production;
                }
                case ACCEPT -> {
                    actionText = "accept";
                    accepted = true;
                }
                default -> throw new IllegalStateException("Unexpected action: " + action);
            }

            trace.append(String.format("%-6d %-35s %-25s %-18s%n", step, stackSnapshot, remainingInput, actionText));
            if (accepted) {
                break;
            }
            step++;
        }

        long parseEnd = System.nanoTime();
        ParserReport report = new ParserReport();
        report.accepted = accepted;
        report.traceText = trace.toString();
        report.parseTree = rootNode != null ? new Tree(rootNode) : null;
        report.parseTreesText = report.treeAsString();
        report.stateCount = automaton.states.size();
        report.tableEntryCount = table.getEntryCount();
        report.buildTimeNanos = buildEnd - buildStart;
        report.parseTimeNanos = parseEnd - buildEnd;
        report.parsingTableText = table.toFormattedString();
        report.conflicts.addAll(table.getConflicts());
        report.conflictText = table.conflictSummary();
        report.itemSetsText = formatAutomaton(automaton);
        return report;
    }

    private ParsingTable buildParsingTable(LR1Automaton automaton) {
        ParsingTable table = new ParsingTable(grammar);
        String augmentedStart = grammar.getAugmentedStartSymbol();
        for (int state = 0; state < automaton.states.size(); state++) {
            Set<LR1Item> items = automaton.states.get(state);
            for (LR1Item item : items) {
                String nextSymbol = item.symbolAfterDot();
                if (nextSymbol != null && grammar.isTerminal(nextSymbol)) {
                    Integer target = automaton.transitions.getOrDefault(state, java.util.Collections.emptyMap()).get(nextSymbol);
                    if (target != null) {
                        table.addShift(state, nextSymbol, target);
                    }
                }
                if (item.isComplete()) {
                    if (item.lhs.equals(augmentedStart) && Grammar.END_MARKER.equals(item.lookahead)) {
                        table.addAccept(state, Grammar.END_MARKER);
                    } else {
                        table.addReduce(state, item.lookahead, findProduction(item));
                    }
                }
            }
            for (String nonTerminal : grammar.getNonTerminals()) {
                if (nonTerminal.equals(augmentedStart)) {
                    continue;
                }
                Integer target = automaton.transitions.getOrDefault(state, java.util.Collections.emptyMap()).get(nonTerminal);
                if (target != null) {
                    table.addGoto(state, nonTerminal, target);
                }
            }
        }
        return table;
    }

    private Production findProduction(LR1Item item) {
        for (Production production : grammar.getProductions()) {
            if (production.lhs.equals(item.lhs) && production.rhs.equals(item.rhs)) {
                return production;
            }
        }
        throw new IllegalStateException("Production not found for item " + item);
    }

    private String formatAutomaton(LR1Automaton automaton) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < automaton.states.size(); i++) {
            builder.append(Items.formatLR1ItemSet(i, automaton.states.get(i)));
            builder.append("  Transitions: ");
            builder.append(automaton.transitions.getOrDefault(i, java.util.Collections.emptyMap()));
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        if (input != null) {
            String trimmed = input.trim();
            if (!trimmed.isEmpty()) {
                for (String token : trimmed.split("\\s+")) {
                    if (!token.isBlank()) {
                        tokens.add(token);
                    }
                }
            }
        }
        return tokens;
    }

    private String joinRemaining(List<String> tokens, int index) {
        if (index >= tokens.size()) {
            return Grammar.END_MARKER;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = index; i < tokens.size(); i++) {
            if (i > index) {
                builder.append(' ');
            }
            builder.append(tokens.get(i));
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(Grammar.END_MARKER);
        return builder.toString();
    }

    private void reverse(List<TreeNode> nodes) {
        for (int left = 0, right = nodes.size() - 1; left < right; left++, right--) {
            TreeNode temp = nodes.get(left);
            nodes.set(left, nodes.get(right));
            nodes.set(right, temp);
        }
    }
}
