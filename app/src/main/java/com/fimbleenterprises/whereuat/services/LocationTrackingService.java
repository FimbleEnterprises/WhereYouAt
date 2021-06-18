package com.fimbleenterprises.whereuat.services;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.fimbleenterprises.whereuat.AppBroadcastHelper;
import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.helpers.StaticHelpers;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.helpers.MyNotificationManager;
import com.fimbleenterprises.whereuat.local_database.LocalUserLocation;
import com.fimbleenterprises.whereuat.local_database.TripReport;
import com.fimbleenterprises.whereuat.rest_api.Requests;
import com.fimbleenterprises.whereuat.rest_api.WebApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

import org.joda.time.DateTime;

import androidx.core.app.ActivityCompat;

public class LocationTrackingService extends Service {

    public static final String STOP_SERVICE_OUTRIGHT = "STOP_SERVICE_OUTRIGHT";
    public static final String MAP_FRAG_SUPPLIED_A_LOCATION = "MAP_FRAG_SUPPLIED_A_LOCATION";
    public static final String SWITCH_TO_ACTIVE_SERVICE = "SWITCH_TO_ACTIVE_SERVICE";
    public static final int NEXT_LOCATION_REQUEST_CODE = 77747774;
    public static final String LOCATION_TYPE = "passive";
    public static final String TRIPCODE = "TRIPCODE";
    private static final String TAG = "PassiveLocationUpdateService";
    private static final int MAIN_NOTIFICATION_ID = 323232;
    public static String tripcode = null;
    public static final int CHECK_FREQUENCY = 10000;
    public static Context context;
    private static Intent alarmIntent = null;
    private static PendingIntent pendingIntent = null;
    public static AlarmManager alarmManager = null;
    public static boolean awaitingServerResponse = false;
    // public static Location lastKnownLocation;
    public static long lastUpdatedServerInMillis = 0;
    public static boolean isRunning = false;

    public LocationTrackingService() {
        this.context = this;
    }

    public interface InstantLocationListener {
        public void onLocationReceived(LocalUserLocation location);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(TAG, "onStartCommand: Service is starting!");

        if (intent.getBooleanExtra(MAP_FRAG_SUPPLIED_A_LOCATION, false)) {
            Log.e(TAG, "onStartCommand | MAP FRAG SUPPLIED A LOCATION!");
            // The map frag supplied a location - we can retrieve it from the app-wide location var
            // and do something with it.
            if (MyApp.isReportingLocation()) {
                Log.e(TAG, "The service is running - will force an immediate server location update");
                PassiveLocationAlarmReceiver.updateServerLocation(MyApp.getLastKnownLocation());
                return super.onStartCommand(intent, flags, startId);
            }
        }

        if (intent.getBooleanExtra(STOP_SERVICE_OUTRIGHT, false)) {
            if (isRunning) {
                stopSelf();
                AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType.SERVER_TRIP_STOPPED);
                Log.w(TAG, " !!!!!!! -= onStartCommand | STOPPING SERVICE OUTRIGHT =- !!!!!!!");
            }
            return super.onStartCommand(intent, flags, startId);
        }

