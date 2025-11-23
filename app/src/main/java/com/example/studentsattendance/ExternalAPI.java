package com.example.studentsattendance;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExternalAPI {
    private static final String BASE_URL = "http://10.0.2.2:5001";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public interface RegistrationCallback {
        void onProgress(String message);
        void onSuccess(String message);
        void onError(String message);
    }
    
    public static void registerAttendance(String bannerId, String email, String password, RegistrationCallback callback) {
        executor.execute(() -> {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            HttpURLConnection conn = null;
            
            try {
                // Disable connection pooling system-wide
                System.setProperty("http.keepAlive", "false");
                System.setProperty("http.maxConnections", "1");
                
                mainHandler.post(() -> callback.onProgress("Connecting to UWS server..."));
                
                // Add timestamp to URL to prevent any caching
                URL url = new URL(BASE_URL + "/login.then.register?t=" + System.currentTimeMillis());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Connection", "close");
                conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                conn.setRequestProperty("Pragma", "no-cache");
                conn.setRequestProperty("Expires", "0");
                conn.setUseCaches(false);
                conn.setDefaultUseCaches(false);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String currentTime = sdf.format(new Date());
                
                JSONObject json = new JSONObject();
                json.put("banner_id", bannerId);
                json.put("email", email);
                json.put("password", password);
                json.put("dt", currentTime);
                
                mainHandler.post(() -> callback.onProgress("Authenticating..."));
                
                // Write request body
                byte[] outputBytes = json.toString().getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(outputBytes.length);
                
                OutputStream os = conn.getOutputStream();
                os.write(outputBytes);
                os.flush();
                os.close();
                
                int responseCode = conn.getResponseCode();
                mainHandler.post(() -> callback.onProgress("Processing response..."));
                
                // Read response
                BufferedReader br = null;
                try {
                    if (responseCode >= 200 && responseCode < 300) {
                        br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    } else {
                        br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    }
                    
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    int result = jsonResponse.optInt("result", -1);
                    
                    if (result == 0) {
                        String message = jsonResponse.optString("message", "Registration successful");
                        mainHandler.post(() -> callback.onSuccess(message));
                    } else {
                        String errorMsg = jsonResponse.optString("error_message", "Registration failed");
                        mainHandler.post(() -> callback.onError(errorMsg));
                    }
                } finally {
                    if (br != null) {
                        try { br.close(); } catch (Exception ignored) {}
                    }
                }
                
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Connection error: " + e.getMessage()));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }
}
