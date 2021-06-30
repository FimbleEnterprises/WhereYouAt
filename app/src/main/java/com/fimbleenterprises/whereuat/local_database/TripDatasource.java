package com.fimbleenterprises.whereuat.local_database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.generic_objs.ProximityAlert;
import com.fimbleenterprises.whereuat.generic_objs.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.ALL_PROXIMITY_ALERT_COLUMNS;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.ALL_TRIPENTRY_COLUMNS;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.ALL_USER_MESSAGE_COLUMNS;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.COLUMN_ACC;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.COLUMN_DTDATETIME;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.COLUMN_ID;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.COLUMN_JSON;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.COLUMN_PROVIDER;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.COLUMN_TRIPCODE;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.COLUMN_USERID;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.COLUMN_USER_MESSAGE_AS_JSON;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.TABLE_NAME_MEMBERUPDATES;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.TABLE_NAME_MESSAGES;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.TABLE_NAME_MYLOCATION;
import static com.fimbleenterprises.whereuat.local_database.MySQLiteHelper.TABLE_NAME_PROXIMITY_ALERTS;

public class TripDatasource {

    // Database fields
    private static SQLiteDatabase database;
    private MySQLiteHelper dbHelper;
    Context context;

    public static final String TAG = "MySqlDatasource";

    public TripDatasource() {
        this.context = MyApp.getAppContext();
        dbHelper = new MySQLiteHelper(this.context);
        this.open();
    }

    public TripDatasource(Context context) {
        dbHelper = new MySQLiteHelper(context);
        this.context = context;
        this.open();
    }

    public void open() throws SQLException {
        if (database == null || !database.isOpen()) {
            database = dbHelper.getWritableDatabase();
            database.setLocale(Locale.US);
        }
    }

