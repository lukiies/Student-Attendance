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
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    handleLocationUpdate(location);
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .build();

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        );
    }

    private void handleLocationUpdate(Location location) {
        boolean wasInsideCampus = isInsideCampus;
        isInsideCampus = isLocationInsideCampus(location);
        
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            return;
        }
        
        long userId = sessionManager.getUserId();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // Save location if inside campus
        if (isInsideCampus) {
            LocationRecord locationRecord = new LocationRecord(
                userId,
                location.getLatitude(),
                location.getLongitude(),
                today
            );
            locationDao.insertLocation(locationRecord);
            
            // Update notification
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, createNotification("You are on campus"));
            }
            
            // Handle campus entry (first time detection)
            if (!wasInsideCampus && !hasNotifiedForCampusEntry) {
                handleCampusEntry(userId, today);
                hasNotifiedForCampusEntry = true;
            }
        } else {
            // Reset notification flag when outside campus
            if (wasInsideCampus) {
                hasNotifiedForCampusEntry = false;
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.notify(NOTIFICATION_ID, createNotification("Tracking location..."));
                }
            }
        }
    }

    private boolean isLocationInsideCampus(Location location) {
        // Check if we should simulate being on campus
        if (MainActivity.simulateLocationOnCampus) {
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
        return distance[0] <= CAMPUS_RADIUS_METERS;
    }

    private void handleCampusEntry(long userId, String today) {
        // Check if already registered for today
        if (attendanceLogDao.isAttendanceRegisteredForDate(userId, today)) {
            return;
        }
        
        // Get manual registration setting
        String manualSetting = settingsDao.getSetting(userId, "manual_registration", "true");
        boolean isManualMode = "true".equals(manualSetting);
        
        if (isManualMode) {
            // Show notification to register manually
            showManualRegistrationNotification();
        } else {
            // Attempt automatic registration
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
        User user = userDao.getUserById(userId);
        if (user == null) {
            return;
        }
        
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
            return;
        }
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                attendanceManager.registerAttendance(
                    userId,
                    user.getEmail(),
                    user.getStudentId(),
                    user.getPassword(),
                    location.getLatitude(),
                    location.getLongitude(),
                    false,
                    new AttendanceManager.AttendanceCallback() {
                        @Override
                        public void onSuccess(String message) {
                            showNotification("Success", message, 4);
                        }
                        
                        @Override
                        public void onError(String errorMessage) {
                            showNotification("Registration Failed", errorMessage, 5);
                        }
                    }
                );
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
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
