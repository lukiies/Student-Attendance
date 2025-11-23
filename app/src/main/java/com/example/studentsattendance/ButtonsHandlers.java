package com.example.studentsattendance;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ButtonsHandlers {
    
    public static void handleManualRegistration(Context context) {
        DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
        if (profile == null) {
            Toast.makeText(context, "Profile not loaded", Toast.LENGTH_SHORT).show();
            return;
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        Date now = new Date();
        String today = dateFormat.format(now);
        
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        if (dbHelper.isAttendanceRegisteredToday(profile.email, today)) {
            Toast.makeText(context, "The day has already been registered", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create attendance record if it doesn't exist
        if (!dbHelper.isAttendanceRecordExists(profile.email, today)) {
            dbHelper.addAttendanceRecord(profile.email, today, timeFormat.format(now));
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Manual Registration");
        
        ScrollView scrollView = new ScrollView(context);
        TextView logText = new TextView(context);
        logText.setPadding(40, 40, 40, 40);
        logText.setTextSize(14);
        scrollView.addView(logText);
        builder.setView(scrollView);
        
        AlertDialog dialog = builder.setNegativeButton("Close", null).create();
        dialog.show();
        
        logText.append("Starting registration process...\n\n");
        
        String password = dbHelper.getPassword(profile.email);
        if (password == null || password.isEmpty()) {
            Toast.makeText(context, "Password not found", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            return;
        }
        
        ExternalAPI.registerAttendance(profile.studentId, profile.email, password, new ExternalAPI.RegistrationCallback() {
            @Override
            public void onProgress(String message) {
                logText.append(message + "\n");
            }
            
            @Override
            public void onSuccess(String message) {
                logText.append("\n✓ SUCCESS: " + message + "\n");
                SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                Date now = new Date();
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now);
                
                dbHelper.updateAttendanceRegistration(profile.email, today, fullFormat.format(now));
                
                Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show();
                SoundUtils.playCheckinSound(context);
            }
            
            @Override
            public void onError(String message) {
                logText.append("\n✗ ERROR: " + message + "\n");
                Toast.makeText(context, "Registration failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public static void handleBarcodeRegistration(Context context, boolean isOnCampus) {
        DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
        if (profile == null || profile.barcodeUri.isEmpty()) {
            Toast.makeText(context, "No barcode available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = dateFormat.format(new Date());
        
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        boolean alreadyRegistered = dbHelper.isAttendanceRegisteredToday(profile.email, today);
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_scan_barcode, null);
        ImageView barcodeImage = dialogView.findViewById(R.id.barcodeImage);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancelBarcode);
        android.widget.Button btnSave = dialogView.findViewById(R.id.btnSaveRegistered);
        
        if (!profile.barcodeUri.isEmpty()) {
            try {
                android.net.Uri uri = android.net.Uri.parse(profile.barcodeUri);
                barcodeImage.setImageURI(uri);
            } catch (Exception e) {
                Toast.makeText(context, "Error loading barcode", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setView(dialogView)
            .create();
        
        // Hide Save button if already registered OR if not on campus
        if (alreadyRegistered || !isOnCampus) {
            btnCancel.setText("Close");
            btnSave.setVisibility(View.GONE);
        } else {
            btnSave.setOnClickListener(v -> {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                Date now = new Date();
                
                dbHelper.addAttendanceRecord(profile.email, today, timeFormat.format(now));
                dbHelper.updateAttendanceRegistration(profile.email, today, fullFormat.format(now));
                
                Toast.makeText(context, "Attendance saved as registered", Toast.LENGTH_SHORT).show();
                SoundUtils.playCheckinSound(context);
                dialog.dismiss();
            });
        }
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.screenBrightness = 1.0f;
            dialog.getWindow().setAttributes(lp);
        }
    }
}
