package helper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Helper {
    public static void print(String... values) {
        for (String s : values) {
            System.out.println(s);
        }
    }

    public static byte[] hash(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(bytes);
        return hash;
    }
}
