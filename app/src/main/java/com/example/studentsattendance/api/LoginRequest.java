package com.example.studentsattendance.api;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("banner_id")
    private String bannerId;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("password")
    private String password;
    
    public LoginRequest(String bannerId, String email, String password) {
        this.bannerId = bannerId;
        this.email = email;
        this.password = password;
    }

    public String getBannerId() {
        return bannerId;
    }

    public void setBannerId(String bannerId) {
        this.bannerId = bannerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
