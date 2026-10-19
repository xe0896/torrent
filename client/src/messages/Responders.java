package messages;

import java.io.DataInputStream;
import java.util.BitSet;
import java.util.Optional;

import entities.PeerConnection;
import entities.PeerConnection.Status;

public class Responders {
    private final Requesters req;
    private final static int MAX = 4;

    public Responders(Requesters req) {
        this.req = req;
    }

    public void handleBitMask(byte[] message, PeerConnection peer) {
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

        BitSet wanted = (BitSet) ours.clone();
        wanted.andNot(theres);

        // If 'wanted' is not empty then there are some things we can request
        // we would initially send an interested message and that is it, then wait
        // for an unchoke which then means we can get precise on what we want

        if (wanted.isEmpty())
            req.sendInterested();
    }

    public void handleChoke(byte[] message, PeerConnection peer) {
        throw new UnsupportedOperationException();
    }

    public void handleUnchoke(byte[] message, PeerConnection peer) {
        throw new UnsupportedOperationException();
    }

    public void handleInterested(byte[] message, PeerConnection peer, String otherPeerId) {
        System.out.println(String.format("'%s' is interested", otherPeerId));

        Status status = peer.getStatus(otherPeerId).orElseThrow(
                () -> new IllegalArgumentException("Provided peerID is not in the status map, should not be possible"));

        if (peer.unchoked.size() <= MAX) {
            // This means that there is space for this interested user
            // meaning we can add it to the unchoked set and make it unchoked
            // and start be sending this peers data to it

            peer.unchoked.add(otherPeerId);
        }

        status.peerInterested = true;
    }

    public void handleNotInterested(byte[] message, PeerConnection peer) {
        throw new UnsupportedOperationException();
    }

    public void handleHave(byte[] message, PeerConnection peer) {
        throw new UnsupportedOperationException();
    }

    public void handleRequest(byte[] message, PeerConnection peer) {
        throw new UnsupportedOperationException();
    }

    public void handlePiece(byte[] message, PeerConnection peer) {
        throw new UnsupportedOperationException();
    }

    public void handleCancel(byte[] message, PeerConnection peer) {
        throw new UnsupportedOperationException();
    }

}
