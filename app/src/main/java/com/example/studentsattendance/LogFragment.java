package com.example.studentsattendance;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogFragment extends Fragment {
    private RecyclerView recyclerView;
    private LogAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewLog);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        view.findViewById(R.id.btnCleanDb).setOnClickListener(v -> cleanDatabase());
        
        loadAttendanceRecords();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAttendanceRecords();
    }

    private void cleanDatabase() {
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Clean Database")
            .setMessage("Are you sure you want to delete all attendance records? This action cannot be undone.")
            .setPositiveButton("Delete All", (dialog, which) -> {
                DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
                if (profile != null) {
                    DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
                    dbHelper.clearAllAttendanceRecords(profile.email);
                    loadAttendanceRecords();
                    android.widget.Toast.makeText(requireContext(), "All attendance records deleted", android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void loadAttendanceRecords() {
        DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
        if (profile == null) return;

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        Cursor cursor = dbHelper.getAllAttendanceRecords(profile.email);

        List<AttendanceRecord> records = new ArrayList<>();
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        String today = inputDateFormat.format(new Date());

        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                String formattedDate = date;
                try {
                    Date d = inputDateFormat.parse(date);
                    if (d != null) formattedDate = outputDateFormat.format(d);
                } catch (ParseException ignored) {}

                records.add(new AttendanceRecord(date, formattedDate, time, status, today));
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter = new LogAdapter(records, this::retryRegistration);
        recyclerView.setAdapter(adapter);
    }

    private void retryRegistration(String date) {
        DatabaseHelper.UserProfile profile = LoginManager.getCurrentUserProfile();
        if (profile == null) return;

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        String password = dbHelper.getPassword(profile.email);
        
        if (password == null || password.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), "Registration failed: No password found", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.Toast.makeText(requireContext(), "Retrying registration...", android.widget.Toast.LENGTH_SHORT).show();
        
        ExternalAPI.registerAttendance(profile.studentId, profile.email, password, new ExternalAPI.RegistrationCallback() {
            @Override
            public void onProgress(String message) {}

            @Override
            public void onSuccess(String message) {
                SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                dbHelper.updateAttendanceRegistration(profile.email, date, fullFormat.format(new Date()));
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(requireContext(), "Registration successful", android.widget.Toast.LENGTH_SHORT).show();
                        SoundUtils.playCheckinSound(requireContext());
                        loadAttendanceRecords();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(requireContext(), "Registration failed: " + message, android.widget.Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private static class AttendanceRecord {
        String rawDate, date, time, status;
        boolean isToday, isRegistered;
        
        AttendanceRecord(String rawDate, String date, String time, String status, String today) {
            this.rawDate = rawDate;
            this.date = date;
            this.time = time;
            this.status = status != null && !status.equals("Not registered") ? status : "Not registered";
            this.isRegistered = status != null && !status.equals("Not registered");
            this.isToday = rawDate.equals(today);
        }
    }

    interface RetryCallback {
        void retry(String date);
    }

    private static class LogAdapter extends RecyclerView.Adapter<LogViewHolder> {
        private final List<AttendanceRecord> records;
        private final RetryCallback retryCallback;

        LogAdapter(List<AttendanceRecord> records, RetryCallback retryCallback) {
            this.records = records;
            this.retryCallback = retryCallback;
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
            return new LogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            AttendanceRecord record = records.get(position);
            holder.bind(record, retryCallback);
        }

        @Override
        public int getItemCount() {
            return records.size();
        }
    }

    private static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvStatus;
        android.widget.Button btnRetry;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvLogDate);
            tvTime = itemView.findViewById(R.id.tvLogTime);
            tvStatus = itemView.findViewById(R.id.tvLogStatus);
            btnRetry = itemView.findViewById(R.id.btnRetryRegistration);
        }

        void bind(AttendanceRecord record, RetryCallback callback) {
            tvDate.setText(record.date);
            tvTime.setText(record.time);
            tvStatus.setText(record.status);
            
            // Show retry button only for today's unregistered records
            if (record.isToday && !record.isRegistered) {
                btnRetry.setVisibility(View.VISIBLE);
                btnRetry.setOnClickListener(v -> callback.retry(record.rawDate));
            } else {
                btnRetry.setVisibility(View.GONE);
            }
        }
    }
}
