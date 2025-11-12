package com.example.studentsattendance;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString();
                if (email.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter your UWS email", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        });

        btnContinueUws.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, "UWS login coming soon!", Toast.LENGTH_SHORT).show();
            }
        });


    }
}