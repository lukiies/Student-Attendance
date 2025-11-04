package com.example.studentsattendance;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MapFragment extends Fragment  {


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        Button manualCheckinButton = view.findViewById(R.id.manualCheckinButton);

        manualCheckinButton.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_scan_barcode, null);

            ImageView barcodeImage = dialogView.findViewById(R.id.barcodeImage);

            // Criar e mostrar o dialog
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        return view;
    }



}
