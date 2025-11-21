package com.example.studentsattendance.api;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("result")
    private int result;
    
    @SerializedName("error_message")
    private String errorMessage;
    
    @SerializedName("token")
    private String token;
    
    public LoginResponse() {
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
    
    public boolean isSuccess() {
        return result == 0;
    }
}
