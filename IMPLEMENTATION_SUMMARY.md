# Student Attendance Application - Implementation Summary

## Overview
This Android application tracks student attendance at the UWS Paisley campus using GPS location tracking, with both manual and automatic registration modes.

## Implemented Features

### 1. ✅ SQLite Database Infrastructure
**Location:** `app/src/main/java/com/example/studentsattendance/database/`

- **DatabaseHelper.java**: Manages database creation and versioning
- **Tables Created:**
  - `users`: Stores user credentials and profile information
  - `locations`: Records GPS coordinates with timestamps when on campus
  - `attendance_logs`: Tracks attendance registration attempts and results
  - `user_settings`: Stores user-specific settings

- **DAO Classes:**
  - `UserDao.java`: User authentication and profile management
  - `LocationDao.java`: Location record operations
  - `AttendanceLogDao.java`: Attendance log operations
  - `UserSettingsDao.java`: User settings management

- **Model Classes:**
  - `User.java`, `LocationRecord.java`, `AttendanceLog.java`

### 2. ✅ Authentication System
**Location:** `LoginActivity.java`, `SessionManager.java`

- **Features:**
  - Sign up with email (must be @studentmail.uws.ac.uk), password, student ID, and program
  - Sign in with email and password
  - Password validation (minimum 6 characters)
  - Email uniqueness check
  - Session persistence using SharedPreferences
  - Automatic login on app restart if session is active
  - Logout functionality

### 3. ✅ Options Screen with Settings
**Location:** `OptionsFragment.java`, `fragment_options.xml`

- **Features:**
  - Display user information (email, student ID, program)
  - Toggle for manual vs automatic attendance registration
  - Banner ID barcode image picker from gallery
  - Image storage in app's internal storage
  - Settings saved per user in database
  - Logout button

### 4. ✅ Background Location Service
**Location:** `service/LocationTrackingService.java`

- **Features:**
  - Foreground service that runs continuously when logged in
  - Location updates every 5 minutes (configurable)
  - Uses FusedLocationProviderClient for accurate GPS
  - Detects when user enters UWS campus boundaries (150m radius)
  - Saves location to database every 5 minutes when on campus
  - Persistent notification showing tracking status

### 5. ✅ Location Data Storage
**Location:** Handled by `LocationTrackingService` and `LocationDao`

- **Features:**
  - Automatically saves GPS coordinates with timestamp
  - Only saves when inside campus boundaries
  - Stores date separately for easy querying
  - Tracks registration status (registered/unregistered)
  - Prevents duplicate saves

### 6. ✅ Manual Registration Notifications
**Location:** `LocationTrackingService.java` (handleCampusEntry method)

- **Features:**
  - Detects first-time campus entry each session
  - Shows notification: "You are on the Paisley UWS campus..."
  - Tapping notification opens app to registration dialog
  - Only triggered when manual mode is ON
  - Prevents duplicate notifications

### 7. ✅ Automatic Registration System
**Location:** `LocationTrackingService.java`, `AttendanceManager.java`

- **Features:**
  - Automatically attempts registration when manual mode is OFF
  - Checks for internet connectivity
  - Shows notification about registration attempt
  - Handles no internet scenario with appropriate message
  - Saves pending status if offline
  - Shows success/failure notifications

### 8. ✅ UWS API Integration Layer
**Location:** `app/src/main/java/com/example/studentsattendance/api/`

- **Components:**
  - `ApiClient.java`: Retrofit configuration with OkHttp
  - `UWSApiService.java`: API endpoint definitions
  - `AttendanceRequest.java`: Request model
  - `AttendanceResponse.java`: Response model
  - `AttendanceManager.java`: Business logic for registration

- **Features:**
  - REST API client using Retrofit
  - Network connectivity checking
  - Error handling for connection issues
  - Timeout configuration (30 seconds)
  - HTTP logging for debugging
  - Currently uses simulation mode (ready for real API)

### 9. ✅ Attendance Log Tracking
**Location:** `AttendanceManager.java`, `AttendanceLogDao.java`

