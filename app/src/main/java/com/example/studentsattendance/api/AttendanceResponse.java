package com.example.studentsattendance.api;

import com.google.gson.annotations.SerializedName;

public class AttendanceResponse {
    @SerializedName("result")
    private int result;
    
    @SerializedName("error_message")
    private String errorMessage;
    
    public AttendanceResponse() {
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return result == 0;
    }
    
    public String getMessage() {
        if (result == 0) {
            return "Attendance registered successfully";
        }
        return errorMessage != null ? errorMessage : "Unknown error occurred";
    }
}
