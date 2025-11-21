package com.example.studentsattendance.api;

import com.google.gson.annotations.SerializedName;

public class CombinedResponse {
    @SerializedName("result")
    private int result;
    
    @SerializedName("token")
    private String token;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("error_message")
    private String errorMessage;
    
    @SerializedName("attendance")
    private AttendanceInfo attendance;
    
    public int getResult() {
        return result;
    }
    
    public String getToken() {
        return token;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public AttendanceInfo getAttendance() {
        return attendance;
    }
    
    public static class AttendanceInfo {
        @SerializedName("banner_id")
        private String bannerId;
        
        @SerializedName("timestamp")
        private String timestamp;
        
        @SerializedName("status")
        private String status;
        
        public String getBannerId() {
            return bannerId;
        }
        
        public String getTimestamp() {
            return timestamp;
        }
        
        public String getStatus() {
            return status;
        }
    }
}
