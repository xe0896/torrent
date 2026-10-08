package bencode.structure;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record BBytes(byte[] value) implements BValue, Comparable<BBytes> {
    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BBytes other && Arrays.equals(value, other.value());
    }

    @Override
    public int compareTo(BBytes other) {
        return Arrays.compareUnsigned(value, other.value());
    }

    @Override
    public String toString() {
        return new String(value, StandardCharsets.ISO_8859_1);
    }

    public static BBytes of(String key) {
        return new BBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
