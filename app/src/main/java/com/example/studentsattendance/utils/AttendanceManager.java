package com.example.studentsattendance.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.example.studentsattendance.api.ApiClient;
import com.example.studentsattendance.api.AttendanceRegistrationRequest;
import com.example.studentsattendance.api.AttendanceResponse;
import com.example.studentsattendance.api.LoginRequest;
import com.example.studentsattendance.api.LoginResponse;
import com.example.studentsattendance.api.UWSApiService;
import com.example.studentsattendance.database.dao.AttendanceLogDao;
import com.example.studentsattendance.database.dao.LocationDao;
import com.example.studentsattendance.database.models.AttendanceLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceManager {
    
    // Toggle between old (login then register) and new (combined) approach
    private static final boolean USE_LOGIN_REGISTER_COMBINED_FUNC = true;
    
    private Context context;
    private AttendanceLogDao attendanceLogDao;
    private LocationDao locationDao;
    // DO NOT cache apiService - get fresh instance for each request to avoid connection reuse
    private SessionManager sessionManager;
    
    public interface AttendanceCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
    }
    
    public AttendanceManager(Context context) {
        this.context = context;
        this.attendanceLogDao = new AttendanceLogDao(context);
        this.locationDao = new LocationDao(context);
        // Removed: this.apiService = ApiClient.getApiService();
        // Now we get fresh service for each API call
        this.sessionManager = new SessionManager(context);
    }
    
    public void registerAttendance(long userId, String email, String studentId, String password,
                                   double latitude, double longitude, 
                                   boolean isManual, AttendanceCallback callback) {
        
        android.util.Log.d("AttendanceManager", "=== ATTENDANCE REGISTRATION STARTED ===");
        android.util.Log.d("AttendanceManager", "Student ID: " + studentId);
        android.util.Log.d("AttendanceManager", "Email: " + email);
        
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        
        // Check if already registered for today
        if (attendanceLogDao.isAttendanceRegisteredForDate(userId, today)) {
            android.util.Log.w("AttendanceManager", "Already registered for today");
            callback.onError("You have already registered attendance for today");
            return;
        }
        
        // Check internet connection
        android.util.Log.d("AttendanceManager", "Step 1: Checking internet connection...");
        if (!isNetworkAvailable()) {
            android.util.Log.e("AttendanceManager", "No internet connection available");
            handleNoInternet(userId, today, currentTime, callback);
            return;
        }
        android.util.Log.d("AttendanceManager", "✓ Internet connection available");
        
        // Choose between combined endpoint or separate login+register
        if (USE_LOGIN_REGISTER_COMBINED_FUNC) {
            android.util.Log.d("AttendanceManager", "Step 2: Using combined login.then.register endpoint...");
            useCombinedEndpoint(userId, email, studentId, password, today, currentTime, isManual, callback);
        } else {
            android.util.Log.d("AttendanceManager", "Step 2: Using separate login then register...");
            authenticateAndRegister(userId, email, studentId, password, today, currentTime, isManual, callback);
        }
    }
    
    private void useCombinedEndpoint(long userId, String email, String studentId, String password,
                                     String date, String time, boolean isManual, 
                                     AttendanceCallback callback) {
        android.util.Log.d("AttendanceManager", "Step 3: Calling combined login.then.register endpoint...");
        
        // Create ISO 8601 timestamp for current time
        SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentTimestamp = iso8601Format.format(new Date());
        
        android.util.Log.d("AttendanceManager", "Banner ID: " + studentId);
        android.util.Log.d("AttendanceManager", "Datetime: " + currentTimestamp);
        
        com.example.studentsattendance.api.CombinedRequest request = 
            new com.example.studentsattendance.api.CombinedRequest(studentId, email, password, currentTimestamp);
        
        // Get fresh API service for every request to avoid connection reuse
        UWSApiService apiService = ApiClient.getApiService();
        Call<com.example.studentsattendance.api.CombinedResponse> call = apiService.loginThenRegister(request);
        call.enqueue(new Callback<com.example.studentsattendance.api.CombinedResponse>() {
            @Override
            public void onResponse(Call<com.example.studentsattendance.api.CombinedResponse> call, 
                                   Response<com.example.studentsattendance.api.CombinedResponse> response) {
                try {
                    android.util.Log.d("AttendanceManager", "✓ Combined response received. Status: " + response.code());
                    android.util.Log.d("AttendanceManager", "Response message: " + response.message());
                    android.util.Log.d("AttendanceManager", "Response successful: " + response.isSuccessful());
                    
                    if (response.isSuccessful()) {
                        com.example.studentsattendance.api.CombinedResponse combinedResponse = response.body();
                        
                        if (combinedResponse == null) {
                            android.util.Log.e("AttendanceManager", "Response body is null despite 200 OK");
                            handleApiError(userId, date, time, isManual, "Empty response from server");
                            callback.onError("Server returned empty response. Please try again.");
                            return;
                        }
                        
                        android.util.Log.d("AttendanceManager", "Result code: " + combinedResponse.getResult());
                        android.util.Log.d("AttendanceManager", "Token received: " + (combinedResponse.getToken() != null ? "Yes" : "No"));
                        
                        if (combinedResponse.getResult() == 0) {
                            // Success! Both login and registration completed
                            if (combinedResponse.getToken() != null) {
                                sessionManager.saveToken(combinedResponse.getToken());
                            }
                            
                            android.util.Log.d("AttendanceManager", "✓ Login and attendance registered successfully!");
                            android.util.Log.d("AttendanceManager", "Message: " + combinedResponse.getMessage());
                            
                            handleSuccessfulRegistration(userId, date, time, 
                                "Attendance registered successfully", isManual);
                            callback.onSuccess("Attendance registered successfully!");
                            
                        } else {
                            // Error occurred
                            String errorMsg = combinedResponse.getErrorMessage() != null ? 
                                combinedResponse.getErrorMessage() : "Unknown error";
                            
                            android.util.Log.e("AttendanceManager", "Combined request failed: " + errorMsg);
                            
                            // Save token if provided even on error
                            if (combinedResponse.getToken() != null) {
                                sessionManager.saveToken(combinedResponse.getToken());
                                android.util.Log.d("AttendanceManager", "Token saved despite error");
                            }
                            
                            handleFailedRegistration(userId, date, time, errorMsg, isManual);
                            callback.onError(errorMsg);
                        }
                    } else {
                        android.util.Log.e("AttendanceManager", "HTTP error: " + response.code());
                        handleApiError(userId, date, time, isManual, "HTTP " + response.code());
                        callback.onError("Server error: " + response.code());
                    }
                } catch (Exception e) {
                    android.util.Log.e("AttendanceManager", "✗ Combined request failed", e);
                    android.util.Log.e("AttendanceManager", "Error type: " + e.getClass().getSimpleName());
                    android.util.Log.e("AttendanceManager", "Error message: " + e.getMessage());
                    
                    handleApiError(userId, date, time, isManual, e.getMessage());
                    callback.onError("Error: " + e.getMessage());
                }
            }
            
            @Override
            public void onFailure(Call<com.example.studentsattendance.api.CombinedResponse> call, Throwable t) {
                android.util.Log.e("AttendanceManager", "✗ Combined request failed completely");
                android.util.Log.e("AttendanceManager", "Error: " + t.getMessage(), t);
                
                handleApiError(userId, date, time, isManual, t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    private void authenticateAndRegister(long userId, String email, String studentId, String password,
                                        String date, String time, boolean isManual, 
                                        AttendanceCallback callback) {
        android.util.Log.d("AttendanceManager", "Step 3: Authenticating with API server...");
        
        // Get fresh API service for every request to avoid connection reuse
        UWSApiService apiService = ApiClient.getApiService();
        android.util.Log.d("AttendanceManager", "Sending login request to: " + apiService.getClass().getName());
        
        LoginRequest loginRequest = new LoginRequest(studentId, email, password);
        
        Call<LoginResponse> call = apiService.login(loginRequest);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                try {
                    android.util.Log.d("AttendanceManager", "✓ Login response received. Status: " + response.code());
                    android.util.Log.d("AttendanceManager", "Response message: " + response.message());
                    android.util.Log.d("AttendanceManager", "Response successful: " + response.isSuccessful());
                    
                    if (response.isSuccessful()) {
                        LoginResponse loginResponse = response.body();
                        
                        if (loginResponse == null) {
                            android.util.Log.e("AttendanceManager", "Response body is null despite 200 OK");
                            handleApiError(userId, date, time, isManual, "Empty response from server");
                            callback.onError("Server returned empty response. Please try again.");
                            return;
                        }
                        
                        android.util.Log.d("AttendanceManager", "Login result code: " + loginResponse.getResult());
                        android.util.Log.d("AttendanceManager", "Token received: " + (loginResponse.getToken() != null ? "Yes" : "No"));
                        
                        if (loginResponse.isSuccess() && loginResponse.getToken() != null) {
                            // Save token
                            sessionManager.saveToken(loginResponse.getToken());
                            android.util.Log.d("AttendanceManager", "✓ Authentication successful!");
                            
                            // Now attempt registration
                            attemptRegistration(loginResponse.getToken(), studentId, userId, 
                                date, time, isManual, email, password, callback);
                        } else {
                            android.util.Log.e("AttendanceManager", "Authentication failed: " + loginResponse.getErrorMessage());
                            handleFailedRegistration(userId, date, time, 
                                "Authentication failed: " + loginResponse.getErrorMessage(), isManual);
                            callback.onError("Authentication failed: " + loginResponse.getErrorMessage());
                        }
                    } else {
                        android.util.Log.e("AttendanceManager", "Login failed with HTTP " + response.code());
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                            android.util.Log.e("AttendanceManager", "Error body: " + errorBody);
                        } catch (Exception e) {
                            android.util.Log.e("AttendanceManager", "Could not read error body", e);
                        }
                        handleApiError(userId, date, time, isManual, 
                            "Login failed: " + response.code());
                        callback.onError("Login failed. Please try again.");
                    }
                } catch (Exception e) {
                    android.util.Log.e("AttendanceManager", "Exception in onResponse", e);
                    handleApiError(userId, date, time, isManual, "Error processing response: " + e.getMessage());
                    callback.onError("Error processing server response: " + e.getMessage());
                }
            }
            
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                android.util.Log.e("AttendanceManager", "✗ Login request failed", t);
                android.util.Log.e("AttendanceManager", "Error type: " + t.getClass().getSimpleName());
                android.util.Log.e("AttendanceManager", "Error message: " + t.getMessage());
                handleApiError(userId, date, time, isManual, 
                    "Login error: " + t.getMessage());
                callback.onError("Connection error: " + t.getMessage());
            }
        });
    }
    
    private void attemptRegistration(String token, String studentId, long userId, 
                                     String date, String time, boolean isManual,
                                     String email, String password, AttendanceCallback callback) {
        android.util.Log.d("AttendanceManager", "Step 4: Registering attendance with API...");
        
        // Create ISO 8601 datetime string
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String datetime = isoFormat.format(new Date());
        
        android.util.Log.d("AttendanceManager", "Banner ID: " + studentId);
        android.util.Log.d("AttendanceManager", "Datetime: " + datetime);
        
        AttendanceRegistrationRequest request = new AttendanceRegistrationRequest(studentId, datetime);
        
        // Get fresh API service for every request to avoid connection reuse
        UWSApiService apiService = ApiClient.getApiService();
        Call<AttendanceResponse> call = apiService.registerAttendance("Bearer " + token, request);
        call.enqueue(new Callback<AttendanceResponse>() {
            @Override
            public void onResponse(Call<AttendanceResponse> call, Response<AttendanceResponse> response) {
                try {
                    android.util.Log.d("AttendanceManager", "✓ Attendance response received. Status: " + response.code());
                    android.util.Log.d("AttendanceManager", "Response message: " + response.message());
                    android.util.Log.d("AttendanceManager", "Response successful: " + response.isSuccessful());
                    
                    if (response.isSuccessful()) {
                        AttendanceResponse apiResponse = response.body();
                        
                        if (apiResponse == null) {
                            android.util.Log.e("AttendanceManager", "Attendance response body is null despite 200 OK");
                            handleApiError(userId, date, time, isManual, "Empty response from server");
                            callback.onError("Server returned empty response. Please try again.");
                            return;
                        }
                        
                        android.util.Log.d("AttendanceManager", "Attendance result code: " + apiResponse.getResult());
                        
                        if (apiResponse.isSuccess()) {
                            android.util.Log.d("AttendanceManager", "✓ Attendance registered successfully!");
                            handleSuccessfulRegistration(userId, date, time, 
                                apiResponse.getMessage(), isManual);
                            callback.onSuccess("Attendance registered successfully!");
                        } else {
                            // Check if token expired (result code 3)
                            if (apiResponse.getResult() == 3 || apiResponse.getResult() == 2) {
                                android.util.Log.w("AttendanceManager", "Token expired, re-authenticating...");
                                // Token expired or invalid, re-authenticate
                                sessionManager.saveToken(null);
                                authenticateAndRegister(userId, email, studentId, password, 
                                    date, time, isManual, callback);
                            } else {
                                android.util.Log.e("AttendanceManager", "Attendance failed: " + apiResponse.getMessage());
                                handleFailedRegistration(userId, date, time, 
                                    apiResponse.getMessage(), isManual);
                                callback.onError(apiResponse.getMessage());
                            }
                        }
                    } else if (response.code() == 401 || response.code() == 403) {
                        android.util.Log.w("AttendanceManager", "Unauthorized response, re-authenticating...");
                        // Unauthorized - token expired
                        sessionManager.saveToken(null);
                        authenticateAndRegister(userId, email, studentId, password, 
                            date, time, isManual, callback);
                    } else {
                        android.util.Log.e("AttendanceManager", "Server error: " + response.code());
                        handleApiError(userId, date, time, isManual, 
                            "Server error: " + response.code());
                        callback.onError("Registration failed. Server error: " + response.code());
                    }
                } catch (Exception e) {
                    android.util.Log.e("AttendanceManager", "Exception in attendance onResponse", e);
                    handleApiError(userId, date, time, isManual, "Error processing response: " + e.getMessage());
                    callback.onError("Error processing server response: " + e.getMessage());
                }
            }
            
            @Override
            public void onFailure(Call<AttendanceResponse> call, Throwable t) {
                android.util.Log.e("AttendanceManager", "✗ Attendance request failed", t);
                android.util.Log.e("AttendanceManager", "Error type: " + t.getClass().getSimpleName());
                android.util.Log.e("AttendanceManager", "Error message: " + t.getMessage());
                handleApiError(userId, date, time, isManual, t.getMessage());
                callback.onError("Connection error: " + t.getMessage());
            }
        });
    }
    
    private void handleSuccessfulRegistration(long userId, String date, String time, 
                                             String message, boolean isManual) {
        android.util.Log.d("AttendanceManager", "");
        android.util.Log.d("AttendanceManager", "📝 SAVING SUCCESS TO DATABASE");
        android.util.Log.d("AttendanceManager", "  User ID: " + userId);
        android.util.Log.d("AttendanceManager", "  Date: " + date);
        android.util.Log.d("AttendanceManager", "  Time: " + time);
        android.util.Log.d("AttendanceManager", "  Status: SUCCESS");
        android.util.Log.d("AttendanceManager", "  Message: " + message);
        android.util.Log.d("AttendanceManager", "  Is Manual: " + isManual);
        
        // Create attendance log
        AttendanceLog log = new AttendanceLog(
            userId, date, time, "SUCCESS", message, isManual
        );
        long insertedId = attendanceLogDao.insertLog(log);
        
        android.util.Log.d("AttendanceManager", "✓ Log saved to database with ID: " + insertedId);
        
        // Mark all locations for this date as registered
        locationDao.markLocationsAsRegistered(userId, date);
        android.util.Log.d("AttendanceManager", "✓ Locations marked as registered");
    }
    
    private void handleFailedRegistration(long userId, String date, String time, 
                                         String message, boolean isManual) {
        android.util.Log.d("AttendanceManager", "");
        android.util.Log.d("AttendanceManager", "📝 SAVING FAILURE TO DATABASE");
        android.util.Log.d("AttendanceManager", "  Status: FAILED");
        android.util.Log.d("AttendanceManager", "  Message: " + message);
        
        AttendanceLog log = new AttendanceLog(
            userId, date, time, "FAILED", message, isManual
        );
        long insertedId = attendanceLogDao.insertLog(log);
        android.util.Log.d("AttendanceManager", "✓ Failure log saved with ID: " + insertedId);
    }
    
    private void handleApiError(long userId, String date, String time, 
                               boolean isManual, String errorMessage) {
        AttendanceLog log = new AttendanceLog(
            userId, date, time, "ERROR", 
            "API Error: " + errorMessage, isManual
        );
        attendanceLogDao.insertLog(log);
    }
    
    private void handleNoInternet(long userId, String date, String time, 
                                 AttendanceCallback callback) {
        AttendanceLog log = new AttendanceLog(
            userId, date, time, "PENDING", 
            "No internet connection. Will retry automatically.", false
        );
        attendanceLogDao.insertLog(log);
        
        callback.onError("No internet connection. Your attendance will be registered automatically when connection is available.");
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}
