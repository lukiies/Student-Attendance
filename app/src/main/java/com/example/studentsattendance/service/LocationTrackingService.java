package com.example.studentsattendance.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.studentsattendance.MainActivity;
import com.example.studentsattendance.R;
import com.example.studentsattendance.database.dao.AttendanceLogDao;
import com.example.studentsattendance.database.dao.LocationDao;
import com.example.studentsattendance.database.dao.UserDao;
import com.example.studentsattendance.database.dao.UserSettingsDao;
import com.example.studentsattendance.database.models.LocationRecord;
import com.example.studentsattendance.database.models.User;
import com.example.studentsattendance.utils.AttendanceManager;
import com.example.studentsattendance.utils.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationTrackingService extends Service {

    private static final String CHANNEL_ID = "LocationTrackingChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 5 * 60 * 1000; // 5 minutes
    private static final long FASTEST_UPDATE_INTERVAL = 2 * 60 * 1000; // 2 minutes
    
    // UWS Campus coordinates
    private static final double CAMPUS_LAT = 55.8440749;
    private static final double CAMPUS_LNG = -4.4303226;
    private static final double CAMPUS_RADIUS_METERS = 150;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationDao locationDao;
    private AttendanceLogDao attendanceLogDao;
    private UserSettingsDao settingsDao;
    private UserDao userDao;
    private SessionManager sessionManager;
    private AttendanceManager attendanceManager;
    private boolean isInsideCampus = false;
    private boolean hasNotifiedForCampusEntry = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        android.util.Log.d("LocationService", "═══════════════════════════════════════");
        android.util.Log.d("LocationService", "SERVICE CREATED - Location tracking service initialized");
        android.util.Log.d("LocationService", "═══════════════════════════════════════");
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationDao = new LocationDao(this);
        attendanceLogDao = new AttendanceLogDao(this);
        settingsDao = new UserSettingsDao(this);
        userDao = new UserDao(this);
        sessionManager = new SessionManager(this);
        attendanceManager = new AttendanceManager(this);
        
        createNotificationChannel();
        setupLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.util.Log.d("LocationService", "SERVICE STARTED - Starting foreground service");
        android.util.Log.d("LocationService", "Update interval: " + (LOCATION_UPDATE_INTERVAL / 1000) + " seconds");
        android.util.Log.d("LocationService", "Campus coordinates: " + CAMPUS_LAT + ", " + CAMPUS_LNG);
        android.util.Log.d("LocationService", "Campus radius: " + CAMPUS_RADIUS_METERS + " meters");
        android.util.Log.d("LocationService", "Simulate on campus: " + MainActivity.simulateLocationOnCampus);
        
        startForeground(NOTIFICATION_ID, createNotification("Tracking location..."));
        startLocationUpdates();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Tracks your location for attendance");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Student Attendance")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_id)
            .setContentIntent(pendingIntent)
            .build();
    }

    private void setupLocationCallback() {
        android.util.Log.d("LocationService", "Setting up location callback listener");
        
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    android.util.Log.w("LocationService", "⚠ Location result is NULL");
                    return;
                }

                android.util.Log.d("LocationService", "📍 Location update received - " + locationResult.getLocations().size() + " location(s)");
                for (Location location : locationResult.getLocations()) {
                    handleLocationUpdate(location);
                }
            }
        };
    }

    private void startLocationUpdates() {
        android.util.Log.d("LocationService", "→ Checking location permissions...");
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("LocationService", "✗ Location permission NOT GRANTED - stopping service");
            stopSelf();
            return;
        }
        
        android.util.Log.d("LocationService", "✓ Location permission granted");
        android.util.Log.d("LocationService", "→ Requesting location updates from FusedLocationProvider");

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .build();

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        );
        
        android.util.Log.d("LocationService", "✓ Location updates REQUESTED successfully");
        android.util.Log.d("LocationService", "Waiting for location updates every " + (LOCATION_UPDATE_INTERVAL / 1000) + " seconds...");
    }

    private void handleLocationUpdate(Location location) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        
        android.util.Log.d("LocationService", "");
        android.util.Log.d("LocationService", "═══════════════ LOCATION UPDATE [" + timestamp + "] ═══════════════");
        android.util.Log.d("LocationService", "Latitude:  " + location.getLatitude());
        android.util.Log.d("LocationService", "Longitude: " + location.getLongitude());
        android.util.Log.d("LocationService", "Accuracy:  " + location.getAccuracy() + " meters");
        
        boolean wasInsideCampus = isInsideCampus;
        isInsideCampus = isLocationInsideCampus(location);
        
        android.util.Log.d("LocationService", "Was inside campus: " + wasInsideCampus);
        android.util.Log.d("LocationService", "Is inside campus:  " + isInsideCampus);
        
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            android.util.Log.w("LocationService", "⚠ User NOT logged in - skipping location processing");
            return;
        }
        
        long userId = sessionManager.getUserId();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        android.util.Log.d("LocationService", "User ID: " + userId);
        android.util.Log.d("LocationService", "Date: " + today);
        
        // Save location if inside campus
        if (isInsideCampus) {
            android.util.Log.d("LocationService", "✓ INSIDE CAMPUS - Saving location to database");
            
            LocationRecord locationRecord = new LocationRecord(
                userId,
                location.getLatitude(),
                location.getLongitude(),
                today
            );
            long recordId = locationDao.insertLocation(locationRecord);
            android.util.Log.d("LocationService", "Location saved with ID: " + recordId);
            
            // Update notification
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, createNotification("You are on campus"));
            }
            
            // Check for automatic registration on EVERY location update (every 5 mins)
            // This ensures we keep checking even if the first attempt failed
            if (!attendanceLogDao.isAttendanceRegisteredForDate(userId, today)) {
                android.util.Log.d("LocationService", "");
                android.util.Log.d("LocationService", "🎯 ON CAMPUS & NOT REGISTERED - Checking registration status");
                handleCampusEntry(userId, today);
            } else {
                android.util.Log.d("LocationService", "Still on campus - already registered for today");
            }
        } else {
            android.util.Log.d("LocationService", "✗ OUTSIDE CAMPUS - No location saved");
            
            // Reset notification flag when outside campus
            if (wasInsideCampus) {
                android.util.Log.d("LocationService", "→ Left campus - resetting notification flag");
                hasNotifiedForCampusEntry = false;
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.notify(NOTIFICATION_ID, createNotification("Tracking location..."));
                }
            }
        }
        
        android.util.Log.d("LocationService", "═══════════════════════════════════════════════════════");
    }

    private boolean isLocationInsideCampus(Location location) {
        // Check if we should simulate being on campus
        if (MainActivity.simulateLocationOnCampus) {
            android.util.Log.d("LocationService", "→ Simulation mode: ENABLED - treating as ON CAMPUS");
            return true;
        }
        
        float[] distance = new float[1];
        Location.distanceBetween(
            location.getLatitude(),
            location.getLongitude(),
            CAMPUS_LAT,
            CAMPUS_LNG,
            distance
        );
        
        boolean insideCampus = distance[0] <= CAMPUS_RADIUS_METERS;
        android.util.Log.d("LocationService", "→ Distance to campus: " + String.format("%.2f", distance[0]) + " meters");
        android.util.Log.d("LocationService", "→ Inside campus? " + (insideCampus ? "YES" : "NO") + " (threshold: " + CAMPUS_RADIUS_METERS + "m)");
        
        return insideCampus;
    }

    private void handleCampusEntry(long userId, String today) {
        android.util.Log.d("LocationService", "");
        android.util.Log.d("LocationService", "▶▶▶ HANDLING CAMPUS ENTRY ◀◀◀");
        android.util.Log.d("LocationService", "Checking if attendance already registered for today...");
        
        // Check if already registered for today
        if (attendanceLogDao.isAttendanceRegisteredForDate(userId, today)) {
            android.util.Log.d("LocationService", "✓ Already registered for today - no action needed");
            return;
        }
        
        android.util.Log.d("LocationService", "✗ NOT registered yet for today");
        
        // Get manual registration setting - DEFAULT TO AUTOMATIC (false)
        String manualSetting = settingsDao.getSetting(userId, "manual_registration", "false");
        boolean isManualMode = "true".equals(manualSetting);
        
        android.util.Log.d("LocationService", "Registration mode: " + (isManualMode ? "MANUAL" : "AUTOMATIC"));
        
        if (isManualMode) {
            // Show notification to register manually
            android.util.Log.d("LocationService", "→ Showing MANUAL registration notification");
            showManualRegistrationNotification();
        } else {
            // Attempt automatic registration
            android.util.Log.d("LocationService", "→ Attempting AUTOMATIC registration via API");
            attemptAutomaticRegistration(userId, today);
        }
    }

    private void showManualRegistrationNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("SHOW_REGISTRATION_DIALOG", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UWS Campus Detected")
            .setContentText("You are on the Paisley UWS campus. Tap to register your attendance.")
            .setSmallIcon(R.drawable.ic_id)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(2, builder.build());
        }
    }

    private void attemptAutomaticRegistration(long userId, String today) {
        android.util.Log.d("LocationService", "");
        android.util.Log.d("LocationService", "┌─────────────────────────────────────────┐");
        android.util.Log.d("LocationService", "│ AUTOMATIC REGISTRATION ATTEMPT          │");
        android.util.Log.d("LocationService", "└─────────────────────────────────────────┘");
        
        User user = userDao.getUserById(userId);
        if (user == null) {
            android.util.Log.e("LocationService", "✗ User not found in database!");
            return;
        }
        
        android.util.Log.d("LocationService", "User found:");
        android.util.Log.d("LocationService", "  - Email: " + user.getEmail());
        android.util.Log.d("LocationService", "  - Student ID: " + user.getStudentId());
        
        // Show notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Automatic Registration")
            .setContentText("Attempting to register your attendance automatically...")
            .setSmallIcon(R.drawable.ic_id)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(3, builder.build());
        }
        
        // Get current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("LocationService", "✗ Location permission not granted!");
            return;
        }
        
        android.util.Log.d("LocationService", "→ Getting current location...");
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                android.util.Log.d("LocationService", "✓ Location obtained:");
                android.util.Log.d("LocationService", "  - Lat: " + location.getLatitude());
                android.util.Log.d("LocationService", "  - Lng: " + location.getLongitude());
                android.util.Log.d("LocationService", "");
                android.util.Log.d("LocationService", "🚀 CALLING AttendanceManager.registerAttendance()");
                android.util.Log.d("LocationService", "   This will trigger API call to external server");
                
                attendanceManager.registerAttendance(
                    userId,
                    user.getEmail(),
                    user.getStudentId(),
                    user.getPassword(),
                    location.getLatitude(),
                    location.getLongitude(),
                    false,  // isManual = false (automatic)
                    new AttendanceManager.AttendanceCallback() {
                        @Override
                        public void onSuccess(String message) {
                            android.util.Log.d("LocationService", "");
                            android.util.Log.d("LocationService", "✓✓✓ AUTOMATIC REGISTRATION SUCCESS ✓✓✓");
                            android.util.Log.d("LocationService", "Message: " + message);
                            showNotification("Success", message, 4);
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            android.util.Log.e("LocationService", "");
                            android.util.Log.e("LocationService", "✗✗✗ AUTOMATIC REGISTRATION FAILED ✗✗✗");
                            android.util.Log.e("LocationService", "Error: " + errorMessage);
                            showNotification("Registration Failed", errorMessage, 5);
                        }
                    }
                );
            } else {
                android.util.Log.e("LocationService", "✗ Location is NULL - cannot proceed with registration");
            }
        });
    }
    
    private void showNotification(String title, String message, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_id)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        android.util.Log.d("LocationService", "");
        android.util.Log.d("LocationService", "═══════════════════════════════════════");
        android.util.Log.d("LocationService", "SERVICE DESTROYED - Stopping location updates");
        android.util.Log.d("LocationService", "═══════════════════════════════════════");
        
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
