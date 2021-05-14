package com.fimbleenterprises.whereuat.services;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.fimbleenterprises.whereuat.AppBroadcastHelper;
import com.fimbleenterprises.whereuat.MainActivity;
import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.R;
import com.fimbleenterprises.whereuat.StaticHelpers;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
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
import androidx.core.app.NotificationCompat;

public class PassiveLocationUpdateService extends Service {
    
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
    public static Location lastKnownLocation;
    public static long lastUpdatedServerInMillis = 0;
    public static final String STOP_SERVICE = "STOP_SERVICE";
    public static boolean isRunning = false;

    public PassiveLocationUpdateService() {
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

        if (intent.hasExtra(SWITCH_TO_ACTIVE_SERVICE)) {
            Log.w(TAG, " !!!!!!! -= onStartCommand | Switching to active service =- !!!!!!!");

            Log.w(TAG, "onStartCommand: Requesting the active service be started!");
            if (intent.getBooleanExtra(SWITCH_TO_ACTIVE_SERVICE, false)) {
                Intent startIntent = new Intent(this, ActiveLocationUpdateService.class);
                startForegroundService(startIntent);

                Log.w(TAG, "onStartCommand: Stopping the passive service with stopSelf()");
                stopSelf();

                // Leave the method returning the super - we done here.
                return super.onStartCommand(intent, flags, startId);
            } else {
                Log.w(TAG, " !!!!!!! -= onStartCommand | STOPPING ALL SERVICES APPARENTLY! =- !!!!!!!");
                stopSelf();
                AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType.SERVER_TRIP_STOPPED);
            }
        }
        
        AlarmReceiver.setNextAlarm();

        if (intent == null || intent.getStringExtra(TRIPCODE) == null) {
            try {
                throw new Exception("No tripcode was included when starting this service!!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        tripcode = intent.getStringExtra(TRIPCODE);

        // Build the notification required for a foreground service
        Notification notification = StaticHelpers.Notifications.showNotification(this,
                "WhereYouAt is Running!", "Your location is passively being monitored " +
                        "and transmitted to your group members.", 0);

        // Formally start as a foreground service
        startForeground(MAIN_NOTIFICATION_ID, notification);

        isRunning = true;

        MyApp.setIsPendingCancel(false);

        AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType.PASSIVE_LOCATION_SERVICE_STARTED);

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

    public static class AlarmReceiver extends BroadcastReceiver {

        public static final int BG_PERMISSION_REQUEST_NOTIFICATION = 88;
        public static final String BG_PERMISSION_REQUEST_NOTIF = "BG_PERMISSION_REQUEST_NOTIF";
        private NotificationManager mNotificationManager;

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
                    if (StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION)) {
                        getMyLastKnownLocation();
                    } else {
                        Notification notification = showNotification(
                                context, "Background Location", "We need to change " +
                                        "the app's permissions to allow background location monitoring " +
                                        "in order to share your location with the members of this group!\n\n" +
                                        "Click here to allow the permission!", NotificationManager
                                        .IMPORTANCE_MAX);
                        Log.w(TAG, "onReceive: APP IN BG - NO PERMS - NEED DO SOME");
                        mNotificationManager.notify(BG_PERMISSION_REQUEST_NOTIFICATION, notification);
                    }
                }

            }
            catch (Exception e) {
                Toast.makeText(context, "There was an error somewhere, but we still received an alarm", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            setNextAlarm();

        }

        public static void setNextAlarm() {
            Log.w("", "setNextAlarm: ****************************************************");
            Log.w("", "setNextAlarm:                  SETTING NEXT ALARM!                ");
            Log.w("", "setNextAlarm: *****************************************************");

            alarmIntent = new Intent ( context, AlarmReceiver.class );
            pendingIntent = PendingIntent.getBroadcast( MyApp.getAppContext().getApplicationContext(), NEXT_LOCATION_REQUEST_CODE, alarmIntent, 0 );
            alarmManager = ( AlarmManager ) MyApp.getAppContext().getSystemService( ALARM_SERVICE );
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + CHECK_FREQUENCY, pendingIntent);
        }

