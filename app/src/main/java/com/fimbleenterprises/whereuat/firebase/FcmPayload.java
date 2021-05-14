package com.fimbleenterprises.whereuat.firebase;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

public class FcmPayload implements Parcelable {

    // OP codes that should match exactly what the server will stipulate
    public static final int OP_CODE_OTHER = 0;
    public static final int OP_CODE_JOINED_TRIP = 1;
    public static final int OP_CODE_CREATED_TRIP = 2;
    public static final int OP_CODE_USER_LEFT_TRIP = 3;
    public static final int OP_CODE_USER_JOINED_TRIP = 4;
    public static final int OP_CODE_LOCATION_REQUESTED = 5;
    public static final int OP_CODE_TRIP_UPDATED = 6;

    // This is used as the tag for the actual intent extra containing the payload object as json
    public static final String FCM_PAYLOAD = "FCM_PAYLOAD";

    // For use as the intent action in broadcast intents after receiving an FCM
    public static final String FCM_RECEIVED_ACTION = "FCM_RECEIVED";

    public DateTime sentOn;
    public String title;
    public String body;
    public String serializedObject;
    public String recipient;
    public int opcode;

    public FcmPayload(String fcmBody) {
        try {
            JSONObject json = new JSONObject(fcmBody);
            try {
                if (!json.isNull("sentOn")) {
                    this.sentOn = (new DateTime(json.getString("sentOn")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("title")) {
                    this.title = (json.getString("title"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("body")) {
                    this.body = (json.getString("body"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("obj")) {
                    this.serializedObject = (json.getString("obj"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("recipient")) {
                    this.recipient = (json.getString("recipient"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (!json.isNull("opcode")) {
                    this.opcode = (json.getInt("opcode"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected FcmPayload(Parcel in) {
        sentOn = (DateTime) in.readValue(DateTime.class.getClassLoader());
        title = in.readString();
        body = in.readString();
        serializedObject = in.readString();
        recipient = in.readString();
        opcode = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(sentOn);
        dest.writeString(title);
        dest.writeString(body);
        dest.writeString(serializedObject);
        dest.writeString(recipient);
        dest.writeInt(opcode);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<FcmPayload> CREATOR = new Parcelable.Creator<FcmPayload>() {
        @Override
        public FcmPayload createFromParcel(Parcel in) {
            return new FcmPayload(in);
        }

        @Override
        public FcmPayload[] newArray(int size) {
            return new FcmPayload[size];
        }
    };
}