package messages;

import java.util.BitSet;

import entities.PeerConnection;

public class Responsers {
    public void handleBitMaskResponse(byte[] message, PeerConnection peer) {
        BitSet theres = new BitSet(peer.pieceCount);

        for (int i = 0; i < peer.pieceCount; i++) {
            int k = i / 8;
            int idx = 7 - (i % 8);

            if ((message[k] & 1 << idx) != 0)
                theres.set(i);
        }

        BitSet ours = peer.has;

        // {1, 1, 1, 1}
        // {0, 0, 0, 0}
        // Output {}

        // {0, 0, 0, 0}
        // {1, 1, 1, 1}
        // Output {1, 1, 1, 1}

        // P = ~A AND B would establish this relationship as
        // the only assertion is 0 and 1 respectively which is what
        // we need when we want to find something that we dont have

        ours.andNot(theres);

        System.out.println(ours);
    }

    public void handleChokeResponse(byte[] message, PeerConnection peer) {
    }

    public void handleUnchokeResponse(byte[] message, PeerConnection peer) {
    }

    public void handleInterestedResponse(byte[] message, PeerConnection peer) {
    }

    public void handleNotInterestedResponse(byte[] message, PeerConnection peer) {
    }

    public void handleHaveResponse(byte[] message, PeerConnection peer) {
    }

    public void handleRequestResponse(byte[] message, PeerConnection peer) {
    }

    public void handlePieceResponse(byte[] message, PeerConnection peer) {
    }

    public void handleCancelResponse(byte[] message, PeerConnection peer) {
    }

}
