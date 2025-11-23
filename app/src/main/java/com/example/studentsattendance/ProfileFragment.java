package com.example.studentsattendance;


import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    private LoginManager loginManager;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private TextView tvProgramValue, tvEmailValue, tvStudentIdValue, tvBarcodeValue;
    private ImageView barcodeImageView;
    private CheckBox manualRegistrationCheckbox;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    requireContext().getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
                    if (profile != null) {
                        loginManager.updateProfileField(profile.email, profile.name, profile.program, 
                            profile.studentId, uri.toString());
                        displayBarcodeImage(uri.toString());
                        Toast.makeText(requireContext(), "Barcode image updated", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        loginManager = new LoginManager(requireContext());
        Button btnLogout = view.findViewById(R.id.btnLogout);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvStudentIdHeader = view.findViewById(R.id.tvStudentIdHeader);
        View nameContainer = tvName.getParent() instanceof View ? (View) tvName.getParent() : null;
        manualRegistrationCheckbox = view.findViewById(R.id.manualRegistrationCheckbox);

        DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
        if (profile != null) {
            tvName.setText(profile.name.isEmpty() ? "Student Name" : profile.name);
            tvStudentIdHeader.setText("StudentId: " + (profile.studentId.isEmpty() ? "Not set" : profile.studentId));
            
            if (nameContainer != null) {
                nameContainer.setOnClickListener(v -> {
                    showEditDialog("Name", profile.name, newName -> {
                        loginManager.updateProfileField(profile.email, newName, profile.program, 
                            profile.studentId, profile.barcodeUri);
                        tvName.setText(newName);
                    });
                });
            }
            
            setupRow(view, R.id.rowProgram, R.drawable.ic_school, "Program", profile.program, false);
            setupRow(view, R.id.rowEmail, R.drawable.ic_email, "Email", profile.email, false);
            setupRow(view, R.id.rowStudentId, R.drawable.ic_id, "Student ID", profile.studentId, false);
            setupRow(view, R.id.rowPassword, R.drawable.ic_key, "Password", "********************", false);
            
            manualRegistrationCheckbox.setChecked(profile.manualRegistrationOnly);
            
            manualRegistrationCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
                dbHelper.updateManualRegistrationOnly(profile.email, isChecked);
                profile.manualRegistrationOnly = isChecked;
                loginManager.loadUserProfile(profile.email);
            });
            
            setupRow(view, R.id.rowBarCode, R.drawable.ic_attach, "BarCode", 
                profile.barcodeUri.isEmpty() ? "No barcode selected" : profile.barcodeUri, true);
        }

        btnLogout.setOnClickListener(v -> {
            LoginManager.setCurrentUserProfile(null);
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    private void setupRow(View parent, int id, int iconRes, String label, String value, boolean isBarcodeRow) {
        View row = parent.findViewById(id);
        ImageView leftIcon = row.findViewById(R.id.leftIcon);
        TextView tvLabel = row.findViewById(R.id.tvLabel);
        TextView tvValue = row.findViewById(R.id.tvValue);
        ImageView editIcon = row.findViewById(R.id.editIcon);

        leftIcon.setImageResource(iconRes);
        tvLabel.setText(label);
        tvValue.setText(value);

        if (id == R.id.rowProgram) tvProgramValue = tvValue;
        else if (id == R.id.rowEmail) tvEmailValue = tvValue;
        else if (id == R.id.rowStudentId) tvStudentIdValue = tvValue;
        else if (id == R.id.rowBarCode) {
            tvBarcodeValue = tvValue;
            barcodeImageView = row.findViewById(R.id.barcodeImage);
            DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
            if (profile != null && !profile.barcodeUri.isEmpty()) {
                displayBarcodeImage(profile.barcodeUri);
            }
        }

        if (label.equals("Email")) {
            editIcon.setVisibility(View.GONE);
            return;
        }

        row.setOnClickListener(v -> {
            DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
            if (profile == null) return;

            if (isBarcodeRow) {
                imagePickerLauncher.launch("image/*");
            } else if (label.equals("Password")) {
                loginManager.showChangePasswordDialog(profile.email, () -> {});
            } else {
                TextView tvStudentIdHeader = getView() != null ? getView().findViewById(R.id.tvStudentIdHeader) : null;
                showEditDialog(label, value, newValue -> {
                    if (label.equals("Program")) {
                        loginManager.updateProfileField(profile.email, profile.name, newValue, 
                            profile.studentId, profile.barcodeUri);
                        tvProgramValue.setText(newValue);
                    } else if (label.equals("Student ID")) {
                        loginManager.updateProfileField(profile.email, profile.name, profile.program, 
                            newValue, profile.barcodeUri);
                        tvStudentIdValue.setText(newValue);
                        if (tvStudentIdHeader != null) {
                            tvStudentIdHeader.setText("StudentId: " + newValue);
                        }
                    }
                });
            }
        });
    }

    private void showEditDialog(String label, String currentValue, ValueChangeCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit " + label);

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentValue);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                callback.onValueChanged(newValue);
                Toast.makeText(requireContext(), label + " updated", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void displayBarcodeImage(String uriString) {
        if (barcodeImageView != null && uriString != null && !uriString.isEmpty()) {
            try {
                Uri uri = Uri.parse(uriString);
                barcodeImageView.setImageURI(uri);
                barcodeImageView.setVisibility(View.VISIBLE);
                tvBarcodeValue.setVisibility(View.GONE);
            } catch (Exception e) {
                tvBarcodeValue.setText("Error loading image");
                barcodeImageView.setVisibility(View.GONE);
            }
        } else if (barcodeImageView != null) {
            barcodeImageView.setVisibility(View.GONE);
            tvBarcodeValue.setVisibility(View.VISIBLE);
        }
    }

    interface ValueChangeCallback {
        void onValueChanged(String newValue);
    }
}
