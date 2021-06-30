package com.fimbleenterprises.whereuat.local_database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.fimbleenterprises.whereuat.helpers.MySettingsHelper;

import java.util.Locale;

public class MySQLiteHelper extends SQLiteOpenHelper {

    private final static String TAG = "MySQLiteHelper.";
    public static final String DATABASE_NAME = "whereyouat.db";
    private static final int DATABASE_VERSION = 6;
    private MySettingsHelper options;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // TABLE NAMES
    //////////////////////////////////////////////////////////////////////////////////////////////
    public static final String TABLE_NAME_MEMBERUPDATES = "TRIPENTRIES";
    public static final String TABLE_NAME_MESSAGES = "TABLE_NAME_MESSAGES";
    public static final String TABLE_NAME_MYLOCATION = "TABLE_NAME_MYLOCATION";
    public static final String TABLE_NAME_PROXIMITY_ALERTS = "TABLE_NAME_PROXIMITY_ALERTS";

    //////////////////////////////////////////////////////////////////////////////////////////////
    // COLUMN NAMES
    //////////////////////////////////////////////////////////////////////////////////////////////
    public static final String COLUMN_ID = "_ID";
    public static final String COLUMN_DTDATETIME = "DTDATETIME"; // Date format: ISO8601
    public static final String COLUMN_JSON = "JSON";
    public static final String COLUMN_USERID = "COLUMN_USERID";
    public static final String COLUMN_TRIPCODE = "tripcode";
    public static final String COLUMN_MISC1 = "MISC1";
    public static final String COLUMN_MISC2 = "MISC2";
    public static final String COLUMN_MISC3 = "MISC3";
    public static final String COLUMN_MISC4 = "MISC4";

    /*public static final String COLUMN_SENDERID = "COLUMN_SENDERID";
    public static final String COLUMN_RECIPIENTID = "COLUMN_RECIPIENTID";
    public static final String COLUMN_MESSAGE = "COLUMN_MESSAGE";
    public static final String COLUMN_ISREAD = "COLUMN_ISREAD";
    public static final String COLUMN_IMAGEURL = "COLUMN_IMAGEURL";*/
    public static final String COLUMN_USER_MESSAGE_AS_JSON = "COLUMN_USER_MESSAGE_AS_JSON";

    public static final String COLUMN_LAT = "COLUMN_LAT";
    public static final String COLUMN_LON = "COLUMN_LON";
    public static final String COLUMN_ACC = "COLUMN_ACC";
    public static final String COLUMN_PROVIDER = "COLUMN_PROVIDER";

    //////////////////////////////////////////////////////////////////////////////////////////////
    // All columns as array
    //////////////////////////////////////////////////////////////////////////////////////////////
    public static final String[] ALL_TRIPENTRY_COLUMNS = {
            COLUMN_ID,
            COLUMN_DTDATETIME,
            COLUMN_JSON,
            COLUMN_TRIPCODE,
            COLUMN_MISC1,
            COLUMN_MISC2,
            COLUMN_MISC3,
            COLUMN_MISC4
    };

    public static final String[] ALL_USER_MESSAGE_COLUMNS = {
            COLUMN_ID,
            COLUMN_TRIPCODE,
            COLUMN_USER_MESSAGE_AS_JSON,
    };

    public static final String[] ALL_MY_LOC_COLUMNS = {
            COLUMN_ID,
            COLUMN_LAT,
            COLUMN_LON,
            COLUMN_ACC,
            COLUMN_PROVIDER,
            COLUMN_DTDATETIME
    };

    public static final String[] ALL_PROXIMITY_ALERT_COLUMNS = {
            COLUMN_ID,
            COLUMN_JSON,
            COLUMN_USERID,
            COLUMN_TRIPCODE
    };

    //////////////////////////////////////////////////////////////////////////////////////////////
    // DATA TYPES
    //////////////////////////////////////////////////////////////////////////////////////////////
    private static final String TYPE_NUMERIC = "NUMERIC";
    private static final String TYPE_INTEGER = "INTEGER";
    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_REAL = "REAL";
    private static final String TYPE_BLOB = "BLOB";

    //////////////////////////////////////////////////////////////////////////////////////////////
    // CREATE TABLE QUERIES
    //////////////////////////////////////////////////////////////////////////////////////////////

    private static final String TRIPENTRIES_TABLE_CREATE_QUERY = "create table " + TABLE_NAME_MEMBERUPDATES + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_DTDATETIME + " numeric not null, " +
            COLUMN_JSON + " text not null, " +
            COLUMN_TRIPCODE + " text, " +
            COLUMN_MISC1 + " text, " +
            COLUMN_MISC2 + " text, " +
            COLUMN_MISC3 + " text, " +
            COLUMN_MISC4 + " text); ";

    private static final String MY_LOC_TABLE_CREATE_QUERY = "create table " + TABLE_NAME_MYLOCATION + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_LAT + " numeric not null, " +
            COLUMN_LON + " numeric not null, " +
            COLUMN_ACC + " numeric not null, " +
            COLUMN_DTDATETIME + " numeric not null, " +
            COLUMN_PROVIDER + " text); ";

