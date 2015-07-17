package tper.findbus;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteHelper extends SQLiteOpenHelper
{
    public static final String TABLE_LINES = "lines";
    public static final String TABLE_STOPS = "stops";
    public static final String TABLE_PATHS = "paths";
    public static final String TABLE_FAVORITES = "pairs";
    public static final String LINE_ID = "line_id";
    public static final String LINE_USAGE = "line_usage";
    public static final String STOP_ID = "stop_id";
    public static final String STOP_ZONE = "stop_zone";
    public static final String STOP_DENOMINATION = "stop_denomination";
    public static final String STOP_LOCATION = "stop_location";
    public static final String STOP_MUNICIPALITY = "stop_municipality";
    public static final String STOP_LATITUDE = "stop_latitude";
    public static final String STOP_LONGITUDE = "stop_longitude";
    public static final String PATH_STOP_ID = "stop_id";
    public static final String PATH_LINE_ID = "line_id";
    public static final String DATABASE_NAME = "bus.db";
    public static final int DATABASE_VERSION = 1;

    public static final String CREATE_TABLE_LINES =
            "create table " + TABLE_LINES + " (" +
            LINE_ID + " text primary key, " +
            LINE_USAGE + " integer not null);";
    public static final String CREATE_TABLE_STOPS =
            "create table " + TABLE_STOPS + " (" +
            STOP_ID + " integer primary key, " +
            STOP_ZONE + " integer, " +
            STOP_DENOMINATION + " text, " +
            STOP_LOCATION + " text, " +
            STOP_MUNICIPALITY + " text, " +
            STOP_LATITUDE + " real, " +
            STOP_LONGITUDE + " real);";
    public static final String CREATE_TABLE_PATHS =
            "create table " + TABLE_PATHS + " (" +
            PATH_STOP_ID + " integer not null, " +
            PATH_LINE_ID + " text not null, " +
            "primary key (" + PATH_STOP_ID + ", " + PATH_LINE_ID + "));";
    public static final String CREATE_TABLE_PAIRS =
            "create table " + TABLE_FAVORITES + " (" +
            STOP_ID + " integer not null, " +
            LINE_ID + " text not null, " +
            "primary key (" + STOP_ID + ", " + LINE_ID + "));";

    public MySQLiteHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database)
    {
        database.execSQL(CREATE_TABLE_LINES);
        database.execSQL(CREATE_TABLE_STOPS);
        database.execSQL(CREATE_TABLE_PATHS);
        database.execSQL(CREATE_TABLE_PAIRS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
    {
        database.execSQL("drop table if exists " + TABLE_LINES);
        database.execSQL("drop table if exists " + TABLE_STOPS);
        database.execSQL("drop table if exists " + TABLE_PATHS);
        database.execSQL("drop table if exists " + TABLE_FAVORITES);
        onCreate(database);
    }
}
