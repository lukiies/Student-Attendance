package com.example.studentsattendance.database.models;

public class User {
    private long id;
    private String email;
    private String password;
    private String studentId;
    private String program;
    private String bannerIdImage;
    private String createdAt;
    
    public User() {
    }
    
    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    public User(String email, String password, String studentId, String program) {
        this.email = email;
        this.password = password;
        this.studentId = studentId;
        this.program = program;
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
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
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getProgram() {
        return program;
    }
    
    public void setProgram(String program) {
        this.program = program;
    }
    
    public String getBannerIdImage() {
        return bannerIdImage;
    }
    
    public void setBannerIdImage(String bannerIdImage) {
        this.bannerIdImage = bannerIdImage;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