- **Features:**
  - Creates log entry after each registration attempt
  - Stores status: SUCCESS, FAILED, ERROR, PENDING
  - Records whether registration was manual or automatic
  - Marks all locations for the date as registered on success
  - Prevents duplicate registrations for same day
  - Stores error messages for troubleshooting

### 10. ✅ Enhanced Map View
**Location:** `MapFragment.java`, `fragment_map.xml`

- **Features:**
  - Displays user's current location on Google Maps
  - Draws visible circle boundary around UWS campus (150m radius)
  - Real-time status indicator (on campus / off campus)
  - Color-coded status (green for on campus, gray for off)
  - Manual registration button (visible only when on campus)
  - Campus marker at UWS Paisley coordinates (55.8440749, -4.4303226)
  - Sound effect when entering campus
  - Zoom controls enabled

### 11. ✅ Navigation and Session Management
**Location:** `MainActivity.java`, `LoginActivity.java`, `SessionManager.java`

- **Features:**
  - Tab-based navigation (Map and Options tabs)
  - Only accessible when logged in
  - Automatic redirect to LoginActivity if not authenticated
  - Session state persists across app restarts
  - Background service starts automatically on login
  - Permission handling (location, notifications, storage)
  - Android 10+ background location support
  - Android 13+ notification permission support

### 12. ✅ Barcode Display Dialog
**Location:** `MapFragment.java` (showBarcodeDialog method), `dialog_scan_barcode.xml`

- **Features:**
  - Shows registration confirmation dialog first
  - Displays user's Banner ID barcode image (from Options)
  - Automatically increases screen brightness to maximum
  - Restores original brightness when dialog closes
  - Large barcode display for easy scanning
  - Instructions for the user
  - "Complete Registration" button to finalize attendance
  - Calls API after barcode is shown

## Permissions Required

All permissions are declared in `AndroidManifest.xml`:

- `ACCESS_FINE_LOCATION`: Precise GPS tracking
- `ACCESS_COARSE_LOCATION`: Approximate location
- `ACCESS_BACKGROUND_LOCATION`: Location tracking when app is in background (Android 10+)
- `INTERNET`: API communication
- `ACCESS_NETWORK_STATE`: Check internet connectivity
- `POST_NOTIFICATIONS`: Show notifications (Android 13+)
- `FOREGROUND_SERVICE`: Run location tracking service
- `FOREGROUND_SERVICE_LOCATION`: Location-specific foreground service
- `READ_EXTERNAL_STORAGE`: Access gallery for barcode image
- `READ_MEDIA_IMAGES`: Access images on Android 13+

## Dependencies Added

In `app/build.gradle.kts`:

```kotlin
implementation("com.google.android.gms:play-services-location:21.0.1")
implementation("androidx.work:work-runtime:2.8.1")
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
```

## App Flow

### First-Time User:
1. Launch app → LoginActivity
2. Click "Sign Up"
3. Enter email (@studentmail.uws.ac.uk), password, student ID, program
4. Automatically logged in → MainActivity
5. Grant location and notification permissions
6. Location service starts in background
7. Navigate to Options tab
8. Upload Banner ID barcode image
9. Set manual/automatic registration preference

### Returning User:
1. Launch app → Automatically logged in (session persists)
2. MainActivity with Map and Options tabs
3. Location tracking active in background

### Manual Registration (Manual Mode ON):
1. User enters campus → Notification appears
2. Tap notification or open app manually
3. See "You are on campus" status on Map tab
4. Tap "Manual Check-in" button
5. Confirm registration in dialog
6. View Banner ID barcode (brightness increased)
7. Tap "Complete Registration"
8. API call made, attendance recorded
9. All locations for today marked as registered

### Automatic Registration (Manual Mode OFF):
1. User enters campus → Service detects
2. Checks internet connectivity
3. Automatically calls API to register
4. Shows notification with result
5. Records in attendance log
6. Marks locations as registered

### Viewing Location Status:
1. Open Map tab
2. See current location and campus boundary
3. Status text shows if on/off campus
4. Green = on campus, Gray = off campus