        public static void getMyLastKnownLocation(InstantLocationListener listener) {

            Log.w(TAG, "onReceive: Querying last known location now...");

            LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));

            // Save this location to the database
            LocalUserLocation localUserLocation = new LocalUserLocation(location);
            localUserLocation.saveToLocalDb();
            listener.onLocationReceived(localUserLocation);
            Log.i(TAG, "onLocationChanged | " + localUserLocation.saveToLocalDb());
        }

        public void getMyLastKnownLocation() {

            Log.w(TAG, "onReceive: Querying last known location now...");

            // Get the location manager service
            LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            // Try to get the device's last known location
            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));

            // If we have a valid location then we can update the server with that location
            if (location != null) {
                Log.i(TAG, "getMyLastKnownLocation There is a cached location here!");
                double lat = location.getLatitude();
                double longi = location.getLongitude();
                LatLng latLng = new LatLng(lat,longi);
                Log.w(TAG, "getMyLastKnownLocation: Provider: " + location.getProvider() + ", Lat: " + lat + ", long: " + longi);
                updateServerLocation(location);
            } else {
                Log.w(TAG, "getMyLastKnownLocation: No cached location found - will have to spin up the sensors...");
                setMyLastLocation();
            }

            if (location != null) {
                // Update the static lastKnonLoc
                lastKnownLocation = location;

                // Send a broadcast stating the local user's location has changed in case there is a caller that cares.
                AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType.LOCATION_CHANGED_LOCALLY, location);

                // Save this local location to the local locations table
                LocalUserLocation localUserLocation = new LocalUserLocation(location);
                Log.i(TAG, "onLocationChanged | " + localUserLocation.saveToLocalDb());
            }
        }

        private void setMyLastLocation() {
            Log.d(TAG, "setMyLastLocation: excecute, and get last location");
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (!StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION)) {

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

        private Notification showNotification(Context context, String title, String contentText, int importance) {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context.getApplicationContext(), "22");

            Intent i = new Intent(context.getApplicationContext(), MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.putExtra(BG_PERMISSION_REQUEST_NOTIF, true);
            i.setAction(BG_PERMISSION_REQUEST_NOTIF);

            Intent ii = new Intent(context.getApplicationContext(), MainActivity.class);
            ii.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.putExtra(BG_PERMISSION_REQUEST_NOTIF + "ii", true);
            i.setAction(BG_PERMISSION_REQUEST_NOTIF + "ii");

            PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), BG_PERMISSION_REQUEST_NOTIFICATION, i
                    , PendingIntent.FLAG_UPDATE_CURRENT);


            PendingIntent stopTripIntent = PendingIntent.getActivity(context.getApplicationContext(), BG_PERMISSION_REQUEST_NOTIFICATION
                    , ii, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(pendingIntent);
            mBuilder.setOngoing(true);
            mBuilder.setSmallIcon(R.drawable.ic_menu_camera);
            mBuilder.setContentTitle(title);
            mBuilder.setContentText(contentText);
            mBuilder.addAction(R.drawable.ic_menu_gallery, "some action",
                    stopTripIntent);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilder.setSmallIcon(R.drawable.ic_menu_gallery);
                mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_round));
                mBuilder.setColor(Color.WHITE);
            } else {
                Log.i(TAG, "getNotification ");
            }

            mNotificationManager = (NotificationManager) context.getApplicationContext()
                    .getSystemService(context.getApplicationContext().NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = "22";
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "WHEREYOUAT",
                        importance);
                mNotificationManager.createNotificationChannel(channel);
                mBuilder.setChannelId(channelId);
            }

            Notification notif = mBuilder.build();

            notif.flags = Notification.FLAG_ONGOING_EVENT;

            return notif;
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
            request.arguments.add(new Requests.Arguments.Argument("passive/active", LOCATION_TYPE));

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
