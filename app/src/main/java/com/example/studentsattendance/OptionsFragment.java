package com.example.studentsattendance;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.studentsattendance.database.DatabaseHelper;
import com.example.studentsattendance.database.dao.AttendanceLogDao;
import com.example.studentsattendance.database.dao.LocationDao;
import com.example.studentsattendance.database.dao.UserDao;
import com.example.studentsattendance.database.dao.UserSettingsDao;
import com.example.studentsattendance.database.models.User;
import com.example.studentsattendance.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class OptionsFragment extends Fragment {

    public static final String SETTING_MANUAL_REGISTRATION = "manual_registration";
    
    private SessionManager sessionManager;
    private UserDao userDao;
    private UserSettingsDao settingsDao;
    private User currentUser;
    
    private Switch switchManualRegistration;
    private ImageView ivBarcodePreview;
    private Button btnSelectBarcode, btnLogout, btnEditUserInfo, btnCleanDatabase;
    private TextView tvEmail, tvStudentId, tvProgram;
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_options, container, false);

        sessionManager = new SessionManager(requireContext());
        userDao = new UserDao(requireContext());
        settingsDao = new UserSettingsDao(requireContext());
        
        // Load current user data
        long userId = sessionManager.getUserId();
        currentUser = userDao.getUserById(userId);

        // Initialize views
        tvEmail = view.findViewById(R.id.tvEmail);
        tvStudentId = view.findViewById(R.id.tvStudentId);
        tvProgram = view.findViewById(R.id.tvProgram);
        switchManualRegistration = view.findViewById(R.id.switchManualRegistration);
        ivBarcodePreview = view.findViewById(R.id.ivBarcodePreview);
        btnSelectBarcode = view.findViewById(R.id.btnSelectBarcode);
        btnEditUserInfo = view.findViewById(R.id.btnEditUserInfo);
        btnCleanDatabase = view.findViewById(R.id.btnCleanDatabase);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Set user info
        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            tvStudentId.setText(currentUser.getStudentId() != null ? currentUser.getStudentId() : "Not set");
            tvProgram.setText(currentUser.getProgram() != null ? currentUser.getProgram() : "Not set");
            
            // Load barcode image if exists
            if (currentUser.getBannerIdImage() != null && !currentUser.getBannerIdImage().isEmpty()) {
                loadBarcodeImage(currentUser.getBannerIdImage());
            }
        }

        // Load settings
        loadSettings();

        // Setup listeners
        switchManualRegistration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveManualRegistrationSetting(isChecked);
        });

        // Register image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        saveBarcodeImage(selectedImage);
                    }
                }
            }
        );

        btnSelectBarcode.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openImagePicker();
            }
        });

        btnEditUserInfo.setOnClickListener(v -> {
            showEditUserInfoDialog();
        });

        btnCleanDatabase.setOnClickListener(v -> {
            showCleanDatabaseConfirmation();
        });

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    private void loadSettings() {
        if (currentUser != null) {
            String manualReg = settingsDao.getSetting(currentUser.getId(), SETTING_MANUAL_REGISTRATION, "true");
            switchManualRegistration.setChecked("true".equals(manualReg));
        }
    }

    private void saveManualRegistrationSetting(boolean enabled) {
        if (currentUser != null) {
            settingsDao.saveSetting(currentUser.getId(), SETTING_MANUAL_REGISTRATION, String.valueOf(enabled));
            Toast.makeText(requireContext(), 
                "Manual registration " + (enabled ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkStoragePermission() {
        // Android 13+ uses READ_MEDIA_IMAGES instead of READ_EXTERNAL_STORAGE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), 
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), 
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                return false;
            }
        }
        return true;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveBarcodeImage(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            // Save to app's internal storage
            File dir = new File(requireContext().getFilesDir(), "barcodes");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File file = new File(dir, "barcode_" + currentUser.getId() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            // Update database
            String imagePath = file.getAbsolutePath();
            userDao.updateBannerIdImage(currentUser.getId(), imagePath);
            currentUser.setBannerIdImage(imagePath);
            
            // Display preview
            ivBarcodePreview.setImageBitmap(bitmap);
            ivBarcodePreview.setVisibility(View.VISIBLE);
            
            Toast.makeText(requireContext(), "Barcode image saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBarcodeImage(String imagePath) {
        File file = new File(imagePath);
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            ivBarcodePreview.setImageBitmap(bitmap);
            ivBarcodePreview.setVisibility(View.VISIBLE);
        }
    }
    
    public boolean isManualRegistrationEnabled() {
        if (currentUser != null) {
            String manualReg = settingsDao.getSetting(currentUser.getId(), SETTING_MANUAL_REGISTRATION, "true");
            return "true".equals(manualReg);
        }
        return true;
    }
    
    private void showEditUserInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user_info, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // Get views from dialog
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        TextInputEditText etStudentId = dialogView.findViewById(R.id.etStudentId);
        TextInputEditText etProgram = dialogView.findViewById(R.id.etProgram);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        
        // Set current values
        if (currentUser != null) {
            etEmail.setText(currentUser.getEmail());
            etStudentId.setText(currentUser.getStudentId());
            etProgram.setText(currentUser.getProgram());
        }
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim();
            String program = etProgram.getText().toString().trim();
            
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (studentId.isEmpty()) {
                Toast.makeText(requireContext(), "Student ID cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Update user information
            currentUser.setEmail(email);
            currentUser.setStudentId(studentId);
            currentUser.setProgram(program);
            
            userDao.updateUser(currentUser);
            
            // Update displayed info
            tvEmail.setText(email);
            tvStudentId.setText(studentId);
            tvProgram.setText(program);
            
            Toast.makeText(requireContext(), "Information updated successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void showCleanDatabaseConfirmation() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Clean Database")
            .setMessage("This will delete all attendance logs and location data. This action cannot be undone. Are you sure?")
            .setPositiveButton("Yes, Clean", (dialog, which) -> {
                cleanDatabase();
            })
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    private void cleanDatabase() {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
            
            // Delete all attendance logs
            db.delete(DatabaseHelper.TABLE_ATTENDANCE_LOGS, null, null);
            
            // Delete all locations
            db.delete(DatabaseHelper.TABLE_LOCATIONS, null, null);
            
            // Optionally delete user settings (except manual registration)
            db.delete(DatabaseHelper.TABLE_USER_SETTINGS, 
                DatabaseHelper.COLUMN_SETTING_KEY + " != ?", 
                new String[]{SETTING_MANUAL_REGISTRATION});
            
            Toast.makeText(requireContext(), "Database cleaned successfully", Toast.LENGTH_LONG).show();
            
            // Optionally refresh any views if needed
            // You might want to notify other fragments about this change
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error cleaning database: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }
}
