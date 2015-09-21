package com.photosynq.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.photosynq.app.UpdateTableForDatabaseInterface;

/*
 * Created by Kevin on 7/17/2015.
 */
public class LocationDatabaseHelper extends SQLiteOpenHelper {
    //Database Version
    private static final int DATABASE_VERSION = 6;
    //Database Name
    private static final String DATABASE_NAME = "WaypointDB";
    //Table Name
    private static final String WAYPOINT_TABLE = "Waypoints";
    private static final String WAYPOINT_TABLE_NEW = "Waypoints_new";
    //Column Names
    private static final String WAYPOINT_COLUMN_ID = "ID";
    private static final String WAYPOINT_COLUMN_NAME = "name";
    private static final String WAYPOINT_COLUMN_LATITUDE = "latitude";
    private static final String WAYPOINT_COLUMN_LONGITUDE = "longitude";
    private static final String WAYPOINT_COLUMN_USER_ANSWERS = "user";
    private static final String WAYPOINT_COLUMN_LARGE_FILEPATH = "filepath_large";
    //Create Waypoint Table
    private static final String CREATE_TABLE_WAYPOINTS = "CREATE TABLE "
            + WAYPOINT_TABLE + "(" + WAYPOINT_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + WAYPOINT_COLUMN_NAME + " TEXT, "
            + WAYPOINT_COLUMN_LATITUDE + " TEXT, " + WAYPOINT_COLUMN_LONGITUDE + " TEXT, " + WAYPOINT_COLUMN_USER_ANSWERS + " TEXT, " +
            WAYPOINT_COLUMN_LARGE_FILEPATH + " TEXT);";
    private static final String CREATE_TABLE_WAYPOINTS_NEW = "CREATE TABLE "
            + WAYPOINT_TABLE_NEW + "(" + WAYPOINT_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + WAYPOINT_COLUMN_NAME + " TEXT, "
            + WAYPOINT_COLUMN_LATITUDE + " TEXT, " + WAYPOINT_COLUMN_LONGITUDE + " TEXT, " + WAYPOINT_COLUMN_USER_ANSWERS + " TEXT, " +
            WAYPOINT_COLUMN_LARGE_FILEPATH + " TEXT);";

    private Context context;

    //callback interface
    private UpdateTableForDatabaseInterface tableInterface;

    private LocationDatabaseHelper(Context context) {
        super(context,DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public void setCallback(UpdateTableForDatabaseInterface tableInterface){
        this.tableInterface = tableInterface;
    }

    private static LocationDatabaseHelper instance;

    public static synchronized LocationDatabaseHelper getHelper(Context context) {
        if (instance == null)
            instance = new LocationDatabaseHelper(context);


        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_WAYPOINTS);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LocationDatabaseHelper.class.getName(), "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + WAYPOINT_TABLE);
        onCreate(db);
    }

    public boolean insertWaypoint(String name, String latitude, String longitude, String user_answers, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WAYPOINT_COLUMN_NAME, name);
        contentValues.put(WAYPOINT_COLUMN_LATITUDE, latitude);
        contentValues.put(WAYPOINT_COLUMN_LONGITUDE, longitude);
        contentValues.put(WAYPOINT_COLUMN_USER_ANSWERS, user_answers);
        contentValues.put(WAYPOINT_COLUMN_LARGE_FILEPATH, filePath);
        db.insert(WAYPOINT_TABLE, null, contentValues);
        return true;
    }

