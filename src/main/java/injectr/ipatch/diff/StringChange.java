package injectr.ipatch.diff;

public class StringChange implements Change {

    private final ChangeType type;
    private final int startLine, endLine; //Both inclusive
    private final String newContent;

    public StringChange(ChangeType type, int startLine, int endLine, String newContent) {
        this.type = type;
        this.startLine = startLine;
        this.endLine = endLine;
        this.newContent = newContent;
    }

    @Override
    public ChangeType type() {
        return type;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getNewContent() {
        return newContent;
    }
}
