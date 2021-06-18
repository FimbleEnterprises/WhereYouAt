package com.fimbleenterprises.whereuat.ui.home;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerTitleStrip;

import com.fimbleenterprises.whereuat.MainActivity;
import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.MyProgressDialog;
import com.fimbleenterprises.whereuat.MyViewPager;
import com.fimbleenterprises.whereuat.NonScrollRecyclerView;
import com.fimbleenterprises.whereuat.R;
import com.fimbleenterprises.whereuat.adapters.MemberListAdapter;
import com.fimbleenterprises.whereuat.adapters.UserMessagesAdapter;
import com.fimbleenterprises.whereuat.generic_objs.MessageDialog;
import com.fimbleenterprises.whereuat.generic_objs.MyMapMarkers;
import com.fimbleenterprises.whereuat.generic_objs.UserMessage;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.helpers.KeyboardHelper;
import com.fimbleenterprises.whereuat.helpers.MyNotificationManager;
import com.fimbleenterprises.whereuat.helpers.StaticHelpers;
import com.fimbleenterprises.whereuat.local_database.TripDatasource;
import com.fimbleenterprises.whereuat.local_database.TripReport;
import com.fimbleenterprises.whereuat.rest_api.Requests;
import com.fimbleenterprises.whereuat.rest_api.WebApi;
// import com.fimbleenterprises.whereuat.services.ActiveLocationUpdateService;
import com.fimbleenterprises.whereuat.AppBroadcastHelper;
import com.fimbleenterprises.whereuat.services.LocationTrackingService;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.scwang.smart.refresh.header.MaterialHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.util.ArrayList;

//////////////////////////////////////////////////////////////////////////////////////////////////
// This is effectively the application's HOME view - 90% of the app will be in this file!
//
// It is technically a fragment but that
// is only because I couldn't find another
// fucking way to implement a side drawer.
// Seriously a confusing fucking implementation!
//
// The view pager's fragments are all classed in this file.  This is the meat of the app.
//
// Matt Weber
// 10 - May - 2021
//////////////////////////////////////////////////////////////////////////////////////////////////

public class MainPager extends Fragment implements OnSuccessListener<LocationResult> {

    private static final String TAG = "HomeFragment";
    public static int curPageIndex = 0;

    private HomeViewModel homeViewModel;

