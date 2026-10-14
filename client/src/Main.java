import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import bencode.BDecoder;
import bencode.structure.BBytes;
import bencode.structure.BDict;
import bencode.structure.BInt;
import bencode.structure.BList;
import bencode.structure.BValue;
import entities.PeerConnection;
import entities.PeerConnection.PeerAddress;
import helper.Helper;
import messages.Requesters;
import messages.Responders;
import request.Request;

public class Main {
    private static final String URL = "http://localhost:8080/"; // Tracker URL 
    private static final String version = "0001"; // Version for peer ID's

    // Representation of the .torrent file that has all been validated
    record MetaInfo(String name, long pieceLength, byte[] pieces, long length) {
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException {
        // src/ because we are in Makefile level
        Path mini = Path.of("src/test-torrents/single.torrent");
        // Byte representation of the torrent file
        byte[] data = Files.readAllBytes(mini);

        // Encode the .torrent representation into bencode, allowing us to parse/hash
        BValue root = new BDecoder(data).parseValue();

        // A bencode first structure is always going to be a dictionary
        if (!(root instanceof BDict dict)) {
            throw new IllegalArgumentException("Torrent root must be a dict");
        }

        // Peer id of the current user, in terms of bytes when it can be decoded to a String, we care
        // about the bytes so then we can percent encode it when we put it into a URL to our tracker URL

        byte[] peerIdBytes = PeerConnection.createPeerId(version);

        BDict info = getDict(dict, "info");

        // Creates an ephemeral port which is gonna be used as this users port, the address for a local
        // build would be the same, just the port would differ
        ServerSocket listener = new ServerSocket(0);
        InetAddress addr = listener.getInetAddress();
        int port = listener.getLocalPort();

        String name = getString(info, "name");
        long pieceLength = getLong(info, "piece length");
        byte[] pieces = getBytes(info, "pieces");
        long length = getLong(info, "length");

        MetaInfo torrent = new MetaInfo(name, pieceLength, pieces, length);

        // torrent.pieces() is the SHA-1 pieces that are 20 long, an individual piece is 20 long 
        // so we divide by 20 to get the pieces and make that our bitfield length
        int n = torrent.pieces().length / 20; // Have to pass since BitSet won't store it
        PeerConnection peerConnection = new PeerConnection(new PeerConnection.PeerAddress(addr, port), new BitSet(n), n,
                peerIdBytes);

        // A listener thread that is the first endpoint between users that would create other threads
        // where that thread would handle the handshake and communication between that user
        Thread ListenerThread = new Thread(new ListenerThread(listener, info.hash(), peerConnection));
        ListenerThread.start();

        // Grabs the other peers IP and port, as well as append itself into the connection pool
        // for this info_hash stored in 'dict'
        List<InetSocketAddress> connections = announce(dict, addr, port, peerIdBytes);

        // The ip + port from the tracker URL we can communicate it with (testing)
        if (connections.size() > 0) {
            InetSocketAddress otherAddr = connections.get(0);
            Socket otherSocket = new Socket(otherAddr.getAddress(), otherAddr.getPort());
            Thread ConnectionThread = new Thread(new ConnectionThread(otherSocket, info.hash(), peerConnection));
            ConnectionThread.start();
        } else {
            peerConnection.has.set(0, peerConnection.pieceCount);
            System.out.println(peerConnection.has);
        }
    }

    public static void performHandshake(DataOutputStream out, DataInputStream in, byte[] infoHash, byte[] peerId)
            throws IOException, IllegalArgumentException {
        int count;

        String protocol = "BitTorrent protocol";
        out.write(protocol.length());
        out.write(protocol.getBytes(StandardCharsets.UTF_8));
        out.write(new byte[8]); // Reserved 8 bytes, makes us to add more protocols without breaking
        out.write(infoHash);
        out.write(peerId);

        // readInt is blocking, so it waits until a handshake is recieved
        count = in.read();
        String otherProtocol = new String(in.readNBytes(count), StandardCharsets.UTF_8);
        in.readNBytes(8); // Reserved
        byte[] otherInfoHash = in.readNBytes(20);
        String otherPeerId = new String(in.readNBytes(20), StandardCharsets.UTF_8);
        if (!Arrays.equals(infoHash, otherInfoHash))
            throw new IllegalArgumentException(
                    "Peer connection doesn't have the same info hash for peer: " + otherPeerId);

        if (!otherProtocol.equals("BitTorrent protocol"))
            throw new IllegalArgumentException(
                    "Peer connection isn't using the BitTorrent protocol: " + otherPeerId + " it is using "
                            + protocol);

        System.out.println(String.format("Established a connection between %s and %s",
                new String(peerId, StandardCharsets.UTF_8), otherPeerId));
    }

    static public class ConnectionThread implements Runnable {
        private final Socket socket;
        private final byte[] infoHash;
        private final PeerConnection peer;

        ConnectionThread(Socket socket, byte[] infoHash, PeerConnection peer) {
            this.socket = socket;
            this.infoHash = infoHash;
            this.peer = peer;
        }

