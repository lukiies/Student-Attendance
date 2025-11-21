package com.example.studentsattendance.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.studentsattendance.database.DatabaseHelper;
import com.example.studentsattendance.database.models.User;

public class UserDao {
    
    private DatabaseHelper dbHelper;
    
    public UserDao(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }
    
    // Create new user (Sign up)
    public long createUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_EMAIL, user.getEmail());
        values.put(DatabaseHelper.COLUMN_PASSWORD, user.getPassword());
        values.put(DatabaseHelper.COLUMN_STUDENT_ID, user.getStudentId());
        values.put(DatabaseHelper.COLUMN_PROGRAM, user.getProgram());
        
        long userId = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        return userId;
    }
    
    // Authenticate user (Login)
    public User authenticateUser(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] columns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_EMAIL,
            DatabaseHelper.COLUMN_PASSWORD,
            DatabaseHelper.COLUMN_STUDENT_ID,
            DatabaseHelper.COLUMN_PROGRAM,
            DatabaseHelper.COLUMN_BANNER_ID_IMAGE,
            DatabaseHelper.COLUMN_CREATED_AT
        };
        
        String selection = DatabaseHelper.COLUMN_EMAIL + " = ? AND " + 
                          DatabaseHelper.COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {email, password};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL)));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD)));
            user.setStudentId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STUDENT_ID)));
            user.setProgram(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROGRAM)));
            user.setBannerIdImage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BANNER_ID_IMAGE)));
            user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT)));
            cursor.close();
        }
        
        return user;
    }
    
    // Get user by ID
    public User getUserById(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] columns = {
            DatabaseHelper.COLUMN_ID,
            DatabaseHelper.COLUMN_EMAIL,
            DatabaseHelper.COLUMN_PASSWORD,
            DatabaseHelper.COLUMN_STUDENT_ID,
            DatabaseHelper.COLUMN_PROGRAM,
            DatabaseHelper.COLUMN_BANNER_ID_IMAGE,
            DatabaseHelper.COLUMN_CREATED_AT
        };
        
        String selection = DatabaseHelper.COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = new User();
            user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL)));
            user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD)));
            user.setStudentId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STUDENT_ID)));
            user.setProgram(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROGRAM)));
            user.setBannerIdImage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BANNER_ID_IMAGE)));
            user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT)));
            cursor.close();
        }
        
        return user;
    }
    
    // Check if email exists
    public boolean emailExists(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String[] columns = {DatabaseHelper.COLUMN_ID};
        String selection = DatabaseHelper.COLUMN_EMAIL + " = ?";
        String[] selectionArgs = {email};
        
        Cursor cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null
        );
        
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        
        return exists;
    }
    
    // Update user
    public int updateUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_EMAIL, user.getEmail());
        values.put(DatabaseHelper.COLUMN_STUDENT_ID, user.getStudentId());
        values.put(DatabaseHelper.COLUMN_PROGRAM, user.getProgram());
        values.put(DatabaseHelper.COLUMN_BANNER_ID_IMAGE, user.getBannerIdImage());
        
        String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(user.getId())};
        
        return db.update(DatabaseHelper.TABLE_USERS, values, whereClause, whereArgs);
    }
    
    // Update banner ID image
    public int updateBannerIdImage(long userId, String imagePath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_BANNER_ID_IMAGE, imagePath);
        
        String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(userId)};
        
        return db.update(DatabaseHelper.TABLE_USERS, values, whereClause, whereArgs);
    }
}
