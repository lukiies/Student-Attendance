package com.example.studentsattendance.api;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    
    // UWS API base URL
    private static final String BASE_URL = "http://178.62.230.186:5001/";
    
    // DO NOT cache the service - create fresh instance every time
    // This ensures completely new HTTP connections with no reuse
    public static UWSApiService getApiService() {
        // Always create fresh Retrofit and OkHttpClient to avoid connection reuse issues
        Retrofit retrofit = createRetrofit();
        return retrofit.create(UWSApiService.class);
    }
    
    private static Retrofit createRetrofit() {
        // Custom logging interceptor
        okhttp3.Interceptor loggingInterceptor = chain -> {
            okhttp3.Request request = chain.request();
            android.util.Log.d("ApiClient", "Request: " + request.method() + " " + request.url());
            
            long t1 = System.nanoTime();
            okhttp3.Response response = chain.proceed(request);
            long t2 = System.nanoTime();
            
            android.util.Log.d("ApiClient", String.format("Response: %d for %s (%.1fms)",
                response.code(), response.request().url(), (t2 - t1) / 1e6d));
            
            return response;
        };
        
        // Create empty connection pool with aggressive eviction
        ConnectionPool connectionPool = new ConnectionPool(0, 1, TimeUnit.NANOSECONDS);
        
        // Create fresh dispatcher with no queuing
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(1);
        dispatcher.setMaxRequestsPerHost(1);
        
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                okhttp3.Request original = chain.request();
                okhttp3.Request request = original.newBuilder()
                    .header("User-Agent", "StudentAttendance/1.0 Android")
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    // CRITICAL: Force HTTP/1.1 with Connection: close
                    // This tells both client and server NOT to keep connection alive
                    .header("Connection", "close")
                    // Additional header to prevent keep-alive
                    .header("Keep-Alive", "timeout=0, max=0")
                    .method(original.method(), original.body())
                    .build();
                return chain.proceed(request);
            })
            .addInterceptor(loggingInterceptor)
            // Network interceptor to close connections after use
            .addNetworkInterceptor(chain -> {
                okhttp3.Response response = chain.proceed(chain.request());
                // Log connection info
                android.util.Log.d("ApiClient", "Connection: " + 
                    (response.request().header("Connection") != null ? 
                    response.request().header("Connection") : "none"));
                return response;
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)  // Changed to false - don't retry on stale connections
            // Force HTTP/1.1 only (no HTTP/2 which has more aggressive connection pooling)
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            // Empty connection pool - immediate eviction
            .connectionPool(connectionPool)
            // Fresh dispatcher for each client
            .dispatcher(dispatcher)
            .build();
        
        return new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            // ScalarsConverterFactory FIRST - reads response as String before Gson tries to parse
            // This prevents "unexpected end of stream" with very short responses
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    }
}
