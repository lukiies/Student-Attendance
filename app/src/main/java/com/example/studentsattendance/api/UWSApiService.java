package com.example.studentsattendance.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface UWSApiService {
    
    @POST("login/email.and.password.credentials")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    @POST("attendance.register")
    Call<AttendanceResponse> registerAttendance(
        @Header("Authorization") String authorization,
        @Body AttendanceRegistrationRequest request
    );
    
    @POST("login.then.register")
    Call<CombinedResponse> loginThenRegister(@Body CombinedRequest request);
}