    public boolean updateUserAnswersToFilePath(String filePath, int position){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WAYPOINT_COLUMN_USER_ANSWERS, filePath);
        db.update(WAYPOINT_TABLE, contentValues, WAYPOINT_COLUMN_ID + "=" + String.valueOf(position), null);
        tableInterface.myUpdateTableForDatabaseInterface();
        return true;
    }

    public boolean updateLargeFilePath(String filePath, int position){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WAYPOINT_COLUMN_LARGE_FILEPATH, filePath);
        db.update(WAYPOINT_TABLE, contentValues, WAYPOINT_COLUMN_ID + "=" + String.valueOf(position), null);
        return true;
    }

    public boolean setOneWaypointLatLong(int position, int id, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        if(id == 1){
            contentValues.put(WAYPOINT_COLUMN_LATITUDE, value);
        }
        else{
            contentValues.put(WAYPOINT_COLUMN_LONGITUDE, value);
        }
        db.update(WAYPOINT_TABLE, contentValues, WAYPOINT_COLUMN_ID + "=" + String.valueOf(position), null);
        tableInterface.myUpdateTableForDatabaseInterface();
        return true;
    }

    public boolean updateName(int position, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(WAYPOINT_COLUMN_NAME, value);
        db.update(WAYPOINT_TABLE, contentValues, WAYPOINT_COLUMN_ID+ "=" + String.valueOf(position), null);
        tableInterface.myUpdateTableForDatabaseInterface();
        return true;
    }

    public Cursor getAllWaypoints() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(WAYPOINT_TABLE, null, null, null, null, null, null);
    }

    public Cursor getOneWaypointLat(int position) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(WAYPOINT_TABLE, new String[]{WAYPOINT_COLUMN_LATITUDE},
                WAYPOINT_COLUMN_ID + "=" + String.valueOf(position), null, null, null, null);
    }

    public Cursor getOneWaypointLong(int position) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(WAYPOINT_TABLE, new String[] {WAYPOINT_COLUMN_LONGITUDE},
                WAYPOINT_COLUMN_ID + "=" + String.valueOf(position), null, null, null, null);
    }

    public Cursor getOneWaypointId(double latitude){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(WAYPOINT_TABLE, new String[]{WAYPOINT_COLUMN_ID}, WAYPOINT_COLUMN_LATITUDE
                + "=" + String.valueOf(latitude), null, null, null, WAYPOINT_COLUMN_ID + " ASC");
    }

    public Cursor getOneWaypointFilePath(int position){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(WAYPOINT_TABLE, new String[] {WAYPOINT_COLUMN_USER_ANSWERS}, WAYPOINT_COLUMN_ID
                + "=" + String.valueOf(position), null, null, null, null);
    }

    public Cursor getOneWaypointLargeFilePath(int Id){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(WAYPOINT_TABLE, new String[] {WAYPOINT_COLUMN_LARGE_FILEPATH}, WAYPOINT_COLUMN_ID
                + "=" + String.valueOf(Id), null, null, null, null);
    }

    public Cursor getOneWaypointName(int position){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(WAYPOINT_TABLE, new String[]{WAYPOINT_COLUMN_NAME}, WAYPOINT_COLUMN_ID
                + "=" + String.valueOf(position), null, null, null, WAYPOINT_COLUMN_ID + " ASC");
    }

    public boolean deleteWaypoint(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(WAYPOINT_TABLE,WAYPOINT_COLUMN_ID + "=" + id,null) > 0;
    }

    public void idReset(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + WAYPOINT_TABLE + "'");
        db.execSQL("DROP TABLE IF EXISTS " + WAYPOINT_TABLE_NEW);
        db.execSQL(CREATE_TABLE_WAYPOINTS_NEW);
        db.execSQL("INSERT INTO " + WAYPOINT_TABLE_NEW + "(" + WAYPOINT_COLUMN_NAME + ", " + WAYPOINT_COLUMN_LATITUDE
                + ", " + WAYPOINT_COLUMN_LONGITUDE + ", " + WAYPOINT_COLUMN_USER_ANSWERS + ", " + WAYPOINT_COLUMN_LARGE_FILEPATH + ")" + " SELECT "
                + WAYPOINT_COLUMN_NAME + ", " + WAYPOINT_COLUMN_LATITUDE + ", "
                + WAYPOINT_COLUMN_LONGITUDE + ", " + WAYPOINT_COLUMN_USER_ANSWERS + ", " + WAYPOINT_COLUMN_LARGE_FILEPATH
                        + " FROM " + WAYPOINT_TABLE
        );
        db.execSQL("DROP TABLE IF EXISTS " + WAYPOINT_TABLE);
        db.execSQL("ALTER TABLE " + WAYPOINT_TABLE_NEW + " RENAME TO " + WAYPOINT_TABLE );

    }

    public void deleteTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + WAYPOINT_TABLE);
        db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + WAYPOINT_TABLE + "'");
//        onCreate(db);
    }

}
