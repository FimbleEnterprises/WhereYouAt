package com.fimbleenterprises.whereuat;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.rest_api.Requests;
import com.fimbleenterprises.whereuat.rest_api.WebApi;
import com.fimbleenterprises.whereuat.services.ActiveLocationUpdateService;
import com.fimbleenterprises.whereuat.services.PassiveLocationUpdateService;
import com.fimbleenterprises.whereuat.ui.other.PermissionsActivity;
import com.google.firebase.FirebaseApp;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class MyApp extends Application {

    public static final int REQUEST_LOC_PERMS = 444;
    private static final String TAG = "MyApp";
    public static final String LEFT_TRIP = "LEFT_TRIP";
    private static Context context;
    private static boolean activityVisible;

    public static boolean isActivityVisible() {
        Log.w(TAG, "*** isActivityVisible() | " + activityVisible + " ***");
        return activityVisible;
    }

    public static void activityResumed() {
        Log.w(TAG, " !!!!!!! -= activityResumed =- !!!!!!!");
        Log.w(TAG, "activityResumed | ActiveLocationService isRunning: " + ActiveLocationUpdateService.isRunning);
        Log.w(TAG, "activityResumed | PassiveLocationService isRunning: " + PassiveLocationUpdateService.isRunning);
        activityVisible = true;
    }

    public static void activityPaused() {
        Log.w(TAG, " !!!!!!! -= activityPaused =- !!!!!!!");
        Log.w(TAG, "activityPaused | ActiveLocationService isRunning: " + ActiveLocationUpdateService.isRunning);
        Log.w(TAG, "activityPaused | PassiveLocationService isRunning: " + PassiveLocationUpdateService.isRunning);
        activityVisible = false;
    }

    public void onCreate() {
        super.onCreate();
        Log.i(TAG, " !!!!!!! -= onCreate | MAIN APPLICATION ONCREATE =- !!!!!!!");
        MyApp.context = getApplicationContext();
        Log.i(TAG, "onCreate | Initializing Firebase application-wide...");
        try {
            FirebaseApp.initializeApp(getAppContext());
            Log.i(TAG, "onCreate | Firebase initialization comnpleted without error.  " +
                    "No further validation was done though sooo... not sure if that's terribly helpful or not.");
        } catch (Exception e) {
            Log.w(TAG, "onCreate: | Firebase initialization had a problem.  See below stacktrace.");
            e.printStackTrace();
        }
    }

    public static boolean hasBgLocationPermission() {
        boolean result = StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION);
        Log.i(TAG, "hasBgLocationPermission | " + result);
        return result;
    }

    public static boolean hasCoarseLocationPermission() {
        boolean result = StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_COARSE_LOCATION);
        Log.i(TAG, "hasCoarseLocationPermission | " + result);
        return result;
    }

    public static boolean hasFineLocationPermission() {
        boolean result = StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_FINE_LOCATION);
        Log.i(TAG, "hasFineLocationPermission | " + result);
        return result;
    }

    public static boolean hasAllLocationPermissions() {
        boolean result = hasBgLocationPermission() && hasCoarseLocationPermission() && hasFineLocationPermission();
        Log.i(TAG, "hasAllLocationPermissions | " + result);
        return result;
    }

    /**
     * Makes the OS permission request for all location perms.
     * @param activity A suitable context that can override onPermissionRequestResult because
     *                 that's what will happen after a selection gets made by the user.
     */
    public static void requestLocPermissions(Activity activity) {
        StaticHelpers.Permissions.RequestContainer container = new StaticHelpers.Permissions.RequestContainer();
        container.add(StaticHelpers.Permissions.PermissionType.ACCESS_FINE_LOCATION);
        container.add(StaticHelpers.Permissions.PermissionType.ACCESS_COARSE_LOCATION);
        container.add(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION);
        activity.requestPermissions(container.toArray(), REQUEST_LOC_PERMS);
    }

    /**
     * Makes the OS permission request for all location perms.
     * @param activity A suitable context that can override onPermissionRequestResult because
     *                 that's what will happen after a selection gets made by the user.
     */
    public static void requestLocPermissions(Fragment activity) {
        StaticHelpers.Permissions.RequestContainer container = new StaticHelpers.Permissions.RequestContainer();
        container.add(StaticHelpers.Permissions.PermissionType.ACCESS_FINE_LOCATION);
        container.add(StaticHelpers.Permissions.PermissionType.ACCESS_COARSE_LOCATION);
        container.add(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION);
        activity.requestPermissions(container.toArray(), REQUEST_LOC_PERMS);
    }

    /**
     * Makes the OS permission request for all location perms.
     * @param activity A suitable context that can override onPermissionRequestResult because
     *                 that's what will happen after a selection gets made by the user.
     */
    public static void requestLocPermissions(FragmentActivity activity) {
        StaticHelpers.Permissions.RequestContainer container = new StaticHelpers.Permissions.RequestContainer();
        container.add(StaticHelpers.Permissions.PermissionType.ACCESS_FINE_LOCATION);
        container.add(StaticHelpers.Permissions.PermissionType.ACCESS_COARSE_LOCATION);
        container.add(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION);
        activity.requestPermissions(container.toArray(), REQUEST_LOC_PERMS);
    }

    /**
     * Nothing more than validating all location permissions and showing the fix permissions activity if necessary.
     */
    public static void validatePermissionWithPathToFix(Activity activity) {

        Log.i(TAG, "validatePermissionWithPathToFix - Checking permissions...");

        if (!hasAllLocationPermissions()) {

            Intent intent = new Intent(getAppContext(), PermissionsActivity.class);
            activity.startActivityForResult(intent, REQUEST_LOC_PERMS);
        }
    }

    /**
     * Nothing more than validating all location permissions and showing the fix permissions activity if necessary.
     */
    public static void validatePermissionWithPathToFix(FragmentActivity activity) {

        if (!hasAllLocationPermissions()) {

            Intent intent = new Intent(getAppContext(), PermissionsActivity.class);
            activity.startActivityForResult(intent, REQUEST_LOC_PERMS);
        }
    }

    /**
     * Nothing more than validating all location permissions and showing the fix permissions activity if necessary.
     */
    public static void validatePermissionWithPathToFix(Fragment activity) {

        if (!hasAllLocationPermissions()) {

            Intent intent = new Intent(getAppContext(), PermissionsActivity.class);
            activity.startActivityForResult(intent, REQUEST_LOC_PERMS);
        }
    }

    public static void stopAllLocationServices() {

        Log.w(TAG, " !!!!!!! -= stopAllLocationServices | STOPPING ALL LOCATION SERVICES! =- !!!!!!!");

        Intent stopActive = new Intent(getAppContext(), ActiveLocationUpdateService.class);
        stopActive.putExtra(ActiveLocationUpdateService.SWITCH_TO_PASSIVE_SERVICE, false);
        Intent stopPassive = new Intent(getAppContext(), PassiveLocationUpdateService.class);
        stopPassive.putExtra(PassiveLocationUpdateService.SWITCH_TO_ACTIVE_SERVICE, false);

        // I feel compelled to remind future me that calling startService() is a perfectly valid way
        // of STOPPING a running service.  Doing it this way allows you to include intent extras that
        // cannot be included if stopService() is called instead.  You're welcome future me!
        getAppContext().startService(stopActive);
        getAppContext().startService(stopPassive);
    }

    public static boolean isReportingLocation() {
        Log.i(TAG, "isReportingLocation | \nActive: " + ActiveLocationUpdateService.isRunning +
                "\nPassive: " + PassiveLocationUpdateService.isRunning);
        return (ActiveLocationUpdateService.isRunning || PassiveLocationUpdateService.isRunning);
    }

    /**
     * I don't feel good about this methodology but gets either the passive or active location service
     * tripcode.
     * @return The tripcode from either the passive or the active location service.
     */
    public static String getCurrentTripcode() {
        if (isReportingLocation()) {
            if (PassiveLocationUpdateService.isRunning) {
                return PassiveLocationUpdateService.tripcode;
            }
            if (ActiveLocationUpdateService.isRunning) {
                return ActiveLocationUpdateService.tripcode;
            }
        }
        return null;
    }

    private static boolean pendingCancel = false;

    public static boolean getIsPendingCancel() {
        return pendingCancel;
    }

    public static void setIsPendingCancel(boolean val) {
        pendingCancel = val;
    }

    /**
     * Sends an API request to the server to remove any rows in the TripEntry table bearing the user's googleid.
     * @param listener
     */
    public static void leaveServerTrip(final WebApi.WebApiResultListener listener) {

        // Send stop requests to both services
        Intent stopActive = new Intent(getAppContext(), ActiveLocationUpdateService.class);
        Intent stopPassive = new Intent(getAppContext(), PassiveLocationUpdateService.class);

        try {
            getAppContext().stopService(stopActive);
            getAppContext().stopService(stopPassive);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Ask the server to remove us from the TripEntries table
        Requests.Request request = new Requests.Request(Requests.Request.Function.LEAVE_TRIP);
        request.arguments.add(new Requests.Arguments.Argument("userid", GoogleUser.getCachedUser().id));
        WebApi api = new WebApi();
        api.makeRequest(request, new WebApi.WebApiResultListener() {
            @Override
            public void onSuccess(WebApi.OperationResults results) {
                listener.onSuccess(results);
                Intent intent = new Intent(TAG);
                intent.putExtra(LEFT_TRIP, true);
                getAppContext().sendBroadcast(intent);

            }

            @Override
            public void onFailure(String message) {
                listener.onFailure(message);
            }
        });
    }

    public static Context getAppContext() {
        return MyApp.context;
    }
}
