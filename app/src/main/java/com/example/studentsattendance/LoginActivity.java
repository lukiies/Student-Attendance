package com.example.studentsattendance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentsattendance.database.dao.UserDao;
import com.example.studentsattendance.database.models.User;
import com.example.studentsattendance.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    
    private EditText emailInput, passwordInput, studentIdInput, programInput;
    private Button btnAction;
    private TextView tvToggleMode, tvTitle, tvSubtitle;
    private boolean isLoginMode = true;
    
    private UserDao userDao;
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize database and session
        userDao = new UserDao(this);
        sessionManager = new SessionManager(this);
        
        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToMainActivity();
            return;
        }
        
        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        studentIdInput = findViewById(R.id.studentIdInput);
        programInput = findViewById(R.id.programInput);
        btnAction = findViewById(R.id.btnContinue);
        tvToggleMode = findViewById(R.id.tvToggleMode);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        
        // Set initial mode to login
        updateUIForMode();
        
        btnAction.setOnClickListener(v -> {
            if (isLoginMode) {
                performLogin();
            } else {
                performSignUp();
            }
        });
        
        tvToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateUIForMode();
        });
    }
    
    private void updateUIForMode() {
        if (isLoginMode) {
            tvTitle.setText("Welcome Back");
            tvSubtitle.setText("Sign in to your account");
            btnAction.setText("Sign In");
            tvToggleMode.setText("Don't have an account? Sign Up");
            studentIdInput.setVisibility(View.GONE);
            programInput.setVisibility(View.GONE);
        } else {
            tvTitle.setText("Create Account");
            tvSubtitle.setText("Sign up to get started");
            btnAction.setText("Sign Up");
            tvToggleMode.setText("Already have an account? Sign In");
            studentIdInput.setVisibility(View.VISIBLE);
            programInput.setVisibility(View.VISIBLE);
        }
    }
    
    private void performLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }
        
        // Authenticate user
        User user = userDao.authenticateUser(email, password);
        
        if (user != null) {
            // Login successful
            sessionManager.createLoginSession(
                user.getId(),
                user.getEmail(),
                user.getStudentId(),
                user.getProgram()
            );
            
            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void performSignUp() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String studentId = studentIdInput.getText().toString().trim();
        String program = programInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }
        
        if (!email.endsWith("@studentmail.uws.ac.uk")) {
            emailInput.setError("Please use your UWS student email");
            emailInput.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }
        
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(studentId)) {
            studentIdInput.setError("Student ID is required");
            studentIdInput.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(program)) {
            programInput.setError("Program is required");
            programInput.requestFocus();
            return;
        }
        
        // Check if email already exists
        if (userDao.emailExists(email)) {
            Toast.makeText(this, "Email already registered. Please login.", Toast.LENGTH_SHORT).show();
            isLoginMode = true;
            updateUIForMode();
            return;
        }
        
        // Create new user
        User newUser = new User(email, password, studentId, program);
        long userId = userDao.createUser(newUser);
        
        if (userId != -1) {
            // Sign up successful
            sessionManager.createLoginSession(userId, email, studentId, program);
            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        } else {
            Toast.makeText(this, "Sign up failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}