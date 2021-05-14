package com.fimbleenterprises.whereuat.local_database;

import android.database.Cursor;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.joda.time.DateTime;

import java.util.ArrayList;

public class LocalUserLocation {

    private static final String TAG = "LocalUserLocation";

    private int id;
    public double lat;
    public double lon;
    public long datetime;
    public String provider;
    public float accuracty;

    public LocalUserLocation() { }

    public LocalUserLocation(Location location) {
        this.datetime = location.getTime();
        this.lat = location.getLatitude();
        this.lon = location.getLongitude();
        this.provider = location.getProvider();
        this.accuracty = location.getAccuracy();
    }

    /**
     * Constructor using a database row.
     * @param c
     */
    public LocalUserLocation(Cursor c) {

        Log.i(TAG, "LocalUserLocation - Building local loc from cursor row.");

        if (c.getCount() == 0) {
            return;
        }

        try {
            while (c.moveToFirst()) {
                int id_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_ID);
                int lat_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_LAT);
                int lon_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_LON);
                int acc_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_ACC);
                int datetime_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_DTDATETIME);
                int provider_Index = c.getColumnIndex(MySQLiteHelper.COLUMN_PROVIDER);

                this.id = c.getInt(id_Index);
                this.lat = c.getDouble(lat_Index);
                this.lon = c.getDouble(lon_Index);
                this.accuracty = c.getFloat(acc_Index);
                this.provider = c.getString(provider_Index);
                this.datetime = c.getLong(datetime_Index);

                Log.i(TAG, "LocalUserLocation | Done.  Built.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LatLng toLatLng() {
        return new LatLng(this.lat, this.lon);
    }
    
    public static ArrayList<LocalUserLocation> getAllLocalLocations(Cursor c) {
        TripDatasource ds = new TripDatasource();
        return ds.getAllLocalLocations(200);
    }

    public DateTime getPrettyDate() {
        return new DateTime(this.datetime);
    }

    public void setDate(DateTime date) {
        this.datetime = date.getMillis();
    }

    public boolean saveToLocalDb() {
        TripDatasource ds = new TripDatasource();
        return ds.saveLocalUserLocation(this);
    }

}
