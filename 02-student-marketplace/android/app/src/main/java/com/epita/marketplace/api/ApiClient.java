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
 * For the emulator use 10.0.2.2 to reach the host machine's localhost.
 * Change BASE_URL if running on a physical device on the same network.
 */
public class ApiClient {

    // 10.0.2.2 is the Android emulator alias for host localhost
    public static final String BASE_URL = "http://10.0.2.2:8000";

    /**
     * Perform a GET request and return the response body as a String.
     */
    public static String get(String path) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("HTTP " + code);
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();
        return sb.toString();
    }

    /**
     * Perform a POST request with a JSON body and return the response body.
     */
    public static String post(String path, String jsonBody) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.close();

        int code = conn.getResponseCode();
        if (code != 200 && code != 201) {
            throw new IOException("HTTP " + code);
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();
        return sb.toString();
    }
}