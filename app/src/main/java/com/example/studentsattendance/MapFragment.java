package com.example.studentsattendance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.studentsattendance.database.dao.UserDao;
import com.example.studentsattendance.database.dao.UserSettingsDao;
import com.example.studentsattendance.database.models.User;
import com.example.studentsattendance.utils.AttendanceManager;
import com.example.studentsattendance.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;

public class MapFragment extends Fragment implements OnMapReadyCallback, TrackGPS.LocationUpdateListener {

    private GoogleMap mMap;
    private TrackGPS trackGPS;
    private TextView statusText, checkInSuccessText;
    private ImageView statusIcon;
    private View statusLayout;
    private Button manualCheckinButton;

    // UWS Campus coordinates
    private static final LatLng UWS_CAMPUS = new LatLng(55.8440749, -4.4303226);
    private static final double CAMPUS_RADIUS_METERS = 150;
    
    private boolean isInsideCampus = false;
    private SessionManager sessionManager;
    private UserDao userDao;
    private UserSettingsDao userSettingsDao;
    private AttendanceManager attendanceManager;
    private Location currentLocation;
    private float originalBrightness;
    private boolean isManualRegistrationEnabled = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_map, container, false);
        
        sessionManager = new SessionManager(requireContext());
        userDao = new UserDao(requireContext());
        userSettingsDao = new UserSettingsDao(requireContext());
        attendanceManager = new AttendanceManager(requireContext());

        statusText = view.findViewById(R.id.statusText);
        statusIcon = view.findViewById(R.id.statusIcon);
        statusLayout = view.findViewById(R.id.statusLayout);
        checkInSuccessText = view.findViewById(R.id.checkInSuccessText);
        checkInSuccessText.setVisibility(View.GONE);
        manualCheckinButton = view.findViewById(R.id.manualCheckinButton);
        
        // Initially hide manual button
        manualCheckinButton.setVisibility(View.GONE);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_container);

        if (mapFragment == null) {
            mapFragment = new SupportMapFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
        }

        mapFragment.getMapAsync(this);

        manualCheckinButton.setOnClickListener(v -> {
            if (isInsideCampus) {
                showRegistrationDialog();
            } else {
                Toast.makeText(requireContext(), 
                    "You must be on campus to register attendance", 
                    Toast.LENGTH_SHORT).show();
            }
        });

        // Load manual registration setting after views are initialized
        loadManualRegistrationSetting();

        return view;
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
        
        // Draw campus boundary circle
        mMap.addCircle(new CircleOptions()
            .center(UWS_CAMPUS)
            .radius(CAMPUS_RADIUS_METERS)
            .strokeColor(Color.parseColor("#007BFF"))
            .strokeWidth(3)
            .fillColor(Color.parseColor("#4D007BFF")));

        trackGPS = new TrackGPS(requireContext(), this);
        if (!trackGPS.canGetLocation()) {
            trackGPS.showAlert();
        }
    }

    @Override
    public void onLocationUpdated(Location location) {
        if (mMap == null) return;

        currentLocation = location;
        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.clear();
        
        // Re-draw campus boundary
        mMap.addCircle(new CircleOptions()
            .center(UWS_CAMPUS)
            .radius(CAMPUS_RADIUS_METERS)
            .strokeColor(Color.parseColor("#007BFF"))
            .strokeWidth(3)
            .fillColor(Color.parseColor("#4D007BFF")));
            
        mMap.addMarker(new MarkerOptions().position(UWS_CAMPUS).title("UWS Paisley Campus"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15));

        updateStatus(current);
    }

    private void updateStatus(LatLng currentLocation) {
        // Check if we should simulate being on campus
        if (MainActivity.simulateLocationOnCampus) {
            isInsideCampus = true;
        } else {
            float[] distance = new float[1];
            Location.distanceBetween(
                    currentLocation.latitude, currentLocation.longitude,
                    UWS_CAMPUS.latitude, UWS_CAMPUS.longitude,
                    distance
            );
            isInsideCampus = (distance[0] <= CAMPUS_RADIUS_METERS);
        }

        if (isInsideCampus) {
            statusLayout.setBackgroundColor(Color.parseColor("#F0FFF0"));
            statusIcon.setColorFilter(Color.parseColor("#38A169"));
            statusText.setText("You are currently on campus");
            statusText.setTextColor(Color.parseColor("#38A169"));
            
            // Show manual button only if manual registration is enabled
            updateManualButtonVisibility();
            showCheckInSuccess();

        } else {
            statusLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
            statusIcon.setColorFilter(Color.GRAY);
            statusText.setText("You are not on campus");
            statusText.setTextColor(Color.GRAY);
            checkInSuccessText.setVisibility(View.GONE);
            manualCheckinButton.setVisibility(View.GONE);
        }
    }
    
    private void loadManualRegistrationSetting() {
        long userId = sessionManager.getUserId();
        String setting = userSettingsDao.getSetting(userId, "manual_registration", "true");
        isManualRegistrationEnabled = Boolean.parseBoolean(setting);
        android.util.Log.d("MapFragment", "Manual registration enabled: " + isManualRegistrationEnabled);
        updateManualButtonVisibility();
    }
    
    private void updateManualButtonVisibility() {
        android.util.Log.d("MapFragment", "Updating button visibility - Manual: " + isManualRegistrationEnabled + ", OnCampus: " + isInsideCampus);
        // Always show button when on campus, regardless of manual registration setting
        if (isInsideCampus) {
            manualCheckinButton.setVisibility(View.VISIBLE);
            android.util.Log.d("MapFragment", "Button set to VISIBLE");
        } else {
            manualCheckinButton.setVisibility(View.GONE);
            android.util.Log.d("MapFragment", "Button set to GONE");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload setting when returning to this fragment
        loadManualRegistrationSetting();
    }
    
    private void showRegistrationDialog() {
        long userId = sessionManager.getUserId();
        User user = userDao.getUserById(userId);
        
        if (user == null || currentLocation == null) {
            Toast.makeText(requireContext(), "Unable to register. Please try again.", 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        // If manual registration is ON, show confirmation dialog with barcode
        if (isManualRegistrationEnabled) {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_registration_confirmation, null);
            ImageView barcodeImagePreview = dialogView.findViewById(R.id.barcodeImagePreview);
            
            // Load user's barcode image in confirmation dialog
            if (user.getBannerIdImage() != null && !user.getBannerIdImage().isEmpty()) {
                File barcodeFile = new File(user.getBannerIdImage());
                if (barcodeFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(user.getBannerIdImage());
                    barcodeImagePreview.setImageBitmap(bitmap);
                    barcodeImagePreview.setVisibility(View.VISIBLE);
                } else {
                    barcodeImagePreview.setVisibility(View.GONE);
                }
            } else {
                barcodeImagePreview.setVisibility(View.GONE);
            }
            
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Register", (d, which) -> {
                    restoreScreenBrightness();
                    // Directly call API registration
                    registerWithApi(user);
                })
                .setNegativeButton("Cancel", (d, which) -> {
                    restoreScreenBrightness();
                })
                .setOnDismissListener(d -> restoreScreenBrightness())
                .create();
                
            dialog.show();
            
            // Increase brightness for barcode scanning
            increaseScreenBrightness();
        } else {
            // If manual registration is OFF, show barcode directly without registration info
            showBarcodeOnlyDialog(user);
        }
    }
    
    private void showBarcodeOnlyDialog(User user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_scan_barcode, null);
        ImageView barcodeImage = dialogView.findViewById(R.id.barcodeImage);
        TextView tvInstructions = dialogView.findViewById(R.id.tvInstructions);
        
        // Update instructions for barcode-only mode
        tvInstructions.setText("Present this barcode to the scanner on campus\n(Automatic registration is enabled)");
        
        // Load user's barcode image
        if (user.getBannerIdImage() != null && !user.getBannerIdImage().isEmpty()) {
            File barcodeFile = new File(user.getBannerIdImage());
            if (barcodeFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(user.getBannerIdImage());
                barcodeImage.setImageBitmap(bitmap);
                android.util.Log.d("MapFragment", "Barcode image loaded (barcode-only mode): " + user.getBannerIdImage());
            } else {
                tvInstructions.setText("No barcode image found. Please upload your Banner ID barcode in Options.");
                android.util.Log.e("MapFragment", "Barcode file does not exist: " + user.getBannerIdImage());
            }
        } else {
            tvInstructions.setText("No barcode image set. Please upload your Banner ID barcode in Options.");
            android.util.Log.w("MapFragment", "No barcode image path in user record");
        }
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Your Banner ID Barcode")
            .setNegativeButton("Close", (d, which) -> {
                restoreScreenBrightness();
            })
            .setOnDismissListener(d -> restoreScreenBrightness())
            .create();
        
        dialog.show();
        
        // Increase screen brightness AFTER dialog is shown
        increaseScreenBrightness();
    }
    
    private void showBarcodeDialog(User user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_scan_barcode, null);
        ImageView barcodeImage = dialogView.findViewById(R.id.barcodeImage);
        TextView tvInstructions = dialogView.findViewById(R.id.tvInstructions);
        
        // Load user's barcode image
        if (user.getBannerIdImage() != null && !user.getBannerIdImage().isEmpty()) {
            File barcodeFile = new File(user.getBannerIdImage());
            if (barcodeFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(user.getBannerIdImage());
                barcodeImage.setImageBitmap(bitmap);
                android.util.Log.d("MapFragment", "Barcode image loaded from: " + user.getBannerIdImage());
            } else {
                tvInstructions.setText("No barcode image found. Please upload your Banner ID barcode in Options.");
                android.util.Log.e("MapFragment", "Barcode file does not exist: " + user.getBannerIdImage());
            }
        } else {
            tvInstructions.setText("No barcode image set. Please upload your Banner ID barcode in Options.");
            android.util.Log.w("MapFragment", "No barcode image path in user record");
        }
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Scan Your Banner ID")
            .setPositiveButton("Complete Registration", (d, which) -> {
                restoreScreenBrightness();
                registerWithApi(user);
            })
            .setNegativeButton("Cancel", (d, which) -> {
                restoreScreenBrightness();
            })
            .setOnDismissListener(d -> restoreScreenBrightness())
            .create();
        
        dialog.show();
        
        // Increase screen brightness AFTER dialog is shown
        increaseScreenBrightness();
    }
    
    private void registerWithApi(User user) {
        android.util.Log.d("MapFragment", "Starting API registration for user: " + user.getEmail());
        Toast.makeText(requireContext(), "Connecting to server...", Toast.LENGTH_SHORT).show();
        
        attendanceManager.registerAttendance(
            user.getId(),
            user.getEmail(),
            user.getStudentId(),
            user.getPassword(),
            currentLocation.getLatitude(),
            currentLocation.getLongitude(),
            true,
            new AttendanceManager.AttendanceCallback() {
                @Override
                public void onSuccess(String message) {
                    android.util.Log.d("MapFragment", "Registration SUCCESS: " + message);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "✓ " + message, Toast.LENGTH_LONG).show();
                        showCheckInSuccess();
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    android.util.Log.e("MapFragment", "Registration ERROR: " + errorMessage);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "✗ " + errorMessage, Toast.LENGTH_LONG).show();
                    });
                }
            }
        );
    }
    
    private void increaseScreenBrightness() {
        try {
            originalBrightness = Settings.System.getFloat(
                requireContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS
            ) / 255.0f;
        } catch (Settings.SettingNotFoundException e) {
            originalBrightness = 0.5f;
        }
        
        WindowManager.LayoutParams layoutParams = requireActivity().getWindow().getAttributes();
        layoutParams.screenBrightness = 1.0f; // Maximum brightness
        requireActivity().getWindow().setAttributes(layoutParams);
    }
    
    private void restoreScreenBrightness() {
        WindowManager.LayoutParams layoutParams = requireActivity().getWindow().getAttributes();
        layoutParams.screenBrightness = originalBrightness;
        requireActivity().getWindow().setAttributes(layoutParams);
    }

    private void showCheckInSuccess() {
        checkInSuccessText.setVisibility(View.VISIBLE);

        MediaPlayer mediaPlayer = MediaPlayer.create(requireContext(), R.raw.checkin_sound);
        mediaPlayer.start();

        //avoid memory leak
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
        });

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (trackGPS != null) trackGPS.stopGPS();
    }
}
