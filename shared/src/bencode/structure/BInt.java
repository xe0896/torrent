package bencode.structure;

// i digits e
public record BInt(long value) implements BValue {
    @Override
    public String toString() {
        return Long.toString(value);
    }
}