    private static final String MESSAGES_TABLE_CREATE_QUERY = "create table " + TABLE_NAME_MESSAGES + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_DTDATETIME + " text, " +
            COLUMN_USER_MESSAGE_AS_JSON + " text);";

    private static final String PROXIMITY_ALERT_TABLE_CREATE_QUERY = "create table " + TABLE_NAME_PROXIMITY_ALERTS + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_USERID + " text, " +
            COLUMN_TRIPCODE + " text, " +
            COLUMN_JSON + " text);";


    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        options = new MySettingsHelper();
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        // database.setVersion(3);
        Log.w(TAG + "onCreate", "Creating the databases...");

        validateTables(database);
        validateColumns(database);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(), "Upgrading database from version " + oldVersion
                + " to " + newVersion);

        validateTables(db);
        validateColumns(db);

        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        Log.d(TAG + "onOpen", "The database has been opened.  Checking that the database contains all necessary tables...");
        db.setLocale(Locale.US);
        Log.i(TAG, "onOpen Database opening.  Local set to: " + Locale.US.getDisplayName());
        // options.setDbPath(db.getPath());

        validateTables(db);
        validateColumns(db);
    }

    /**
     * Hard-coded check of the required application database tables.  Checks that the tables exist and
     * creates them if not.
     * @param db
     */
    private void validateTables(SQLiteDatabase db) {

        Log.i(TAG, "validateTables ************************************");
        Log.i(TAG, "validateTables         VALIDATING DB TABLES");
        Log.i(TAG, "validateTables ************************************");

        try {
            if (! this.tableExists(TABLE_NAME_MEMBERUPDATES, db)) {
                Log.e(TAG + "onOpen", "The trip entries table does not exist.  Will try to create it now.");
                createTable(db, TRIPENTRIES_TABLE_CREATE_QUERY, TABLE_NAME_MEMBERUPDATES);
            } else {
                Log.d(TAG + "onOpen", "The trip entries table exists.  No need to create it.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (! this.tableExists(TABLE_NAME_MESSAGES, db)) {
                Log.e(TAG + "onOpen", "The messages table does not exist.  Will try to create it now.");
                createTable(db, MESSAGES_TABLE_CREATE_QUERY, TABLE_NAME_MESSAGES);
            } else {
                Log.d(TAG + "onOpen", "The messages table exists.  No need to create it.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (! this.tableExists(TABLE_NAME_MESSAGES, db)) {
                Log.e(TAG + "onOpen", "The messages table does not exist.  Will try to create it now.");
                createTable(db, MESSAGES_TABLE_CREATE_QUERY, TABLE_NAME_MESSAGES);
            } else {
                Log.d(TAG + "onOpen", "The messages table exists.  No need to create it.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (! this.tableExists(TABLE_NAME_MYLOCATION, db)) {
                Log.e(TAG + "onOpen", "The local location table does not exist.  Will try to create it now.");
                createTable(db, MY_LOC_TABLE_CREATE_QUERY, TABLE_NAME_MYLOCATION);
            } else {
                Log.d(TAG + "onOpen", "The local location table exists.  No need to create it.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (! this.tableExists(TABLE_NAME_PROXIMITY_ALERTS, db)) {
                Log.e(TAG + "onOpen", "The proximity alerts table does not exist.  Will try to create it now.");
                createTable(db, PROXIMITY_ALERT_TABLE_CREATE_QUERY, TABLE_NAME_PROXIMITY_ALERTS);
            } else {
                Log.d(TAG + "onOpen", "The proximity alerts table exists.  No need to create it.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Validates columns in each table within the database, creating columns that are missing if necessary.
     * This method's contents are hard-coded and will need to be updated if database schema changes.
     * @param db The database to validate.
     */
    private void validateColumns(SQLiteDatabase db) {

        Log.i(TAG, "validateTables ************************************");
        Log.i(TAG, "validateTables       VALIDATING DB COLUMNS");
        Log.i(TAG, "validateTables ************************************");

        // Trip entries
        addColumnIfMissing(TABLE_NAME_MEMBERUPDATES, COLUMN_ID, TYPE_INTEGER, db);
        addColumnIfMissing(TABLE_NAME_MEMBERUPDATES, COLUMN_DTDATETIME, TYPE_NUMERIC, db);
        addColumnIfMissing(TABLE_NAME_MEMBERUPDATES, COLUMN_MISC1, TYPE_REAL, db);
        addColumnIfMissing(TABLE_NAME_MEMBERUPDATES, COLUMN_MISC2, TYPE_REAL, db);
        addColumnIfMissing(TABLE_NAME_MEMBERUPDATES, COLUMN_MISC3, TYPE_REAL, db);
        addColumnIfMissing(TABLE_NAME_MEMBERUPDATES, COLUMN_MISC4, TYPE_REAL, db);
        addColumnIfMissing(TABLE_NAME_MEMBERUPDATES, COLUMN_TRIPCODE, TYPE_REAL, db);

        // Messages
        /*addColumnIfMissing(TABLE_NAME_MESSAGES, COLUMN_SENDERID, TYPE_TEXT, db);
        addColumnIfMissing(TABLE_NAME_MESSAGES, COLUMN_RECIPIENTID, TYPE_TEXT, db);
        addColumnIfMissing(TABLE_NAME_MESSAGES, COLUMN_DTDATETIME, TYPE_TEXT, db);
        addColumnIfMissing(TABLE_NAME_MESSAGES, COLUMN_MESSAGE, TYPE_TEXT, db);
        addColumnIfMissing(TABLE_NAME_MESSAGES, COLUMN_IMAGEURL, TYPE_TEXT, db);
        addColumnIfMissing(TABLE_NAME_MESSAGES, COLUMN_ISREAD, TYPE_INTEGER, db);*/
        addColumnIfMissing(TABLE_NAME_MESSAGES, COLUMN_ID, TYPE_INTEGER, db);
        addColumnIfMissing(TABLE_NAME_MESSAGES, COLUMN_TRIPCODE, TYPE_TEXT, db);
        addColumnIfMissing(TABLE_NAME_MESSAGES, COLUMN_USER_MESSAGE_AS_JSON, TYPE_TEXT, db);

        // My location table
        addColumnIfMissing(TABLE_NAME_MYLOCATION, COLUMN_ID, TYPE_INTEGER, db);
        addColumnIfMissing(TABLE_NAME_MYLOCATION, COLUMN_LAT, TYPE_REAL, db);
        addColumnIfMissing(TABLE_NAME_MYLOCATION, COLUMN_LON, TYPE_REAL, db);
        addColumnIfMissing(TABLE_NAME_MYLOCATION, COLUMN_ACC, TYPE_REAL, db);
        addColumnIfMissing(TABLE_NAME_MYLOCATION, COLUMN_DTDATETIME, TYPE_REAL, db);
        addColumnIfMissing(TABLE_NAME_MYLOCATION, COLUMN_PROVIDER, TYPE_TEXT, db);

        // Proximity alerts table
        addColumnIfMissing(TABLE_NAME_PROXIMITY_ALERTS, COLUMN_ID, TYPE_INTEGER, db);
        addColumnIfMissing(TABLE_NAME_PROXIMITY_ALERTS, COLUMN_JSON, TYPE_TEXT, db);
        addColumnIfMissing(TABLE_NAME_PROXIMITY_ALERTS, COLUMN_USERID, TYPE_TEXT, db);
        addColumnIfMissing(TABLE_NAME_PROXIMITY_ALERTS, COLUMN_TRIPCODE, TYPE_TEXT, db);


    }

    /**
     * Executes the proper command to create a table in the database stipulated.
     * @param database The db to modify
     * @param query The create table SQL query (e.g. create table MYTABLE (_mycolumn_name_ integer not null...)
     * @param tableName
     */
    private void createTable(SQLiteDatabase database, String query, String tableName) {
        try {
            database.execSQL(query);
            if (tableExists(tableName, database)) {
                Log.d("createTable", "The '" + tableName + "' table was created");
            } else {
                Log.e( "createTable", "Failed to create the '" + tableName + "' table");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the table name exists in the database.
     * @param tableName The table to search for
     * @param mDatabase The database within which to search.
     * @return Result as t/f
     */
    private boolean tableExists(String tableName, SQLiteDatabase mDatabase) {
        Cursor cursor = mDatabase.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name ='" + tableName + "'", null);
        if(cursor!=null) {
            if(cursor.getCount()>0) {
                cursor.close();
                Log.d("tableExists", "The " + tableName + " table exists");
                return true;
            }
            cursor.close();
        }
        Log.w("tableExists", "The " + tableName + " table doesn't exist!");
        return false;
    }

    /**
     * This method will check if column exists in your table
     * @param columnName The column to search for.
     * @param tableName The table within which to search.
     * @param db The database containing all of the stuffz
     * @return t/f
     */
    private boolean columnExists(String columnName, String tableName, SQLiteDatabase db)
    {
        boolean isExist = false;
        Cursor res = db.rawQuery("PRAGMA table_info("+tableName+")",null);
        if (res != null) {
            res.moveToFirst();
            do {
                String currentColumn = res.getString(1);
                if (currentColumn.equals(columnName)) {
                    isExist = true;
                }
            } while (res.moveToNext());
            res.close();
        }
        return isExist;
    }

    /**
     * Evaluates whether a column exists in a table and adds it if not.
     * @param tableName The table to search
     * @param columnName The column to search
     * @param dataType The data type of the column in case it must be created.
     * @param db The database to search.
     */
    private void addColumnIfMissing(String tableName, String columnName, String dataType, SQLiteDatabase db) {
        if ( ! columnExists(columnName, tableName, db)) {
            db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + dataType);
            Log.i(TAG, "addColumnIfMissing:: Added column: (" + columnName + ")");
        } else {
            Log.i(TAG, "addColumnIfMissing:: Column already exists (" + columnName + ")");
        }
    }

}
