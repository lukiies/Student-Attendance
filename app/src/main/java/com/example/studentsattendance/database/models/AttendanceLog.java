package com.example.studentsattendance.database.models;

public class AttendanceLog {
    private long id;
    private long userId;
    private String registrationDate;
    private String registrationTime;
    private String status; // SUCCESS, FAILED, PENDING
    private String message;
    private boolean isManual;
    private String createdAt;
    
    public AttendanceLog() {
    }
    
    public AttendanceLog(long userId, String registrationDate, String registrationTime, 
                        String status, String message, boolean isManual) {
        this.userId = userId;
        this.registrationDate = registrationDate;
        this.registrationTime = registrationTime;
        this.status = status;
        this.message = message;
        this.isManual = isManual;
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
    
    public String getRegistrationDate() {
        return registrationDate;
    }
    
    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }
    
    public String getRegistrationTime() {
        return registrationTime;
    }
    
    public void setRegistrationTime(String registrationTime) {
        this.registrationTime = registrationTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isManual() {
        return isManual;
    }
    
    public void setManual(boolean manual) {
        isManual = manual;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
