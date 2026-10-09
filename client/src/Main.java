import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import bencode.BDecoder;
import bencode.BEncoder;
import bencode.structure.BBytes;
import bencode.structure.BDict;
import bencode.structure.BInt;
import bencode.structure.BValue;
import entities.PeerConnection;
import entities.PeerConnection.PeerAddress;
import helper.Helper;
import request.Request;

public class Main {
    private static final String URL = "http://localhost:8080/";
    private static final String version = "0001";

    record MetaInfo(String name, long pieceLength, byte[] pieces, long length) {
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException {
        // src/ because we are in Makefile level
        Path mini = Path.of("src/test-torrents/single.torrent");
        byte[] data = Files.readAllBytes(mini);
        BValue root = new BDecoder(data).parseValue();

        if (!(root instanceof BDict dict)) {
            throw new IllegalArgumentException("Torrent root must be a dict");
        }

        // 'root' is instance of BDict, "announce" field from the root
        // is the tracker URL

        // "info" field is a BDict that contains: "name", "piece_length", "pieces"
        // also "length" which is for single-field torrents

        // Since 'root' is an instance of BDict we can use the SortedMap<BBytes, BValue>
        // we provide the string to get the BValue which for the "announce" would just
        // be a straight BByte whereas the 'info' would point elsewhere

        BDict info = getDict(dict, "info");
        PeerAddress p = PeerConnection.createAddress(InetAddress.getByName("127.0.0.1"), 6080);

        System.out.println(HexFormat.of().formatHex(info.hash()));
        System.out.println(HexFormat.of().formatHex(PeerConnection.createPeerId(version)));

        String infoHash = percentEncode(info.hash());
        String peerId = percentEncode(PeerConnection.createPeerId(version));

        long length = getLong(info, "length");

        Helper.print(infoHash, peerId);

        String queries = Request.queries(Map.of(
                "info_hash", infoHash,
                "peer_id", peerId,
                "port", String.valueOf(p.port()),
                "uploaded", "0",
                "downloaded", "0",
                "left", String.valueOf(length),
                "compact", "1",
                "event", "started"));

        System.out.println(request("announce", queries, BodyHandlers.ofString()));
    }

    public static String percentEncode(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        // %BA -> byte 0xBA
        // BA -> literal characters 'B' and 'A'

        // %02X outputs a byte as two hex digits, then we need to prepend a double '%' to get
        // an actual percentage sign in the string

        for (byte b : bytes) {
            sb.append(String.format("%%%02X", b & 0xFF)); // 0xFF to make it unsigned
        }

        return sb.toString();
    }

    public static <T> T request(String endpoint, String queries, BodyHandler<T> handler)
            throws IOException, InterruptedException {
        return Request.fetchAt(String.format("%s%s?%s", URL, endpoint, queries), handler);
    }

    public static long getLong(BDict dict, String key) {
        Optional<BValue> _bValue = dict.get(key);
        if (_bValue.isEmpty())
            throw new IllegalArgumentException(String.format("Must contain '%s' int field", key));
        if (_bValue.get() instanceof BInt bInt)
            return bInt.value();
        throw new IllegalArgumentException(String.format("Provided field '%s' is not a BInt", key));
    }

    public static String getString(BDict dict, String key) {
        Optional<BValue> _bValue = dict.get(key);
        if (_bValue.isEmpty())
            throw new IllegalArgumentException(String.format("Must contain '%s' byte field", key));
        if (_bValue.get() instanceof BBytes bBytes)
            return new String(bBytes.value(), StandardCharsets.UTF_8);
        throw new IllegalArgumentException(String.format("Provided field '%s' is not a BByte", key));
    }

    public static byte[] getBytes(BDict dict, String key) {
        Optional<BValue> _bValue = dict.get(key);
        if (_bValue.isEmpty())
            throw new IllegalArgumentException(String.format("Must contain '%s' byte field", key));
        if (_bValue.get() instanceof BBytes bBytes)
            return bBytes.value();
        throw new IllegalArgumentException(String.format("Provided field '%s' is not a BByte", key));
    }

    public static BDict getDict(BDict dict, String key) {
        Optional<BValue> _bValue = dict.get(key);
        if (_bValue.isEmpty())
            throw new IllegalArgumentException(String.format("Must contain '%s' dict field", key));
        if (_bValue.get() instanceof BDict bDict)
            return bDict;
        throw new IllegalArgumentException(String.format("Provided field '%s' is not a BDict", key));
    }

}