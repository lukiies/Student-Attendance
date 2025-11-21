package com.example.studentsattendance.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studentsattendance.database.DatabaseHelper;
import com.example.studentsattendance.database.models.AttendanceLog;

import java.util.ArrayList;
import java.util.List;

public class AttendanceLogDao {
    
    private DatabaseHelper dbHelper;
    
    public AttendanceLogDao(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }
    
    // Insert attendance log
    public long insertLog(AttendanceLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, log.getUserId());
        values.put(DatabaseHelper.COLUMN_REGISTRATION_DATE, log.getRegistrationDate());
        values.put(DatabaseHelper.COLUMN_REGISTRATION_TIME, log.getRegistrationTime());
        values.put(DatabaseHelper.COLUMN_STATUS, log.getStatus());
        values.put(DatabaseHelper.COLUMN_MESSAGE, log.getMessage());
        values.put(DatabaseHelper.COLUMN_IS_MANUAL, log.isManual() ? 1 : 0);
        
        return db.insert(DatabaseHelper.TABLE_ATTENDANCE_LOGS, null, values);
    }
    
    // Get all logs for a user
    public List<AttendanceLog> getLogsForUser(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<AttendanceLog> logs = new ArrayList<>();
        
        String[] columns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_USER_ID,
            DatabaseHelper.COLUMN_REGISTRATION_DATE,
            DatabaseHelper.COLUMN_REGISTRATION_TIME,
            DatabaseHelper.COLUMN_STATUS,
            DatabaseHelper.COLUMN_MESSAGE,
            DatabaseHelper.COLUMN_IS_MANUAL,
            DatabaseHelper.COLUMN_CREATED_AT
        };
        
        String selection = DatabaseHelper.COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_ATTENDANCE_LOGS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            DatabaseHelper.COLUMN_CREATED_AT + " DESC"
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                AttendanceLog log = new AttendanceLog();
                log.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                log.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)));
                log.setRegistrationDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REGISTRATION_DATE)));
                log.setRegistrationTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REGISTRATION_TIME)));
                log.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS)));
                log.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MESSAGE)));
                log.setManual(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_MANUAL)) == 1);
                log.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT)));
                logs.add(log);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return logs;
    }
    
    // Check if attendance already registered for a date
    public boolean isAttendanceRegisteredForDate(long userId, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] columns = {DatabaseHelper.COLUMN_ID};
        String selection = DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                          DatabaseHelper.COLUMN_REGISTRATION_DATE + " = ? AND " +
                          DatabaseHelper.COLUMN_STATUS + " = ?";
        String[] selectionArgs = {String.valueOf(userId), date, "SUCCESS"};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_ATTENDANCE_LOGS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        boolean isRegistered = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        
        return isRegistered;
    }
    
    // Get logs for a specific date
    public List<AttendanceLog> getLogsForDate(long userId, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<AttendanceLog> logs = new ArrayList<>();
        
        String[] columns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_USER_ID,
            DatabaseHelper.COLUMN_REGISTRATION_DATE,
            DatabaseHelper.COLUMN_REGISTRATION_TIME,
            DatabaseHelper.COLUMN_STATUS,
            DatabaseHelper.COLUMN_MESSAGE,
            DatabaseHelper.COLUMN_IS_MANUAL,
            DatabaseHelper.COLUMN_CREATED_AT
        };
        
        String selection = DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                          DatabaseHelper.COLUMN_REGISTRATION_DATE + " = ?";
        String[] selectionArgs = {String.valueOf(userId), date};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_ATTENDANCE_LOGS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            DatabaseHelper.COLUMN_CREATED_AT + " DESC"
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                AttendanceLog log = new AttendanceLog();
                log.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                log.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)));
                log.setRegistrationDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REGISTRATION_DATE)));
                log.setRegistrationTime(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REGISTRATION_TIME)));
                log.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS)));
                log.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MESSAGE)));
                log.setManual(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_MANUAL)) == 1);
                log.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT)));
                logs.add(log);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return logs;
    }
}
