package com.fimbleenterprises.whereuat.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.fimbleenterprises.whereuat.AppBroadcastHelper;
import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.helpers.MyNotificationManager;
import com.fimbleenterprises.whereuat.local_database.LocalUserLocation;
import com.fimbleenterprises.whereuat.local_database.TripReport;
import com.fimbleenterprises.whereuat.rest_api.Requests;
import com.fimbleenterprises.whereuat.rest_api.WebApi;

import org.joda.time.DateTime;

import static com.fimbleenterprises.whereuat.AppBroadcastHelper.BroadcastType.ACTIVE_LOCATION_SERVICE_STARTED;
import static com.fimbleenterprises.whereuat.AppBroadcastHelper.BroadcastType.LOCATION_CHANGED_LOCALLY;
import static com.fimbleenterprises.whereuat.AppBroadcastHelper.BroadcastType.SERVER_LOCATION_UPDATED;

public class ActiveLocationUpdateService2 extends Service implements LocationListener {

    // region STATIC DECLARATIONS

    // -= ************* Static strings *********** =- //
    public static final String STOP_SERVICE_OUTRIGHT = "STOP_SERVICE_OUTRIGHT";
    public static final String SWITCH_TO_PASSIVE_SERVICE = "SWITCH_TO_PASSIVE_SERVICE";
    public static final String LOCATION_TYPE = "active";
    private static final String TAG = "ActiveLocationUpdateService";
    public static final String WAKE_LOCK_TAG = "WhereYouAt:ActiveLocServiceWakeLock";
    public static final String MYLOCATION_UPDATED = "MYLOCATION_UPDATED";
    public static final String RAW_LOCATION = "RAW_LOCATION";
    public static final String SERVICE_STOPPED = "SERVICE_STOPPED";
    public static final String SERVICE_STARTED = "SERVICE_STARTED";
    private static final String SERVICE_STATUS_CHANGED = "SERVICE_STATUS_CHANGED";
    public static final String TRIPCODE = "TRIPCODE";
    public static final String LEFT_TRIP = "LEFT_TRIP";

    // -= *************  Static ints  *********** =- //
    private static final int LOCATION_INTERVAL = 3000;
    public static final int ACTIVE_LOC_SERVICE_NOTIFICATIONID = 1000;

    // -= ************* Static floats *********** =- //
    private static final float LOCATION_DISTANCE = 10f;

    private static long lastUpdatedServerInMillis = 0;
    public static boolean awaitingServerResponse = false;

    // endregion

    public static final IntentFilter ifltr_ServiceStatus = new IntentFilter(SERVICE_STATUS_CHANGED);

    private PowerManager.WakeLock wakeLock;
    private LocationManager mLocationManager;
    public static boolean isRunning = false;
    public static String tripcode;



    private void acquireWakeLock() {
        // Only get a wakelock if it doesn't already exist or exists but isn't held.
        if (wakeLock == null || ! wakeLock.isHeld()) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
            wakeLock.acquire();
            Log.i(TAG, "acquireWakeLock | Acquired a wake lock.");
            return;
        }

