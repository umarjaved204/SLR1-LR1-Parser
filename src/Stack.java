import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Stack {
    private final Deque<StackEntry> entries = new ArrayDeque<>();

    public Stack() {
        entries.addLast(new StackEntry(null, 0, null));
    }

    public int topState() {
        return entries.peekLast().state;
    }

    public void shift(String symbol, int state, TreeNode node) {
        entries.addLast(new StackEntry(symbol, state, node));
    }

    public List<TreeNode> popNodes(int count) {
        List<TreeNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StackEntry removed = entries.removeLast();
            if (removed.node != null) {
                nodes.add(removed.node);
            }
        }
        return nodes;
    }

    public void pushNonTerminal(String symbol, int state, TreeNode node) {
        entries.addLast(new StackEntry(symbol, state, node));
    }

    public String snapshot() {
        List<StackEntry> ordered = new ArrayList<>(entries);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ordered.size(); i++) {
            StackEntry entry = ordered.get(i);
            if (i == 0) {
                builder.append(entry.state);
            } else {
                builder.append(' ').append(entry.symbol).append(' ').append(entry.state);
            }
        }
        return builder.toString();
    }

    public TreeNode topNode() {
        java.util.Iterator<StackEntry> iterator = entries.descendingIterator();
        while (iterator.hasNext()) {
            StackEntry entry = iterator.next();
            if (entry.node != null) {
                return entry.node;
            }
        }
        return null;
    }

    private static final class StackEntry {
        final String symbol;
        final int state;
        final TreeNode node;

        StackEntry(String symbol, int state, TreeNode node) {
            this.symbol = symbol;
            this.state = state;
            this.node = node;
        }
    }
}