    public void close() {
        dbHelper.close();
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    boolean columnExists(int colIndex, Cursor cursor) {
        try {
            String name = cursor.getColumnName(colIndex);
            return (name != null && name.length() > 0);
        } catch (Exception e) {
            Log.w(TAG, "columnExists: column index: " + colIndex + " does not exist");
            return false;
        }
    }

    boolean columnExists(String colName, Cursor cursor) {
        try {
            String[] columnNames = cursor.getColumnNames();
            for (String name : columnNames) {
                if (colName.equals(name)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.w(TAG, "columnExists: column index: " + colName + " does not exist");
            return false;
        }
    }

    int getColumnIndex(String colName, Cursor cursor) {
        int index = cursor.getColumnIndex(colName);
        return index;
    }

    /**
     * Appends a received user message to the local device's database.
     * @param message The received, deserialized UserMessage object.
     * @return Boolean indicating success or failure.
     */
    public boolean appendUserMessage(UserMessage message) {
        boolean result;
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_TRIPCODE, message.tripcode);
            values.put(COLUMN_USER_MESSAGE_AS_JSON, message.toGson());

            result = (database.insert(MySQLiteHelper.TABLE_NAME_MESSAGES, null, values) > 0);
            Log.i(TAG, "appendUserMessage | Result: " + result);

        } catch (SQLException e) {
            result = false;
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Retrieves all user messages from oldest to newest for a particular trip code.
     * @param tripcode The tripcode to constrain the query with.
     * @return An arraylist of UserMessage objects.
     */
    public ArrayList<UserMessage> getAllUserMessages(String tripcode) {

        ArrayList<UserMessage> allUserMessages = new ArrayList<>();

        String whereClause = COLUMN_TRIPCODE + " = ?";
        String[] whereArgs = {String.valueOf(tripcode)};

        Cursor c = database.query(TABLE_NAME_MESSAGES, ALL_USER_MESSAGE_COLUMNS, whereClause, whereArgs,
                null, null, COLUMN_ID + " asc");

        try {
            while (c.moveToNext()) {
                int colIndex = c.getColumnIndex(COLUMN_USER_MESSAGE_AS_JSON);
                String json = c.getString(colIndex);
                UserMessage msg = new UserMessage(json);
                allUserMessages.add(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return allUserMessages;
    }

    /**
     * Removes all messages from the database associated with the supplied trip code.
     * @param tripcode
     * @return
     */
    public boolean deleteAllLocalMessages(String tripcode) {
        boolean result;

        String whereClause = COLUMN_TRIPCODE + " = ?";
        String[] whereArgs = { tripcode };
        result = (database.delete(TABLE_NAME_MESSAGES, whereClause, whereArgs)) > 0;
        Log.i(TAG, "deleteMemberUpdatesByTripcode " + result);
        return result;
    }

    public boolean insertMemberLocations(TripReport tripReport) {
        boolean result = true;
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_JSON, tripReport.toJson());
            values.put(MySQLiteHelper.COLUMN_TRIPCODE, tripReport.tripcode);
            values.put(COLUMN_DTDATETIME, tripReport.getInitiatedOnUtc().toString());

            result = (database.insert(MySQLiteHelper.TABLE_NAME_MEMBERUPDATES, null, values) > 0);
            Log.i(TAG, "insertMemberLocations - row inserted. (trip: " + tripReport.tripcode + ")");

        } catch (SQLException e) {
            result = false;
            e.printStackTrace();
        }
        Log.i(TAG, "updated full trip " + result);
        return result;
    }

    /**
     * Gets an array of MemberUpdates in the database.  Keep in mind that each row represents a
     * MemberUpdates (plural) object and those objects have individual MemberUpdate objects within them.
     * @param tripcode
     * @return An ArrayList<MemberUpdates>
     */
    public List<TripReport> getAllMemberLocs(String tripcode) {

        ArrayList<TripReport> allMemberUpdates = new ArrayList<>();

        String whereClause = COLUMN_TRIPCODE + " = ?";
        String[] whereArgs = {String.valueOf(tripcode)};

        Cursor c = database.query(TABLE_NAME_MEMBERUPDATES, ALL_TRIPENTRY_COLUMNS, whereClause, whereArgs,
                null, null, COLUMN_DTDATETIME + " desc");

        try {
            while (c.moveToNext()) {
                int colIndex = c.getColumnIndex(COLUMN_JSON);
                String json = c.getString(colIndex);
                TripReport tripReport = TripReport.buildFromLocalJson(json);
                allMemberUpdates.add(tripReport);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return allMemberUpdates;
    }

    /**
     * Gets an array of MemberUpdates in the database.  Keep in mind that each row represents a
     * MemberUpdates (plural) object and those objects have individual MemberUpdate objects within them.
     * @return A MemberUpdate object representing the device user.
     */
    public LocalUserLocation getLastKnownLocation() {
        try {
            Cursor c = database.rawQuery("SELECT * FROM " + TABLE_NAME_MYLOCATION + " order by " + COLUMN_ID + " desc", null);
            c.close();
            return new LocalUserLocation(c);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets all ProximityAlert objects from the database for a given userid.
     * @return An array list of alerts, empty if none found and null on error.
     */
    public ArrayList<ProximityAlert> getAllAlerts() {

        ArrayList<ProximityAlert> alerts = new ArrayList<>();

        Cursor c = database.query(TABLE_NAME_PROXIMITY_ALERTS, ALL_PROXIMITY_ALERT_COLUMNS, null, null,
                null, null, COLUMN_ID + " asc");

        try {
            while (c.moveToNext()) {
                int jsonIndex = c.getColumnIndex(COLUMN_JSON);
                String json = c.getString(jsonIndex);
                ProximityAlert alert = new ProximityAlert(json);
                alerts.add(alert);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return alerts;
    }

    /**
     * Gets all ProximityAlert objects from the database for a given userid.
     * @return An array list of alerts, empty if none found and null on error.
     */
    public ArrayList<ProximityAlert> getAlertsForUser(String userid) {

        ArrayList<ProximityAlert> alerts = new ArrayList<>();

        String whereClause = COLUMN_USERID + " = ?";
        String[] whereArgs = {String.valueOf(userid)};

        Cursor c = database.query(TABLE_NAME_PROXIMITY_ALERTS, ALL_PROXIMITY_ALERT_COLUMNS, whereClause, whereArgs,
                null, null, COLUMN_ID + " asc");

        try {
            while (c.moveToNext()) {
                int jsonIndex = c.getColumnIndex(COLUMN_JSON);
                String json = c.getString(jsonIndex);
                ProximityAlert alert = new ProximityAlert(json);
                alerts.add(alert);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return alerts;
    }

    /**
     * Gets all ProximityAlert objects from the database for a given trip.
     * @return An array list of alerts, empty if none found and null on error.
     */
    public ArrayList<ProximityAlert> getAlertsForTrip(String tripcode) {

        ArrayList<ProximityAlert> alerts = new ArrayList<>();

        String whereClause = COLUMN_TRIPCODE + " = ?";
        String[] whereArgs = {String.valueOf(tripcode)};

        Cursor c = database.query(TABLE_NAME_PROXIMITY_ALERTS, ALL_PROXIMITY_ALERT_COLUMNS, whereClause, whereArgs,
                null, null, COLUMN_ID + " asc");

        try {
            while (c.moveToNext()) {
                int jsonIndex = c.getColumnIndex(COLUMN_JSON);
                String json = c.getString(jsonIndex);
                ProximityAlert alert = new ProximityAlert(json);
                alerts.add(alert);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return alerts;
    }

    public boolean saveAlert(ProximityAlert alert) {
        boolean result = true;
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_JSON, alert.toGson());
            values.put(COLUMN_TRIPCODE, alert.tripcode);
            values.put(COLUMN_USERID, alert.trackedUser.id);

            result = (database.insert(TABLE_NAME_PROXIMITY_ALERTS, null, values) > 0);
            Log.i(TAG, "saveAlert | Result: " + result);

        } catch (SQLException e) {
            result = false;
            e.printStackTrace();
        }
        Log.i(TAG, "updated full trip " + result);
        return result;
    }

    public boolean deleteAlert(int id) {
        boolean result;

        try {
            database.rawQuery("DELETE FROM " + TABLE_NAME_PROXIMITY_ALERTS + " WHERE "
                    + COLUMN_ID + " = " + id, null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int deleteAllUserAlerts(String userid) {
        boolean result;

        String whereClause = COLUMN_USERID + " = ?";
        String[] whereArgs = { userid };

        return (database.delete(TABLE_NAME_PROXIMITY_ALERTS, whereClause, whereArgs));
    }

    public int deleteAllTripAlerts(String tripcode) {
        boolean result;

        String whereClause = COLUMN_TRIPCODE + " = ?";
        String[] whereArgs = { tripcode };

        return (database.delete(TABLE_NAME_PROXIMITY_ALERTS, whereClause, whereArgs));
    }

    public int deleteAlerts() {
        boolean result;
        return (database.delete(TABLE_NAME_PROXIMITY_ALERTS, null, null));
    }

    /**
     * Gets an array of MemberUpdates in the database.  Keep in mind that each row represents a
     * MemberUpdates (plural) object and those objects have individual MemberUpdate objects within them.
     * @return A MemberUpdate object representing the device user.
     */
    public ArrayList<LocalUserLocation> getAllLocalLocations(int count) {
        ArrayList<LocalUserLocation> locations = new ArrayList<>();

        Cursor c = database.rawQuery("SELECT * FROM " + TABLE_NAME_MYLOCATION
                + " order by " + COLUMN_ID + " desc limit " + count, null);

        while (c.moveToNext()) {
            LocalUserLocation localUserLocation = new LocalUserLocation();
            int id_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_ID);
            int lat_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_LAT);
            int lon_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_LON);
            int acc_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_ACC);
            int datetime_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_DTDATETIME);
            int provider_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_PROVIDER);

            localUserLocation.lat = c.getDouble(lat_Index);
            localUserLocation.lon = c.getDouble(lon_Index);
            localUserLocation.accuracy = c.getFloat(acc_Index);
            localUserLocation.provider = c.getString(provider_Index);
            localUserLocation.datetime = c.getLong(datetime_Index);

            locations.add(localUserLocation);
        }

        c.close();

        return locations;
    }

    /**
     * Saves a device location to the local database.
     * @param localUserLocation
     * @return
     */
    public boolean saveLocalUserLocation(LocalUserLocation localUserLocation) {
        boolean result = true;
        try {
            ContentValues values = new ContentValues();
            values.put(MySQLiteHelper.COLUMN_LAT, localUserLocation.lat);
            values.put(MySQLiteHelper.COLUMN_LON, localUserLocation.lon);
            values.put(COLUMN_ACC, localUserLocation.accuracy);
            values.put(COLUMN_DTDATETIME, localUserLocation.datetime);
            values.put(COLUMN_PROVIDER, localUserLocation.provider);

            result = (database.insert(TABLE_NAME_MYLOCATION, null, values) > 0);
            Log.i(TAG, "saveLocalUserLocation | " + result);

        } catch (SQLException e) {
            result = false;
            e.printStackTrace();
        }
        Log.i(TAG, "Local user location was saved." + result);
        return result;
    }

    /**
     * Builds the last x amount of MemberUpdates objects saved to the database in descending order of creation.
     * @param x The amount of objects to return
     * @param tripcode The tripcode to limit results
     * @return An array list of MemberUpdates objects order in descending order of initial creation.
     */
    public ArrayList<TripReport> getLastXmemberUpdates(int x, String tripcode) {

        if (tripcode == null) {
            Log.w(TAG, "getLastXmemberUpdates: | No tripcode was supplied!  Cannot query db without a code!");
            return null;
        }

        ArrayList<TripReport> lastXupdates = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_NAME_MEMBERUPDATES + " WHERE " + COLUMN_TRIPCODE +
                " = ?" + " ORDER BY " + COLUMN_ID + " DESC LIMIT " + x;

        try {

            Cursor c = database.rawQuery(query, new String[] {tripcode});

            while (c.moveToNext()) {
                int colIndex = c.getColumnIndex(COLUMN_JSON);
                String json = c.getString(colIndex);
                lastXupdates.add(new TripReport(json));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return lastXupdates;
    }

    public TripReport getMostRecentMemberLocEntry(String tripcode) {

        String query = "SELECT * FROM " + TABLE_NAME_MEMBERUPDATES + " WHERE " + COLUMN_TRIPCODE +
                " = ?" + " ORDER BY " + COLUMN_ID + " DESC LIMIT 1";

        try {

            Cursor c = database.rawQuery(query, new String[] {tripcode});

            while (c.moveToNext()) {
                int colIndex = c.getColumnIndex(COLUMN_JSON);
                String json = c.getString(colIndex);
                return TripReport.buildFromLocalJson(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

    public boolean deleteMemberUpdatesByTripcode(String tripcode) {
        boolean result;

        String whereClause = COLUMN_TRIPCODE + " = ?";
        String[] whereArgs = { tripcode };
        result = (database.delete(TABLE_NAME_MEMBERUPDATES, whereClause, whereArgs)) > 0;
        Log.i(TAG, "deleteMemberUpdatesByTripcode " + result);
        return result;
    }

    public boolean deleteAllMemberUpdates() {
        boolean result;

        result = (database.delete(TABLE_NAME_MEMBERUPDATES, null, null)) > 0;
        Log.i(TAG, "deleteAllMemberUpdates " + result);
        return result;
    }

}
