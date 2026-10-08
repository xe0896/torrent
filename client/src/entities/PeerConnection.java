package entities;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import security.Random;

public class PeerConnection {
    public record PeerAddress(InetAddress ip, int port) {
    }

    PeerAddress address;
    byte[] peerId;
    BitSet has;
    boolean amChoking = true;
    boolean amInterested = false;
    boolean peerChoking = true;
    boolean peerInterested = false;

    PeerConnection(PeerAddress address) {
        this.address = address;
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