        getLastKnownDeviceLocation(new InstantLocationListener() {
            @Override
            public void onLocationReceived(LocalUserLocation location) {
                Log.i(TAG, "onLocationReceived | Initial location received - service is running.");
                // Send a broadcast stating the local user's location has changed in case there is a caller that cares.
                AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType
                        .LOCATION_CHANGED_LOCALLY, location.toCrudeLocation());
            }
        });

        PassiveLocationAlarmReceiver.setNextAlarm();

        if (intent == null || intent.getStringExtra(TRIPCODE) == null) {
            try {
                throw new Exception("No tripcode was included when starting this service!!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        tripcode = intent.getStringExtra(TRIPCODE);

        MyNotificationManager myNotificationManager = new MyNotificationManager();
        myNotificationManager.showLowPriorityNotification();

        // Formally start as a foreground service
        startForeground(MAIN_NOTIFICATION_ID, myNotificationManager.mLowNotification);

        isRunning = true;

        MyApp.setIsPendingCancel(false);

        AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType.LOCATION_TRACKING_SERVICE_STARTED);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "onDestroy: Service is being destroyed!");
        Log.w(TAG, "onDestroy: Cancelling the pending alarm manager location check!");
        alarmManager.cancel(pendingIntent);
        isRunning = false;
        Log.w(TAG, "onDestroy: Set passive service isRunning: " + isRunning);

        AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType.PASSIVE_LOCATION_SERVICE_STOPPED);

    }

    public static void getLastKnownDeviceLocation(InstantLocationListener listener) {
        PassiveLocationAlarmReceiver.getMyLastKnownLocation(listener);
    }

    public static class PassiveLocationAlarmReceiver extends BroadcastReceiver {

        public static final int BG_PERMISSION_REQUEST_NOTIFICATION = 88;
        public static final String BG_PERMISSION_REQUEST_NOTIF = "BG_PERMISSION_REQUEST_NOTIF";

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Bundle bundle = intent.getExtras();
                String message = bundle.getString("alarm_message");
                // Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                Log.w(TAG, "onReceive: *** ALARM MANAGER FIRED ***");

                if (MyApp.isActivityVisible()) {
                    // Get and do something with the last known location
                    getMyLastKnownLocation();
                } else {
                    if (StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType
                            .ACCESS_BACKGROUND_LOCATION)) {
                        getMyLastKnownLocation();
                    } else {
                        // todo Make a notification alerting a grievous permission problem!
                    }
                }

            }
            catch (Exception e) {
                Toast.makeText(context, "There was an error somewhere, but we still received " +
                        "an alarm", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            setNextAlarm();

        }

        public static void setNextAlarm() {
            Log.w("", "setNextAlarm: ****************************************************");
            Log.w("", "setNextAlarm:                  SETTING NEXT ALARM!                ");
            Log.w("", "setNextAlarm: *****************************************************");

            alarmIntent = new Intent ( context, PassiveLocationAlarmReceiver.class );
            pendingIntent = PendingIntent.getBroadcast( MyApp.getAppContext().getApplicationContext()
                    , NEXT_LOCATION_REQUEST_CODE, alarmIntent, 0 );
            alarmManager = ( AlarmManager ) MyApp.getAppContext().getSystemService( ALARM_SERVICE );
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                    + CHECK_FREQUENCY, pendingIntent);
        }

        public static void getMyLastKnownLocation(InstantLocationListener listener) {

            context = MyApp.getAppContext();

            Log.w(TAG, "onReceive: Querying last known location now...");

            LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));

            MyApp.setLastKnownLocation(location);

            // If location is null we leave.
            if (location == null) {
                return;
            }

            // Save this location to the database
            LocalUserLocation localUserLocation = new LocalUserLocation(location);
            localUserLocation.saveToLocalDb();
            listener.onLocationReceived(localUserLocation);
            Log.i(TAG, "onLocationChanged | " + localUserLocation.saveToLocalDb());

            setMyLastLocation();
        }

        private static void getMyLastKnownLocation() {

            Log.w(TAG, "onReceive: Querying last known location now...");

            // Get the location manager service
            LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            // Okay, this is hacky - if the map fragment is visible and the map is ready and showing the
            // cute little blue, location dot we want to get THAT location because it is just downright better!
            if (MyApp.isMapFragVisible()) {
                Log.w(TAG, "getMyLastKnownLocation: | WE ARE NOT GOING TO USE THE FUSED LOC PROVIDER!  WE ARE GOING TO USE THE MAP FRAG'S LOCATION SINCE IT IS CURRENTLY SHOWING!");
                updateServerLocation(MyApp.getLastKnownLocation());
                return;
            }

            // Try to get the device's last known location
            Location location = locationManager.getLastKnownLocation(locationManager
                    .getBestProvider(criteria, true));

            // If we have a valid location then we can update the server with that location
            if (location != null) {

                Log.i(TAG, "getMyLastKnownLocation There is a cached location here!");
                double lat = location.getLatitude();
                double longi = location.getLongitude();

                Log.w(TAG, "getMyLastKnownLocation: Provider: " + location.getProvider() + "," +
                        " Lat: " + lat + ", long: " + longi);
                updateServerLocation(location);

                // Update the static lastKnonLoc
                MyApp.setLastKnownLocation(location);

                // Send a broadcast stating the local user's location has changed in case there is a caller that cares.
                AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType
                        .LOCATION_CHANGED_LOCALLY, location);

                // Save this local location to the local locations table
                LocalUserLocation localUserLocation = new LocalUserLocation(location);
                Log.i(TAG, "onLocationChanged | " + localUserLocation.saveToLocalDb());

            } else {
                Log.w(TAG, "getMyLastKnownLocation: No cached location found - will have to " +
                        "spin up the sensors...");
                setMyLastLocation();
            }

        }

        private static void setMyLastLocation() {
            Log.d(TAG, "setMyLastLocation: excecute, and get last location");
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (!StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION)) {
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null){
                        double lat = location.getLatitude();
                        double longi = location.getLongitude();
                        LatLng latLng = new LatLng(lat,longi);
                        String provider = location.getProvider();
                        Log.w(TAG, "MyLastLocation | Provider: " + provider + ", Coords: " + latLng);
                        updateServerLocation(location);
                    }
                }

            });
        }

        /**
         * Sends our local location to the server.  The server returns all member's most recent locations
         * as far as it knows (including the one we just sent, obviously).  The server will also send an
         * FCM message with all member's locations as a payload that can (and should) be handled by the
         * FCM message receiver.
         * @param location
         */
        public static void updateServerLocation(Location location) {

            awaitingServerResponse = true;

            Requests.Request request = new Requests.Request(Requests.Request.Function.UPDATE_TRIP);
            request.arguments.add(new Requests.Arguments.Argument("userid", GoogleUser.getCachedUser().id));
            request.arguments.add(new Requests.Arguments.Argument("tripcode", tripcode));
            request.arguments.add(new Requests.Arguments.Argument("lat", location.getLatitude()));
            request.arguments.add(new Requests.Arguments.Argument("lon", location.getLongitude()));
            request.arguments.add(new Requests.Arguments.Argument("accuracy", location.getAccuracy()));
            request.arguments.add(new Requests.Arguments.Argument("passive/active", LOCATION_TYPE));
            request.arguments.add(new Requests.Arguments.Argument("velocity", location.getSpeed()));

            WebApi api = new WebApi();
            api.makeRequest(request, new WebApi.WebApiResultListener() {
                @Override
                public void onSuccess(WebApi.OperationResults results) {
                    Log.i(TAG, "onSuccess Successfully uploaded our location data to the server.");
                    awaitingServerResponse = false;
                    lastUpdatedServerInMillis = DateTime.now().getMillis();
                    TripReport updates = new TripReport(results.list.get(0).result);
                    updates.saveToLocalDb();

                    AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType.SERVER_LOCATION_UPDATED);

                }

                @Override
                public void onFailure(String message) {
                    awaitingServerResponse = false;
                    // TODO Do something with this failure.
                }
            });
        }

    }
}
