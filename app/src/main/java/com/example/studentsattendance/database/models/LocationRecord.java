package com.example.studentsattendance.database.models;

public class LocationRecord {
    private long id;
    private long userId;
    private double latitude;
    private double longitude;
    private String timestamp;
    private String date;
    private boolean isRegistered;
    
    public LocationRecord() {
    }
    
    public LocationRecord(long userId, double latitude, double longitude, String date) {
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
        this.isRegistered = false;
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public boolean isRegistered() {
        return isRegistered;
    }
    
    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }
}
