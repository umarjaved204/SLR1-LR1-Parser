import java.util.ArrayList;
import java.util.List;

public class Tree {
    private final TreeNode root;

    public Tree(TreeNode root) {
        this.root = root;
    }

    public TreeNode getRoot() {
        return root;
    }

    public String toIndentedString() {
        if (root == null) {
            return "<empty tree>\n";
        }
        StringBuilder builder = new StringBuilder();
        render(root, builder, 0);
        return builder.toString();
    }

    private void render(TreeNode node, StringBuilder builder, int depth) {
        builder.append("  ".repeat(Math.max(0, depth)));
        builder.append(node.symbol).append(System.lineSeparator());
        List<TreeNode> children = node.children;
        for (int i = 0; i < children.size(); i++) {
            render(children.get(i), builder, depth + 1);
        }
    }

    public String toBracketString() {
        if (root == null) {
            return "[]";
        }
        return root.toBracketString();
    }
}

final class TreeNode {
    final String symbol;
    final List<TreeNode> children;

    TreeNode(String symbol) {
        this(symbol, new ArrayList<>());
    }

    TreeNode(String symbol, List<TreeNode> children) {
        this.symbol = symbol;
        this.children = new ArrayList<>(children);
    }

    String toBracketString() {
        if (children.isEmpty()) {
            return symbol;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(symbol).append('(');
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(children.get(i).toBracketString());
        }
        builder.append(')');
        return builder.toString();
    }
}

final class ParserReport {
    String grammarText = "";
    String augmentedGrammarText = "";
    String itemSetsText = "";
    String parsingTableText = "";
    String traceText = "";
    String comparisonText = "";
    String conflictText = "";
    String parseTreesText = "";
    boolean accepted;
    int stateCount;
    int tableEntryCount;
    long buildTimeNanos;
    long parseTimeNanos;
    Tree parseTree;
    final List<String> conflicts = new ArrayList<>();

    String summaryLine() {
        return "accepted=" + accepted + ", states=" + stateCount + ", entries=" + tableEntryCount;
    }

    void appendConflict(String conflict) {
        conflicts.add(conflict);
    }

    String conflictsAsString() {
        if (conflicts.isEmpty()) {
            return "No conflicts found.\n";
        }
        StringBuilder builder = new StringBuilder();
        for (String conflict : conflicts) {
            builder.append(conflict).append(System.lineSeparator());
        }
        return builder.toString();
    }

    String treeAsString() {
        if (parseTree == null) {
            return "<no parse tree>\n";
        }
        return parseTree.toIndentedString();
    }
}
