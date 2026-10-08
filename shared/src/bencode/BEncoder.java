package bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import bencode.structure.BBytes;
import bencode.structure.BDict;
import bencode.structure.BInt;
import bencode.structure.BList;
import bencode.structure.BValue;

public class BEncoder {
    private final BValue data;
    private ByteArrayOutputStream out = new ByteArrayOutputStream();

    public BEncoder(BValue data) {
        this.data = data;
    }

    // An encoder does the reverse of a decoder, it is meant to take input
    // of a valid torrent structure represented by BValue and create it back
    // into a stream where it can be decoded again via the BDecoder

    public byte[] encode() throws IOException {
        encode(data);
        return out.toByteArray();
    }

    public void encode(BValue v) throws IOException {
        switch (v) {
            case BInt i -> {
                out.write('i');
                out.writeBytes(Long.toString(i.value()).getBytes(StandardCharsets.US_ASCII));
                out.write('e');
            }

            case BBytes b -> {
                out.write(Integer.toString(b.value().length).getBytes(StandardCharsets.US_ASCII));
                out.write(':');
                out.write(b.value());
            }

            case BList l -> {
                out.write('l');
                for (BValue value : l.values())
                    encode(value);
                out.write('e');
            }

            case BDict d -> {
                out.write('d');
                for (var entry : d.entries().entrySet()) {
                    encode(entry.getKey());
                    encode(entry.getValue());
                }
                out.write('e');
            }
        }
    }
}
