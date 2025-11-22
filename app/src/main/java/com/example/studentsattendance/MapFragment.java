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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends Fragment implements OnMapReadyCallback, TrackGPS.LocationUpdateListener {

    private GoogleMap mMap;
    private TrackGPS trackGPS;
    private TextView statusText, checkInSuccessText;
    private ImageView statusIcon;
    private View statusLayout;
    private View rootView;
    private ViewGroup containerGroup;

    // UWS Campus coordinates
    private static final LatLng UWS_CAMPUS = new LatLng(55.8440749, -4.4303226);
    private static final double CAMPUS_RADIUS_METERS = 150;

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
        checkAndDisplayContent();
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
        Button manualCheckinButton = view.findViewById(R.id.manualCheckinButton);

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

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_scan_barcode, null);
            ImageView barcodeImage = dialogView.findViewById(R.id.barcodeImage);

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setPositiveButton("OK", null)
                    .show();
        });
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

        trackGPS = new TrackGPS(requireContext(), this);
        if (!trackGPS.canGetLocation()) {
            trackGPS.showAlert();
        }
    }

    @Override
    public void onLocationUpdated(Location location) {
        if (mMap == null) return;

        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(UWS_CAMPUS).title("UWS Paisley Campus"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 15));

        updateStatus(current);
    }

    private void updateStatus(LatLng currentLocation) {
        float[] distance = new float[1];
        Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                UWS_CAMPUS.latitude, UWS_CAMPUS.longitude,
                distance
        );

        if (distance[0] <= CAMPUS_RADIUS_METERS) {
            statusLayout.setBackgroundColor(Color.parseColor("#F0FFF0"));
            statusIcon.setColorFilter(Color.parseColor("#38A169"));
            statusText.setText("You are currently on campus");
            statusText.setTextColor(Color.parseColor("#38A169"));
            showCheckInSuccess();

        } else {
            statusLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
            statusIcon.setColorFilter(Color.GRAY);
            statusText.setText("You are not on campus");
            statusText.setTextColor(Color.GRAY);
            checkInSuccessText.setVisibility(View.GONE);
        }
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
