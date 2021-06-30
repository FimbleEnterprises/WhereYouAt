package com.fimbleenterprises.whereuat;

import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.local_database.TripReport;

import androidx.annotation.Nullable;

/**
 * This class is a helper designed to uniformly send broadcasts that can be predictably parsed by
 * any broadcast receiver listening for them.
 */
public class AppBroadcastHelper {

    /**
     * Inter-class tag used for logging.
     */
    private static final String TAG = "AppBroadcastHelper";

    /**
     * This is used as the action string for the base intents.  All broadcasts will have this string as their action.
     * As such, all intent filters created by receivers should use this value as their action when created.
     */
    public static final String GLOBAL_BROADCAST = "GLOBAL_BROADCAST";

    /**
     * This string is used to identify the serializable BroadcastType intent extra - this is extra's tag string value.
     */
    public static final String BROADCAST_TYPE = "BROADCAST_TYPE";

    /**
     * This string is used as the tag for any optional payload included as an intent extra.
     */
    public static final String PARCELED_EXTRA = "PARCELED_EXTRA";

    /**
     * Used to identify app-wide broadcasts - ALWAYS included as an intent extra using the tag: "BROADCAST_TYPE".
     * Receiver should unpack the extra as a Serializable object cast to a BroadcastType object.<br/><br/>
     * Example:<br/>
     * BroadcastType broadcastType = (BroadcastType) intent.getSerializableExtra(AppBroadcastHelper.BROADCAST_TYPE)
     */
    public enum BroadcastType {
        ACTIVE_LOCATION_SERVICE_STARTED, ACTIVE_LOCATION_SERVICE_STOPPED, PASSIVE_LOCATION_SERVICE_STOPPED,
        LOCATION_TRACKING_SERVICE_STARTED, SERVER_TRIP_STOPPED, SERVER_TRIP_STARTED, LOCATION_CHANGED_LOCALLY,
        SERVER_LOCATION_UPDATED, USER_JOINED_TRIP, USER_LEFT_TRIP, USER_MARKER_CLICKED, MESSAGE_RECEIVED,
        PAGE_CHANGED, ALERT_BY_SPOT, ALERT_BY_ME;
    }

    /**
     * Sends an app-wide broadcast using a strongly-typed syntax and a defined structure for consumption
     * by any receivers sprinkled throughout the app.
     * <br/><br/>
     * Every broadcast will be sent with an action of "GLOBAL_BROADCAST".  These intents
     * will always have an intent extra with the tag, "BROADCAST_TYPE" and be of the type
     * Serializable.  The receiver should cast these extras to a BroadcastType object and use switch
     * statements to make decisions on what to do with their payload (or lack thereof).
     * <br/><br/>
     * Receiver example:<br/>BroadcastType broadcastType = (BroadcastType) intent.getSerializableExtra("BROADCAST_TYPE")
     * <br/>
     * if (broadcastType == BroadcastType.SERVER_TRIP_STARTED) {...}
     * <br/><br/>
     * Also, some broadcasts will contain
     * an additional intent extra (of varying types) with a tag of PARCELED_EXTRA.  These will have to
     * be cast to whatever object they were originally sent as (Location, TripReport, GoogleUser etc.).
     * <br/><br/>
     * Check the source code or documentation (if I ever write it) to determine which broadcast types will
     * include the additional intent extra and the type to cast it to.
     * @param type The type to classify this broadcast as.  Uses the enum BroadcastType and will be
     *             included as an intent extra and should be picked out and deserialized by the receiver.
     * @param object A parcelable object that can be included to act as a payload.  This extra will
     *               be added as an intent extra with a tag of: PARCELED_EXTRA.
     */
    public static void sendGeneralBroadcast(BroadcastType type, @Nullable Object object) {
        Log.i(TAG, "sendGeneralBroadcast | Sending broadcast of type: " + type.name() + "...");
        Intent intent = new Intent(GLOBAL_BROADCAST);
        intent.putExtra(BROADCAST_TYPE, type);
        switch (type) {
            case SERVER_TRIP_STARTED:
                // Nothing to append to broadcast
                break;
            case SERVER_TRIP_STOPPED:
                // Nothing to append to broadcast
                break;
            case ACTIVE_LOCATION_SERVICE_STARTED:
                // Nothing to append to broadcast
                break;
            case ACTIVE_LOCATION_SERVICE_STOPPED:
                // Nothing to append to broadcast
                break;
            case LOCATION_TRACKING_SERVICE_STARTED:
                // Nothing to append to broadcast
                break;
            case PASSIVE_LOCATION_SERVICE_STOPPED:
                // Nothing to append to broadcast
                break;
            case LOCATION_CHANGED_LOCALLY:
                intent.putExtra(PARCELED_EXTRA, (Location) object);
                break;
            case SERVER_LOCATION_UPDATED:
                intent.putExtra(PARCELED_EXTRA, (TripReport) object);
                break;
            case USER_JOINED_TRIP:
                intent.putExtra(PARCELED_EXTRA, (GoogleUser) object);
                break;
            case USER_LEFT_TRIP:
                intent.putExtra(PARCELED_EXTRA, (GoogleUser) object);
                break;
            case USER_MARKER_CLICKED:
                intent.putExtra(PARCELED_EXTRA, (TripReport.MemberUpdate) object);
                break;
            case MESSAGE_RECEIVED:
                // Nothing to append to broadcast
                break;
            case ALERT_BY_ME:
                //Nothing to append
                break;
            case ALERT_BY_SPOT:
                intent.putExtra(PARCELED_EXTRA, (GoogleUser) object);
        }

        MyApp.getAppContext().sendBroadcast(intent);
    }

    /**
     * Sends an app-wide broadcast using a strongly-typed syntax and a defined structure for consumption
     * by any receivers sprinkled throughout the app.
     * <br/><br/>
     * Every broadcast will be sent with an action of "GLOBAL_BROADCAST".  These intents
     * will always have an intent extra with the tag, "BROADCAST_TYPE" and be of the type
     * Serializable.  The receiver should cast these extras to a BroadcastType object and use switch
     * statements to make decisions on what to do with their payload (or lack thereof).
     * <br/><br/>
     * Receiver example:<br/>BroadcastType broadcastType = (BroadcastType) intent.getSerializableExtra("BROADCAST_TYPE")
     * <br/>
     * if (broadcastType == BroadcastType.SERVER_TRIP_STARTED) {...}
     */
    public static void sendGeneralBroadcast(BroadcastType type) {
        // The intents will always have an action of: GLOBAL_BROADCAST_ACTION
        Intent intent = new Intent(GLOBAL_BROADCAST);
        intent.putExtra(BROADCAST_TYPE, type);
        MyApp.getAppContext().sendBroadcast(intent);
    }



}
