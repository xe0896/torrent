package entities;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import security.Random;

public class PeerConnection {
    public record PeerAddress(InetAddress ip, int port) {
    }

    class Status {
        public boolean amChoking = true;
        public boolean amInterested = false;
        public boolean peerChoking = true;
        public boolean peerInterested = false;
    }

    public PeerAddress address;
    public byte[] peerId;
    public BitSet has;
    public int pieceCount;
    public Map<String, Status> statusMap = new HashMap<>();

    public PeerConnection(PeerAddress address, BitSet has, int pieceCount, byte[] peerId) {
        this.address = address;
        this.has = has;
        this.peerId = peerId;
        this.pieceCount = pieceCount;
    }

    public void newPeer(String peerId) {
        if (statusMap.containsKey(peerId))
            throw new IllegalArgumentException(String.format("User %s already is in the status map", peerId));

        statusMap.put(peerId, new Status());
    }

    public void removePeer(String peerId) {
        statusMap.remove(peerId);
    }

    public static byte[] createPeerId(String version) {
        // -JT0001-(12 random characters) = 20 characters, JT = JitTorrent; 0001 = version
        String random = Random.randomString(12);
        // UTF_8 is safe here since the random pool is just ASCII characters so there is no
        // case where a singular character takes more then one byte
        return String.format("-JT%s-%s", version, random).getBytes(StandardCharsets.UTF_8);
    }

    public static PeerAddress createAddress(InetAddress ip, int port) {
        return new PeerAddress(ip, port);
    }

}
