package messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;

import entities.PeerConnection;

public class Requesters {
    private static final int CHOKE = 0;
    private static final int UNCHOKE = 1;
    private static final int INTERESTED = 2;
    private static final int NOT_INTERESTED = 3;
    private static final int HAVE = 4;
    private static final int BITFIELD = 5;
    private static final int REQUEST = 6;
    private static final int PIECE = 7;
    private static final int CANCEL = 8;

    private final DataOutputStream out;

    public Requesters(DataOutputStream out) {
        this.out = out;
    }

    public void sendMessage(int len, int id, byte[] payload) {
        int byteId = (byte) id;
        try {
            out.writeInt(len);
            out.write(byteId);
            if (payload != null)
                out.write(payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendBitMask(PeerConnection peer)
            throws IOException {
        BitSet bitMask = peer.has;

        byte[] bitfield = new byte[(peer.pieceCount + 7) / 8];

        for (int i = bitMask.nextSetBit(0); i >= 0 && i < peer.pieceCount; i = bitMask.nextSetBit(i + 1)) {
            bitfield[i / 8] |= 1 << (7 - (i % 8));
        }

        // 1 + for the id
        sendMessage(1 + bitfield.length, (byte) 5, bitfield);
    }

    public void sendInterested() {
        sendMessage(1, INTERESTED, null);
    }
}