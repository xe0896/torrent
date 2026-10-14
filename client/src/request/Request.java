package request;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Request {
    public static <T> T fetchJsonAt(String url, TypeReference<T> type) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.body(), type);
    }

    public static <T> T fetchAt(String url, BodyHandler<T> handler) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<T> response = client.send(request, handler);
        return response.body();
    }

    public static String queries(Map<String, String> qMap) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (var e : qMap.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();

            sb.append(key);
            sb.append('=');
            sb.append(val);

            if (i != qMap.size() - 1)
                sb.append("&");
            i++;
        }

        return sb.toString();
    }

    // Percent-encodes raw bytes for use in a URL query value.
    // Each byte becomes '%' + two uppercase hex digits, e.g. 0xAB -> "%AB".
    // The '%' marks "this is an encoded byte", so a bare "ab" still means the
    // two literal characters 'a','b'. Used for info_hash and peer_id.
    // Takes raw bytes, never a String, so binary values aren't corrupted
    public static String percentEncode(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        // %BA -> byte 0xBA
        // BA -> literal characters 'B' and 'A'

        // %02X outputs a byte as two hex digits, then we need to prepend a double '%' to get
        // an actual percentage sign in the string

        for (byte b : bytes) {
            sb.append(String.format("%%%02X", b & 0xFF)); // 0xFF to make it unsigned
        }

        return sb.toString();
    }
}