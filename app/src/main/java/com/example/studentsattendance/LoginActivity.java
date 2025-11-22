package com.example.studentsattendance;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;


public class LoginActivity extends AppCompatActivity {
    EditText emailInput;
    Button btnContinue, btnContinueUws;
    LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        btnContinue = findViewById(R.id.btnContinue);
        btnContinueUws = findViewById(R.id.btnContinueUws);
        loginManager = new LoginManager(this);

        btnContinue.setOnClickListener(v -> {
            String email = emailInput.getText().toString();
            loginManager.handleLogin(email, new LoginManager.LoginCallback() {
                @Override
                public void onUserExists(String email) {
                    loginManager.loadUserProfile(email);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                }

                @Override
                public void onUserCreated(String email) {
                    loginManager.loadUserProfile(email);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("show_profile_tab", true);
                    startActivity(intent);
                }
            });
        });

        btnContinueUws.setOnClickListener(v -> {
            Toast.makeText(this, "UWS login coming soon!", Toast.LENGTH_SHORT).show();
        });

    }
}