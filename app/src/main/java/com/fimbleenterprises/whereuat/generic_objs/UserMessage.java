package com.fimbleenterprises.whereuat.generic_objs;

import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.helpers.MyNotificationManager;
import com.fimbleenterprises.whereuat.helpers.StaticHelpers;
import com.fimbleenterprises.whereuat.local_database.TripDatasource;
import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UserMessage {

    /**
     * The user who sent the message.
     */
    public GoogleUser sender;

    /**
     * The trip code this message is associated with.
     */
    public String tripcode;

    /**
     * The actual message body.
     */
    public String messageBody;

    /**
     * A loose date roughly showing when the message was sent/received.
     */
    public long createdonutc;

    /**
     * Constructs a new UserMessage object from JSON.  This JSON (at least at the time of this writing)
     * is intended to be received as an FcmPayload object within a received FCM message.  To see an
     * example of this constructor in use check the FirebaseMessageReceiver where the OpCode is parsed
     * on message receipt using switch logic.  Opcode used by the sending server: OP_CODE_USER_MESSAGE.
     * @param serializedPayload A serialized (JSON) representation of a UserMessage object defined by
     *                          the server (and duplicated here).
     */
    public UserMessage(String serializedPayload) {
        try {
            JSONObject json = new JSONObject(serializedPayload);

            try {
                if (!json.isNull("sender")) {
                    this.sender = new GoogleUser(json.getString("sender"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
               if (!json.isNull("createdonutc")) {
                   this.createdonutc = json.getLong("createdonutc");
               } else {
                   this.createdonutc = new DateTime(DateTime.now(), DateTimeZone.UTC).getMillis();
               }
            } catch (JSONException e) {
                this.createdonutc = new DateTime(DateTime.now(), DateTimeZone.UTC).getMillis();
               e.printStackTrace();
            }

            try {
                if (!json.isNull("messageBody")) {
                    this.messageBody = (json.getString("messageBody"));
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates an array list of UserMessage objects from our server api's response.
     * @param serverJsonArray A serialized list of UserMessage objects.
     * @return ArrayList of UserMessage objects.
     */
    public static ArrayList<UserMessage> parseMany(String serverJsonArray) {
        ArrayList<UserMessage> messages = new ArrayList<>();
        try {
            JSONArray rootArray = new JSONArray(serverJsonArray);
            for (int i = 0; i < rootArray.length(); i++) {
                messages.add(new UserMessage(rootArray.getJSONObject(i).toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return messages;
    }

    /**
     * Uses our MyNotificationManager class to construct either a low or high priority notification
     * with the message body as its body.
     */
    public void createNotification() {
        MyNotificationManager mgr = new MyNotificationManager();
        mgr.showMessageReceivedNotification(this.sender.fullname, this.messageBody); mgr.showMessageReceivedNotification(this.sender.fullname, this.messageBody);
    }

    /**
     * Adds an entry to the messages table in the local database.
     */
    public boolean appendToDb() {
        TripDatasource ds = new TripDatasource();
        return ds.appendUserMessage(this);
    }

    /**
     * Converts this message's date/time from UTC to the device's local date/time.
     * @return
     */
    public DateTime getMessageLocalDateTime() {
        try {
            return StaticHelpers.DatesAndTimes.convertUtcToLocal(new DateTime(this.createdonutc));
        } catch (Exception e) {
            e.printStackTrace();
            return DateTime.now();
        }
    }

    /**
     * Converts this object to JSON using the Gson library.  The outputted JSON may or may not work
     * as the string argument in this class's default constructor as it is designed to consume JSON
     * constructed by the server.  Gson can do things a bit... differently at times.
     * @return This class, serialized using the Gson library.
     */
    public String toGson() {
        return new Gson().toJson(this);
    }

    /**
     * Checks if a message's sender.id equals your google id.
     * @return True if you sent this message, yo.
     */
    public boolean sentByMe() {
        return this.sender.id.equals(GoogleUser.getCachedUser().id);
    }

}
