package com.example.studentsattendance.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "StudentAttendance.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_LOCATIONS = "locations";
    public static final String TABLE_ATTENDANCE_LOGS = "attendance_logs";
    public static final String TABLE_USER_SETTINGS = "user_settings";
    
    // Common columns
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_CREATED_AT = "created_at";
    
    // Users table columns
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_STUDENT_ID = "student_id";
    public static final String COLUMN_PROGRAM = "program";
    public static final String COLUMN_BANNER_ID_IMAGE = "banner_id_image";
    
    // Locations table columns
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_IS_REGISTERED = "is_registered";
    public static final String COLUMN_DATE = "date";
    
    // Attendance logs table columns
    public static final String COLUMN_REGISTRATION_DATE = "registration_date";
    public static final String COLUMN_REGISTRATION_TIME = "registration_time";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_IS_MANUAL = "is_manual";
    
    // User settings table columns
    public static final String COLUMN_SETTING_KEY = "setting_key";
    public static final String COLUMN_SETTING_VALUE = "setting_value";
    
    // Create tables SQL
    private static final String CREATE_USERS_TABLE = 
        "CREATE TABLE " + TABLE_USERS + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
        COLUMN_PASSWORD + " TEXT NOT NULL, " +
        COLUMN_STUDENT_ID + " TEXT, " +
        COLUMN_PROGRAM + " TEXT, " +
        COLUMN_BANNER_ID_IMAGE + " TEXT, " +
        COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
        ")";
    
    private static final String CREATE_LOCATIONS_TABLE = 
        "CREATE TABLE " + TABLE_LOCATIONS + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_USER_ID + " INTEGER NOT NULL, " +
        COLUMN_LATITUDE + " REAL NOT NULL, " +
        COLUMN_LONGITUDE + " REAL NOT NULL, " +
        COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        COLUMN_DATE + " TEXT NOT NULL, " +
        COLUMN_IS_REGISTERED + " INTEGER DEFAULT 0, " +
        "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")" +
        ")";
    
    private static final String CREATE_ATTENDANCE_LOGS_TABLE = 
        "CREATE TABLE " + TABLE_ATTENDANCE_LOGS + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_USER_ID + " INTEGER NOT NULL, " +
        COLUMN_REGISTRATION_DATE + " TEXT NOT NULL, " +
        COLUMN_REGISTRATION_TIME + " TEXT NOT NULL, " +
        COLUMN_STATUS + " TEXT NOT NULL, " +
        COLUMN_MESSAGE + " TEXT, " +
        COLUMN_IS_MANUAL + " INTEGER DEFAULT 0, " +
        COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
        "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")" +
        ")";
    
    private static final String CREATE_USER_SETTINGS_TABLE = 
        "CREATE TABLE " + TABLE_USER_SETTINGS + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_USER_ID + " INTEGER NOT NULL, " +
        COLUMN_SETTING_KEY + " TEXT NOT NULL, " +
        COLUMN_SETTING_VALUE + " TEXT, " +
        "UNIQUE(" + COLUMN_USER_ID + ", " + COLUMN_SETTING_KEY + "), " +
        "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")" +
        ")";
    
    private static DatabaseHelper instance;
    
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_LOCATIONS_TABLE);
        db.execSQL(CREATE_ATTENDANCE_LOGS_TABLE);
        db.execSQL(CREATE_USER_SETTINGS_TABLE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_SETTINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }
}
