package bencode.structure;

import java.util.List;

public record BList(List<BValue> values) implements BValue {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < values.size(); i++) {
            sb.append(values.get(i));
            if (i != values.size() - 1)
                sb.append(", ");
        }

        sb.append("]");
        return sb.toString();
    }
}
