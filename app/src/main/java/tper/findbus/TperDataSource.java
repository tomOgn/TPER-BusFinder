package tper.findbus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class TperDataSource
{
    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;
    private final String[] LineFields =
        {
            MySQLiteHelper.LINE_ID
        };
    private final String[] LineUsageFields =
        {
            MySQLiteHelper.LINE_ID,
            MySQLiteHelper.LINE_USAGE
        };
    private final String[] StopFields =
        {
            MySQLiteHelper.STOP_ID,
            MySQLiteHelper.STOP_DENOMINATION,
            MySQLiteHelper.STOP_LATITUDE,
            MySQLiteHelper.STOP_LONGITUDE
        };
    public TperDataSource(Context context)
    {
        //context.deleteDatabase(MySQLiteHelper.DATABASE_NAME);
        dbHelper = new MySQLiteHelper(context);
    }

    public void open() throws SQLException
    {
        database = dbHelper.getWritableDatabase();
    }

    public void close()
    {
        dbHelper.close();
    }

    public void insertLine(String line, int usage)
    {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.LINE_ID, line);
        values.put(MySQLiteHelper.LINE_USAGE, usage);
        database.insert(MySQLiteHelper.TABLE_LINES, null, values);
    }

    public void insertStop(Stop stop)
    {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.STOP_ID, stop.Code);
        values.put(MySQLiteHelper.STOP_ZONE, stop.Zone);
        values.put(MySQLiteHelper.STOP_DENOMINATION, stop.Denomination);
        values.put(MySQLiteHelper.STOP_LOCATION, stop.Location);
        values.put(MySQLiteHelper.STOP_MUNICIPALITY, stop.Municipality);
        values.put(MySQLiteHelper.STOP_LATITUDE, stop.Latitude);
        values.put(MySQLiteHelper.STOP_LONGITUDE, stop.Longitude);
        database.insert(MySQLiteHelper.TABLE_STOPS, null, values);
    }

    public void insertPath(String line, int stop)
    {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.PATH_LINE_ID, line);
        values.put(MySQLiteHelper.PATH_STOP_ID, stop);
        database.insert(MySQLiteHelper.TABLE_PATHS, null, values);
    }

    public void incrementLineUsage(String line, int newValue)
    {
        ContentValues value = new ContentValues();
        value.put(MySQLiteHelper.LINE_USAGE, newValue);
        database.update(MySQLiteHelper.TABLE_LINES, value, MySQLiteHelper.LINE_ID + "=" + line, null);
    }

    public void setFavoriteStop(int stop, String line, boolean favorite)
    {
        if (favorite)
        {
            ContentValues values = new ContentValues();
            values.put(MySQLiteHelper.STOP_ID, stop);
            values.put(MySQLiteHelper.LINE_ID, line);
            database.insert(MySQLiteHelper.TABLE_FAVORITES, null, values);
        }
        else
        {
            String whereClause =
                    MySQLiteHelper.STOP_ID + " = " + stop + " and " +
                    MySQLiteHelper.LINE_ID + " = " + line;
            database.delete(MySQLiteHelper.TABLE_FAVORITES, whereClause, null);
        }
    }

    public void deleteFavorite(int stop, String line)
    {
        String whereClause =
                MySQLiteHelper.STOP_ID + " = " + stop + " and " +
                MySQLiteHelper.LINE_ID + " = " + line;
        database.delete(MySQLiteHelper.TABLE_FAVORITES, whereClause, null);
    }

    public void erase()
    {
        database.delete(MySQLiteHelper.TABLE_LINES, null, null);
        database.delete(MySQLiteHelper.TABLE_STOPS, null, null);
        database.delete(MySQLiteHelper.TABLE_PATHS, null, null);
    }

    public void reset()
    {
        database.delete(MySQLiteHelper.TABLE_FAVORITES, null, null);
        ContentValues value = new ContentValues();
        value.put(MySQLiteHelper.LINE_USAGE, 0);
        database.update(MySQLiteHelper.TABLE_LINES, value, null, null);
    }

    public HashMap<String, Integer> getLinesUsage()
    {
        HashMap<String, Integer> lines = new HashMap<>();
        Cursor cursor = database.query(MySQLiteHelper.TABLE_LINES, LineUsageFields, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            String line = cursor.getString(0);
            int usage = cursor.getInt(1);
            lines.put(line, usage);
            cursor.moveToNext();
        }
        cursor.close();
        return lines;
    }

    public ArrayList<String> getLines()
    {
        ArrayList<String> lines = new ArrayList<>();
        Cursor cursor = database.query(MySQLiteHelper.TABLE_LINES, LineFields, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            String line = cursor.getString(0);
            lines.add(line);
            cursor.moveToNext();
        }
        cursor.close();
        return lines;
    }

    public HashMap<Integer, Stop> getFavoritePairsHashMap()
    {
        HashMap<Integer, Stop> favorites = new HashMap<>();
        final String[] columns = { MySQLiteHelper.STOP_ID, MySQLiteHelper.LINE_ID };
        Cursor cursor = database.query(MySQLiteHelper.TABLE_FAVORITES, columns, null, null, null, null, null);
        int stopCode;
        String line;
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            stopCode = cursor.getInt(0);
            line = cursor.getString(1);

            if (!favorites.containsKey(stopCode))
                favorites.put(stopCode, getStop(stopCode));

            favorites.get(stopCode).Favorite.add(line);

            cursor.moveToNext();
        }
        cursor.close();

        return favorites;
    }

    public ArrayList<StopLine> getFavoritePairs()
    {
        ArrayList<StopLine> favorites = new ArrayList<>();

        final String[] columns = { MySQLiteHelper.STOP_ID, MySQLiteHelper.LINE_ID };
        Cursor cursor = database.query(MySQLiteHelper.TABLE_FAVORITES, columns, null, null, null, null, MySQLiteHelper.STOP_ID);

        Stop stop = null;
        int stopCode;
        String line;
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            stopCode = cursor.getInt(0);
            line = cursor.getString(1);

            if (stop == null || stopCode != stop.Code)
                stop = getStop(stopCode);

            StopLine stopLine = new StopLine(stop, line);
            favorites.add(stopLine);

            cursor.moveToNext();
        }
        cursor.close();

        return favorites;
    }

    public Stop getStop(int id)
    {
        Stop stop = new Stop();
        String whereClause = MySQLiteHelper.STOP_ID + " = " + id;

        Cursor cursor = database.query(MySQLiteHelper.TABLE_STOPS, StopFields, whereClause, null, null, null, null);
        cursor.moveToFirst();
        if (!cursor.isAfterLast())
        {
            stop.Code = cursor.getInt(0);
            stop.Denomination = cursor.getString(1);
            stop.Latitude = cursor.getFloat(2);
            stop.Longitude = cursor.getFloat(3);
        }
        cursor.close();

        return stop;
    }

    public ArrayList<String> getFavoriteLines()
    {
        ArrayList<String> lines = new ArrayList<>();

        String whereClause = MySQLiteHelper.LINE_USAGE + " > 0";
        String orderBy = MySQLiteHelper.LINE_USAGE + " DESC";

        Cursor cursor = database.query(MySQLiteHelper.TABLE_LINES, LineFields, whereClause, null, null, null, orderBy);
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            String line = cursor.getString(0);
            lines.add(line);
            cursor.moveToNext();
        }
        cursor.close();

        return lines;
    }

    public ArrayList<Stop> getLineStops(String line)
    {
        ArrayList<Stop> stops = new ArrayList<>();
        Cursor cursor;
        String whereClause;
        String[] tableColumns;

        // Retrieve all the stops touched by the given bus line.
        whereClause = MySQLiteHelper.PATH_LINE_ID + " = " + line;
        tableColumns = new String[]
            {
                    MySQLiteHelper.PATH_STOP_ID
            };
        cursor = database.query(MySQLiteHelper.TABLE_PATHS, tableColumns, whereClause, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Stop stop = new Stop();
            stop.Code = Integer.parseInt(cursor.getString(0));
            stops.add(stop);
            cursor.moveToNext();
        }
        cursor.close();

        // For each stop retrieve its data.
        tableColumns = new String[]
            {
                MySQLiteHelper.STOP_DENOMINATION,
                MySQLiteHelper.STOP_LATITUDE,
                MySQLiteHelper.STOP_LONGITUDE
            };

        for (Stop stop : stops)
        {
            whereClause = MySQLiteHelper.STOP_ID + " = " + stop.Code;
            cursor = database.query(MySQLiteHelper.TABLE_STOPS, tableColumns, whereClause, null, null, null, null);
            cursor.moveToFirst();
            stop.Denomination = cursor.getString(0);
            stop.Latitude = cursor.getFloat(1);
            stop.Longitude = cursor.getFloat(2);
            cursor.close();
        }

        return stops;
    }
}
