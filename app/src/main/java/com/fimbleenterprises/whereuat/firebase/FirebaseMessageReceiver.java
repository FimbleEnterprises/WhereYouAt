package com.fimbleenterprises.whereuat.firebase;

import android.content.Intent;
import android.util.Log;

import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.StaticHelpers;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.local_database.TripDatasource;
import com.fimbleenterprises.whereuat.preferences.MySettingsHelper;
import com.fimbleenterprises.whereuat.local_database.TripReport;
import com.fimbleenterprises.whereuat.AppBroadcastHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import androidx.annotation.NonNull;

public class FirebaseMessageReceiver extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessagingService";

    @Override
    public void onNewToken(String token) {
        Log.d("MY_TOKEN", "Refreshed token: " + token);
        MySettingsHelper options = new MySettingsHelper();
        options.cacheFcmToken(token);
        try {
            MyFcmHelper.upsertCachedFcm(new MyFcmHelper.UpsertFcmListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "onSuccess Upserted FCM token!");
                }

                @Override
                public void onFailure(String msg) {
                    Log.w(TAG, "onFailure: Failed to upsert FCM token! " + msg);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public FirebaseMessageReceiver() {
        super();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.i("", "onMessageReceived **************************************************\n");
        Log.i("", "onMessageReceived                    FCM RECEIVED                   \n");
        Log.i("", "onMessageReceived **************************************************");

        // Get the notification as a Notification object.
        RemoteMessage.Notification notification = remoteMessage.getNotification();

        // The server encodes a JSON object called FcmPaylod into the body of the FCM message.  The
        // payload has additional properties beyond what an FCM message typically has (title and body).
        // We can use the FCM's body property as the value in the FcmPayload's constructor and thus
        // parse out those additional properties.
        FcmPayload payload = new FcmPayload(notification.getBody());

        // Package the payload as an intent extra and broadcast it to anyone listening for broadcasts
        // with an FCM_RECEIVED_ACTION "action".
        Intent intent = new Intent(FcmPayload.FCM_RECEIVED_ACTION);
        intent.putExtra(FcmPayload.FCM_PAYLOAD, payload);
        sendBroadcast(intent);

        // Tailor a notification based on the op code received from the server,
        String notificationTitle = "";
        String notificationBody = "";

        switch (payload.opcode) {
            case FcmPayload.OP_CODE_CREATED_TRIP:
                notificationTitle = "Group was created!!";
                notificationBody  = "Group code " + MyApp.getCurrentTripcode() + " was created!";
                break;
            case FcmPayload.OP_CODE_USER_JOINED_TRIP:
                notificationTitle = "Someone joined the group";
                try {
                    TripReport t = new TripReport(payload.serializedObject);

                    if (t.list.size() > 0) {
                        t.saveToLocalDb();
                    }

                    GoogleUser user = t.getInitiator();
                    Log.i(TAG, "onMessageReceived | User joined: " + user.fullname);
                    AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType
                            .USER_JOINED_TRIP, user);
                    notificationBody = user.fullname + " has joined the group!";
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case FcmPayload.OP_CODE_USER_LEFT_TRIP:
                notificationTitle = "Someone left the group!";

                break;
            case FcmPayload.OP_CODE_LOCATION_REQUESTED:
                notificationTitle = "Your location was requested";
                break;
            case FcmPayload.OP_CODE_TRIP_UPDATED:
                notificationTitle = "The trip was updated";

                // The FCM payload sent by the server will be a MemberUpdates object in JSON.  Deserialize
                // into a MemberUpdates object and save that entry to the local database.
                TripDatasource ds = new TripDatasource();
                TripReport tripReport = new TripReport(payload.serializedObject);
                boolean dbResult = ds.insertMemberLocations(tripReport);
                Log.i(TAG, "onMessageReceived DbInsertResult: " + dbResult);
                Log.i(TAG, "onMessageReceived Converted FCM payload to a MemberUpdates object and saved it to the local database.");

                // Also, broadcast that shit because we have a cool-as-hell broadcast mechanism and I wanna use it!
                AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType
                        .SERVER_LOCATION_UPDATED, tripReport);
                Log.i(TAG, "onMessageReceived Sent an app-wide broadcast with the MemberUpdtes object to anyone listening.");

                notificationBody = tripReport.getInitiator().fullname + " has updated their location!";
                break;
            default:
                notificationTitle = "OTHER";
                break;
        }

        // Release a notification on the device
        StaticHelpers.Notifications.showNotification(this, notificationTitle, notificationBody, 0);

    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }

    @Override
    public void onMessageSent(@NonNull String s) {
        super.onMessageSent(s);
    }

    @Override
    public void onSendError(@NonNull String s, @NonNull Exception e) {
        super.onSendError(s, e);
    }

    @Override
    protected Intent getStartCommandIntent(Intent intent) {
        return super.getStartCommandIntent(intent);
    }

    @Override
    public boolean handleIntentOnMainThread(Intent intent) {
        return super.handleIntentOnMainThread(intent);
    }

    @Override
    public void handleIntent(Intent intent) {
        super.handleIntent(intent);
    }
}
