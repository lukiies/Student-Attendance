package com.example.studentsattendance;

import static android.content.Context.LOCATION_SERVICE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class TrackGPS implements LocationListener {

    public interface LocationUpdateListener {
        void onLocationUpdated(Location location);
    }

    private final Context ctxt;
    private LocationUpdateListener listener;

    boolean checkGPS = false;
    Location myLocation;
    double latitude;
    double longitude;
    protected LocationManager locationManager;

    private static final long MINDISTANCE = 10;
    private static final long MINDELAY = 300000;

    public TrackGPS(Context context, LocationUpdateListener listener) {
        this.ctxt = context;
        this.listener = listener;
        getLocation();
    }

    public Location getLocation() {
        try {
            locationManager = (LocationManager) ctxt.getSystemService(LOCATION_SERVICE);
            checkGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (checkGPS) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINDELAY, MINDISTANCE, this);
                    if (locationManager != null) {
                        myLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (myLocation != null) {
                            latitude = myLocation.getLatitude();
                            longitude = myLocation.getLongitude();
                            if (listener != null) listener.onLocationUpdated(myLocation);
                        }
                    }
                } catch (SecurityException e) {
                    Toast.makeText(ctxt, "No permission to access GPS", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ctxt, "No service provider available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return myLocation;
    }

    public boolean canGetLocation()
    {
        return this.checkGPS;
    }

    public void showAlert() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(ctxt);
        dialog.setTitle("GPS disabled");
        dialog.setMessage("Do you want to turn on GPS?");
        dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                ctxt.startActivity(intent);
            }
        });
        dialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    public void stopGPS() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(TrackGPS.this);
            } catch (SecurityException e) {
                Toast.makeText(ctxt, "No permission to access GPS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.myLocation = location;
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        if (listener != null) {
            listener.onLocationUpdated(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }
}
