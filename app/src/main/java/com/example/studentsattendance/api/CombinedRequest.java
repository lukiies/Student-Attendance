package com.example.studentsattendance.api;

import com.google.gson.annotations.SerializedName;

public class CombinedRequest {
    @SerializedName("banner_id")
    private String bannerId;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("password")
    private String password;
    
    @SerializedName("dt")
    private String datetime;
    
    public CombinedRequest(String bannerId, String email, String password, String datetime) {
        this.bannerId = bannerId;
        this.email = email;
        this.password = password;
        this.datetime = datetime;
    }
    
    public String getBannerId() {
        return bannerId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getDatetime() {
        return datetime;
    }
}
