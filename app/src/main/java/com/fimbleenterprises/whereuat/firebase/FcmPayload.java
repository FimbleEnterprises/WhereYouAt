package com.fimbleenterprises.whereuat.firebase;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.fimbleenterprises.whereuat.local_database.TripReport;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

import androidx.collection.ArrayMap;

    public class FcmPayload implements Parcelable {

        private static final String TAG = "FcmPayload";

        // OP codes that should match exactly what the server will stipulate
        public static final int OP_CODE_OTHER = 0;
        public static final int OP_CODE_JOINED_TRIP = 1;
        public static final int OP_CODE_CREATED_TRIP = 2;
        public static final int OP_CODE_USER_LEFT_TRIP = 3;
        public static final int OP_CODE_USER_JOINED_TRIP = 4;
        public static final int OP_CODE_LOCATION_REQUESTED = 5;
        public static final int OP_CODE_TRIP_UPDATED = 6;
        public static final int OP_CODE_USER_MESSAGE = 8;

        // This is used as the tag for the actual intent extra containing the payload object as json
        public static final String FCM_PAYLOAD = "FCM_PAYLOAD";

        // For use as the intent action in broadcast intents after receiving an FCM
        public static final String FCM_RECEIVED_ACTION = "FCM_RECEIVED";

        public DateTime sentOn;
        public String title;
        public String serializedObject;
        public String recipient;
        public int opcode;

        public FcmPayload(Object object) {
            ArrayMap arrayMap = (ArrayMap) object;
            Collection collection = arrayMap.values();
            Object[] objects = collection.toArray();
            String strOpCode = (String) objects[0];
            this.opcode = Integer.parseInt(strOpCode);
            this.sentOn = new DateTime(objects[1]);
            this.serializedObject = (String) objects[2];
            this.title = (String) objects[3];
            Log.i(TAG, "FcmPayload ");
        }

        protected FcmPayload(Parcel in) {
            sentOn = (DateTime) in.readValue(DateTime.class.getClassLoader());
            title = in.readString();
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