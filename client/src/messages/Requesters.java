package messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;

import entities.PeerConnection;

public class Requesters {
    public void sendMessage(DataOutputStream out, int len, byte id, byte[] payload) throws IOException {
        out.writeInt(len);
        out.write(id);
        out.write(payload);
    }

    public void sendBitMask(PeerConnection peer, DataOutputStream out, DataInputStream in)
            throws IOException {
        BitSet bitMask = peer.has;

        byte[] bitfield = new byte[(peer.pieceCount + 7) / 8];

        for (int i = bitMask.nextSetBit(0); i >= 0 && i < peer.pieceCount; i = bitMask.nextSetBit(i + 1)) {
            bitfield[i / 8] |= 1 << (7 - (i % 8));
        }

        // 1 + for the id
        sendMessage(out, 1 + bitfield.length, (byte) 5, bitfield);
    }
}