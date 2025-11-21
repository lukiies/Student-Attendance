package com.example.studentsattendance.api;

import com.google.gson.annotations.SerializedName;

public class AttendanceRegistrationRequest {
    @SerializedName("banner_id")
    private String bannerId;
    
    @SerializedName("dt")
    private String datetime;
    
    public AttendanceRegistrationRequest(String bannerId, String datetime) {
        this.bannerId = bannerId;
        this.datetime = datetime;
    }

    public String getBannerId() {
        return bannerId;
    }

    public void setBannerId(String bannerId) {
        this.bannerId = bannerId;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }
}
