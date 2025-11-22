package com.example.studentsattendance;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginManager {
    private Context context;
    private DatabaseHelper databaseHelper;
    private static DatabaseHelper.UserProfile currentUserProfile;

    public LoginManager(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
    }

    public static DatabaseHelper.UserProfile getCurrentUserProfile() {
        return currentUserProfile;
    }

    public static void setCurrentUserProfile(DatabaseHelper.UserProfile profile) {
        currentUserProfile = profile;
    }

    public void handleLogin(String email, LoginCallback callback) {
        if (email.isEmpty()) {
            Toast.makeText(context, "Please enter your UWS email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (databaseHelper.checkUserExists(email)) {
            showLoginDialog(email, callback);
        } else {
            showPasswordDialog(email, callback);
        }
    }

    private void showLoginDialog(String email, LoginCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_login_password, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText passwordInput = dialogView.findViewById(R.id.passwordInput);
        Button btnLogin = dialogView.findViewById(R.id.btnLogin);
        Button btnCancelLogin = dialogView.findViewById(R.id.btnCancelLogin);

        btnLogin.setOnClickListener(v -> {
            String password = passwordInput.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show();
            } else if (databaseHelper.validateUser(email, password)) {
                dialog.dismiss();
                callback.onUserExists(email);
            } else {
                Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelLogin.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showPasswordDialog(String email, LoginCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_password, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText password1 = dialogView.findViewById(R.id.password1);
        EditText password2 = dialogView.findViewById(R.id.password2);
        Button btnCreateAccount = dialogView.findViewById(R.id.btnCreateAccount);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCreateAccount.setEnabled(false);

        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pass1 = password1.getText().toString();
                String pass2 = password2.getText().toString();
                btnCreateAccount.setEnabled(!pass1.isEmpty() && !pass2.isEmpty() && pass1.equals(pass2));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        password1.addTextChangedListener(passwordWatcher);
        password2.addTextChangedListener(passwordWatcher);

        btnCreateAccount.setOnClickListener(v -> {
            String password = password1.getText().toString();
            if (databaseHelper.createUser(email, password)) {
                Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                callback.onUserCreated(email);
            } else {
                Toast.makeText(context, "Failed to create account", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public void loadUserProfile(String email) {
        currentUserProfile = databaseHelper.getUserProfile(email);
    }

    public boolean updateProfileField(String email, String name, String program, String studentId, String barcodeUri) {
        boolean success = databaseHelper.updateProfile(email, name, program, studentId, barcodeUri);
        if (success) {
            loadUserProfile(email);
        }
        return success;
    }

    public void showChangePasswordDialog(String email, PasswordChangeCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_password, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText password1 = dialogView.findViewById(R.id.password1);
        EditText password2 = dialogView.findViewById(R.id.password2);
        Button btnCreateAccount = dialogView.findViewById(R.id.btnCreateAccount);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnCreateAccount.setText("Update Password");
        btnCreateAccount.setEnabled(false);

        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pass1 = password1.getText().toString();
                String pass2 = password2.getText().toString();
                btnCreateAccount.setEnabled(!pass1.isEmpty() && !pass2.isEmpty() && pass1.equals(pass2));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        password1.addTextChangedListener(passwordWatcher);
        password2.addTextChangedListener(passwordWatcher);

        btnCreateAccount.setOnClickListener(v -> {
            String newPassword = password1.getText().toString();
            if (databaseHelper.updatePassword(email, newPassword)) {
                Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                callback.onPasswordChanged();
            } else {
                Toast.makeText(context, "Failed to update password", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public interface LoginCallback {
        void onUserExists(String email);
        void onUserCreated(String email);
    }

    public interface PasswordChangeCallback {
        void onPasswordChanged();
    }
}
