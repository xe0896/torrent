package bencode.structure;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.SortedMap;

import bencode.BEncoder;

public record BDict(SortedMap<BBytes, BValue> entries) implements BValue {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;

        for (var pair : entries.entrySet()) {
            BBytes key = pair.getKey();
            BValue value = pair.getValue();

            sb.append(String.format("[%s : %s]", key, value));
            if (i != entries.size() - 1)
                sb.append(", ");
            i++;
        }

        sb.append("}");
        return sb.toString();
    }

    public byte[] hash() throws IOException, NoSuchAlgorithmException {
        byte[] bytes = new BEncoder(this).encode();

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(bytes);
        return hash;
    }

    public Optional<BValue> get(String key) {
        return Optional.ofNullable(entries.get(BBytes.of(key)));
    }
}
