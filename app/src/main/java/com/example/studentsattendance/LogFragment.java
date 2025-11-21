package com.example.studentsattendance;

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

import com.example.studentsattendance.database.dao.AttendanceLogDao;
import com.example.studentsattendance.database.models.AttendanceLog;
import com.example.studentsattendance.utils.SessionManager;

import java.util.List;

public class LogFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private TextView emptyView;
    private LogAdapter adapter;
    private AttendanceLogDao attendanceLogDao;
    private SessionManager sessionManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.logRecyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        
        sessionManager = new SessionManager(requireContext());
        attendanceLogDao = new AttendanceLogDao(requireContext());
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        loadLogs();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadLogs();
    }
    
    private void loadLogs() {
        long userId = sessionManager.getUserId();
        List<AttendanceLog> logs = attendanceLogDao.getLogsForUser(userId);
        
        if (logs.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            
            adapter = new LogAdapter(logs);
            recyclerView.setAdapter(adapter);
        }
    }
}
