package com.example.studentsattendance.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    
    private static final String PREF_NAME = "StudentAttendanceSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_STUDENT_ID = "studentId";
    private static final String KEY_PROGRAM = "program";
    private static final String KEY_JWT_TOKEN = "jwtToken";
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;
    
    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }
    
    // Create login session
    public void createLoginSession(long userId, String email, String studentId, String program) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_STUDENT_ID, studentId);
        editor.putString(KEY_PROGRAM, program);
        editor.commit();
    }
    
    // Save JWT token
    public void saveToken(String token) {
        editor.putString(KEY_JWT_TOKEN, token);
        editor.commit();
    }
    
    // Get JWT token
    public String getToken() {
        return pref.getString(KEY_JWT_TOKEN, null);
    }
    
    // Check if user is logged in
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    // Get user ID
    public long getUserId() {
        return pref.getLong(KEY_USER_ID, -1);
    }
    
    // Get user email
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }
    
    // Get student ID
    public String getStudentId() {
        return pref.getString(KEY_STUDENT_ID, null);
    }
    
    // Get program
    public String getProgram() {
        return pref.getString(KEY_PROGRAM, null);
    }
    
    // Logout user
    public void logout() {
        editor.clear();
        editor.commit();
    }
}
