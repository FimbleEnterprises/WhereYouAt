package com.fimbleenterprises.whereuat.generic_objs;

import android.location.Location;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Since the stock Android Location class is not serializable this class can act as a serializable alternative.
 */
public class MyLocation {

    private static final String TAG = "MyLocation";

    public double lat;
    public double lon;
    public float accuracy;
    public long time;
    public String provider;
    public double altitude;
    public float bearing;
    public float bearingAccuracyDegrees;
    public float velocity;
    public float speedAccuracyInMps;
    public float verticalAccuracyInMeters;
    public boolean isMock;
    public boolean hasAccuracy;
    public boolean hasSpeed;
    public boolean hasAltitude;
    public boolean hasBearing;
    public boolean hasBearingAccuracy;
    public boolean hasSpeedAccuracy;
    public boolean hasVerticalAccuracy;

    public static MyLocation fromGson(String json) {
        return new Gson().fromJson(json, MyLocation.class);
    }

    /**
     * Constructs a MyLocation object from a stock Android Location object.
     * @param location A stock Android Location object.
     */
    public MyLocation(Location location) {
        this.lat = location.getLatitude();
        this.lon = location.getLongitude();
        this.accuracy = location.getAccuracy();
        this.time = location.getTime();
        this.provider = location.getProvider();
        this.altitude = location.getAltitude();
        this.bearing = location.getBearing();
        this.bearingAccuracyDegrees = location.getBearingAccuracyDegrees();
        this.velocity = location.getSpeed();
        this.speedAccuracyInMps = location.getSpeedAccuracyMetersPerSecond();
        this.verticalAccuracyInMeters = location.getVerticalAccuracyMeters();
        this.isMock = location.isFromMockProvider();

        this.hasAccuracy = location.hasAccuracy();
        this.hasSpeed = location.hasSpeed();
        this.hasAltitude = location.hasAltitude();
        this.hasBearing = location.hasBearing();
        this.hasBearingAccuracy = location.hasBearingAccuracy();
        this.hasSpeedAccuracy = location.hasSpeedAccuracy();
    }

    public MyLocation(String serverJson) {
        Log.i(TAG, "MyLocation ");

        /*
        	"accuracy": 76.585,
	"altitude": 265.40286290749108,
	"bearing": 327.4325,
	"bearingAccuracyDegrees": 96.75965,
	"hasAccuracy": true,
	"hasAltitude": true,
	"hasBearing": true,
	"hasBearingAccuracy": true,
	"hasSpeed": true,
	"hasSpeedAccuracy": true,
	"hasVerticalAccuracy": false,
	"isMock": false,
	"lat": 45.007354,
	"lon": -93.4594651,
	"provider": "fused",
	"speed": 0.120853364,
	"speedAccuracyInMps": 4.0660667,
	"time": 1624299509644,
	"verticalAccuracyInMeters": 3.0
         */

        try {
            JSONObject json = new JSONObject(serverJson);

            try {
                if (!json.isNull("speedAccuracyInMps")) {
                    this.speedAccuracyInMps = (json.getLong("speedAccuracyInMps"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
               if (!json.isNull("time")) {
                   this.time = json.getLong("time");
               }
            } catch (JSONException e) {
               e.printStackTrace();
            }

            try {
                if (!json.isNull("verticalAccuracyInMeters")) {
                    this.verticalAccuracyInMeters = (json.getLong("verticalAccuracyInMeters"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
               if (!json.isNull("speed")) {
                   this.velocity = json.getLong("speed");
               }
            } catch (JSONException e) {
               e.printStackTrace();
            }

            try {
                if (!json.isNull("provider")) {
                    this.provider = (json.getString("provider"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                if (!json.isNull("accuracy")) {
                    this.accuracy = (json.getLong("accuracy"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        try {
            if (!json.isNull("altitude")) {
                this.altitude = (json.getLong("altitude"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (!json.isNull("bearing")) {
                this.bearing = (json.getLong("bearing"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (!json.isNull("bearingAccuracyDegrees")) {
                this.bearingAccuracyDegrees = (json.getLong("bearingAccuracyDegrees"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (!json.isNull("hasAccuracy")) {
                this.hasAccuracy = (json.getBoolean("hasAccuracy"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (!json.isNull("hasAltitude")) {
                this.hasAltitude = (json.getBoolean("hasAltitude"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (!json.isNull("hasBearing")) {
                this.hasBearing = (json.getBoolean("hasBearing"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (!json.isNull("hasBearingAccuracy")) {
                this.hasBearingAccuracy = (json.getBoolean("hasBearingAccuracy"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (!json.isNull("hasSpeedAccuracy")) {
                this.hasSpeedAccuracy = (json.getBoolean("hasSpeedAccuracy"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (!json.isNull("hasSpeed")) {
                this.hasSpeed = (json.getBoolean("hasSpeed"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (!json.isNull("hasVerticalAccuracy")) {
                this.hasVerticalAccuracy = (json.getBoolean("hasVerticalAccuracy"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (!json.isNull("isMock")) {
                this.isMock = (json.getBoolean("isMock"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
           if (!json.isNull("lat")) {
               this.lat = json.getDouble("lat");
           }
        } catch (JSONException e) {
           e.printStackTrace();
        }

        try {
           if (!json.isNull("lon")) {
               this.lon = json.getDouble("lon");
           }
        } catch (JSONException e) {
           e.printStackTrace();
        }



        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Uses Gson() to convert this class to JSON.
     * @return A Gson-created JSON representation of this object.
     */
    public String toJson() {
        return new Gson().toJson(this);
    }

    /**
     * Constructs an Android Location object from this object's properties.
     * @return A Location object.
     */
    public Location toLocation() {
        Location location = new Location(this.provider);
        location.setLatitude(this.lat);
        location.setAccuracy(this.accuracy);
        location.setLongitude(this.lon);
        location.setAltitude(this.altitude);
        location.setBearing(this.bearing);
        location.setBearingAccuracyDegrees(this.bearingAccuracyDegrees);
        location.setSpeed(this.velocity);
        location.setSpeedAccuracyMetersPerSecond(this.speedAccuracyInMps);
        location.setVerticalAccuracyMeters(this.verticalAccuracyInMeters);
        location.setTime(this.time);
        return location;
    }

}
