package com.example.studentsattendance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "student_attendance.db";
    private static final int DATABASE_VERSION = 4;
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_PROGRAM = "program";
    private static final String COLUMN_STUDENT_ID = "student_id";
    private static final String COLUMN_BARCODE_URI = "barcode_uri";
    private static final String COLUMN_PROFILE_COMPLETED = "profile_completed";
    private static final String COLUMN_MANUAL_REGISTRATION_ONLY = "manual_registration_only";
    
    private static final String TABLE_ATTENDANCE = "attendance";
    private static final String COLUMN_ATTENDANCE_ID = "id";
    private static final String COLUMN_ATTENDANCE_EMAIL = "email";
    private static final String COLUMN_ATTENDANCE_DATE = "date";
    private static final String COLUMN_ATTENDANCE_TIME = "time";
    private static final String COLUMN_ATTENDANCE_REGISTERED_DATE = "registered_date";
    private static final String COLUMN_ATTENDANCE_STATUS = "status";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
                COLUMN_PASSWORD + " TEXT NOT NULL, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_PROGRAM + " TEXT, " +
                COLUMN_STUDENT_ID + " TEXT, " +
                COLUMN_BARCODE_URI + " TEXT, " +
                COLUMN_PROFILE_COMPLETED + " INTEGER DEFAULT 0, " +
                COLUMN_MANUAL_REGISTRATION_ONLY + " INTEGER DEFAULT 0)";
        db.execSQL(createUsersTable);
        
        String createAttendanceTable = "CREATE TABLE " + TABLE_ATTENDANCE + " (" +
                COLUMN_ATTENDANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_ATTENDANCE_EMAIL + " TEXT NOT NULL, " +
                COLUMN_ATTENDANCE_DATE + " TEXT NOT NULL, " +
                COLUMN_ATTENDANCE_TIME + " TEXT NOT NULL, " +
                COLUMN_ATTENDANCE_REGISTERED_DATE + " TEXT, " +
                COLUMN_ATTENDANCE_STATUS + " TEXT DEFAULT 'Not registered', " +
                "UNIQUE(" + COLUMN_ATTENDANCE_EMAIL + ", " + COLUMN_ATTENDANCE_DATE + "))";
        db.execSQL(createAttendanceTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_MANUAL_REGISTRATION_ONLY + " INTEGER DEFAULT 0");
            String createAttendanceTable = "CREATE TABLE IF NOT EXISTS " + TABLE_ATTENDANCE + " (" +
                    COLUMN_ATTENDANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ATTENDANCE_EMAIL + " TEXT NOT NULL, " +
                    COLUMN_ATTENDANCE_DATE + " TEXT NOT NULL, " +
                    COLUMN_ATTENDANCE_TIME + " TEXT NOT NULL, " +
                    COLUMN_ATTENDANCE_REGISTERED_DATE + " TEXT, " +
                    COLUMN_ATTENDANCE_STATUS + " TEXT DEFAULT 'Not registered', " +
                    "UNIQUE(" + COLUMN_ATTENDANCE_EMAIL + ", " + COLUMN_ATTENDANCE_DATE + "))";
            db.execSQL(createAttendanceTable);
        }
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
            int manualRegIdx = cursor.getColumnIndex(COLUMN_MANUAL_REGISTRATION_ONLY);
            boolean manualReg = manualRegIdx != -1 && cursor.getInt(manualRegIdx) == 1;
            profile = new UserProfile(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROGRAM)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDENT_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE_URI)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_COMPLETED)) == 1,
                manualReg
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
        
        // Reload the profile after update to reflect changes
        if (rows > 0 && LoginManager.getCurrentUserProfile() != null && 
            LoginManager.getCurrentUserProfile().email.equals(email)) {
            UserProfile updatedProfile = getUserProfile(email);
            LoginManager.setCurrentUserProfile(updatedProfile);
        }
        
        return rows > 0;
    }

    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, newPassword);
        int rows = db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
        return rows > 0;
    }
    
    public boolean updateManualRegistrationOnly(String email, boolean manualOnly) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MANUAL_REGISTRATION_ONLY, manualOnly ? 1 : 0);
        int rows = db.update(TABLE_USERS, values, COLUMN_EMAIL + "=?", new String[]{email});
        return rows > 0;
    }
    
    public boolean addAttendanceRecord(String email, String date, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ATTENDANCE_EMAIL, email);
        values.put(COLUMN_ATTENDANCE_DATE, date);
        values.put(COLUMN_ATTENDANCE_TIME, time);
        values.put(COLUMN_ATTENDANCE_STATUS, "Not registered");
        long result = db.insertWithOnConflict(TABLE_ATTENDANCE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return result != -1;
    }
    
    public boolean updateAttendanceRegistration(String email, String date, String registeredDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ATTENDANCE_REGISTERED_DATE, registeredDate);
        values.put(COLUMN_ATTENDANCE_STATUS, registeredDate);
        int rows = db.update(TABLE_ATTENDANCE, values, 
                COLUMN_ATTENDANCE_EMAIL + "=? AND " + COLUMN_ATTENDANCE_DATE + "=?", 
                new String[]{email, date});
        return rows > 0;
    }
    
    public Cursor getAllAttendanceRecords(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_ATTENDANCE, null, COLUMN_ATTENDANCE_EMAIL + "=?", 
                new String[]{email}, null, null, COLUMN_ATTENDANCE_DATE + " DESC");
    }
    
    public boolean isAttendanceRecordExists(String email, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, 
                new String[]{COLUMN_ATTENDANCE_DATE}, 
                COLUMN_ATTENDANCE_EMAIL + "=? AND " + COLUMN_ATTENDANCE_DATE + "=?", 
                new String[]{email, date}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
    
    public boolean isAttendanceRegisteredToday(String email, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, 
                new String[]{COLUMN_ATTENDANCE_REGISTERED_DATE}, 
                COLUMN_ATTENDANCE_EMAIL + "=? AND " + COLUMN_ATTENDANCE_DATE + "=?", 
                new String[]{email, date}, null, null, null);
        boolean registered = false;
        if (cursor.moveToFirst()) {
            String regDate = cursor.getString(0);
            registered = regDate != null && !regDate.isEmpty();
        }
        cursor.close();
        return registered;
    }
    
    public int clearAllAttendanceRecords(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_ATTENDANCE, COLUMN_ATTENDANCE_EMAIL + "=?", new String[]{email});
    }
    
    public String getPassword(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_PASSWORD}, 
                COLUMN_EMAIL + "=?", new String[]{email}, null, null, null);
        String password = null;
        if (cursor.moveToFirst()) {
            password = cursor.getString(0);
        }
        cursor.close();
        return password;
    }

    public static class UserProfile {
        public String email;
        public String name;
        public String program;
        public String studentId;
        public String barcodeUri;
        public boolean profileCompleted;
        public boolean manualRegistrationOnly;

        public UserProfile(String email, String name, String program, String studentId, String barcodeUri, boolean profileCompleted, boolean manualRegistrationOnly) {
            this.email = email;
            this.name = name != null ? name : "";
            this.program = program != null ? program : "";
            this.studentId = studentId != null ? studentId : "";
            this.barcodeUri = barcodeUri != null ? barcodeUri : "";
            this.profileCompleted = profileCompleted;
            this.manualRegistrationOnly = manualRegistrationOnly;
        }
    }
}
