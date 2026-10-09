package bencode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import bencode.structure.*;

public class BDecoder {
    private final byte[] data;
    private int pos = 0;

    public BDecoder(byte[] data) {
        this.data = data;
    }

    public BValue parseValue() throws RuntimeException {
        byte b = data[pos];

        if (b == 'i')
            return parseInt();
        if (b == 'l')
            return parseList();
        if (b == 'd')
            return parseDict();
        if (b >= '0' || b <= '9')
            return parseBytes();

        throw new RuntimeException("Unexpected byte: " + b);
    }

    BInt parseInt() {
        pos++; // Skip over 'i'
        // Read up until 'e' to create a 'long' 

        StringBuilder sb = new StringBuilder();

        while (data[pos] != 'e') {
            sb.append((char) data[pos++]);
        }

        long value = Long.valueOf(sb.toString());

        pos++;
        // Skip over 'e'

        return new BInt(value);
    }

    BList parseList() {
        pos++;
        // Skip over 'i'

        List<BValue> list = new ArrayList<>();

        while (data[pos] != 'e') {
            list.add(parseValue());
        }

        pos++; // Skip over 'e'

        return new BList(list);
    }

    BDict parseDict() {
        pos++; // Skip over 'd'

        // d3:cow3:moo4:spam4:eggse

        // 3:cow -> cow
        // 3:moo -> moo
        // 4:spam -> spam
        // 4:eggse -> eggse

        // Alternation is key1, value1, key2, value2..

        // The key has to be a string so we call parseBytes(), but the value
        // can be anything, so then we call parseValue() for it

        SortedMap<BBytes, BValue> map = new TreeMap<>();

        while (data[pos] != 'e') {
            BBytes key = parseBytes();
            BValue value = parseValue();

            map.put(key, value);
        }

        pos++; // Skip over 'e'

        return new BDict(map);
    }

    // (Byte string)
    BBytes parseBytes() {
        // Points directly at the start of the length, where : is the delimiter

        StringBuilder sb = new StringBuilder();

        while (data[pos] != ':') {
            sb.append((char) data[pos++]);
        }

        int length = Integer.valueOf(sb.toString());

        pos++; // Skip over :

        byte[] value = Arrays.copyOfRange(data, pos, pos + length);

        pos += length;

        return new BBytes(value);
    }
}