## Database Schema

### users table
- id (PRIMARY KEY)
- email (UNIQUE, NOT NULL)
- password (NOT NULL)
- student_id
- program
- banner_id_image (file path)
- created_at (DATETIME)

### locations table
- id (PRIMARY KEY)
- user_id (FOREIGN KEY)
- latitude (REAL)
- longitude (REAL)
- timestamp (DATETIME)
- date (TEXT, format: YYYY-MM-DD)
- is_registered (INTEGER, 0 or 1)

### attendance_logs table
- id (PRIMARY KEY)
- user_id (FOREIGN KEY)
- registration_date (TEXT)
- registration_time (TEXT)
- status (TEXT: SUCCESS/FAILED/ERROR/PENDING)
- message (TEXT)
- is_manual (INTEGER, 0 or 1)
- created_at (DATETIME)

### user_settings table
- id (PRIMARY KEY)
- user_id (FOREIGN KEY)
- setting_key (TEXT)
- setting_value (TEXT)
- UNIQUE(user_id, setting_key)

## Configuration Constants

### UWS Campus Location:
- Latitude: 55.8440749
- Longitude: -4.4303226
- Radius: 150 meters

### Location Updates:
- Interval: 5 minutes (300,000 ms)
- Fastest Interval: 2 minutes (120,000 ms)
- Priority: HIGH_ACCURACY

### API Configuration:
- Base URL: `https://api.uws.ac.uk/attendance/` (placeholder)
- Timeout: 30 seconds

## Testing Notes

1. **API Simulation**: The app currently uses simulated API responses. To connect to real UWS API:
   - Update `BASE_URL` in `ApiClient.java`
   - Uncomment real API call in `AttendanceManager.java`
   - Implement authentication token management

2. **Location Testing**: Use Android Studio's location emulator to simulate being at UWS campus coordinates

3. **Background Service**: Ensure battery optimization is disabled for the app to maintain location tracking

## Known Limitations & Future Enhancements

1. **API Integration**: Currently simulated - needs real UWS API endpoint
2. **Authentication Token**: Needs proper OAuth/JWT implementation
3. **Offline Queue**: Could implement queue for pending registrations when offline
4. **Battery Optimization**: Consider using WorkManager for better battery efficiency
5. **Location Accuracy**: Could add Wi-Fi/Bluetooth beacons for indoor accuracy
6. **Attendance History**: Could add a tab to view past attendance records
7. **Geofencing**: Could use Android Geofencing API for more efficient battery usage

## File Structure Summary

```
app/src/main/java/com/example/studentsattendance/
├── api/
│   ├── ApiClient.java
│   ├── AttendanceRequest.java
│   ├── AttendanceResponse.java
│   └── UWSApiService.java
├── database/
│   ├── DatabaseHelper.java
│   ├── dao/
│   │   ├── AttendanceLogDao.java
│   │   ├── LocationDao.java
│   │   ├── UserDao.java
│   │   └── UserSettingsDao.java
│   └── models/
│       ├── AttendanceLog.java
│       ├── LocationRecord.java
│       └── User.java
├── service/
│   └── LocationTrackingService.java
├── utils/
│   ├── AttendanceManager.java
│   └── SessionManager.java
├── LoginActivity.java
├── MainActivity.java
├── MapFragment.java
├── OptionsFragment.java
├── ProfileFragment.java (legacy)
└── TrackGPS.java
```

## Conclusion

All 12 features from the requirements have been successfully implemented. The application provides a complete attendance tracking solution with:

- ✅ User authentication with persistent sessions
- ✅ Background GPS tracking every 5 minutes
- ✅ Campus boundary detection
- ✅ Both manual and automatic registration modes
- ✅ Banner ID barcode display with brightness control
- ✅ Comprehensive database for all data
- ✅ API integration framework (ready for production API)
- ✅ Proper error handling and user notifications
- ✅ Settings management per user
- ✅ Visual map interface with campus boundaries

The app is ready for testing and can be connected to the real UWS attendance API by updating the API configuration.