    TextView txtStatus;
    public static MyViewPager mViewPager;
    public static PagerTitleStrip mPagerStrip;
    public static SectionsPagerAdapter sectionsPagerAdapter;
    BroadcastReceiver mainAppRootFragmentReceiver;
    public static ArrayList<TripReport.MemberUpdate> cachedAvatars = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, " !!!!!!! -= onCreateView  =- !!!!!!!");

        // new TripDatasource().deleteAllLocalMessages("0000");

        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.main_pager_layout, container, false);
        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                // textView.setText(s);
            }
        });

        sectionsPagerAdapter = new SectionsPagerAdapter(getActivity().getSupportFragmentManager());
        mViewPager = (MyViewPager) root.findViewById(R.id.main_pager_yo_sales_perf);
        mPagerStrip = (PagerTitleStrip) root.findViewById(R.id.pager_title_strip_sales_perf);
        mViewPager.setAdapter(sectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(0);
        mViewPager.setCurrentItem(0);
        mViewPager.setPageCount(4);
        mViewPager.addOnPageChangeChangedListener(new MyViewPager.MyPageChangedListener() {
            @Override
            public void onPageChanged(@Nullable Intent intent) {
                Log.i(TAG, "onPageChanged ");
                if (intent != null && intent.getAction() != null) {
                    Log.i(TAG, " !!!!!!! -= onPageChanged | Page changed with an intent ("
                            + intent.getAction() + ") | Passing it along to any listeners! =- !!!!!!!");
                    getContext().sendBroadcast(intent);
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        txtStatus = root.findViewById(R.id.txtServiceStatus);

        // This receiver listens for broadcasts sent from the AppBroadcastHelper class.
        mainAppRootFragmentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // There should always be a serializable extra representing a BroadcastType enum
                // with a tag of BROADCAST_TYPE.  This can be extrapolated using a switch statement.
                // There will also often be an extra titled, "PARCELLED_EXTRA" that can be cast to
                // various objects depending on the aforementioned type (e.g. a Location object
                // if the type is LOCATION_CHANGED_LOCALLY or a TripReport object on type
                // SERVER_LOCATION_UPDATED).
                AppBroadcastHelper.BroadcastType type = null;

                try {
                    type = (AppBroadcastHelper.BroadcastType)
                            intent.getSerializableExtra(AppBroadcastHelper.BROADCAST_TYPE);
                    Log.i(TAG, "onReceive | BroadcastType: " + type.name());
                    Log.i(TAG, "onReceive | Has PARCELLED_EXTRA: " + intent.hasExtra(AppBroadcastHelper.PARCELED_EXTRA));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (type == null) {
                    return;
                }

                switch (type) {
                    case ACTIVE_LOCATION_SERVICE_STARTED:
                        txtStatus.setText("Actively monitoring - " + MyApp.getCurrentTripcode());
                        Log.i(TAG, "onReceive | Active location start broadcast");
                        mViewPager.setCurrentItem(SectionsPagerAdapter.VIEW_MEMBERS);
                        break;
                    case ACTIVE_LOCATION_SERVICE_STOPPED:
                        Log.i(TAG, "onReceive | Active location stop broadcast");
                        if (!MyApp.isReportingLocation()) {
                            txtStatus.setText("Not currently monitoring");
                        }
                        break;
                    case LOCATION_TRACKING_SERVICE_STARTED:
                        txtStatus.setText("Passively monitoring - " + MyApp.getCurrentTripcode());
                        Log.i(TAG, "onReceive | Passive location start broadcast");
                        break;
                    case PASSIVE_LOCATION_SERVICE_STOPPED:
                        if (!MyApp.isReportingLocation()) {
                            txtStatus.setText("Not currently monitoring");
                        }
                        Log.i(TAG, "onReceive | Passive location stop broadcast");
                        break;
                    case LOCATION_CHANGED_LOCALLY:
                        try {
                            Location localLoc = (Location) intent.getParcelableExtra(AppBroadcastHelper.PARCELED_EXTRA);
                            Log.i(TAG, "onReceive | Local loc | Lat: " + localLoc.getLatitude()
                                    + " Lon: " + localLoc.getLongitude() + " Acc: " + localLoc.getAccuracy() + " meters");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case SERVER_LOCATION_UPDATED:

                        break;
                    case SERVER_TRIP_STARTED:

                        break;
                    case SERVER_TRIP_STOPPED:

                        break;
                    case USER_LEFT_TRIP:

                        break;
                    case USER_JOINED_TRIP:

                        break;
                    case MESSAGE_RECEIVED:
                        // Toast.makeText(context, "Should go to the messages frag!", Toast.LENGTH_SHORT).show();
                        mViewPager.setCurrentItem(SectionsPagerAdapter.VIEW_MESSAGES, true);
                        break;
                }

            }
        };

        // Check if a trip is already in progress - if so take the user to the map page
        //
        // EDIT (9/Jun/21):
        // Okay, so this needs a de;aued handler in order to work consistently.  I think I could even
        // get away without a delay but regardless, if I try to directly set the mViewPager's
        // page from here in onCreateView (what a sane person would assume to be a good and
        // proper way and place) it will just not work. It is probably being called before the
        // pager's adapter is actually set or something else lifecycle-related.
        //
        // Honestly, I would love to be able to add an intent extra to the notification click to take
        // the user to the messages page to view the new message they received but I cannot figure out
        // how.  So a yucky global variable is used instead.  But I digress, YOU NEED THIS HANDLER OR
        // SHIT WON'T CHANGE PAGES!
        if(MyApp.isNewMessagePending()) {
            Log.i(TAG, "onCreateView A new message is awaiting our eager eyes!");
            Handler handler = new Handler();
            Runnable runner = new Runnable() {
                @Override
                public void run() {
                    mViewPager.setCurrentItem(3);
                }
            };
            handler.postDelayed(runner, 10);
        // END HACK TO MAKE PAGE CHANGE TO VIEW NEW MESSAGE

        } else if (MyApp.isReportingLocation()) {
            mViewPager.setCurrentItem(SectionsPagerAdapter.VIEW_MAP);
        } else {
            mViewPager.setCurrentItem(SectionsPagerAdapter.JOIN_CREATE);
        }

        return root;
    }

    @Override
    public void onStart() {

        getContext().registerReceiver(mainAppRootFragmentReceiver, new IntentFilter(AppBroadcastHelper.GLOBAL_BROADCAST));

        super.onStart();
    }

    @Override
    public void onStop() {

        getContext().unregisterReceiver(mainAppRootFragmentReceiver);

        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        getServiceStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        getServiceStatus();
    }



    void getServiceStatus() {
        if (MyApp.isReportingLocation()) {
            if (LocationTrackingService.isRunning) {
                txtStatus.setText("Passively tracking your location");
                Log.i(TAG, "getServiceStatus | passive");
            } else {
                txtStatus.setText("In a group but no services started?");
                Log.i(TAG, "getServiceStatus | no service?");
            }
        } else {
            txtStatus.setText("Not in a group");
            Log.i(TAG, "getServiceStatus | no group");
        }
    }

    @Override
    public void onSuccess(LocationResult locationResult) {

    }

    //region ****************************** PAGER FRAGS *****************************************

    public static class Frag_JoinCreate extends Fragment {
        private static final String TAG = "Frag_JoinCreate";
        public static final String ARG_SECTION_NUMBER = "section_number";
        public View root;

        // A receiver for catching app-wide broadcasts sent from a variety of different code locations.
        BroadcastReceiver mainAppReceiver;

        // Onscreen keyboard reference.
        InputMethodManager imm;

        Button btnJoin;
        Button btnCreate;
        Button btnLeave;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            root = inflater.inflate(R.layout.frag_join_create, container, false);
            super.onCreateView(inflater, container, savedInstanceState);

            // Get references to our views
            btnJoin = root.findViewById(R.id.btnJoin);
            btnCreate = root.findViewById(R.id.btnCreate);
            btnLeave = root.findViewById(R.id.btnLeave);

            // Add listeners to our views
            btnJoin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Dialog dialog = new Dialog(getContext());
                    final Context c = getContext();
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
                                Toast.makeText(getContext(), "Please enter a valid trip code", Toast.LENGTH_SHORT).show();
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
                            KeyboardHelper keyboardHelper = new KeyboardHelper(getContext());
                            keyboardHelper.hideSoftKeyboard(txtTripCode);
                        }
                    });

                    dialog.show();
                    KeyboardHelper keyboardHelper = new KeyboardHelper(getContext());
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
                    final MyProgressDialog dg = new MyProgressDialog(getContext(), "Leaving group...");
                    dg.show();

                    // Send stop intents to both active and passive services - though only the active
                    // service should be running whenever this method is called.  Sending stops to both
                    // anyway
                    MyApp.stopAllLocationServices();

                    // Send a request to the server to remove the user's entries from the TripEntries
                    // table.  This will also send an FCM to all members with the user's GoogleUser object as the obj
                    // property of the payload.
                    WebApi api = new WebApi(getContext());
                    Requests.Request request = new Requests.Request(Requests.Request.Function.LEAVE_TRIP);
                    request.arguments.add(new Requests.Arguments.Argument("userid", GoogleUser.getCachedUser().id));
                    request.arguments.add(new Requests.Arguments.Argument("tripcode", "0000"));
                    api.makeRequest(request, new WebApi.WebApiResultListener() {
                        @Override
                        public void onSuccess(WebApi.OperationResults results) {
                            if (results.list.get(0).wasSuccessful) {
                                Toast.makeText(getContext(), "You have left the group!", Toast.LENGTH_SHORT).show();
                            }
                            dg.dismiss();
                            sectionsPagerAdapter.setItemCount();
                        }

                        @Override
                        public void onFailure(String message) {
                            Log.w(TAG, " !!!!!!! -= onFailure | " + message + " =- !!!!!!!");
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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

            getParentFragmentManager().popBackStack();

            return root;
        }

        @Override
        public void onStart() {

            Log.i(TAG, " !!!!!!! -= onStart  =- !!!!!!!");

            // Register the app-wide broadcast receiver
            getContext().registerReceiver(mainAppReceiver,
                    new IntentFilter(AppBroadcastHelper.GLOBAL_BROADCAST));
            Log.i(TAG, "onResume | Registered the mainAppReceiver - listening for app-wide broadcasts!");

            super.onStart();
        }

        @Override
        public void onStop() {

            Log.i(TAG, " !!!!!!! -= onStop  =- !!!!!!!");

            // Unregister the broadcast receiver
            getContext().unregisterReceiver(mainAppReceiver);
            Log.i(TAG, "onPause | Unregistered the mainAppReceiver - app-wide broadcasts will go unnoticed here.");

            super.onStop();
        }

        @Override
        public void onResume() {
            Log.i(TAG, " !!!!!!! -= onResume =- !!!!!!!");
            updateUI();

            sectionsPagerAdapter.setItemCount(MyApp.isReportingLocation() ? 4 : 1);

            super.onResume();
        }

        @Override
        public void onPause() {
            Log.i(TAG, " !!!!!!! -= onPause  =- !!!!!!!");
            super.onPause();
        }

        void joinTrip(String tripcode) {
            final MyProgressDialog dg = new MyProgressDialog(getContext(), "Joining group...");
            dg.show();

            WebApi api = new WebApi(getContext());
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
                        Log.w(TAG, "onClick: DELET.ED ALL EXISTING LOCAL UPDATES FOR TRIPCODE: "
                                + tripReport + " | Result: " + delResult);

                        Log.i(TAG, "onSuccess | " + results.list.size() + " results");

                        // Save the new, current trip report to the server.
                        tripReport.saveToLocalDb();

                        // Start the service up - we inna trip!
                        String tripcode = tripReport.tripcode;
                        Intent startServiceIntent = new Intent(getContext(), LocationTrackingService.class);
                        startServiceIntent.putExtra(LocationTrackingService.TRIPCODE, tripcode);
                        getContext().startForegroundService(startServiceIntent);

                        sectionsPagerAdapter.setItemCount(4);
                        mViewPager.setCurrentItem(2);

                    } else {
                        if (results.list.get(0).operationSummary.equals(WebApi.OperationResults.ERROR_TRIP_NOT_FOUND)) {
                            Toast.makeText(getContext(), "Trip code not found!", Toast.LENGTH_SHORT).show();
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
            final MyProgressDialog dg = new MyProgressDialog(getContext(), "Joining group...");
            dg.show();

            WebApi api = new WebApi(getContext());
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
                        Intent startServiceIntent = new Intent(getContext(), LocationTrackingService.class);
                        startServiceIntent.putExtra(LocationTrackingService.TRIPCODE, tripcode);
                        getContext().startForegroundService(startServiceIntent);

                        sectionsPagerAdapter.setItemCount(4);
                        mViewPager.setCurrentItem(2);


                    } else {
                        if (results.list.get(0).operationSummary.equals(WebApi.OperationResults.ERROR_TRIP_NOT_FOUND)) {
                            Toast.makeText(getContext(), "Trip code not found!", Toast.LENGTH_SHORT).show();
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

    public static class Frag_ViewMembers extends Fragment
            implements
            MemberListAdapter.OnMemberClickListener,
            MemberListAdapter.OnMemberLongClickListener,
            OnRefreshListener {

        private static final String TAG = "Frag_ViewMembers";
        public static final String ARG_SECTION_NUMBER = "section_number";
        public TextView txtNotInTrip;
        public View root;
        SmartRefreshLayout refreshLayout;
        MemberListAdapter adapter;
        NonScrollRecyclerView recyclerView;
        BroadcastReceiver mainAppReceiver;
        BroadcastReceiver pageChangedToThisReceiver;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {

            Log.w(TAG, " !!!!!!! -= onCreateView =- !!!!!!!");

            root = inflater.inflate(R.layout.frag_trip_members, container, false);
            super.onCreateView(inflater, container, savedInstanceState);

            refreshLayout = root.findViewById(R.id.refreshLayout);
            refreshLayout.setOnRefreshListener(this);
            refreshLayout.setEnableRefresh(true);
            refreshLayout.setRefreshHeader(new MaterialHeader(getContext()));

            recyclerView = root.findViewById(R.id.recyclerView);
            txtNotInTrip = root.findViewById(R.id.txtNotInTrip);
            txtNotInTrip.setVisibility(MyApp.isReportingLocation() ? View.GONE : View.VISIBLE);

            mainAppReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.hasExtra(AppBroadcastHelper.BROADCAST_TYPE)) {

                        // Parse out the broadcast type
                        AppBroadcastHelper.BroadcastType type = (AppBroadcastHelper.BroadcastType)
                                intent.getSerializableExtra(AppBroadcastHelper.BROADCAST_TYPE);

                        // Act based on the broadcast type
                        switch (type) {
                            case SERVER_LOCATION_UPDATED:
                                Log.i(TAG, "onReceive | Server sent a location update");
                                TripReport tripReport = (TripReport) intent.getParcelableExtra(AppBroadcastHelper.PARCELED_EXTRA);
                                buildList();
                                break;
                            case USER_LEFT_TRIP:
                                GoogleUser formerUser = (GoogleUser) intent.getParcelableExtra(AppBroadcastHelper.PARCELED_EXTRA);
                                Log.i(TAG, "onReceive | (" + formerUser.fullname + ") has left the trip!");
                                buildList();
                                // Log.i(TAG, "onReceive | User (" +  + ") left the trip!");
                                break;
                            case USER_JOINED_TRIP:
                                GoogleUser newUser = (GoogleUser) intent.getParcelableExtra(AppBroadcastHelper.PARCELED_EXTRA);
                                Log.i(TAG, "onReceive | " + "(" + newUser.fullname + ") has joined the trip!");
                                buildList();
                                break;
                            case SERVER_TRIP_STOPPED:
                                Log.i(TAG, "onReceive | Trip was ended");
                                destroyList();
                        }

                    } else {
                        Log.i(TAG, "onReceive | Broadcast didn't have a type!  What the fuck?!");
                    }
                }
            };

            pageChangedToThisReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent != null && intent.getAction() != null) {
                        if (intent.getAction().equals(Frag_Map.SELECTED_MEMBER)) {
                            String selectedUserid = intent.getStringExtra(Frag_Map.SELECTED_MEMBER);
                            if (selectedUserid != null) {
                                Log.i(TAG, "onReceive | User selected a map marker and now we're here!");

                                adapter.highlightUser(selectedUserid);

                            }
                        }
                    }
                }
            };

            recyclerView = root.findViewById(R.id.recyclerView);

            buildList();

            return root;
        }

        @Override
        public void onPause() {
            super.onPause();

            getContext().unregisterReceiver(mainAppReceiver);
            getContext().unregisterReceiver(pageChangedToThisReceiver);

        }

        @Override
        public void onResume() {
            super.onResume();

            Log.i(TAG, "onResume | Registered the main app receiver");
            getContext().registerReceiver(mainAppReceiver, new IntentFilter(AppBroadcastHelper.GLOBAL_BROADCAST));

            buildList();

            getContext().registerReceiver(pageChangedToThisReceiver, new IntentFilter(Frag_Map.SELECTED_MEMBER));

        }

        void destroyList() {
            recyclerView.setAdapter(null);
            txtNotInTrip.setText("Not in a group");
            txtNotInTrip.setVisibility(View.VISIBLE);
        }

        void buildList() {

            if (MyApp.getCurrentTripcode() == null) {
                Log.i(TAG, "buildList | No trip running - clearly we cannot get a list of members!");
                return;
            }

            txtNotInTrip.setVisibility(View.GONE);

            if (refreshLayout != null) {
                refreshLayout.autoRefresh();
            }

            // Always get the latest data from the db
            ArrayList<TripReport> reports = new TripDatasource().getLastXmemberUpdates(1, MyApp.getCurrentTripcode());
            TripReport report = reports.get(0);

            // Calculate distances if there is an available last known location.
            if (MyApp.getLastKnownLocation() != null) {
                report.calculateMemberDistances(MyApp.getLastKnownLocation());
            }

            if (adapter == null) {
                adapter = new MemberListAdapter(getContext(), report);
                adapter.setClickListener(this);
                adapter.setLongClickListener(this);
                adapter.setSendMessageClickListener(new MemberListAdapter.OnSendMessageClickListener() {
                    @Override
                    public void onClick(TripReport.MemberUpdate memberUpdate) {
                        MessageDialog msgDialog = new MessageDialog(getContext(), new MessageDialog.MessageSubmitResultListener() {
                            @Override
                            public void onSuccess() {
                                Intent intent = new Intent(Frag_ViewMessages.SENT_MESSAGE);
                                mViewPager.setCurrentItem(SectionsPagerAdapter.VIEW_MESSAGES, intent);
                            }

                            @Override
                            public void onFailure(String msg) {

                            }
                        });
                    }
                });
                adapter.setNavigateToUserClickListener(new MemberListAdapter.OnNavigateToUserClickListener() {
                    @Override
                    public void onClick(TripReport.MemberUpdate clickedMember) {
                        String uri = "geo: latitude,longtitude ?q= " + clickedMember.lat + ", "
                                + clickedMember.lon + "";
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                    }
                });
            } else {
                adapter.updateAdapterData(report);
                adapter.notifyDataSetChanged();
            }

            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adapter);

            if (refreshLayout != null) {
                refreshLayout.finishRefresh();
            }

        }

        @Override
        public void onRefresh(@NonNull RefreshLayout refreshLayout) {
            buildList();
        }

        @Override
        public void onClick(TripReport.MemberUpdate memberUpdate) {

        }

        @Override
        public void onSubrowClick(TripReport.MemberUpdate memberUpdate) {
            Intent pendingIntent = new Intent(Frag_Map.SELECTED_MEMBER);
            pendingIntent.putExtra(Frag_Map.SELECTED_MEMBER, memberUpdate.userid);
            mViewPager.setCurrentItem(2, pendingIntent);

        }

        @Override
        public void onLongClick(TripReport.MemberUpdate memberUpdate) {
            Toast.makeText(getContext(), "Long clicked: " + memberUpdate.displayName, Toast.LENGTH_SHORT).show();

            WebApi api = new WebApi(getContext());
            Requests.Request request = new Requests.Request(Requests.Request.Function.LOCATION_UPDATE_REQUESTED);
            request.arguments.add(new Requests.Arguments.Argument("userid", memberUpdate.userid));
            request.arguments.add(new Requests.Arguments.Argument("tripcode", MyApp.getCurrentTripcode()));
            request.arguments.add(new Requests.Arguments.Argument("requestinguser", GoogleUser.getCachedUser().id));
            api.makeRequest(request, new WebApi.WebApiResultListener() {
                @Override
                public void onSuccess(WebApi.OperationResults results) {
                    if (results.list.get(0).wasSuccessful) {
                        Toast.makeText(getContext(), "Location update was requested.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(String message) {
                    Log.w(TAG, " !!!!!!! -= onFailure | " + message + " =- !!!!!!!");
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    public static class Frag_Map extends Fragment implements OnMapReadyCallback,
            GoogleMap.OnMapClickListener,
            GoogleMap.OnCircleClickListener,
            GoogleMap.OnPolylineClickListener,
            GoogleMap.OnPoiClickListener,
            GoogleMap.OnMarkerClickListener,
            GoogleMap.OnInfoWindowClickListener {

        private static final String TAG = "Frag_Map";
        public static final String SELECTED_MEMBER = "SELECTED_MEMBER";
        public static final String ARG_SECTION_NUMBER = "section_number";
        public TextView txtNotInTrip;
        public static TextView txtDebugText;
        BroadcastReceiver mainAppReceiver;
        BroadcastReceiver pageChangedToThisReceiver;
        BroadcastReceiver floatingActionButtonClickReceiver;
        Polyline activePolyline;
        MyMapMarkers mapMarkers = new MyMapMarkers();
        public View root;
        GoogleMap map;
        MapCam mapCam;

        public Frag_Map() {
            super();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {

            Log.w(TAG, " !!!!!!! -= onCreateView =- !!!!!!!");

            root = inflater.inflate(R.layout.frag_map, container, false);
            super.onCreateView(inflater, container, savedInstanceState);

            txtNotInTrip = root.findViewById(R.id.txtNotInTrip);
            txtNotInTrip.setVisibility(MyApp.isReportingLocation() ? View.GONE : View.VISIBLE);

            txtDebugText = root.findViewById(R.id.txtDebugText);
            if (Debug.isDebuggerConnected()) {
                txtDebugText.setVisibility(View.VISIBLE);
                txtDebugText.setText("Debugger attached!");
            }

            mainAppReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.hasExtra(AppBroadcastHelper.BROADCAST_TYPE)) {

                        // Parse out the broadcast type
                        AppBroadcastHelper.BroadcastType type = (AppBroadcastHelper.BroadcastType)
                                intent.getSerializableExtra(AppBroadcastHelper.BROADCAST_TYPE);

                        // Act based on the broadcast type
                        switch (type) {
                            case LOCATION_CHANGED_LOCALLY:
                                populateMap();
                                break;
                            case SERVER_LOCATION_UPDATED:
                                Log.i(TAG, "onReceive | Server sent a location update");
                                TripReport tripReport = (TripReport) intent.getParcelableExtra(AppBroadcastHelper.PARCELED_EXTRA);
                                TripDatasource ds = new TripDatasource();
                                ds.getAllLocalLocations(100);
                                populateMap();
                                break;
                            case USER_LEFT_TRIP:
                                GoogleUser formerUser = (GoogleUser) intent.getParcelableExtra(AppBroadcastHelper.PARCELED_EXTRA);
                                Log.i(TAG, "onReceive | (" + formerUser.fullname + ") has left the trip!");
                                // Log.i(TAG, "onReceive | User (" +  + ") left the trip!");
                                break;
                            case USER_JOINED_TRIP:
                                GoogleUser newUser = (GoogleUser) intent.getParcelableExtra(AppBroadcastHelper.PARCELED_EXTRA);
                                Log.i(TAG, "onReceive | " + "(" + newUser.fullname + ") has joined the trip!");
                                break;
                            case SERVER_TRIP_STOPPED:
                                Log.i(TAG, "onReceive | Trip was ended");
                        }

                    } else {
                        Log.i(TAG, "onReceive | Broadcast didn't have a type!  What the fuck?!");
                    }
                }
            };

            pageChangedToThisReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i(TAG, "onReceive | Got a page changed to this receiver!");
                    if (MyApp.isReportingLocation()) {
                        TripReport report = new TripDatasource().getMostRecentMemberLocEntry(MyApp.getCurrentTripcode());
                        Log.i(TAG, "onReceive | Got a SELECTED_MEMBER broadcast!");
                        String selectedUserid = intent.getStringExtra(SELECTED_MEMBER);
                        TripReport.MemberUpdate selectedMemberUpdate = report.find(selectedUserid);
                        Toast.makeText(context, "User " + selectedMemberUpdate.displayName
                                + " was selected from the list - will locate on the map...", Toast.LENGTH_SHORT).show();

                        // Now try to find the clicked user's marker on teh map
                        if (mapMarkers != null && mapMarkers.list.size() > 0) {

                            MyMapMarkers.MyMapMarker selectedUsersMarker = mapMarkers.find(selectedUserid);
                            MyMapMarkers.MyMapMarker myMapMarker = mapMarkers.find(GoogleUser.getCachedUser().id);

                            if (selectedUsersMarker != null && myMapMarker != null) {
                                Toast.makeText(getContext(), myMapMarker.distanceTo(selectedUsersMarker)
                                        + " meters away.", Toast.LENGTH_SHORT).show();

                                if (activePolyline != null) {
                                    activePolyline.remove();
                                }

                                PolylineOptions polylineOptions = new PolylineOptions();
                                polylineOptions.add(selectedUsersMarker.toLatLng());
                                polylineOptions.add(myMapMarker.toLatLng());
                                polylineOptions.color(Color.RED);
                                activePolyline = map.addPolyline(polylineOptions);
                            }
                        }

                        if (mapMarkers.list.size() > 0) {
                            MyMapMarkers.MyMapMarker selectedMarker = mapMarkers.find(selectedUserid);
                            selectedMarker.mapMarker.showInfoWindow();
                            Log.i(TAG, "onReceive ");
                        }

                        mapCam.moveCameraToShowMarkers(report);

                    }
                }
            };

            FragmentManager fm = getChildFragmentManager();
            SupportMapFragment mMapFragment = (SupportMapFragment) getActivity()
                    .getSupportFragmentManager().findFragmentById(R.id.map);

            if (mMapFragment == null) {
                mMapFragment = SupportMapFragment.newInstance();
                fm.beginTransaction().replace(R.id.map, mMapFragment).commit();
            }

            mMapFragment.getMapAsync(this);

            floatingActionButtonClickReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i(TAG, "onReceive Floating button was clicked!");
                    mapCam.moveCameraToShowMarkers(new TripDatasource().getMostRecentMemberLocEntry(MyApp.getCurrentTripcode()));
                }
            };

            return root;
        }

        @Override
        public void onCircleClick(Circle circle) {
            Log.i(TAG, "onCircleClick ");
        }

        @Override
        public void onMapClick(LatLng latLng) {
            Log.i(TAG, "onMapClick ");
        }

        @Override
        public boolean onMarkerClick(Marker marker) {
            Log.i(TAG, "onMarkerClick ");

            try {
                Intent pendingIntent = new Intent(Frag_Map.SELECTED_MEMBER);
                pendingIntent.putExtra(Frag_Map.SELECTED_MEMBER, mapMarkers.find(marker).memberUpdate.userid);
                mViewPager.setCurrentItem(SectionsPagerAdapter.VIEW_MEMBERS, pendingIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }

            MyMapMarkers.MyMapMarker myMapMarker = mapMarkers.find(GoogleUser.getCachedUser().id);
            if (myMapMarker.mapMarker.equals(marker)) {
                Log.w(TAG, " !!!!!!! -= onMarkerClick | CLICKED ON OWN MARKER! =- !!!!!!!");
            } else {
                try {
                    Log.w(TAG, "onMarkerClick: | CLICKED ON OTHER DUDE'S MARKER!");
                    Toast.makeText(getContext(), myMapMarker.distanceTo(marker) + " meters away.", Toast.LENGTH_SHORT).show();

                    if (activePolyline != null) {
                        activePolyline.remove();
                    }

                    PolylineOptions polylineOptions = new PolylineOptions();
                    polylineOptions.add(marker.getPosition());
                    polylineOptions.add(myMapMarker.mapMarker.getPosition());
                    polylineOptions.color(Color.RED);
                    activePolyline = map.addPolyline(polylineOptions);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return false;
        }

        @Override
        public void onPoiClick(PointOfInterest pointOfInterest) {
            Log.i(TAG, "onPoiClick ");
        }

        @Override
        public void onPolylineClick(Polyline polyline) {
            Log.i(TAG, "onPolylineClick ");

            if (activePolyline != null) {
                Toast.makeText(getContext(), "Clicked polyline!", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onInfoWindowClick(Marker marker) {
            Log.i(TAG, "onInfoWindowClick ");



        }

        @Override
        public void onPause() {
            super.onPause();

            getContext().unregisterReceiver(mainAppReceiver);
            getContext().unregisterReceiver(floatingActionButtonClickReceiver);
            getContext().unregisterReceiver(pageChangedToThisReceiver);

            // Update teh app-wide flag that the map has gone underground.
            MyApp.mapFragPaused();

        }

        @Override
        public void onResume() {
            super.onResume();

            Log.i(TAG, "onResume | Registered the main app receiver");
            getContext().registerReceiver(mainAppReceiver, new IntentFilter(AppBroadcastHelper.GLOBAL_BROADCAST));
            getContext().registerReceiver(floatingActionButtonClickReceiver, new IntentFilter(MainActivity.FLOATING_BUTTON_CLICKED));
            getContext().registerReceiver(pageChangedToThisReceiver, new IntentFilter(SELECTED_MEMBER));

            // Update the app-wide flag that the map is visible!
            MyApp.mapFragResumed();

        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            this.map = googleMap;
            this.mapCam = new MapCam(this.map);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            this.map.setMyLocationEnabled(true);
            this.map.setOnMarkerClickListener(this);
            this.map.setOnCircleClickListener(this);
            this.map.setOnMapClickListener(this);
            this.map.setOnPoiClickListener(this);
            this.map.setOnInfoWindowClickListener(this);
            this.map.setOnPolylineClickListener(this);
            this.map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location location) {
                    try {
                        Log.i(TAG, "onMyLocationChange " + location.distanceTo(MyApp.getLastKnownLocation()) + " meters from service reported loc.");
                        MyApp.setLastKnownLocation(location);
                        Intent forceLocationUpdateIntent = new Intent(getContext(), LocationTrackingService.class);
                        forceLocationUpdateIntent.putExtra(LocationTrackingService.MAP_FRAG_SUPPLIED_A_LOCATION, true);
                        getContext().startForegroundService(forceLocationUpdateIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Set the map frag visible to true app-wide.
            MyApp.mapFragResumed();

            populateMap();
            // populateMemberList();
        }

        private static float calculatePulseRadius(float zoomLevel) {
            return (float) Math.pow(2, 16 - zoomLevel) * 160;
        }

        protected void populateMap() {
            if (map == null) { return; }

            // Get the last x number of group member updates from the local db
            ArrayList<TripReport> lastXTripReport = new TripDatasource().getLastXmemberUpdates(5,
                    MyApp.getCurrentTripcode());

            // Fuck outta here if it's null
            if (lastXTripReport == null || lastXTripReport.size() < 1) {
                return;
            }

            // Pull out the most recent group entry
            TripReport mostRecentGroupUpdate = lastXTripReport.get(0);

            // Clear the map entirely
            // map.clear();
            for (MyMapMarkers.MyMapMarker marker : mapMarkers.list) {
                marker.mapMarker.remove();
            }
            mapMarkers.list.clear();

            // Loop through each member in the most recent update
            for (TripReport.MemberUpdate member : mostRecentGroupUpdate.list) {

                // See if the user has an avatar cached and get and cache it if not
                member.avatar = getCachedAvatar(member.userid);

                // Start creating a map marker for this user's location update
                MarkerOptions m = new MarkerOptions();
                m.position(member.toLatLng());

                BitmapDescriptor bitmap;



                if (getCachedAvatar(member.userid) == null) {
                    getAvatar(member);
                    bitmap = BitmapDescriptorFactory.fromBitmap(constructMapPin(R.drawable.car_icon_circular));
                } else {
                    bitmap = BitmapDescriptorFactory.fromBitmap(constructMapPin(getCachedAvatar(member.userid)));
                }

                // Flag as the initiator if this member prompted this server location updated broadcast
                boolean wasInitiator = false;
                if (mostRecentGroupUpdate.initiatedbygoogleid != null
                        && mostRecentGroupUpdate.initiatedbygoogleid.equals(member.userid)) {
                    wasInitiator = true;
                }

                // Set the pin icon and add to map
                m.icon(bitmap);
                m.title(member.displayName);
                MyMapMarkers.MyMapMarker myMapMarker = new MyMapMarkers.MyMapMarker();
                myMapMarker.memberUpdate = member;
                myMapMarker.mapMarker = map.addMarker(m);
                mapMarkers.add(myMapMarker);

                // Add the accuracy circle
                CircleOptions circleOptions = new CircleOptions()
                        .center(member.toLatLng())
                        .clickable(true)
                        .radius(member.accuracy_meters)
                        .strokeWidth(1)
                        .strokeColor(Color.parseColor("#BBFF9000"))
                        .fillColor(Color.parseColor("#44FF9000"));

                // mapCam.moveCameraToShowMarkers(mostRecentGroupUpdate);
            }

        }

        public Bitmap getCachedAvatar(String googleid) {
            for (TripReport.MemberUpdate cachedMember : cachedAvatars) {
                if (cachedMember.userid.equals(googleid)) {
                    return cachedMember.avatar;
                }
            }
            return null;
        }

        private Bitmap constructMapPin(Bitmap bitmap) {

            View customMarkerView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.view_custom_marker, null);
            ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image);
            markerImageView.setImageBitmap(bitmap);
            customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
            customMarkerView.buildDrawingCache();
            Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(returnedBitmap);
            canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
            Drawable drawable = customMarkerView.getBackground();
            if (drawable != null)
                drawable.draw(canvas);
            customMarkerView.draw(canvas);
            return returnedBitmap;
        }

        private Bitmap constructMapPin(int imageResource) {

            View customMarkerView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.view_custom_marker, null);
            ImageView markerImageView = (ImageView) customMarkerView.findViewById(R.id.profile_image);
            markerImageView.setImageDrawable(getResources().getDrawable(imageResource));
            customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
            customMarkerView.buildDrawingCache();
            Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(returnedBitmap);
            canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
            Drawable drawable = customMarkerView.getBackground();
            if (drawable != null)
                drawable.draw(canvas);
            customMarkerView.draw(canvas);
            return returnedBitmap;
        }

        public void setCachedAvatar(TripReport.MemberUpdate member, Bitmap bitmap) {
            for (TripReport.MemberUpdate existingMember : cachedAvatars) {
                if (existingMember.userid.equals(member.userid)) {
                    existingMember.avatar = bitmap;
                }
            }
            member.avatar = bitmap;
            cachedAvatars.add(member);
        }

        private void getAvatar(final TripReport.MemberUpdate memberUpdate) {
            StaticHelpers.Bitmaps.getFromUrl(memberUpdate.photoUrl, new StaticHelpers.Bitmaps.GetImageFromUrlListener() {
                @Override
                public void onSuccess(Bitmap bitmap) {

                    setCachedAvatar(memberUpdate, bitmap);
                    // populateMap();
                }

                @Override
                public void onFailure(String msg) {
                    Bitmap defaultBitmap = StaticHelpers.Bitmaps.getBitmapFromResource(getContext(), R.drawable.car2);
                    memberUpdate.avatar = defaultBitmap;
                    // populateMap();
                }
            });
        }

        private GroundOverlay drawCircle(LatLng latLng) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setSize(250, 250);
            d.setColor(Color.parseColor("#441336B5"));
            d.setStroke(0, Color.TRANSPARENT);

            final Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth()
                    , d.getIntrinsicHeight()
                    , Bitmap.Config.ARGB_8888);

            // Convert the drawable to bitmap
            final Canvas canvas = new Canvas(bitmap);
            d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            d.draw(canvas);

            // Radius of the circle for current zoom level and latitude (because Earth is sphere at first approach)
            double meters_to_pixels = (Math.cos(map.getCameraPosition().target.latitude * Math.PI / 180) * 2 * Math.PI * 6378137) / (256 * Math.pow(2, map.getCameraPosition().zoom));
            final int radius = (int) (meters_to_pixels * 35);

            // Add the circle to the map
            final GroundOverlay circle = map.addGroundOverlay(new GroundOverlayOptions()
                    .position(latLng, 2 * radius).image(BitmapDescriptorFactory.fromBitmap(bitmap)));

            return circle;
        }

    }

    public static class MapCam implements GoogleMap.OnCameraIdleListener,
            GoogleMap.OnCameraMoveListener,
            GoogleMap.OnCameraMoveStartedListener,
            GoogleMap.OnCameraMoveCanceledListener {

        private GoogleMap map;

        long cameraResetLastRunMillis;
        public int resetCameraDelay = 10000;

        public Handler timerHandler = new Handler();
        public Handler timerDisplayHandler = new Handler();

        public MapCam(GoogleMap map) {
            this.map = map;
            this.map.setOnCameraMoveListener(this);
            this.map.setOnCameraMoveStartedListener(this);
            this.map.setOnCameraIdleListener(this);
            this.map.setOnCameraMoveCanceledListener(this);
            cameraResetLastRunMillis = System.currentTimeMillis();
            timerDisplayHandler.postDelayed(timerDisplayRunnable, 100);
        }

        //runs without a timer by reposting this handler at the end of the runnable
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run | Resetting the camera");
                TripReport updates = new TripDatasource().getMostRecentMemberLocEntry(MyApp.getCurrentTripcode());
                if (updates != null) {
                    moveCameraToShowMarkers(updates);
                } else {
                    Log.w(TAG, "run: | timerRunnable | datasource returned null when calling: getMostRecentMemberLocEntry(...)");
                }


            }
        };

        Runnable timerDisplayRunnable = new Runnable() {
            @Override
            public void run() {

                long diffMs = System.currentTimeMillis() - cameraResetLastRunMillis;
                long diffS = diffMs / 1000;
                long secsUntil = (resetCameraDelay / 1000) - diffS;

                Frag_Map.txtDebugText.setText("DEBUG: " + secsUntil + " seconds till camera reset");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (timerDisplayHandler.hasCallbacks(this)) {
                        timerDisplayHandler.removeCallbacks(timerDisplayRunnable);
                    }
                }
                timerDisplayHandler.postDelayed(timerDisplayRunnable, 100);
            }
        };

        @Override
        public void onCameraMoveStarted(int i) {
            Log.w(TAG, " !!!!!!! -= onCameraMoveStarted =- !!!!!!!");
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onCameraMove() {
            if (timerHandler.hasCallbacks(timerRunnable)) {
                timerHandler.removeCallbacks(timerRunnable);
                cameraResetLastRunMillis = System.currentTimeMillis();
            }
        }

        @Override
        public void onCameraMoveCanceled() {

        }

        @Override
        public void onCameraIdle() {

            if (!MyApp.isReportingLocation()) {
                return;
            }

            Log.i(TAG, " !!!!!!! -= onCameraIdle =- !!!!!!!");
            Log.w(TAG, " !!!!!!! -= The camera resetter is now cocked and will fire soon! =- !!!!!!!");
            timerHandler.postDelayed(timerRunnable, this.resetCameraDelay);
            cameraResetLastRunMillis = System.currentTimeMillis();
            TripReport updates = new TripDatasource().getMostRecentMemberLocEntry(MyApp.getCurrentTripcode());
            if (updates != null) {
                // doPulseEvaluations(updates);
            }
        }

        /** Moves the camera to a position such that both the start and end map markers are viewable on screen. **/
        private void moveCameraToShowMarkers(TripReport updates) {

            if (!MyApp.isReportingLocation()) {
                return;
            }

            Log.d(TAG, "Moving the camera to get all the markers in view");

            CameraUpdate cu;

            // Create a new LatLngBounds.Builder object
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            Log.w(TAG, "moveCameraToShowMarkers We have two markers - will animate using bounds.");
            // Create a populated LatLngBounds object by calling the builder object's build() method

            for (TripReport.MemberUpdate mu : updates.list) {
                builder.include(mu.toLatLng());
            }
            LatLngBounds markerBounds = builder.build();
            cu = CameraUpdateFactory.newLatLngBounds(markerBounds,300);

            try {
                // Finally we actually get to move the damn camera
                map.animateCamera(cu, 750, null);
                cameraResetLastRunMillis = System.currentTimeMillis();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

/*        public void doPulseEvaluations(TripReport report) {

            // Remove all pulsing indicators so we don't pile em up
            for (GroundOverlay gu : circles) {
                gu.remove();
            }
            circles.clear();

            for (TripReport.MemberUpdate mu : report.list) {
                if (mu.locationtype.equals(TripReport.MemberUpdate.LOCATION_TYPE_ACTIVE)) {
                    circles.add(pulseLocation(mu.toLatLng()));
                }
            }
        }

        *//**
         * Shows a animated, pulsing circle at the specified position.  Don't forget to invalidate and
         * re-call if the map gets moved at all!
         * @param latLng Where to put the thing on the thing.
         * @return Yo face.
         *//*
        private GroundOverlay pulseLocation(LatLng latLng) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setSize(500, 500);
            d.setColor(Color.RED);
            d.setStroke(0, Color.TRANSPARENT);

            final Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth()
                    , d.getIntrinsicHeight()
                    , Bitmap.Config.ARGB_8888);

            // Convert the drawable to bitmap
            final Canvas canvas = new Canvas(bitmap);
            d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            d.draw(canvas);

            // Radius of the circle for current zoom level and latitude (because Earth is sphere at first approach)
            double meters_to_pixels = (Math.cos(map.getCameraPosition().target.latitude
                    * Math.PI /180) * 2 * Math.PI * 6378137) / (256
                    * Math.pow(2, map.getCameraPosition().zoom));

            final int radius = (int)(meters_to_pixels * 30);

            Log.i(TAG, " !!!!!!! -= pulseLocation RADIUS: " + radius + " =- !!!!!!!");


            // Add the circle to the map
            final GroundOverlay circle = map.addGroundOverlay(new GroundOverlayOptions()
                    .position(latLng, 2 * radius).image(BitmapDescriptorFactory.fromBitmap(bitmap)));

            // Prep the animator
            PropertyValuesHolder radiusHolder = PropertyValuesHolder.ofFloat("radius", 1, radius);
            PropertyValuesHolder transparencyHolder = PropertyValuesHolder.ofFloat("transparency", 0, 1);

            ValueAnimator valueAnimator = new ValueAnimator();
            valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
            valueAnimator.setRepeatMode(ValueAnimator.RESTART);
            valueAnimator.setValues(radiusHolder, transparencyHolder);
            valueAnimator.setDuration(1000);
            valueAnimator.setEvaluator(new FloatEvaluator());
            valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float animatedRadius = (float) valueAnimator.getAnimatedValue("radius");
                    float animatedAlpha = (float) valueAnimator.getAnimatedValue("transparency");
                    circle.setDimensions(animatedRadius * 2);
                    circle.setTransparency(animatedAlpha);

                }
            });

            // start the animation
            valueAnimator.start();

            return circle;
        }*/

    } // MapCam class

    public static class Frag_ViewMessages extends Fragment implements
            UserMessagesAdapter.OnUserMessageClickListener,
            UserMessagesAdapter.OnUserMessageLongClickListener {

        public static final String SENT_MESSAGE = "SENT_MESSAGE";
        private static final String TAG = "Frag_ViewMessages";
        public static final String ARG_SECTION_NUMBER = "section_number";
        public TextView txtNotInTrip;
        public View root;
        SmartRefreshLayout refreshLayout;
        TextView txtLoadingMsgs;
        RecyclerView recyclerView;
        UserMessagesAdapter adapter;
        BroadcastReceiver mainAppReceiver;
        BroadcastReceiver pageChangedToThisReceiver;
        BroadcastReceiver fabReceiver;


        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {

            MyApp.setNewMessagePending(false);

            Log.w(TAG, " !!!!!!! -= onCreateView =- !!!!!!!");

            root = inflater.inflate(R.layout.frag_messages, container, false);
            super.onCreateView(inflater, container, savedInstanceState);

            refreshLayout = root.findViewById(R.id.refreshLayout);
            refreshLayout.setEnableRefresh(true);
            refreshLayout.setRefreshHeader(new MaterialHeader(getContext()));

            txtLoadingMsgs = root.findViewById(R.id.txtLoadingMsgs);

                    recyclerView = root.findViewById(R.id.recyclerView);
            txtNotInTrip = root.findViewById(R.id.txtNotInTrip);
            txtNotInTrip.setVisibility(MyApp.isReportingLocation() ? View.GONE : View.VISIBLE);

            mainAppReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.hasExtra(AppBroadcastHelper.BROADCAST_TYPE)) {

                        // Parse out the broadcast type
                        AppBroadcastHelper.BroadcastType type = (AppBroadcastHelper.BroadcastType)
                                intent.getSerializableExtra(AppBroadcastHelper.BROADCAST_TYPE);

                        // Act based on the broadcast type
                        switch (type) {
                            case MESSAGE_RECEIVED:
                                Log.i(TAG, "onReceive | MESSAGE RECEIVED WE ARE AT THE MSGS FRAG!");
                                buildList();
                                break;
                        }

                    } else {
                        Log.i(TAG, "onReceive | Broadcast didn't have a type!  What the fuck?!");
                    }
                }
            };

            /*pageChangedToThisReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent != null && intent.getAction() != null) {
                        if (intent.getAction().equals(Frag_Map.SELECTED_MEMBER)) {
                            String selectedUserid = intent.getStringExtra(Frag_Map.SELECTED_MEMBER);
                            if (selectedUserid != null) {
                                Log.i(TAG, "onReceive | User selected a map marker and now we're here!");

                                adapter.highlightUser(selectedUserid);

                            }
                        }
                    }
                }
            };*/

            recyclerView = root.findViewById(R.id.recyclerView);

            fabReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    MessageDialog msgDialog = new MessageDialog(getContext(), new MessageDialog.MessageSubmitResultListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Message submitted", Toast.LENGTH_SHORT).show();
                            buildList();
                        }

                        @Override
                        public void onFailure(String msg) {
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    });

                    msgDialog.show();

                }
            };

            pageChangedToThisReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    buildList();
                }
            };

            return root;
        }

        @Override
        public void onPause() {
            super.onPause();

            getContext().unregisterReceiver(mainAppReceiver);
            getContext().unregisterReceiver(pageChangedToThisReceiver);
            getContext().unregisterReceiver(fabReceiver);

        }

        @Override
        public void onResume() {
            super.onResume();

            Log.i(TAG, "onResume | Registered the main app receiver");
            getContext().registerReceiver(mainAppReceiver, new IntentFilter(AppBroadcastHelper.GLOBAL_BROADCAST));
            getContext().registerReceiver(fabReceiver, new IntentFilter(MainActivity.FLOATING_BUTTON_CLICKED));

            getContext().registerReceiver(pageChangedToThisReceiver, new IntentFilter(Frag_ViewMessages.SENT_MESSAGE));
            buildList();
        }

        void buildList() {

            // Grab available messages as an ArrayList<UserMessage>
            TripDatasource ds = new TripDatasource();
            ArrayList<UserMessage> allTripMessages = ds.getAllUserMessages(MyApp.getCurrentTripcode());

            if (allTripMessages == null || allTripMessages.size() == 0) {
                getServerMessages();
                return;
            }

            // Instantiate and populate our custom adapter.
            adapter = new UserMessagesAdapter(getContext(), allTripMessages, this, this);

            // Apply the adapter to the recyclerview.
            recyclerView.setAdapter(adapter);

            // This is still new to me and specific to RecyclerViews but it is necessary to create a
            // LinearLayoutManager (configure it if needed) and then apply it to the recyclerview.
            // If you do not do this it simply will not appear.
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            layoutManager.setReverseLayout(true); // EXPERIMENTAL -
            recyclerView.setLayoutManager(layoutManager);

            adapter.notifyDataSetChanged();

            // Scroll to the end since this is representing chat messages.
            recyclerView.scrollToPosition(adapter.getItemCount() - 1); // Not working

        }

        void getServerMessages() {
            txtLoadingMsgs.setVisibility(View.VISIBLE);

            WebApi api = new WebApi(getContext());
            Requests.Request request = new Requests.Request(Requests.Request.Function.GET_TRIP_MESSAGES);
            request.arguments.add(new Requests.Arguments.Argument("tripcode", MyApp.getCurrentTripcode()));
            request.arguments.add(new Requests.Arguments.Argument("limit", "100"));
            request.arguments.add(new Requests.Arguments.Argument("orderargument", "asc"));
            api.makeRequest(request, new WebApi.WebApiResultListener() {
                @Override
                public void onSuccess(WebApi.OperationResults results) {
                    txtLoadingMsgs.setVisibility(View.INVISIBLE);
                    txtLoadingMsgs.setVisibility(View.INVISIBLE);
                    String serializedMsgs = results.list.get(0).result;
                    ArrayList<UserMessage> msgs = null;
                    try {
                        msgs = UserMessage.parseMany(serializedMsgs);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (msgs != null && msgs.size() > 0) {
                        for (UserMessage msg : msgs) {
                            msg.appendToDb();
                        }
                    }
                    buildList();
                }

                @Override
                public void onFailure(String message) {
                    txtLoadingMsgs.setVisibility(View.INVISIBLE);
                }
            });
        }

        @Override
        public void onClick(UserMessage message) {

        }

        @Override
        public void onLongClick(UserMessage message) {

        }
    }

    //endregion *********************************************************************************

    // region PAGER ADAPTER

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private int itemCount = 1;
        public static final int JOIN_CREATE = 0;
        public static final int VIEW_MEMBERS= 1;
        public static final int VIEW_MAP = 2;
        public static final int VIEW_MESSAGES = 3;

        public SectionsPagerAdapter(androidx.fragment.app.FragmentManager fm) {
            super(fm);
            sectionsPagerAdapter = this;
        }

        @Override
        public Fragment getItem(int position) {

            Log.d("getItem", "Creating Fragment in pager at index: " + position);
            Log.w(TAG, "getItem: PAGER POSITION: " + position);

            if (position == JOIN_CREATE) {
                Fragment fragment = new Frag_JoinCreate();
                Bundle args = new Bundle();
                args.putInt(Frag_JoinCreate.ARG_SECTION_NUMBER, position + 1);
                fragment.setArguments(args);
                return fragment;
            }

            if (position == VIEW_MEMBERS) {
                Fragment fragment = new Frag_ViewMembers();
                Bundle args = new Bundle();
                args.putInt(Frag_ViewMembers.ARG_SECTION_NUMBER, position + 1);
                fragment.setArguments(args);
                return fragment;
            }

            if (position == VIEW_MAP) {
                Fragment fragment = new Frag_Map();
                Bundle args = new Bundle();
                args.putInt(Frag_Map.ARG_SECTION_NUMBER, position + 1);
                args.putString("SOME_ARG", "Fuck you!");
                fragment.setArguments(args);
                return fragment;
            }

            if (position == VIEW_MESSAGES) {
                Fragment fragment = new Frag_ViewMessages();
                Bundle args = new Bundle();
                args.putInt(Frag_Map.ARG_SECTION_NUMBER, position + 1);
                args.putString("SOME_ARG", "Fuck you!");
                fragment.setArguments(args);
                return fragment;
            }

            /* if (position == VIEW_MAP) {
                Fragment fragment = new Frag_ViewMap();
                Bundle args = new Bundle();
                args.putInt(Frag_ViewMap.ARG_SECTION_NUMBER, position + 1);
                fragment.setArguments(args);
                return fragment;
            }*/

            return null;
        }

        @Override
        public int getCount() {
            return this.itemCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            curPageIndex = position;

            switch (position) {
                case JOIN_CREATE:
                    return "Join/Create";
                case VIEW_MEMBERS:
                    return "View Members";
                case VIEW_MAP:
                    return "View Map";
                case VIEW_MESSAGES:
                    return "Messages";
            }
            return null;
        }

        public void setItemCount(int count) {
            this.itemCount = count;
            this.notifyDataSetChanged();
        }

        public void setItemCount() {
            this.itemCount = MyApp.isReportingLocation() ? 3 : 1;
            this.notifyDataSetChanged();
        }

    }

    // endregion
}