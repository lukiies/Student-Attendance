package com.example.studentsattendance.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studentsattendance.database.DatabaseHelper;

import java.util.HashMap;
import java.util.Map;

public class UserSettingsDao {
    
    private DatabaseHelper dbHelper;
    
    public UserSettingsDao(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }
    
    // Save or update setting
    public long saveSetting(long userId, String key, String value) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, userId);
        values.put(DatabaseHelper.COLUMN_SETTING_KEY, key);
        values.put(DatabaseHelper.COLUMN_SETTING_VALUE, value);
        
        // Try to update first
        String whereClause = DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                            DatabaseHelper.COLUMN_SETTING_KEY + " = ?";
        String[] whereArgs = {String.valueOf(userId), key};
        
        int rowsAffected = db.update(DatabaseHelper.TABLE_USER_SETTINGS, values, whereClause, whereArgs);
        
        // If no rows updated, insert new
        if (rowsAffected == 0) {
            return db.insert(DatabaseHelper.TABLE_USER_SETTINGS, null, values);
        }
        
        return rowsAffected;
    }
    
    // Get setting value
    public String getSetting(long userId, String key, String defaultValue) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] columns = {DatabaseHelper.COLUMN_SETTING_VALUE};
        String selection = DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                          DatabaseHelper.COLUMN_SETTING_KEY + " = ?";
        String[] selectionArgs = {String.valueOf(userId), key};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USER_SETTINGS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        String value = defaultValue;
        if (cursor != null && cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SETTING_VALUE));
            cursor.close();
        }
        
        return value;
    }
    
    // Get all settings for a user
    public Map<String, String> getAllSettings(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, String> settings = new HashMap<>();
        
        String[] columns = {
            DatabaseHelper.COLUMN_SETTING_KEY,
            DatabaseHelper.COLUMN_SETTING_VALUE
        };
        
        String selection = DatabaseHelper.COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USER_SETTINGS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String key = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SETTING_KEY));
                String value = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SETTING_VALUE));
                settings.put(key, value);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return settings;
    }
    
    // Delete setting
    public int deleteSetting(long userId, String key) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        String whereClause = DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                            DatabaseHelper.COLUMN_SETTING_KEY + " = ?";
        String[] whereArgs = {String.valueOf(userId), key};
        
        return db.delete(DatabaseHelper.TABLE_USER_SETTINGS, whereClause, whereArgs);
    }
}