        Log.i(TAG, "acquireWakeLock | Wakelock was already found and held - nothing to do!");
    }
    
    private void releaseWakeLock() {
        // If a lock is held, release it.
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                Log.i(TAG, "releaseWakeLock | Held wakelock was released.");
                return;
            }
        }

        Log.i(TAG, "releaseWakeLock | No held wakelock found, nothing to release.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind ");
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate ");
        super.onCreate();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "onStartCommand ");

        if (intent.hasExtra(SWITCH_TO_PASSIVE_SERVICE)) {
            // Check if this is actually a stop command!
            if (intent.getBooleanExtra(SWITCH_TO_PASSIVE_SERVICE, false)) {
                Log.w(TAG, "onStartCommand: | IS ACTUALLY A STOP COMMAND!");

                // Start the passive service
                Log.w(TAG, "onStartCommand: Starting the passive service!");
                Intent startPassive = new Intent(this, LocationTrackingService.class);
                startPassive.putExtra(TRIPCODE, tripcode);
                startService(startPassive);

                Log.w(TAG, "onStartCommand: | Stoping the active service with stopSelf()!");
                stopSelf();

                return super.onStartCommand(intent, flags, startId);
            }
        } else if (intent.getBooleanExtra(STOP_SERVICE_OUTRIGHT, false)) {
            if (isRunning) {
                AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType.SERVER_TRIP_STOPPED);
                Log.w(TAG, " !!!!!!! -= onStartCommand | STOPPING SERVICE OUTRIGHT =- !!!!!!!");
                stopSelf();
            }
            return super.onStartCommand(intent, flags, startId);
        }

        // Get a wake lock
        acquireWakeLock();

        // Grab the tripcode from the start intent
        if (intent != null) {
            if (intent.getStringExtra(TRIPCODE) != null) {
                tripcode = intent.getStringExtra(TRIPCODE);
            }
        }

        // Start paying attention to device's location
        startListeningForLocationUpdates();

        MyNotificationManager notificationManager = new MyNotificationManager();
        notificationManager.showLowPriorityNotification();

        // Formally start as a foreground service
        startForeground(ACTIVE_LOC_SERVICE_NOTIFICATIONID, notificationManager.mLowNotification);

        // Flip the running flag to true.
        isRunning = true;

        // Send a global broadcast - no parcelled extra needed for this - PARCELLED_EXTRA will be null.
        AppBroadcastHelper.sendGeneralBroadcast(ACTIVE_LOCATION_SERVICE_STARTED, null);

        // This flag gets set when a stop request is received - since we are decidedly not stopping
        // the service at present we can reset/set this flag to false.
        MyApp.setIsPendingCancel(false);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, " !!!!!!! -= onUnbind =- !!!!!!!");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy ");
        super.onDestroy();

        // Release the wakelock
        releaseWakeLock();

        // Stop requesting location updates
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(this);
            } catch (Exception ex) {
                Log.i(TAG, "fail to remove location listners, ignore", ex);
            }
        }

        // Flip the running flag to false
        isRunning = false;

        // Send a global broadcast alerting listeners that this service has been destroyed.
        AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType
                .ACTIVE_LOCATION_SERVICE_STOPPED, null);

    }

    @Override
    public void onLowMemory() {
        Log.e(TAG, "onLowMemory: ");
        super.onLowMemory();
    }

    // region LOCATION_LISTENER


    @Override
    public void onLocationChanged(Location location) {

        // Save this local location to the local locations table
        LocalUserLocation localUserLocation = new LocalUserLocation(location);
        Log.i(TAG, "onLocationChanged Saving location to local db: | " + localUserLocation.saveToLocalDb());

        // Locally broadcast the local location change just detected.
        Log.w(TAG, "onLocationChanged | Provider: " + location.getProvider() + " - "
                + location.getAccuracy() + " meters accurate");

        // Send the local location as a broadcast to anyone with ears to hear.
        AppBroadcastHelper.sendGeneralBroadcast(LOCATION_CHANGED_LOCALLY, location);

        // Update the global last known location
        MyApp.setLastKnownLocation(location);

        // It can be nice to know what location provider was used to produce this location.
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            Log.i(TAG, "\n*** GPS ANTENNA PROVIDER ***\n");
        } else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
            Log.i(TAG, "\n*** NETWORK PROVIDER ***\n");
        }

        // If it isn't too soon to tell the server about us - do so.
        if (!isTooSoonToUpdate()) {
            Log.i(TAG, "onLocationChanged | UPDATING SERVER WITH OUR LOCATION!");
            updateServerLocation(location);
        } else {
            Log.i(TAG, "onLocationChanged | It is too soon to update the server.");
        }

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.w(TAG, "onStatusChanged | " + s);
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.w(TAG, "onProviderEnabled | " + s);
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.w(TAG, "onProviderDisabled | " + s);
    }

    /**
     * Evaluates when the last update was sent to the server in order to determine if it is too soon
     * to do it again.
     * @return
     */
    private boolean isTooSoonToUpdate() {

        boolean isTooSoon = false;

        long timespan = (DateTime.now().getMillis() - lastUpdatedServerInMillis);
        Log.i(TAG, "isTooSoonToUpdate It's been " + timespan + " ms since we updated the db.");

        // See if it is too soon to update.
        if (timespan <= LOCATION_INTERVAL
                || awaitingServerResponse) {
            Log.i(TAG, "onLocationChanged It is okay to update server!");
            isTooSoon = true;
        }

        return isTooSoon;
    }

    /**
     * Sends our local location to the server.  The server returns all member's most recent locations
     * as far as it knows (including the one we just sent, obviously).  The server will also send an
     * FCM message with all member's locations as a payload that can (and should) be handled by the
     * FCM message receiver.
     * @param location
     */
    private void updateServerLocation(Location location) {

        awaitingServerResponse = true;

        Requests.Request request = new Requests.Request(Requests.Request.Function.UPDATE_TRIP);
        request.arguments.add(new Requests.Arguments.Argument("userid", GoogleUser.getCachedUser().id));
        request.arguments.add(new Requests.Arguments.Argument("tripcode", tripcode));
        request.arguments.add(new Requests.Arguments.Argument("lat", location.getLatitude()));
        request.arguments.add(new Requests.Arguments.Argument("lon", location.getLongitude()));
        request.arguments.add(new Requests.Arguments.Argument("accuracy", location.getAccuracy()));
        request.arguments.add(new Requests.Arguments.Argument("active/passive", LOCATION_TYPE));
        request.arguments.add(new Requests.Arguments.Argument("velocity", location.getSpeed()));

        WebApi api = new WebApi();
        api.makeRequest(request, new WebApi.WebApiResultListener() {
            @Override
            public void onSuccess(WebApi.OperationResults results) {
                Log.i(TAG, "onSuccess Successfully uploaded our location data to the server.");
                awaitingServerResponse = false;
                lastUpdatedServerInMillis = DateTime.now().getMillis();
                TripReport updates = new TripReport(results.list.get(0).result);
                if (updates != null && updates.list.size() > 0) {
                    updates.saveToLocalDb();
                    // Send a broadcast app-wide with the memberupdates object we just got from
                    // the server for any callers listening.
                    AppBroadcastHelper.sendGeneralBroadcast(SERVER_LOCATION_UPDATED, updates);
                }
            }

            @Override
            public void onFailure(String message) {
                awaitingServerResponse = false;
                // TODO Do something with this failure.
            }
        });
    }

    /**
     * Sets the device up to actively listen for location sensor updates.
     */
    private void startListeningForLocationUpdates() {
        Log.i(TAG, "startListeningForLocationUpdates");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager)
                    getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    this);
            Log.i(TAG, "startListeningForLocationUpdates | Now listening to network location updates.");

            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    this);
            Log.i(TAG, "startListeningForLocationUpdates | Now listening to location updates.");

        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }

    }

    // endregion
}
