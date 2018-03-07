package injectr.ipatch.diff;

public class ByteChange implements Change {

    private final ChangeType type;
    private final int startIndex, endIndex; //Both inclusive
    private final byte[] newContent;

    public ByteChange(ChangeType type, int startIndex, int endIndex, byte[] newContent) {
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.newContent = newContent;
    }

    @Override
    public ChangeType type() {
        return type;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public byte[] getNewContent() {
        return newContent;
    }
}
