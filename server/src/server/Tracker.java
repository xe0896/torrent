package server;

import com.sun.net.httpserver.HttpServer;

import bencode.BDecoder;
import bencode.BEncoder;
import bencode.structure.BBytes;
import bencode.structure.BDict;
import bencode.structure.BInt;
import bencode.structure.BValue;
import helper.Helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Tracker {
    public final HttpServer server;

    record PeerInformation(String ip, String port, String left, Instant lastAnnounce) {
    }

    record PeerKey(String infoHash, String peerId) {
    }

    record Request(Optional<String> failure, Optional<Long> interval, Optional<Integer> complete,
            Optional<Integer> incomplete) {
    }

    // A mapping between a PeerKey which represents a user in a particular session and the users
    // information
    Map<PeerKey, PeerInformation> session = new HashMap<>();

    // A mapping between an infoHash that represents a .torrent session and a set of PeerKey's that
    // represent the users in that session
    Map<String, Set<PeerKey>> sessions = new HashMap<>();

    public Tracker(int port) throws IOException {
        // Backlog determines how many incoming TCP connections
        // it will internally queue, no IP specifies implies
        // wildcard address
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        // createContext expects a HttpHandler, so then the lambda
        // would create it for us and given its an interface
        // with a single function 'handle(HttpExchange)' then it 
        // can infer that we are calling that
        server.createContext("/ping", exchange -> {
            byte[] body = "pong\n".getBytes(StandardCharsets.UTF_8);
            // Status code of our response, as well as the length
            // of the data we are going to provide back
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
                // Closes for us
            }
        });

        server.createContext("/announce", exchange -> {
            try {
                Map<String, String> query = params(exchange.getRequestURI())
                        .orElseThrow(() -> new IllegalArgumentException("Missing parameters"));

                byte[] response = new BEncoder(createResponse(newRequest(query, exchange.getRemoteAddress()))).encode();
                System.out.println(exchange.getRemoteAddress());
                exchange.sendResponseHeaders(200, response.length);

                //byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                //exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            } catch (IllegalArgumentException e) {
                byte[] body = e.getMessage().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, body.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }

            } finally {
                exchange.close();
            }
        });
    }

    private byte[] decodePercent(String in) {
        int cur = 0;
        byte[] bytes = new byte[20];

        for (int i = 0; i < in.length(); i += 3) {
            // %A5%85%13%C7%11%C9%0A%F5%5F%E7%0F%18%22%1D%BE%95%AB%AE%EB%83
            // %A5
            // %85
            // %13

            char x = in.charAt(i + 1); // 1st hex
            char y = in.charAt(i + 2); // 2nd hex

            String hex = new String(new char[] { x, y });

            // Integer.parseInt(hex, 16) gives 0-255 making it go back to Java's signed byte
            byte b = (byte) Integer.parseInt(hex, 16);
            bytes[cur++] = b;
        }

        return bytes;
    }

    private Request newRequest(Map<String, String> query, InetSocketAddress addr) {
        String _infoHash = getValue(query, "info_hash");
        String _peerId = getValue(query, "peer_id");
        String peerPort = getValue(query, "port");
        String uploaded = getValue(query, "uploaded");
        String downloaded = getValue(query, "downloaded");
        String left = getValue(query, "left");
        String compact = getValue(query, "compact");
        String event = getValue(query, "event");

        String ip = addr.getAddress().getHostAddress();
        String peerId = new String(decodePercent(_peerId), StandardCharsets.UTF_8);
        String infoHash = HexFormat.of().formatHex(decodePercent(_infoHash));

        // Given that we have never seen this session before, we should add it to our session map
        if (!sessions.containsKey(infoHash)) {
            sessions.put(infoHash, new HashSet<>());
        }
        // Create a key representing a user and its session, making it indexable for a singular session for a user
        PeerKey key = new PeerKey(infoHash, peerId);
        // Add this to the sessions map which says that this session has the participant of peerId
        sessions.get(infoHash).add(key);
        // A new request meaning it has not been in this session, if not it just overides what was provided
        session.put(key, new PeerInformation(ip, peerPort, left, Instant.now()));

        System.out.println(sessions);
        System.out.println(session);

        return new Request(Optional.empty(), Optional.of(Long.valueOf(10)), Optional.of(5), Optional.of(10));
    }

    private BValue createResponse(Request request) {

        var entries = new TreeMap<BBytes, BValue>();

        // Add peers compact BBytes field

        if (request.failure().isPresent())
            entries.put(BBytes.of("failure"), BBytes.of(request.failure.get()));
        if (request.interval.isPresent())
            entries.put(BBytes.of("interval"), new BInt(request.interval.get()));
        if (request.complete.isPresent())
            entries.put(BBytes.of("complete"), new BInt(request.complete.get()));
        if (request.incomplete.isPresent())
            entries.put(BBytes.of("incomplete"), new BInt(request.incomplete.get()));

        return new BDict(entries);
    }

    private String getValue(Map<String, String> query, String key) throws IllegalArgumentException {
        if (query.containsKey(key))
            return query.get(key);
        throw new IllegalArgumentException(String.format("Missing key %s and possibly more\n", key));
    }

    private Optional<Map<String, String>> params(URI uri) {
        Map<String, String> map = new HashMap<>();
        // /announce?x=1&y=2&z=3..
        String str = uri.toString();
        int idx = str.indexOf("?");

        if (idx == -1)
            return Optional.empty();

        String[] arr = str.substring(idx + 1).split("&");

        for (String kv : arr) {
            String[] x = kv.split("=");
            map.put(x[0], x[1]);
        }

        return Optional.of(map);
    }

    public void start() {
        server.start();
    }
}
