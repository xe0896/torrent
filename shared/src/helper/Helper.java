package helper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Helper {
    public static <T> void print(T... values) {
        for (T t : values) {
            System.out.println(t);
        }
    }

    public static byte[] hash(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(bytes);
        return hash;
    }
}
