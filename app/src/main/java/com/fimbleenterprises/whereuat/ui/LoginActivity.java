package com.fimbleenterprises.whereuat.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;

import com.fimbleenterprises.whereuat.AppBroadcastHelper;
import com.fimbleenterprises.whereuat.MainActivity;
import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.MyProgressDialog;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.helpers.KeyboardHelper;
import com.fimbleenterprises.whereuat.helpers.MyNotificationManager;
import com.fimbleenterprises.whereuat.local_database.TripDatasource;
import com.fimbleenterprises.whereuat.local_database.TripReport;
import com.fimbleenterprises.whereuat.rest_api.Requests;
import com.fimbleenterprises.whereuat.rest_api.WebApi;
import com.fimbleenterprises.whereuat.services.LocationTrackingService;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.fimbleenterprises.whereuat.R;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Frag_JoinCreate";
    public static final String ARG_SECTION_NUMBER = "section_number";

    // A receiver for catching app-wide broadcasts sent from a variety of different code locations.
    BroadcastReceiver mainAppReceiver;
    
    Context context;

    // Onscreen keyboard reference.
    InputMethodManager imm;

    Button btnJoin;
    Button btnCreate;
    Button btnLeave;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.frag_join_create);
        super.onCreate(savedInstanceState);
        this.context = this;

        // Get references to our views
        btnJoin = findViewById(R.id.btnJoin);
        btnCreate = findViewById(R.id.btnCreate);
        btnLeave = findViewById(R.id.btnLeave);

        // Add listeners to our views
        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(context);
                final Context c = context;
                dialog.setContentView(R.layout.dialog_enter_trip_code);
                dialog.setCancelable(true);
                final EditText txtTripCode = dialog.findViewById(R.id.txtTripCode);
                Button btnJoin = dialog.findViewById(R.id.btnDialogJoinTrip);

                btnJoin.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String code = txtTripCode.getText().toString();
                        if (code != null && code.length() > 3) {
                            joinTrip(code);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(context, "Please enter a valid trip code", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            dialog.dismiss();
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        KeyboardHelper keyboardHelper = new KeyboardHelper(context);
                        keyboardHelper.hideSoftKeyboard(txtTripCode);
                    }
                });

                dialog.show();
                KeyboardHelper keyboardHelper = new KeyboardHelper(context);
                keyboardHelper.showSoftKeyboard(txtTripCode);
            }
        });

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyNotificationManager notificationManager = new MyNotificationManager();
                Log.i(TAG, "onClick | Low channel: " + notificationManager.lowPriorityChannelExists());
                Log.i(TAG, "onClick | High channel: " + notificationManager.highPriorityChannelExists());

                // notificationManager.showHighPriorityNotification();
                notificationManager.showLowPriorityNotification();

                createTrip();

            }
        });

        btnLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Dialog because we're classy like that.
                final MyProgressDialog dg = new MyProgressDialog(context, "Leaving group...");
                dg.show();

                // Send stop intents to both active and passive services - though only the active
                // service should be running whenever this method is called.  Sending stops to both
                // anyway
                MyApp.stopAllLocationServices();

                // Send a request to the server to remove the user's entries from the TripEntries
                // table.  This will also send an FCM to all members with the user's GoogleUser object as the obj
                // property of the payload.
                WebApi api = new WebApi(context);
                Requests.Request request = new Requests.Request(Requests.Request.Function.LEAVE_TRIP);
                request.arguments.add(new Requests.Arguments.Argument("userid", GoogleUser.getCachedUser().id));
                request.arguments.add(new Requests.Arguments.Argument("tripcode", "0000"));
                api.makeRequest(request, new WebApi.WebApiResultListener() {
                    @Override
                    public void onSuccess(WebApi.OperationResults results) {
                        if (results.list.get(0).wasSuccessful) {
                            Toast.makeText(context, "You have left the group!", Toast.LENGTH_SHORT).show();
                        }
                        dg.dismiss();
                    }

                    @Override
                    public void onFailure(String message) {
                        Log.w(TAG, " !!!!!!! -= onFailure | " + message + " =- !!!!!!!");
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        dg.dismiss();
                    }
                });
            }
        });

        mainAppReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // There should always be a serializable extra representing a BroadcastType enum
                // with a tag of BROADCAST_TYPE.  This can be extrapolated using a switch statement.
                // There will also often be an extra titled, "PARCELLED_EXTRA" that can be cast to
                // various objects depending on the aforementioned type (e.g. a Location object
                // if the type is LOCATION_CHANGED_LOCALLY or a TripReport object on type
                // SERVER_LOCATION_UPDATED).
                AppBroadcastHelper.BroadcastType type = (AppBroadcastHelper.BroadcastType)
                        intent.getSerializableExtra(AppBroadcastHelper.BROADCAST_TYPE);

                Log.i(TAG, "onReceive | BroadcastType: " + type.name());
                Log.i(TAG, "onReceive | Has PARCELLED_EXTRA: " + intent.hasExtra(AppBroadcastHelper.PARCELED_EXTRA));

                switch (type) {
                    case ACTIVE_LOCATION_SERVICE_STARTED:

                        break;
                    case ACTIVE_LOCATION_SERVICE_STOPPED:

                        break;
                    case LOCATION_TRACKING_SERVICE_STARTED:

                        break;
                    case PASSIVE_LOCATION_SERVICE_STOPPED:

                        break;
                    case LOCATION_CHANGED_LOCALLY:
                        try {
                            Location localLoc = (Location) intent.getParcelableExtra(AppBroadcastHelper.PARCELED_EXTRA);
                            Log.i(TAG, "onReceive | Extra is of type: Location!\nLocal loc | Lat: " + localLoc.getLatitude()
                                    + " Lon: " + localLoc.getLongitude() + " Acc: " +
                                    localLoc.getAccuracy() + " meters");

                            // mViewPager.setCurrentItem(1);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case SERVER_LOCATION_UPDATED:

                        break;
                    case SERVER_TRIP_STARTED:

                        break;
                    case SERVER_TRIP_STOPPED:
                        updateUI();
                        break;
                    case USER_LEFT_TRIP:

                        break;
                    case USER_JOINED_TRIP:
                        GoogleUser user = (GoogleUser) intent.getParcelableExtra(AppBroadcastHelper.PARCELED_EXTRA);
                        Log.i(TAG, "onReceive | User joined trip (" + user.fullname + ")");
                        break;
                }

                // Regardless of the broadcast type we update the UI accordingly
                updateUI();

            }
        };
    }

    @Override
    public void onStart() {

        Log.i(TAG, " !!!!!!! -= onStart  =- !!!!!!!");

        // Register the app-wide broadcast receiver
        context.registerReceiver(mainAppReceiver,
                new IntentFilter(AppBroadcastHelper.GLOBAL_BROADCAST));
        Log.i(TAG, "onResume | Registered the mainAppReceiver - listening for app-wide broadcasts!");

        updateUI();

        super.onStart();
    }

    @Override
    public void onStop() {

        Log.i(TAG, " !!!!!!! -= onStop  =- !!!!!!!");

        // Unregister the broadcast receiver
        context.unregisterReceiver(mainAppReceiver);
        Log.i(TAG, "onPause | Unregistered the mainAppReceiver - app-wide broadcasts will go unnoticed here.");

        super.onStop();
    }

    @Override
    public void onResume() {
        Log.i(TAG, " !!!!!!! -= onResume =- !!!!!!!");
        updateUI();


        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, " !!!!!!! -= onPause  =- !!!!!!!");
        super.onPause();
    }

    void joinTrip(String tripcode) {
        final MyProgressDialog dg = new MyProgressDialog(context, "Joining group...");
        dg.show();

        WebApi api = new WebApi(context);
        Requests.Request request = new Requests.Request(Requests.Request.Function.JOIN_TRIP);
        request.arguments.add(new Requests.Arguments.Argument("userid", GoogleUser.getCachedUser().id));
        request.arguments.add(new Requests.Arguments.Argument("tripcode", tripcode));
        api.makeRequest(request, new WebApi.WebApiResultListener() {
            @Override
            public void onSuccess(WebApi.OperationResults results) {
                if (results.list.get(0).wasSuccessful) {

                    // Get the new, current trip report as reported by the server just now
                    TripReport tripReport = new TripReport(results.list.get(0).result);

                    // Clear the existing local db completely!
                    TripDatasource ds = new TripDatasource();

                    boolean delResult = ds.deleteMemberUpdatesByTripcode(tripReport.tripcode);
                    Log.w(TAG, "onClick: DELETED ALL EXISTING LOCAL UPDATES FOR TRIPCODE: "
                            + tripReport + " | Result: " + delResult);

                    Log.i(TAG, "onSuccess | " + results.list.size() + " results");

                    // Save the new, current trip report to the server.
                    tripReport.saveToLocalDb();

                    // Start the service up - we inna trip!
                    String tripcode = tripReport.tripcode;
                    Intent startServiceIntent = new Intent(context, LocationTrackingService.class);
                    startServiceIntent.putExtra(LocationTrackingService.TRIPCODE, tripcode);
                    context.startForegroundService(startServiceIntent);

                    finishAndRemoveTask();
                    Intent intent = new Intent(context, MainActivity.class);
                    startActivity(intent);

                    // new TripDatasource().deleteAlerts();

                } else {
                    if (results.list.get(0).operationSummary.equals(WebApi.OperationResults.ERROR_TRIP_NOT_FOUND)) {
                        Toast.makeText(context, "Trip code not found!", Toast.LENGTH_SHORT).show();
                    }
                }
                dg.dismiss();
            }

            @Override
            public void onFailure(String message) {
                Log.w(TAG, " !!!!!!! -= onFailure | " + message + " =- !!!!!!!");
                dg.dismiss();
            }
        });
    }

    void createTrip() {
        final MyProgressDialog dg = new MyProgressDialog(context, "Joining group...");
        dg.show();

        WebApi api = new WebApi(context);
        Requests.Request request = new Requests.Request(Requests.Request.Function.CREATE_NEW_TRIP);
        request.arguments.add(new Requests.Arguments.Argument("userid", GoogleUser.getCachedUser().id));
        api.makeRequest(request, new WebApi.WebApiResultListener() {
            @Override
            public void onSuccess(WebApi.OperationResults results) {
                if (results.list.get(0).wasSuccessful) {

                    Log.i(TAG, "onSuccess ");

                    TripReport tripReport = new TripReport(results.list.get(0).result);
                    // Save the new, current trip report to the server.
                    tripReport.saveToLocalDb();

                    // Start the service up - we inna trip!
                    String tripcode = tripReport.tripcode;
                    Intent startServiceIntent = new Intent(context, LocationTrackingService.class);
                    startServiceIntent.putExtra(LocationTrackingService.TRIPCODE, tripcode);
                    context.startForegroundService(startServiceIntent);

                    finishAndRemoveTask();
                    Intent intent = new Intent(context, MainActivity.class);
                    startActivity(intent);

                    // new TripDatasource().deleteAlerts();

                } else {
                    if (results.list.get(0).operationSummary.equals(WebApi.OperationResults.ERROR_TRIP_NOT_FOUND)) {
                        Toast.makeText(context, "Trip code not found!", Toast.LENGTH_SHORT).show();
                    }
                }
                dg.dismiss();
            }

            @Override
            public void onFailure(String message) {
                Log.w(TAG, " !!!!!!! -= onFailure | " + message + " =- !!!!!!!");
                dg.dismiss();
            }
        });
    }

    void updateUI() {
        btnLeave.setEnabled(MyApp.isReportingLocation());
        btnJoin.setEnabled(!MyApp.isReportingLocation());
        btnCreate.setEnabled(!MyApp.isReportingLocation());
    }
}