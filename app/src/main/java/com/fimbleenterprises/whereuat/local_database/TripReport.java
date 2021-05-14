package com.fimbleenterprises.whereuat.local_database;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.fimbleenterprises.whereuat.generic_objs.BasicObjects;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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
    public String initiatedon;

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
                    this.initiatedon = (new DateTime(root.getString(INITIATED_ON)).toString());
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
    public BasicObjects toBasicObjects() {
        BasicObjects basicObjects = new BasicObjects();
        for (MemberUpdate memberUpdate : this.list) {
            BasicObjects.BasicObject object = new BasicObjects.BasicObject();
            object.title = memberUpdate.displayName;
            object.subtitle = memberUpdate.email;
            object.obj = memberUpdate;
            basicObjects.list.add(object);
        }

        return basicObjects;
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

    @Override
    public String toString() {
        return "Tripcode: " + this.tripcode + " | Members: " + this.list.size();
    }

    /**
     * Represents a single location entry for a single user in a given trip.
     */
    public static class MemberUpdate implements Parcelable {

        public static final String LOCATION_TYPE_ACTIVE = "active";
        public static final String LOCATION_TYPE_PASSIVE = "passive";

        private String createdOn;
        /**
         * The user's full/display name
         */
        public String displayName;
        /**
         * The user's email address
         */
        public String email;
        /**
         * The user's latitude
         */
        public double lat;
        /**
         * The user's longitude
         */
        public double lon;
        /**
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
         * Can be used in list adapters for aesthetic purposes.
         */
        public boolean isSeparator;

        /**
         * The service used to obtain this location, typically, "passive" or "active"
         */
        public String locationtype;

        public MemberUpdate(JSONObject json) {
            try {
                if (!json.isNull("createdOn")) {
                    this.createdOn = (json.getString("createdOn"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("displayName")) {
                    this.displayName = (json.getString("displayName"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("email")) {
                    this.email = (json.getString("email"));
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
            try {
                if (!json.isNull("accuracy_meters")) {
                    this.accuracy_meters = (json.getLong("accuracy_meters"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("modifiedOn")) {
                    this.modifiedOn = (json.getString("modifiedOn"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("photoUrl")) {
                    this.photoUrl = (json.getString("photoUrl"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("tripcode")) {
                    this.tripcode = (json.getString("tripcode"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("userid")) {
                    this.userid = (json.getString("userid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("locationtype")) {
                    this.locationtype = (json.getString("locationtype"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public LatLng toLatLng() {
            return new LatLng(this.lat, this.lon);
        }

        public DateTime getCreatedOn() {
            return new DateTime(this.createdOn);
        }

        public void setCreatedOn(DateTime dateTime) {
            this.createdOn = dateTime.toString();
        }

        public DateTime getModifiedOn() {
            return new DateTime(this.modifiedOn);
        }

        public void setModifiedOn(DateTime dateTime) {
            this.modifiedOn = dateTime.toString();
        }

        protected MemberUpdate(Parcel in) {
            createdOn = in.readString();
            displayName = in.readString();
            email = in.readString();
            lat = in.readDouble();
            lon = in.readDouble();
            accuracy_meters = in.readFloat();
            modifiedOn = in.readString();
            photoUrl = in.readString();
            tripcode = in.readString();
            userid = in.readString();
            isSeparator = in.readByte() != 0x00;
            locationtype = in.readString();
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
            dest.writeDouble(lat);
            dest.writeDouble(lon);
            dest.writeFloat(accuracy_meters);
            dest.writeString(modifiedOn);
            dest.writeString(photoUrl);
            dest.writeString(tripcode);
            dest.writeString(userid);
            dest.writeByte((byte) (isSeparator ? 0x01 : 0x00));
            dest.writeString(locationtype);
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
}
