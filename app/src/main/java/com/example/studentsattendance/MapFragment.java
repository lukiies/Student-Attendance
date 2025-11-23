package com.example.studentsattendance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MapFragment extends Fragment implements OnMapReadyCallback, TrackGPS.LocationUpdateListener {

    private GoogleMap mMap;
    private TrackGPS trackGPS;
    private TextView statusText, checkInSuccessText;
    private ImageView statusIcon;
    private View statusLayout;
    private View rootView;
    private ViewGroup containerGroup;
    private CheckBox simulateCheckbox;
    private Button manualRegButton, barcodeRegButton;
    private boolean lastOnCampusState = false;
    private Circle campusCircle;
    private Marker currentLocationMarker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_map, container, false);
        containerGroup = (ViewGroup) rootView;
        checkAndDisplayContent();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile from database to get latest data
        DatabaseHelper.UserProfile currentProfile = LoginManager.getCurrentUserProfile();
        if (currentProfile != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
            DatabaseHelper.UserProfile freshProfile = dbHelper.getUserProfile(currentProfile.email);
            LoginManager.setCurrentUserProfile(freshProfile);
        }
        checkAndDisplayContent();
        updateManualRegistrationButtonVisibility();
    }

    private void checkAndDisplayContent() {
        if (rootView == null || containerGroup == null) return;

        DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
        if (profile != null && !profile.profileCompleted) {
            showWarningMessage();
        } else {
            showNormalView();
        }
    }

    private void showWarningMessage() {
        containerGroup.removeAllViews();
        TextView warningText = new TextView(requireContext());
        warningText.setText("Start by fulfilling your Profile");
        warningText.setTextSize(24);
        warningText.setTextColor(android.graphics.Color.parseColor("#D53F8C"));
        warningText.setGravity(android.view.Gravity.CENTER);
        warningText.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        containerGroup.addView(warningText);
    }

    private void showNormalView() {
        if (statusText != null) return;

        containerGroup.removeAllViews();
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_map, containerGroup, false);
        containerGroup.addView(view);

        statusText = view.findViewById(R.id.statusText);
        statusIcon = view.findViewById(R.id.statusIcon);
        statusLayout = view.findViewById(R.id.statusLayout);
        checkInSuccessText = view.findViewById(R.id.checkInSuccessText);
        checkInSuccessText.setVisibility(View.GONE);
        simulateCheckbox = view.findViewById(R.id.simulateOnCampusCheckbox);
        manualRegButton = view.findViewById(R.id.manualRegistrationButton);
        barcodeRegButton = view.findViewById(R.id.barcodeRegistrationButton);

        simulateCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GPSLocation.setSimulateOnCampus(isChecked);
            if (trackGPS != null && trackGPS.myLocation != null) {
                onLocationUpdated(trackGPS.myLocation);
            }
        });

        updateManualRegistrationButtonVisibility();

        manualRegButton.setOnClickListener(v -> {
            if (trackGPS == null || trackGPS.myLocation == null) {
                Toast.makeText(requireContext(), "Location not available yet", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!GPSLocation.isOnCampus(trackGPS.myLocation)) {
                Toast.makeText(requireContext(), "You can't register outside of the campus", Toast.LENGTH_SHORT).show();
                return;
            }
            
            ButtonsHandlers.handleManualRegistration(requireContext());
        });
        
        barcodeRegButton.setOnClickListener(v -> {
            boolean isOnCampus = trackGPS != null && trackGPS.myLocation != null && GPSLocation.isOnCampus(trackGPS.myLocation);
            ButtonsHandlers.handleBarcodeRegistration(requireContext(), isOnCampus);
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_container);

        if (mapFragment == null) {
            mapFragment = new SupportMapFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
        }

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        mMap.setMyLocationEnabled(true);

        LatLng campusCenter = GPSLocation.getCampusCenter();
        campusCircle = mMap.addCircle(new CircleOptions()
                .center(campusCenter)
                .radius(GPSLocation.getCampusRadius())
                .strokeColor(Color.parseColor("#38A169"))
                .strokeWidth(2)
                .fillColor(Color.parseColor("#2038A169")));

        mMap.addMarker(new MarkerOptions().position(campusCenter).title("UWS Paisley Campus"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, 15));
        
        // Enable My Location layer
        try {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException e) {
            // Permissions not granted
        }

        trackGPS = new TrackGPS(requireContext(), this);
        if (!trackGPS.canGetLocation()) {
            trackGPS.showAlert();
        }
    }

    @Override
    public void onLocationUpdated(Location location) {
        if (mMap == null) return;

        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
        
        // Update or create current location marker
        if (currentLocationMarker == null) {
            currentLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(current)
                .title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            currentLocationMarker.setPosition(current);
        }
        
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15));

        updateStatus(location);
    }

    private void updateStatus(Location location) {
        // Calculate actual distance to campus
        float[] distance = new float[1];
        LatLng campusCenter = GPSLocation.getCampusCenter();
        Location.distanceBetween(
            location.getLatitude(), location.getLongitude(),
            campusCenter.latitude, campusCenter.longitude,
            distance
        );
        
        boolean onCampus = GPSLocation.isOnCampus(location);
        boolean isSimulating = GPSLocation.isSimulateOnCampus();
        
        String statusMessage;
        if (isSimulating) {
            statusMessage = "SIMULATED: On campus (actual: " + Math.round(distance[0]) + "m away)";
        } else if (onCampus) {
            statusMessage = "You are currently on campus (" + Math.round(distance[0]) + "m from center)";
        } else {
            statusMessage = "You're outside the campus now (" + Math.round(distance[0]) + "m away)";
        }

        if (onCampus) {
            statusLayout.setBackgroundColor(Color.parseColor("#F0FFF0"));
            statusIcon.setColorFilter(Color.parseColor("#38A169"));
            statusText.setText(statusMessage);
            statusText.setTextColor(Color.parseColor("#38A169"));
            
            if (!lastOnCampusState) {
                handleCampusEntry();
            }
        } else {
            statusLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
            statusIcon.setColorFilter(Color.GRAY);
            statusText.setText(statusMessage);
            statusText.setTextColor(Color.GRAY);
            checkInSuccessText.setVisibility(View.GONE);
        }
        
        lastOnCampusState = onCampus;
    }

    private void handleCampusEntry() {
        DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
        if (profile == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        Date now = new Date();
        String today = dateFormat.format(now);

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        
        // Check if record already exists
        boolean recordExists = dbHelper.isAttendanceRecordExists(profile.email, today);
        
        if (!recordExists) {
            // Create new attendance record
            dbHelper.addAttendanceRecord(profile.email, today, timeFormat.format(now));
        }
        
        // Only show success message after registration completes (or if manual mode)
        if (profile.manualRegistrationOnly) {
            // Manual mode: show success immediately since no API call needed
            checkInSuccessText.setVisibility(View.VISIBLE);
        } else {
            // Auto mode: attempt registration, success message shown in callback
            autoRegister(profile, today);
        }
    }

    private void autoRegister(DatabaseHelper.UserProfile profile, String date) {
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        
        // If already registered today, just show success message
        if (dbHelper.isAttendanceRegisteredToday(profile.email, date)) {
            checkInSuccessText.setVisibility(View.VISIBLE);
            return;
        }

        String password = dbHelper.getPassword(profile.email);
        if (password == null || password.isEmpty()) {
            Toast.makeText(requireContext(), "Registration failed: No password found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Call external API for registration
        ExternalAPI.registerAttendance(profile.studentId, profile.email, password, new ExternalAPI.RegistrationCallback() {
            @Override
            public void onProgress(String message) {
                // Optional: could show progress indicator
            }

            @Override
            public void onSuccess(String message) {
                SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                dbHelper.updateAttendanceRegistration(profile.email, date, fullFormat.format(new Date()));
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        checkInSuccessText.setVisibility(View.VISIBLE);
                        Toast.makeText(requireContext(), "Check-in registered successfully", Toast.LENGTH_SHORT).show();
                        SoundUtils.playCheckinSound(requireContext());
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Registration failed: " + message, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }


    private void updateManualRegistrationButtonVisibility() {
        if (manualRegButton == null) return;
        DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
        if (profile != null) {
            manualRegButton.setVisibility(profile.manualRegistrationOnly ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (trackGPS != null) trackGPS.stopGPS();
    }
}
