package com.example.studentsattendance;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Button btnLogout = view.findViewById(R.id.btnLogout);

        setupRow(view, R.id.rowProgram, R.drawable.ic_school, "Program", "MSc. Artificial Intelligence", false);
        setupRow(view, R.id.rowEmail, R.drawable.ic_email, "Email", "B01819068@studentmail.uws.ac.uk", false);
        setupRow(view, R.id.rowStudentId, R.drawable.ic_id, "Student ID", "B01819068", false);
        setupRow(view, R.id.rowPassword, R.drawable.ic_key, "Password", "********************", false);
        setupRow(view, R.id.rowBarCode, R.drawable.ic_attach, "BarCode", "", true);

        btnLogout.setOnClickListener(v -> {
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
    }
}