        @Override
        public void run() {
            try (socket) { // Closes for us
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                // A buffer for a message, we need some count to allow us to know
                // when a message ends, the buffer would overwrite data depending
                // on the 'count' prefix
                int count;

                // A requester would need access to the 'out' stream and write whatever they want, whereas
                // when responding we can parse the message first then send meaning 'in' is not needed for it
                // however, once we are going through a response we likely would need to do one ourselves so
                // we give it access to the Requester
                Requesters req = new Requesters(out);
                Responders res = new Responders(req);

                // Handshake
                // 19 | 'BitTorrent Protocol' | 8 bytes | info_hash | peer_id
                if (!socket.isClosed()) {
                    // Waits for a response in the function as this is what establishes the torrent
                    // connection, for other messages such as the bit mask one below, that is going
                    // to be handled in the dispatch loop
                    performHandshake(out, in, infoHash, peer.peerId); // Throws if any mismatches
                    req.sendBitMask(peer);
                }

                while (!socket.isClosed()) {
                    count = in.readInt();
                    int id = in.read();
                    // Throws if disconnected mid way, more ideal than readNBytes() which
                    // won't throw and -1 because we already read 'id'
                    byte[] message = new byte[count - 1];
                    in.readFully(message);

                    // Depending on the message ID received, pass to the appopriate handler
                    switch (id) {
                        case 0 -> res.handleChoke(message, peer);
                        case 1 -> res.handleUnchoke(message, peer);
                        case 2 -> res.handleInterested(message, peer);
                        case 3 -> res.handleNotInterested(message, peer);
                        case 4 -> res.handleHave(message, peer);
                        case 5 -> res.handleBitMask(message, peer);
                        case 6 -> res.handleRequest(message, peer);
                        case 7 -> res.handlePiece(message, peer);
                        case 8 -> res.handleCancel(message, peer);
                        default -> throw new IllegalArgumentException("Received an unrecognisable id: " + id);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Peer connection failed: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

        }
    }

    static public class ListenerThread implements Runnable {
        private final ServerSocket listener;
        private final byte[] infoHash;
        private final PeerConnection peer;

        public ListenerThread(ServerSocket listener, byte[] infoHash, PeerConnection peer) {
            this.listener = listener;
            this.infoHash = infoHash;
            this.peer = peer;
        }

        @Override
        public void run() {
            try {
                while (!listener.isClosed()) {
                    Socket otherPeer = listener.accept();

                    // Call another reader thread to receive the information
                    // and utilise it

                    ConnectionThread conn = new ConnectionThread(otherPeer, infoHash, peer);
                    Thread connThread = new Thread(conn);
                    connThread.start();
                }

                listener.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<InetSocketAddress> announce(BDict dict, InetAddress addr, int port, byte[] peerId)
            throws IOException, NoSuchAlgorithmException, InterruptedException {
        // 'root' is instance of BDict, "announce" field from the root
        // is the tracker URL

        // "info" field is a BDict that contains: "name", "piece_length", "pieces"
        // also "length" which is for single-field torrents

        // Since 'root' is an instance of BDict we can use the SortedMap<BBytes, BValue>
        // we provide the string to get the BValue which for the "announce" would just
        // be a straight BByte whereas the 'info' would point elsewhere

        String percentEncodedPeerId = Request.percentEncode(peerId);
        BDict info = getDict(dict, "info");
        PeerAddress p = PeerConnection.createAddress(addr, port);

        String infoHash = Request.percentEncode(info.hash());

        long length = getLong(info, "length");

        String queries = Request.queries(Map.of(
                "info_hash", infoHash,
                "peer_id", percentEncodedPeerId,
                "port", String.valueOf(p.port()),
                "uploaded", "0",
                "downloaded", "0",
                "left", String.valueOf(length),
                "compact", "1",
                "event", "started"));

        BValue responseValue = new BDecoder(request("announce", queries, BodyHandlers.ofByteArray())).parseValue();

        if (!(responseValue instanceof BDict response)) {
            throw new IllegalArgumentException("Torrent root must be a dict");
        }

        byte[] peers = getBytes(response, "peers");

        List<InetSocketAddress> sockets = new ArrayList<>();

        for (int i = 0; i < peers.length; i += 6) {
            byte[] ip = Arrays.copyOfRange(peers, i, i + 4);
            InetAddress _addr = InetAddress.getByAddress(ip);

            byte high = peers[i + 4];
            byte low = peers[i + 5];
            int _port = ((high & 0xFF) << 8) | (low & 0xFF);

            sockets.add(new InetSocketAddress(_addr, _port));
        }

        return sockets;
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

    public static List<BValue> getList(BDict dict, String key) {
        Optional<BValue> _bValue = dict.get(key);
        if (_bValue.isEmpty())
            throw new IllegalArgumentException(String.format("Must contain '%s' byte field", key));
        if (_bValue.get() instanceof BList bList)
            return bList.values();
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