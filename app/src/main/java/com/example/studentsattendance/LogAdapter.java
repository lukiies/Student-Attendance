package com.example.studentsattendance;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentsattendance.database.models.AttendanceLog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {
    
    private List<AttendanceLog> logs;
    private SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
    private SimpleDateFormat inputTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat outputTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    public LogAdapter(List<AttendanceLog> logs) {
        this.logs = logs;
    }
    
    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        AttendanceLog log = logs.get(position);
        
        // Format date
        try {
            Date date = inputDateFormat.parse(log.getRegistrationDate());
            if (date != null) {
                holder.dateText.setText(outputDateFormat.format(date));
            } else {
                holder.dateText.setText(log.getRegistrationDate());
            }
        } catch (ParseException e) {
            holder.dateText.setText(log.getRegistrationDate());
        }
        
        // Format time
        try {
            Date time = inputTimeFormat.parse(log.getRegistrationTime());
            if (time != null) {
                holder.timeText.setText(outputTimeFormat.format(time));
            } else {
                holder.timeText.setText(log.getRegistrationTime());
            }
        } catch (ParseException e) {
            holder.timeText.setText(log.getRegistrationTime());
        }
        
        // Set status
        holder.statusText.setText(log.getStatus());
        if ("SUCCESS".equals(log.getStatus())) {
            holder.statusText.setTextColor(Color.parseColor("#4CAF50"));
        } else if ("FAILED".equals(log.getStatus())) {
            holder.statusText.setTextColor(Color.parseColor("#F44336"));
        } else {
            holder.statusText.setTextColor(Color.parseColor("#FF9800"));
        }
        
        // Set registration type
        holder.typeText.setText(log.isManual() ? "Manual" : "Automatic");
        
        // Set message if available
        if (log.getMessage() != null && !log.getMessage().isEmpty()) {
            holder.messageText.setVisibility(View.VISIBLE);
            holder.messageText.setText(log.getMessage());
        } else {
            holder.messageText.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return logs.size();
    }
    
    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView dateText;
        TextView timeText;
        TextView statusText;
        TextView typeText;
        TextView messageText;
        
        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            timeText = itemView.findViewById(R.id.timeText);
            statusText = itemView.findViewById(R.id.statusText);
            typeText = itemView.findViewById(R.id.typeText);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }
}
