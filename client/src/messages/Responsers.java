package messages;

import java.util.BitSet;

import entities.PeerConnection;

public class Responsers {
    public void handleBitMaskResponse(byte[] message, PeerConnection peer) {
        BitSet has = new BitSet(peer.pieceCount);

        for (int i = 0; i < peer.pieceCount; i++) {
            int k = i / 8;
            int idx = 7 - (i % 8);

            if ((message[k] & 1 << idx) != 0)
                has.set(i);
        }

        System.out.println(has);
    }

    public void handleChokeResponse(byte[] message, PeerConnection peer) {
    }

}
