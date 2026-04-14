package com.epita.marketplace.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Minimal HTTP helper for communicating with the FastAPI backend.
 *
 * For the emulator, 10.0.2.2 maps to the host machine's localhost.
 * Change BASE_URL if running on a physical device on the same network.
 */
public class ApiClient {

    /** Android emulator alias for host localhost. */
    public static final String BASE_URL = "http://10.0.2.2:5000";

    /** Perform a GET request and return the response body as a String. */
    public static String get(String path) throws IOException {
        HttpURLConnection conn = openConnection(path, "GET");
        checkResponseCode(conn);
        return readBody(conn);
    }

    /** Perform a POST request with a JSON body and return the response body. */
    public static String post(String path, String jsonBody) throws IOException {
        HttpURLConnection conn = openConnection(path, "POST");
        writeBody(conn, jsonBody);
        checkResponseCode(conn);
        return readBody(conn);
    }

    /** Perform a PATCH request with a JSON body and return the response body. */
    public static String patch(String path, String jsonBody) throws IOException {
        HttpURLConnection conn = openConnection(path, "PATCH");
        writeBody(conn, jsonBody);
        checkResponseCode(conn);
        return readBody(conn);
    }

    // ---- Private helpers ----

    private static HttpURLConnection openConnection(String path, String method) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return conn;
    }

    private static void writeBody(HttpURLConnection conn, String jsonBody) throws IOException {
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }
    }

    private static void checkResponseCode(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code);
        }
    }

    private static String readBody(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
