package com.example.studentsattendance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "student_attendance.db";
    private static final int DATABASE_VERSION = 3;
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_PROGRAM = "program";
    private static final String COLUMN_STUDENT_ID = "student_id";
    private static final String COLUMN_BARCODE_URI = "barcode_uri";
    private static final String COLUMN_PROFILE_COMPLETED = "profile_completed";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
                COLUMN_PASSWORD + " TEXT NOT NULL, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_PROGRAM + " TEXT, " +
                COLUMN_STUDENT_ID + " TEXT, " +
                COLUMN_BARCODE_URI + " TEXT, " +
                COLUMN_PROFILE_COMPLETED + " INTEGER DEFAULT 0)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public boolean checkUserExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_EMAIL}, 
                COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean createUser(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean validateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_EMAIL}, 
                COLUMN_EMAIL + "=? AND " + COLUMN_PASSWORD + "=?", 
                new String[]{email, password}, null, null, null);
        boolean valid = cursor.getCount() > 0;
        cursor.close();
        return valid;
    }

    public UserProfile getUserProfile(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_EMAIL + "=?", 
                new String[]{email}, null, null, null);
        UserProfile profile = null;
        if (cursor.moveToFirst()) {
            profile = new UserProfile(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROGRAM)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE_URI)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_COMPLETED)) == 1
            );
        }
        cursor.close();
        return profile;
    }

    public boolean updateProfile(String email, String name, String program, String studentId, String barcodeUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PROGRAM, program);
        values.put(COLUMN_STUDENT_ID, studentId);
        values.put(COLUMN_BARCODE_URI, barcodeUri);
        boolean allFieldsFilled = name != null && !name.isEmpty() &&
                                  program != null && !program.isEmpty() && 
                                  studentId != null && !studentId.isEmpty() && 
                                  barcodeUri != null && !barcodeUri.isEmpty();
        values.put(COLUMN_PROFILE_COMPLETED, allFieldsFilled ? 1 : 0);
        int rows = db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
        return rows > 0;
    }

    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, newPassword);
        int rows = db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
        return rows > 0;
    }

    public static class UserProfile {
        public String email;
        public String name;
        public String program;
        public String studentId;
        public String barcodeUri;
        public boolean profileCompleted;

        public UserProfile(String email, String name, String program, String studentId, String barcodeUri, boolean profileCompleted) {
            this.email = email;
            this.name = name != null ? name : "";
            this.program = program != null ? program : "";
            this.studentId = studentId != null ? studentId : "";
            this.barcodeUri = barcodeUri != null ? barcodeUri : "";
            this.profileCompleted = profileCompleted;
        }
    }
}
