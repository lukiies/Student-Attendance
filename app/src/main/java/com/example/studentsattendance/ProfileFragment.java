package com.example.studentsattendance;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studentsattendance.database.dao.UserDao;
import com.example.studentsattendance.database.models.User;
import com.example.studentsattendance.utils.SessionManager;

public class ProfileFragment extends Fragment {

    private SessionManager sessionManager;
    private UserDao userDao;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(requireContext());
        userDao = new UserDao(requireContext());
        
        // Load current user data
        long userId = sessionManager.getUserId();
        currentUser = userDao.getUserById(userId);

        Button btnLogout = view.findViewById(R.id.btnLogout);

        // Setup rows with actual user data
        if (currentUser != null) {
            setupRow(view, R.id.rowProgram, R.drawable.ic_school, "Program", 
                    currentUser.getProgram() != null ? currentUser.getProgram() : "Not set", false);
            setupRow(view, R.id.rowEmail, R.drawable.ic_email, "Email", 
                    currentUser.getEmail(), false);
            setupRow(view, R.id.rowStudentId, R.drawable.ic_id, "Student ID", 
                    currentUser.getStudentId() != null ? currentUser.getStudentId() : "Not set", false);
            setupRow(view, R.id.rowPassword, R.drawable.ic_key, "Password", "********************", false);
            setupRow(view, R.id.rowBarCode, R.drawable.ic_attach, "BarCode", 
                    currentUser.getBannerIdImage() != null ? "Set" : "Not set", true);
        }

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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

        leftIcon.setImageResource(iconRes);
        tvLabel.setText(label);
        tvValue.setText(value);
    }
}
