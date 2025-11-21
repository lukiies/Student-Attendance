package com.example.studentsattendance.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studentsattendance.database.DatabaseHelper;
import com.example.studentsattendance.database.models.LocationRecord;

import java.util.ArrayList;
import java.util.List;

public class LocationDao {
    
    private DatabaseHelper dbHelper;
    
    public LocationDao(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }
    
    // Insert location record
    public long insertLocation(LocationRecord location) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_ID, location.getUserId());
        values.put(DatabaseHelper.COLUMN_LATITUDE, location.getLatitude());
        values.put(DatabaseHelper.COLUMN_LONGITUDE, location.getLongitude());
        values.put(DatabaseHelper.COLUMN_DATE, location.getDate());
        values.put(DatabaseHelper.COLUMN_IS_REGISTERED, location.isRegistered() ? 1 : 0);
        
        return db.insert(DatabaseHelper.TABLE_LOCATIONS, null, values);
    }
    
    // Get unregistered locations for a user on a specific date
    public List<LocationRecord> getUnregisteredLocationsForDate(long userId, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<LocationRecord> locations = new ArrayList<>();
        
        String[] columns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_USER_ID,
            DatabaseHelper.COLUMN_LATITUDE,
            DatabaseHelper.COLUMN_LONGITUDE,
            DatabaseHelper.COLUMN_TIMESTAMP,
            DatabaseHelper.COLUMN_DATE,
            DatabaseHelper.COLUMN_IS_REGISTERED
        };
        
        String selection = DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                          DatabaseHelper.COLUMN_DATE + " = ? AND " +
                          DatabaseHelper.COLUMN_IS_REGISTERED + " = 0";
        String[] selectionArgs = {String.valueOf(userId), date};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_LOCATIONS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            DatabaseHelper.COLUMN_TIMESTAMP + " DESC"
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                LocationRecord location = new LocationRecord();
                location.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                location.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)));
                location.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LATITUDE)));
                location.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LONGITUDE)));
                location.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)));
                location.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE)));
                location.setRegistered(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_REGISTERED)) == 1);
                locations.add(location);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return locations;
    }
    
    // Mark all locations for a date as registered
    public int markLocationsAsRegistered(long userId, String date) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_IS_REGISTERED, 1);
        
        String whereClause = DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                            DatabaseHelper.COLUMN_DATE + " = ?";
        String[] whereArgs = {String.valueOf(userId), date};
        
        return db.update(DatabaseHelper.TABLE_LOCATIONS, values, whereClause, whereArgs);
    }
    
    // Get all locations for a user
    public List<LocationRecord> getAllLocationsForUser(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<LocationRecord> locations = new ArrayList<>();
        
        String[] columns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_USER_ID,
            DatabaseHelper.COLUMN_LATITUDE,
            DatabaseHelper.COLUMN_LONGITUDE,
            DatabaseHelper.COLUMN_TIMESTAMP,
            DatabaseHelper.COLUMN_DATE,
            DatabaseHelper.COLUMN_IS_REGISTERED
        };
        
        String selection = DatabaseHelper.COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_LOCATIONS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            DatabaseHelper.COLUMN_TIMESTAMP + " DESC"
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                LocationRecord location = new LocationRecord();
                location.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                location.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ID)));
                location.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LATITUDE)));
                location.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LONGITUDE)));
                location.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)));
                location.setDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE)));
                location.setRegistered(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_REGISTERED)) == 1);
                locations.add(location);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return locations;
    }
    
    // Check if there's any unregistered location for today
    public boolean hasUnregisteredLocationForToday(long userId, String today) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] columns = {DatabaseHelper.COLUMN_ID};
        String selection = DatabaseHelper.COLUMN_USER_ID + " = ? AND " +
                          DatabaseHelper.COLUMN_DATE + " = ? AND " +
                          DatabaseHelper.COLUMN_IS_REGISTERED + " = 0";
        String[] selectionArgs = {String.valueOf(userId), today};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_LOCATIONS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        boolean hasUnregistered = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        
        return hasUnregistered;
    }
}
