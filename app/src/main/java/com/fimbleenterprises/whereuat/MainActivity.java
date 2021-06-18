package com.fimbleenterprises.whereuat;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.Toast;

import com.fimbleenterprises.whereuat.firebase.MyFcmHelper;
import com.fimbleenterprises.whereuat.generic_objs.UserMessage;
import com.fimbleenterprises.whereuat.googleuser.MyGoogleSignInHelper;
import com.fimbleenterprises.whereuat.helpers.MyNotificationManager;
import com.fimbleenterprises.whereuat.helpers.StaticHelpers;
// import com.fimbleenterprises.whereuat.services.ActiveLocationUpdateService;
import com.fimbleenterprises.whereuat.local_database.TripDatasource;
import com.fimbleenterprises.whereuat.services.LocationTrackingService;
import com.fimbleenterprises.whereuat.ui.home.MainPager;
import com.fimbleenterprises.whereuat.ui.other.NotSignedInActivity;
import com.fimbleenterprises.whereuat.ui.other.PermissionsActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity implements DrawerLayout.DrawerListener {

    private static final String TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;

    public static final String FLOATING_BUTTON_CLICKED = "FLOATING_BUTTON_CLICKED";

    // These flags will prevent circular logic when resuming the app and their respective
    // conditions are not met.
    public static boolean shouldShowSignIn = true;
    public static boolean shouldRequestPermissions = true;
    public DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, " !!!!!!! -= onCreate  =- !!!!!!!");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                */
                sendBroadcast(new Intent(FLOATING_BUTTON_CLICKED));
            }
        });


        // Add a reference to the drawer and use this activity as the listener (implements DrawerListener)
        drawer = findViewById(R.id.drawer_layout);
        drawer.addDrawerListener(this);

        // Fuck this nav view.  I just don't get it!  Shit works but don't fuck with it or it may stop working!
        NavigationView navigationView = findViewById(R.id.nav_view);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Check if we are here due to the user clicking the new message received notification
        if (getIntent() != null &&
            getIntent().getAction() != null &&
            getIntent().getAction().equals(MyNotificationManager.IS_MESSAGE) &&
            getIntent().hasExtra(MyNotificationManager.IS_MESSAGE)) {
            Log.w(TAG, "onCreate | User clicked a message received notificatioN!");

            // Grab the messages from the database - the new message was written to the db by the
            // FcmReceiver when it received the FCM so it will be there.
            TripDatasource ds = new TripDatasource();
            ArrayList<UserMessage> msgs = ds.getAllUserMessages(MyApp.getCurrentTripcode());

            // Send a global broadcast in case the user had the app open when they clicked the notification.
            // The broadcast receivers won't be registered if it was in the background so this won't
            // ever be heard - but if it was in the foreground we can use this broadcast to move pages.
            AppBroadcastHelper.sendGeneralBroadcast(AppBroadcastHelper.BroadcastType.MESSAGE_RECEIVED);

            // Logging because who doesn't love spamming LogCat?
            Log.i(TAG, "onCreate Get all user msgs for tripcode | " + msgs.size() + " items.");

            // Set this global flag.  It is sort of hacky but this will be how the MainPager class
            // will know to move to the messages page after it loads up.  For the record,
            // it will get reset to false when the Frag_Messages' OnCreateView(...) method is called.
            // I have commented where this flag is evaluated in the MainPager class if you want more
            // details (all this fuckery cost me a full half day and resulted in this stupid flag).
            MyApp.setNewMessagePending(true);
        }

    }

    // region OTHER OVERRIDES

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.w(TAG, " !!!!!!! -= onKeyDown | BACK PRESS DETECTED AT MAINACTIVITY =- !!!!!!!");

            // If the drawer is open we always close it, call it a day, and do no more.
            if (drawer.isOpen()) {
                Log.i(TAG, "onKeyDown | Drawer is open so back press will close it and do no more.");
                drawer.close();
                return true;
            }

            // Check if a trip is running
            if (MyApp.isReportingLocation()) {
                switch (MainPager.mViewPager.getCurrentItem()) {
                    case MainPager.SectionsPagerAdapter.JOIN_CREATE:
                        finish();
                        break;
                    case MainPager.SectionsPagerAdapter.VIEW_MEMBERS:
                        MainPager.mViewPager.setCurrentItem(MainPager.SectionsPagerAdapter.JOIN_CREATE);
                        break;
                    case MainPager.SectionsPagerAdapter.VIEW_MAP:
                        MainPager.mViewPager.setCurrentItem(MainPager.SectionsPagerAdapter.VIEW_MEMBERS);
                        break;
                }
            } else {
                if (MainPager.mViewPager.getCurrentItem() == MainPager.SectionsPagerAdapter.JOIN_CREATE) {
                    finish();
                } else {
                    MainPager.mViewPager.setCurrentItem(MainPager.SectionsPagerAdapter.JOIN_CREATE);
                }
            }

            return true;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // We send request codes with both the permission and login activities.  Check if they are
        // the source of this activity result so we can act accordingly (show sign in/permission fix etc.).
        if (requestCode == NotSignedInActivity.SIGN_IN_REQUEST_CODE) {
            if (!MyGoogleSignInHelper.isSignedIn(this)) {
                // Flag this so as not to pester the user or create an infinite loop of failure.
                shouldShowSignIn = false;
            }
        } else if (requestCode == PermissionsActivity.REQUEST_PERMS) {
            if (!MyApp.hasFineLocationPermission() || !MyApp.hasCoarseLocationPermission()) {
                // Flag this so as not to pester the user or create an infinite loop of failure.
                shouldRequestPermissions = false;
            }
        }



    }

    @Override
    protected void onPause() {
        Log.i(TAG, " !!!!!!! -= onPause  =- !!!!!!!");
        MyApp.activityPaused();

        // Check if location is currently being tracked - if so, stop the active tracker and start
        // the passive.
        if (MyApp.isReportingLocation()) {

            Log.w(TAG, "onPause | App is pausing.  Will kill the active service and start the passive service...");

            /*if (ActiveLocationUpdateService.isRunning) {
                // Stop the active service
                Intent stopIntent = new Intent(this, ActiveLocationUpdateService.class);
                stopIntent.putExtra(ActiveLocationUpdateService.SWITCH_TO_PASSIVE_SERVICE, true);
                startForegroundService(stopIntent);
            } else if (PassiveLocationUpdateService.isRunning) {
                // Stop the passive service
                Intent stopIntent = new Intent(this, PassiveLocationUpdateService.class);
                stopIntent.putExtra(PassiveLocationUpdateService.SWITCH_TO_ACTIVE_SERVICE, true);
                startForegroundService(stopIntent);
            }*/

        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, " !!!!!!! -= onResume  =- !!!!!!!");
        super.onResume();
        MyApp.activityResumed();

        // Check if the user is signed in and whether or not the flag allows us to ask them to sign in if not.
        if (!MyGoogleSignInHelper.isSignedIn(this) && shouldShowSignIn) {
            MyGoogleSignInHelper.showSignInActivity(this);
        } else if (MyGoogleSignInHelper.isSignedIn(this)) { // User is signed in!
            // Now check permissions, ugh.
            if (StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION) &&
                    StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION) &&
                    StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION)) {
                // Has all permissions!  Resume the app!
                super.onResume();
            } else if (StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_FINE_LOCATION) &&
                    StaticHelpers.Permissions.isGranted(StaticHelpers.Permissions.PermissionType.ACCESS_COARSE_LOCATION)) {
                super.onResume();
                Toast.makeText(this, "Background location not granted.  It'll still work, just not nearly as cool", Toast.LENGTH_SHORT).show();
            } else { // User lacks permissions - see if the flag allows pestering them to remedy that.
                if (!shouldRequestPermissions) {
                    // Give up and close.
                    finish();
                    shouldRequestPermissions = true;
                } else {
                    // Give the user the opportunity to rectify the perm problem.
                    Intent intent = new Intent(this, PermissionsActivity.class);
                    startActivityForResult(intent, PermissionsActivity.REQUEST_PERMS);
                }
            }

            super.onResume();
        } else {
            // Allow the sign in dialog the next time we get to onResume()
            shouldShowSignIn = true;
            // Finish the activity without fanfare.
            finish();
        }

        // Do FCM validation with all of the fucking callbacks!
        MyFcmHelper.getNewToken(new MyFcmHelper.RequestFcmTokenListener() {
            @Override
            public void onSuccess(String token) {
                Log.i(TAG, "onSuccess | Fcm token was received from Google!");
            }

            @Override
            public void onFailure(String msg) {
                Log.w(TAG, "onFailure: | Fcm token was not received from Google!  Error:\n" + msg);
            }
        }, new MyFcmHelper.UpsertFcmListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess | Fcm token was upserted to our server!");
            }

            @Override
            public void onFailure(String msg) {
                Log.w(TAG, "onFailure: | Fcm token was not upserted!  Error:\n" + msg);
            }
        });

        /*// If a trip is running, stop the passive location service and switch to the active one.
        if (MyApp.isReportingLocation()) {
            Log.i(TAG, "onResume | Requesting the passive be stopped and the active be started!");

            Intent stopPassiveIntent = new Intent(this, LocationTrackingService.class);
            stopPassiveIntent.putExtra(LocationTrackingService.SWITCH_TO_ACTIVE_SERVICE, true);
            startService(stopPassiveIntent);
        }*/

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, " !!!!!!! -= onRequestPermissionsResult  =- !!!!!!!");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, " !!!!!!! -= onCreateOptionsMenu  =- !!!!!!!");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        menu.findItem(R.id.action_signinout).setTitle(MyGoogleSignInHelper.isSignedIn(this) ? "Sign out" : "Sign in");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_signinout:
                if (MyGoogleSignInHelper.isSignedIn(this)) {
                    MyGoogleSignInHelper.signOut(this);
                }

                MyGoogleSignInHelper.showSignInActivity(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.w(TAG, " !!!!!!! -= onSupportNavigateUp  =- !!!!!!!");
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
        Log.i(TAG, " !!!!!!! -= onDrawerSlide  =- !!!!!!!");

    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {
        Log.i(TAG, " !!!!!!! -= onDrawerOpened  =- !!!!!!!");
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        Log.i(TAG, " !!!!!!! -= onDrawerClosed  =- !!!!!!!");
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        Log.i(TAG, " !!!!!!! -= onDrawerStateChanged  =- !!!!!!!");
        Log.i(TAG, "onDrawerStateChanged | State: " + newState);
    }

    // endregion


}