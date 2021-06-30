package com.fimbleenterprises.whereuat.firebase;

import android.content.Intent;
import android.os.UserManager;
import android.util.Log;

import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.generic_objs.UserMessage;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.helpers.MyNotificationManager;
import com.fimbleenterprises.whereuat.local_database.LocalUserLocation;
import com.fimbleenterprises.whereuat.local_database.TripDatasource;
import com.fimbleenterprises.whereuat.helpers.MySettingsHelper;
import com.fimbleenterprises.whereuat.local_database.TripReport;
import com.fimbleenterprises.whereuat.AppBroadcastHelper;
import com.fimbleenterprises.whereuat.services.LocationTrackingService;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.Map;

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
        Map<String, String> data = remoteMessage.getData();
        FcmPayload payload = null;

        // The server encodes a JSON object called FcmPaylod into the body of the FCM message.  The
        // payload has additional properties beyond what an FCM message typically has (title and body).
        // We can use the FCM's body property as the value in the FcmPayload's constructor and thus
        // parse out those additional properties.
        try {
            payload = new FcmPayload(remoteMessage.getData());
            Log.i(TAG, "onMessageReceived | Fcm parsed.");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Package the payload as an intent extra and broadcast it to anyone listening for broadcasts
        // with an FCM_RECEIVED_ACTION "action".
        Intent intent = new Intent(FcmPayload.FCM_RECEIVED_ACTION);
        intent.putExtra(FcmPayload.FCM_PAYLOAD, payload);
        sendBroadcast(intent);

        /*******************************************************************************************
         ** -= OPCODE EVALUATION =-
         ******************************************************************************************/
        switch (payload.opcode) {
            /** CREATED TRIP **/
            case FcmPayload.OP_CODE_CREATED_TRIP:
                break;

            /** JOINED TRIP **/
            case FcmPayload.OP_CODE_USER_JOINED_TRIP:
                try {
                    TripReport t = new TripReport(payload.serializedObject);

                    if (t.list.size() > 0) {
                        t.saveToLocalDb();
                    }

                    GoogleUser user = t.getInitiator();
                    Log.i(TAG, "onMessageReceived | User joined: " + user.fullname);
                    AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType
                            .USER_JOINED_TRIP, user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            /** LEFT TRIP **/
            case FcmPayload.OP_CODE_USER_LEFT_TRIP:
                Log.w(TAG, "onMessageReceived: | OP CODE USER LEFT TRIP");
                try {
                    GoogleUser user = new GoogleUser(payload.serializedObject);
                    if (user != null) {
                        Log.w(TAG, "onMessageReceived: " + user.fullname + " has left the trip!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            /** YOUR LOC WAS REQUESTED **/
            case FcmPayload.OP_CODE_LOCATION_REQUESTED:
                MyNotificationManager manager = new MyNotificationManager();
                GoogleUser requestingUser = new GoogleUser(payload.serializedObject);
                manager.showLowPriorityNotification("Location requested", requestingUser
                        .fullname + " has requested your current location.");

                LocationTrackingService.PassiveLocationAlarmReceiver.getMyLastKnownLocation(new LocationTrackingService.InstantLocationListener() {
                    @Override
                    public void onLocationReceived(LocalUserLocation location) {
                        Log.i(TAG, "onLocationReceived | Location was recieved and will be uploaded.");
                    }
                });

                break;

            /** TRIP WAS UPDATED **/
            case FcmPayload.OP_CODE_TRIP_UPDATED:

                // The FCM payload sent by the server will be a MemberUpdates object in JSON.  Deserialize
                // into a MemberUpdates object and save that entry to the local database.
                TripDatasource ds = new TripDatasource();
                TripReport tripReport = new TripReport(payload.serializedObject);

                int mins = tripReport.list.get(0).minutesAgo();

                boolean dbResult = ds.insertMemberLocations(tripReport);
                Log.i(TAG, "onMessageReceived DbInsertResult: " + dbResult);
                Log.i(TAG, "onMessageReceived Converted FCM payload to a MemberUpdates object and saved it to the local database.");

                // Also, broadcast that shit because we have a cool-as-hell broadcast mechanism and I wanna use it!
                AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType
                        .SERVER_LOCATION_UPDATED, tripReport);
                Log.i(TAG, "onMessageReceived Sent an app-wide broadcast with the MemberUpdtes object to anyone listening.");
                break;



            /**
             * MESSAGE RECEIVED
             */
            case FcmPayload.OP_CODE_USER_MESSAGE:
                Log.i(TAG, "onMessageReceived " + payload.serializedObject);
                
                // Okay, there's a lot to unpack here.
                // Each FCM that indicates a user message will have a payload consisting of the last
                // 100 messages for this trip from newest to oldest (asc).  Deserialize that list.
                ArrayList<UserMessage> tripMessages = UserMessage.parseMany(payload.serializedObject);
                
                // Get a reference to the latest message sent (the one that prompted this).
                UserMessage latestMessage = null;
                
                // Validate the payload before doing math on it.
                if (tripMessages != null && tripMessages.size() > 0) {
                    // Remove all trip messages from the local database.
                    new TripDatasource().deleteAllLocalMessages(MyApp.getCurrentTripcode());
                    // Get the message that was just sent from the list of 100 trip messages.
                    latestMessage = tripMessages.get(0);
                    
                    // Repopulate the local database with each of those 100 messages
                    for (UserMessage m : tripMessages) {
                        m.appendToDb();
                    }
                }
                
                // Show a notification with the latest message
                latestMessage.createNotification();
                
                // Send a general broadcast so that whatever receivers can do whatever they need to 
                // do upon the receipt of a new message.
                AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType.MESSAGE_RECEIVED, latestMessage);

                Log.i(TAG, "onMessageReceived | Processing of the latest trip message is complete.");
                break;
            default:
                break;
        }

        // Release a notification on the device
        /*
        MyNotificationManager notificationManager = new MyNotificationManager();
        notificationManager.showLowPriorityNotification();
        */
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
