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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        btnContinue = findViewById(R.id.btnContinue);
        btnContinueUws = findViewById(R.id.btnContinueUws);

        btnContinue.setOnClickListener(v -> {
            String email = emailInput.getText().toString();

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your UWS email", Toast.LENGTH_SHORT).show();
            } else {
                // Move to next screen - mainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        btnContinueUws.setOnClickListener(v -> {
            Toast.makeText(this, "UWS login coming soon!", Toast.LENGTH_SHORT).show();
        });

    }
}