package server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Tracker t = new Tracker(8080);
        t.start();
    }
}
