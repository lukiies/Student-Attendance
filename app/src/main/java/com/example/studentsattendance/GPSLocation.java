package com.example.studentsattendance;

import android.location.Location;
import com.google.android.gms.maps.model.LatLng;

public class GPSLocation {
    private static final LatLng UWS_CAMPUS = new LatLng(55.8440749, -4.4303226);
    private static final double CAMPUS_RADIUS_METERS = 300;
    
    private static boolean simulateOnCampus = false;
    
    public static void setSimulateOnCampus(boolean simulate) {
        simulateOnCampus = simulate;
    }
    
    public static boolean isSimulateOnCampus() {
        return simulateOnCampus;
    }
    
    public static boolean isOnCampus(Location location) {
        if (simulateOnCampus) return true;
        if (location == null) return false;
        
        float[] distance = new float[1];
        Location.distanceBetween(
            location.getLatitude(), location.getLongitude(),
            UWS_CAMPUS.latitude, UWS_CAMPUS.longitude,
            distance
        );
        return distance[0] <= CAMPUS_RADIUS_METERS;
    }
    
    public static boolean isOnCampus(LatLng location) {
        if (simulateOnCampus) return true;
        if (location == null) return false;
        
        float[] distance = new float[1];
        Location.distanceBetween(
            location.latitude, location.longitude,
            UWS_CAMPUS.latitude, UWS_CAMPUS.longitude,
            distance
        );
        return distance[0] <= CAMPUS_RADIUS_METERS;
    }
    
    public static LatLng getCampusCenter() {
        return UWS_CAMPUS;
    }
    
    public static double getCampusRadius() {
        return CAMPUS_RADIUS_METERS;
    }
}
