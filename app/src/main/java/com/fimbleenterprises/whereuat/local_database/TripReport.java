package com.fimbleenterprises.whereuat.local_database;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.generic_objs.ListObjects;
import com.fimbleenterprises.whereuat.generic_objs.MyLocation;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.helpers.StaticHelpers;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TimeZone;

import androidx.annotation.NonNull;

public class TripReport implements Parcelable {

    private static final String TAG = "MemberUpdates";

    // ************************************************************************
    // RETURNED JSON NODE NAMES
    // ************************************************************************
    private static final String MEMBER_LIST = "list";
    private static final String TRIPCODE = "tripcode";
    private static final String INITIATED_ON = "initiatedon";
    private static final String INITIATED_BY = "initiatedby";
    // ************************************************************************
    // RETURNED JSON NODE NAMES
    // ************************************************************************

    public ArrayList<MemberUpdate> list = new ArrayList<>();
    public String tripcode;
    public String initiatedbygoogleid;
    private String initiatedon;

    public TripReport() { }

    /**
     * Using the pure json string returned by our server's api - construct a new MemberUpdates object.
     * @param serverJson
     */
    public TripReport(String serverJson) {
        try {
            JSONObject root = new JSONObject(serverJson);

            // Get the root properties (tripcode etc.)
            try {
                if (!root.isNull(TRIPCODE)) {
                    this.tripcode = (root.getString(TRIPCODE));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                if (!root.isNull(INITIATED_ON)) {

                    this.initiatedon = new DateTime(root.getString(INITIATED_ON), DateTimeZone.UTC).toString();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                if (!root.isNull(INITIATED_BY)) {
                    this.initiatedbygoogleid = (root.getString(INITIATED_BY));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Parse out the member locs
            JSONArray rootArray = root.getJSONArray(MEMBER_LIST);
            for (int i = 0; i < rootArray.length(); i++) {
                this.list.add(new MemberUpdate(rootArray.getJSONObject(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loops through the arraylist of MemberUpdate objects looking for a matching Google id.
     * @param googleUserid The userid to locate in the list.
     * @return Null if not found.
     */
    public MemberUpdate find(String googleUserid) {
        for (MemberUpdate member : this.list) {
            if (member.userid.equals(googleUserid)) {
                return member;
            }
        }
        return null;
    }

    /**
     * Parses the trip report looking through all MemberUpdate objects scraping out the user info
     * from each entry if its userid property matches the TripReport's initiatedbygoogleid.
     * Using that it builds a GoogleUser object and returns it.
     * @return A GoogleUser object scraped from a TripReport using the TripReport's initiatedbygoogleid property.
     */
    public GoogleUser getInitiator() {
        MemberUpdate mu = this.find(this.initiatedbygoogleid);
        GoogleUser user = new GoogleUser();
        user.email = mu.email;
        user.fullname = mu.displayName;
        user.id = mu.userid;
        user.photourl = mu.photoUrl;
        return user;
    }

    public MemberUpdate findMyUpdate() {
        for (MemberUpdate mem : this.list) {
            if (mem.userid.equals(GoogleUser.getCachedUser().id)) {
                return mem;
            }
        }
        return null;
    }

    public MemberUpdate findMemberUpdate(String userid) {
        for (MemberUpdate mem : this.list) {
            if (mem.userid.equals(userid)) {
                return mem;
            }
        }
        return null;
    }

    /**
     * Calculates the distance (in meters) to the specified location.
     * @param location The arbitrary location to measure to.
     */
    public void calculateMemberDistances(Location location) {
        Log.i(TAG, "calculateMemberDistances | Calculating distances to members...");
        for (MemberUpdate memberUpdate : this.list) {
            memberUpdate.distanceFromMe = memberUpdate.distanceTo(location);
            Log.i(TAG, "calculateMemberDistances | " + memberUpdate.displayName + " " +
                    memberUpdate.distanceFromMe + " meters away.");
        }
    }

    public DateTime getInitiatedOnLocalTime() {
        return new DateTime(this.initiatedon, DateTimeZone.forID(TimeZone.getDefault().getID()));
    }

    public DateTime getInitiatedOnUtc() {
        return new DateTime(this.initiatedon, DateTimeZone.UTC);
    }

    /**
     * Sorts the MemberUpdate array list from closest to furthest using the distanceFromMe property.
     * @throws NullPointerException While the MemberUpdate array can never be null it is treated
     * as such if it has 0 entries.
     */
    public void sortByDistanceFromMeAscending() throws NullPointerException {

        Log.i(TAG, "sortByDistanceFromMeAscending | Sorting member updates from closest to furthest.");

        if (this.list.size() < 1) {
            Log.e("Cannot sort because there are no members to sort!", "ERROR");
            throw new NullPointerException("MemberUpdate list is empty!");
        }

        this.list.sort(new Comparator<MemberUpdate>() {
            @Override
            public int compare(TripReport.MemberUpdate memberUpdate, TripReport.MemberUpdate t1) {
                boolean thisIsCloserThanThat = t1.distanceFromMe < (memberUpdate.distanceFromMe);
                if (thisIsCloserThanThat) {
                    Log.i(TAG, "compare | THIS IS CLOSER THAN THAT");
                    return 1;
                } else {
                    Log.i(TAG, "compare | THAT IS CLOSER THAN THIS");
                    return -1;
                }
            }
        });
    }

    /**
     * Sorts the MemberUpdate array list from furthest to closest using the distanceFromMe property.
     * @throws NullPointerException While the MemberUpdate array can never be null it is treated
     * as such if it has 0 entries.
     */
    public void sortByDistanceFromMeDescending() throws NullPointerException {

        Log.i(TAG, "sortByDistanceFromMeDescending | Sorting member updates from furthest to closest");

        if (this.list.size() < 1) {
            Log.e("Cannot sort because there are no members to sort!", "ERROR");
            throw new NullPointerException("MemberUpdate list is empty!");
        }

        this.list.sort(new Comparator<MemberUpdate>() {
            @Override
            public int compare(TripReport.MemberUpdate memberUpdate, TripReport.MemberUpdate t1) {
                boolean thisIsCloserThanThat = t1.distanceFromMe > (memberUpdate.distanceFromMe);
                if (thisIsCloserThanThat) {
                    Log.i(TAG, "compare | THAT IS CLOSER THAN THIS");
                    return 1;
                } else {
                    Log.i(TAG, "compare | THIS IS CLOSER THAN THAT");
                    return -1;
                }
            }
        });
    }

    /**
     * Using the Gson library, deserializes a JSON representation of the MemberUpdates object.  This
     * is not intended to be used with our server api's JSON string - it is used to deserialize json
     * that was serialized by Gson.
     * @param json
     * @return
     */
    public static TripReport buildFromLocalJson(String json) {
        Gson gson = new Gson();
        TripReport result = gson.fromJson(json, TripReport.class);
        Log.i(TAG, "buildFromLocalJson | " + result.toString());
        return result;
    }

    /**
     * Converts this to a BasicObjects object for use in generic listviews.
     * @return A BasicObjects object
     */
    public ListObjects toBasicObjects() {
        ListObjects listObjects = new ListObjects();
        for (MemberUpdate memberUpdate : this.list) {
            ListObjects.ListObject object = new ListObjects.ListObject();
            object.title = memberUpdate.displayName;
            object.subtitle = memberUpdate.email;
            object.obj = memberUpdate;
            listObjects.list.add(object);
        }

        return listObjects;
    }

    /**
     * Convenience method to retrieve the most recent MemberUpdates object from the local db.  This is
     * functionally no different than instantiating a TripDatasource object and calling its,
     * "getMostRecentMemberLocs() method.
     * @param tripcode A valid, non-null tripcode.
     * @return A reassembled MemberUpdates object.
     */
    public TripReport getMostRecentFromLocalDb(@NonNull String tripcode) {
        TripDatasource ds = new TripDatasource();
        TripReport obj = ds.getMostRecentMemberLocEntry(tripcode);
        Log.i(TAG, "getMostRecentFromLocalDb | " + obj.toString());
        return obj;
    }

    /**
     * Inserts this MemberUpdates object as JSON to the local db.
     * @return Boolean result.
     */
    public boolean saveToLocalDb() {
        TripDatasource ds = new TripDatasource();
        boolean result = ds.insertMemberLocations(this);
        Log.i(TAG, "saveToLocalDb | MemberUpdates saved to local db | Result: " + result);
        return result;
    }

    /**
     * Using the Gson library, serializes this object to JSON.
     * @return
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public LatLngBounds toLatLngBounds() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (MemberUpdate update : this.list) {
            builder.include(update.toLatLng());
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "Tripcode: " + this.tripcode + " | Members: " + this.list.size();
    }

    protected TripReport(Parcel in) {
        if (in.readByte() == 0x01) {
            list = new ArrayList<MemberUpdate>();
            in.readList(list, MemberUpdate.class.getClassLoader());
        } else {
            list = null;
        }
        tripcode = in.readString();
        initiatedbygoogleid = in.readString();
        initiatedon = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (list == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(list);
        }
        dest.writeString(tripcode);
        dest.writeString(initiatedbygoogleid);
        dest.writeString(initiatedon);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<TripReport> CREATOR = new Parcelable.Creator<TripReport>() {
        @Override
        public TripReport createFromParcel(Parcel in) {
            return new TripReport(in);
        }

        @Override
        public TripReport[] newArray(int size) {
            return new TripReport[size];
        }
    };

    // region MemberUpdate class
    /**
     * Represents a single location entry for a single user in a given trip.
     */
    public static class MemberUpdate implements Parcelable {

        private static final String JSON_CREATEDON = "createdOn";
        private static final String JSON_MODIFIEDON = "modifiedOn";
        private static final String JSON_DISPLAYNAME = "displayName";
        private static final String JSON_EMAIL = "email";
        private static final String JSON_PHOTOURL = "photoUrl";
        private static final String JSON_TRIPCODE = "tripcode";
        private static final String JSON_USERID = "userid";
        private static final String JSON_LOCATION_TYPE = "locationtype";
        private static final String JSON_MYLOCTION = "location";
        public Bitmap bitmap;

        private String createdOn;
        /**
         * The user's full/display name
         */
        public String displayName;
        /**
         * The user's email address
         */
        public String email;
/*        *//**
         * The user's latitude
         *//*
        public double lat;
        *//**
         * The user's longitude
         *//*
        public double lon;
        *//**
         * The accuracy, in meters, as reported by the sensor obtaining the location.
         */
        public float accuracy_meters;
        private String modifiedOn;
        /**
         * The url to the user's Google user avatar (if available)
         */
        public String photoUrl;
        /**
         * The trip to associate this entry with
         */
        public String tripcode;
        /**
         * The user that generated this location udpate.
         */
        public String userid;
        /**
         * The distance, in meters, from the current user's device.  This can be and often will be
         * -1 (which is effectively, not calculated/not applicable).
         */
        public float distanceFromMe = -1f;
        /**
         * The speed of the user at the time of the update
         */
        // public double velocity = 0f;
        /**
         * A bitmap of the user's avatar
         */
        public Bitmap avatar;
        /**
         * The location object itself (as json) for all that sexy meta data.
         */
        private String location;
        /**
         * The location object itself for all that sexy meta data.
         */
        public MyLocation myLocation;
        /**
         * Can be used in list adapters for aesthetic purposes.
         */
        public boolean isSeparator;
        /**
         * The service used to obtain this location, typically, "passive" or "active"
         */
        public String locationtype;

        public MemberUpdate() {}

        public MemberUpdate(JSONObject json) {
            try {
                if (!json.isNull(JSON_CREATEDON)) {
                    this.createdOn = new DateTime(json.getString(JSON_CREATEDON), DateTimeZone.UTC).toString();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } // createdon
            try {
                if (!json.isNull(JSON_DISPLAYNAME)) {
                    this.displayName = (json.getString(JSON_DISPLAYNAME));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } // displayname
            try {
                if (!json.isNull(JSON_EMAIL)) {
                    this.email = (json.getString(JSON_EMAIL));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } // email
            try {
                if (!json.isNull(JSON_MODIFIEDON)) {
                    this.modifiedOn = new DateTime(json.getString(JSON_MODIFIEDON), DateTimeZone.UTC).toString();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } // modifiedon
            try {
                if (!json.isNull(JSON_PHOTOURL)) {
                    this.photoUrl = (json.getString(JSON_PHOTOURL));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } // photourl
            try {
                if (!json.isNull(JSON_TRIPCODE)) {
                    this.tripcode = (json.getString(JSON_TRIPCODE));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } // tripcode
            try {
                if (!json.isNull(JSON_USERID)) {
                    this.userid = (json.getString(JSON_USERID));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } // userid
            try {
                if (!json.isNull(JSON_LOCATION_TYPE)) {
                    this.locationtype = (json.getString(JSON_LOCATION_TYPE));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } // locationtype
            try {
                if (!json.isNull(JSON_MYLOCTION)) {
                    this.location = json.getString(JSON_MYLOCTION);
                    this.myLocation = new MyLocation(this.location);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } // location
        }

        public GoogleUser toGoogleUser() {
            GoogleUser user = new GoogleUser();
            user.fullname = this.displayName;
            user.photourl = this.photoUrl;
            user.email = this.email;
            user.id = this.userid;

            return user;
        }

        public LatLng toLatLng() {

            // This should rarely, if ever, evaluate to true!
            if (this.myLocation == null && this.location == null) {
                return null;
            }

            // One of the two is not null, if it's location then myLocation must be, thus we construct it.
            if (this.myLocation == null) {
                this.myLocation = new MyLocation(this.location);
            }

            // Finally, return a fucking LatLng
            return new LatLng(this.myLocation.lat, this.myLocation.lon);
        }

        public ListObjects.ListObject toBasicObject() {
            ListObjects.ListObject object = new ListObjects.ListObject();
            object.title = this.displayName;
            object.subtitle = this.email;
            object.obj = this;
            return object;
        }

        /**
         * Returns the distance between two places in meters
         * @return The distance, in meters, as the crow flies.
         */
        public float distanceTo(LatLng position) {
            Location locationA = new Location("LOGICAL");
            Location locationB = new Location("LOGICAL");

            locationA.setLongitude(position.longitude);
            locationA.setLatitude(position.latitude);

            locationB.setLongitude(this.myLocation.lon);
            locationB.setLatitude(this.myLocation.lat);

            return locationA.distanceTo(locationB);
        }

        /**
         * Returns the distance between two places in meters
         * @return The distance, in meters, as the crow flies.
         */

        public float distanceTo(MemberUpdate position) {
            Location locationA = new Location("LOGICAL");
            Location locationB = new Location("LOGICAL");

            locationA.setLongitude(position.myLocation.lon);
            locationA.setLatitude(position.myLocation.lat);

            locationB.setLongitude(this.myLocation.lon);
            locationB.setLatitude(this.myLocation.lat);

            return locationA.distanceTo(locationB);
        }

        /**
         * Returns the distance between two places in meters
         * @return The distance, in meters, as the crow flies.
         */
        public float distanceTo(Location location) {

            Log.i(TAG, "distanceTo");

            if (location == null || this.myLocation == null) {
                Log.w(TAG, "distanceTo: | location to measure to was null!");
                return 0;
            }

            Location locationA = new Location("LOGICAL");
            locationA.setLongitude(this.myLocation.lon);
            locationA.setLatitude(this.myLocation.lat);

            float dist = locationA.distanceTo(location);

            Log.i(TAG, "distanceTo | " + dist + " meters.");

            return dist;
        }

        public DateTime getModifiedOnLocalTime() {
            return new DateTime(this.modifiedOn, DateTimeZone.forID(TimeZone.getDefault().getID()));
        }

        public DateTime getModifiedOnUtc() {
            return new DateTime(this.modifiedOn, DateTimeZone.UTC);
        }

        public DateTime getCreatedOnLocalTime() {
            return new DateTime(this.createdOn, DateTimeZone.forID(TimeZone.getDefault().getID()));
        }

        public DateTime getCreatedOnUtc() {
            return new DateTime(this.createdOn, DateTimeZone.UTC);
        }

        private DateTime getCurrentUtcTime() {
            return new DateTime(DateTime.now(), DateTimeZone.UTC);
        }

        public int minutesAgo() {
            DateTime createdOn = new DateTime(this.createdOn);
            DateTime now = getCurrentUtcTime();
            long cMillis = createdOn.getMillis();
            long nMillis = now.getMillis();
            long diff = nMillis - cMillis;

            long secs = diff / 1000;
            long mins = secs / 60;
            long hours = mins / 60;
            long days = hours / 24;

            return (int) mins;

        }

        @Override
        public String toString() {
            return this.displayName;
        }

        public float getDistanceInMiles() {
            return StaticHelpers.Geo.convertMetersToMiles(this.distanceFromMe, 2);
        }

        public String getDistanceInRelevantUnits() {

            String resultText = "n/a";

            if (this.distanceFromMe > 1000) {
                resultText = StaticHelpers.Geo.convertMetersToMiles(this.distanceFromMe, 2) + " miles away";
            } else {
                resultText = StaticHelpers.Geo.convertMetersToFeet(this.distanceFromMe, MyApp.getAppContext(), true);
            }

            return resultText;
        }

        public String getUpdatedOn() {
            return StaticHelpers.DatesAndTimes.getPrettyDateAndTime(this.getCreatedOnUtc());
        }

        public float getMinutesAgo() {
            return StaticHelpers.DatesAndTimes.getMinutesBetween(getCreatedOnUtc());
        }

        /**
         * Calculates the time since the last update and converts it to a helpful value.  E.g.
         * 2345 seconds ago will be returned as 39 minutes ago.  14560 seconds ago will be returned as
         * 4 hours ago etc.
         * @return A useful representation of how long since the user last updated their location.
         */
        public String getUpdatedLastInRelevantUnits() {
            // 3600 seconds = 1 hour (3,600,000 ms)
            // 86400 seconds = 1 day (86,400,000 ms)

            DateTime modified = this.getCreatedOnUtc();
            Log.i(TAG, "getUpdatedLastInRelevantUnits | " + modified);

            float minutes = StaticHelpers.DatesAndTimes.getMinutesBetween(getCreatedOnUtc());
            float seconds = StaticHelpers.DatesAndTimes.getSecondsBetween(getCreatedOnUtc());

            // Less than a minute - return seconds ago
            if (minutes < 1) {
                return StaticHelpers.Numbers.formatAsZeroDecimalPointNumber(seconds) + " seconds ago";
            }

            // Less than an hour - return minutes ago
            if (minutes < 60) {
                return StaticHelpers.Numbers.formatAsZeroDecimalPointNumber(minutes) + " minutes ago";
            }

            // More than an hour but less than a day - return hours ago
            if (minutes >= 60 && minutes < 1440) {
                float hours = StaticHelpers.DatesAndTimes.getHoursBetween(getCreatedOnUtc());
                return StaticHelpers.Numbers.formatAsOneDecimalPointNumber(hours) + " hours ago";
            }

            // More than a day - return days ago
            if (minutes >= 1440) {
                float days = StaticHelpers.DatesAndTimes.getDaysBetween(getCreatedOnUtc());
                return StaticHelpers.Numbers.formatAsOneDecimalPointNumber(days) + " days ago";
            }

            return null;
        }

        public float getSpeedInMilesPerHour() {

            if (this.myLocation == null) {
                return 0;
            }

            float strVelocity = Float.parseFloat(Double.toString(this.myLocation.velocity));
            return Float.parseFloat(StaticHelpers.Geo.getSpeedInMph(strVelocity, false, 2));
        }

        public float getAccuracy() {
            return StaticHelpers.Numbers.formatAsZeroDecimalPointNumber(this.accuracy_meters);
        }

        protected MemberUpdate(Parcel in) {
            createdOn = in.readString();
            displayName = in.readString();
            email = in.readString();
            accuracy_meters = in.readFloat();
            modifiedOn = in.readString();
            photoUrl = in.readString();
            tripcode = in.readString();
            userid = in.readString();
            distanceFromMe = in.readFloat();
            avatar = (Bitmap) in.readValue(Bitmap.class.getClassLoader());
            location = in.readString();
            // myLocation = (MyLocation) in.readValue(MyLocation.class.getClassLoader());
            isSeparator = in.readByte() != 0x00;
            locationtype = in.readString();
            bitmap = (Bitmap) in.readValue(Bitmap.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(createdOn);
            dest.writeString(displayName);
            dest.writeString(email);
            dest.writeFloat(accuracy_meters);
            dest.writeString(modifiedOn);
            dest.writeString(photoUrl);
            dest.writeString(tripcode);
            dest.writeString(userid);
            dest.writeFloat(distanceFromMe);
            dest.writeValue(avatar);
            dest.writeString(location);
            dest.writeByte((byte) (isSeparator ? 0x01 : 0x00));
            dest.writeString(locationtype);
            dest.writeValue(bitmap);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<MemberUpdate> CREATOR = new Parcelable.Creator<MemberUpdate>() {
            @Override
            public MemberUpdate createFromParcel(Parcel in) {
                return new MemberUpdate(in);
            }

            @Override
            public MemberUpdate[] newArray(int size) {
                return new MemberUpdate[size];
            }
        };
    }
    // endregion
}
