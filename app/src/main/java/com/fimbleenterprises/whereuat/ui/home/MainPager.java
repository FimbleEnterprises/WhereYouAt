package com.fimbleenterprises.whereuat.ui.home;

import android.animation.FloatEvaluator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
import com.fimbleenterprises.whereuat.R;
import com.fimbleenterprises.whereuat.adapters.BasicObjectsAdapter;
import com.fimbleenterprises.whereuat.generic_objs.BasicObjects;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.local_database.TripDatasource;
import com.fimbleenterprises.whereuat.local_database.TripReport;
import com.fimbleenterprises.whereuat.rest_api.Requests;
import com.fimbleenterprises.whereuat.rest_api.WebApi;
import com.fimbleenterprises.whereuat.services.ActiveLocationUpdateService;
import com.fimbleenterprises.whereuat.AppBroadcastHelper;
import com.fimbleenterprises.whereuat.services.PassiveLocationUpdateService;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import javax.sql.DataSource;

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

public class MainPager extends Fragment {

    private static final String TAG = "HomeFragment";
    public static int curPageIndex = 0;

    private HomeViewModel homeViewModel;

    TextView txtStatus;
    public static MyViewPager mViewPager;
    public static PagerTitleStrip mPagerStrip;
    public static SectionsPagerAdapter sectionsPagerAdapter;
    BroadcastReceiver mainAppRootFragmentReceiver;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, " !!!!!!! -= onCreateView  =- !!!!!!!");



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
        mViewPager.setPageCount(2);
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
                AppBroadcastHelper.BroadcastType type = (AppBroadcastHelper.BroadcastType)
                        intent.getSerializableExtra(AppBroadcastHelper.BROADCAST_TYPE);

                Log.i(TAG, "onReceive | BroadcastType: " + type.name());
                Log.i(TAG, "onReceive | Has PARCELLED_EXTRA: " + intent.hasExtra(AppBroadcastHelper.PARCELED_EXTRA));

                switch (type) {
                    case ACTIVE_LOCATION_SERVICE_STARTED:
                        txtStatus.setText("Actively monitoring");
                        Log.i(TAG, "onReceive | Active location start broadcast");
                        break;
                    case ACTIVE_LOCATION_SERVICE_STOPPED:
                        Log.i(TAG, "onReceive | Active location stop broadcast");
                        if (!MyApp.isReportingLocation()) {
                            txtStatus.setText("Not currently monitoring");
                        }
                        break;
                    case PASSIVE_LOCATION_SERVICE_STARTED:
                        txtStatus.setText("Passively monitoring");
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
                }

            }
        };

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
            if (PassiveLocationUpdateService.isRunning) {
                txtStatus.setText("Passively tracking your location");
                Log.i(TAG, "getServiceStatus | passive");
            } else if(ActiveLocationUpdateService.isRunning) {
                txtStatus.setText("Actively tracking your location");
                Log.i(TAG, "getServiceStatus | active");
            } else {
                txtStatus.setText("In a group but no services started?");
                Log.i(TAG, "getServiceStatus | no service?");
            }
        } else {
            txtStatus.setText("Not in a group");
            Log.i(TAG, "getServiceStatus | no group");
        }
    }

    //region ****************************** PAGER FRAGS *****************************************

    public static class Frag_JoinCreate extends Fragment {
        private static final String TAG = "Frag_JoinCreate";
        public static final String ARG_SECTION_NUMBER = "section_number";
        public View root;

        // A receiver for catching app-wide broadcasts sent from a variety of different code locations.
        BroadcastReceiver mainAppReceiver;

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

                    final MyProgressDialog dg = new MyProgressDialog(getContext(), "Joining group...");
                    dg.show();

                    WebApi api = new WebApi(getContext());
                    Requests.Request request = new Requests.Request(Requests.Request.Function.JOIN_TRIP);
                    request.arguments.add(new Requests.Arguments.Argument("userid", GoogleUser.getCachedUser().id));
                    request.arguments.add(new Requests.Arguments.Argument("tripcode", "0000"));
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
                                Intent startServiceIntent = new Intent(getContext(), ActiveLocationUpdateService.class);
                                startServiceIntent.putExtra(ActiveLocationUpdateService.TRIPCODE, tripcode);
                                getContext().startForegroundService(startServiceIntent);

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
            });

            btnCreate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            btnLeave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Send stop intents to both active and passive services - though only the active
                    // service should be running whenever this method is called.  Sending stops to both
                    // anyway
                    MyApp.stopAllLocationServices();
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
                        case PASSIVE_LOCATION_SERVICE_STARTED:

                            break;
                        case PASSIVE_LOCATION_SERVICE_STOPPED:

                            break;
                        case LOCATION_CHANGED_LOCALLY:
                            try {
                                Location localLoc = (Location) intent.getParcelableExtra(AppBroadcastHelper.PARCELED_EXTRA);
                                Log.i(TAG, "onReceive | Extra is of type: Location!\nLocal loc | Lat: " + localLoc.getLatitude()
                                        + " Lon: " + localLoc.getLongitude() + " Acc: " +
                                        localLoc.getAccuracy() + " meters");
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
            Log.i(TAG, " !!!!!!! -= onResume  =- !!!!!!!");
            updateUI();
            super.onResume();
        }

        @Override
        public void onPause() {
            Log.i(TAG, " !!!!!!! -= onPause  =- !!!!!!!");
            super.onPause();
        }

        void updateUI() {
            btnLeave.setEnabled(MyApp.isReportingLocation());
            btnJoin.setEnabled(!MyApp.isReportingLocation());
            btnCreate.setEnabled(!MyApp.isReportingLocation());
        }
    }

    public static class Frag_ViewMembers extends Fragment
                                         implements
                                                 BasicObjectsAdapter.OnBasicObjectClickListener,
                                                 BasicObjectsAdapter.OnBasicObjectLongClickListener {

        private static final String TAG = "Frag_ViewMembers";
        public static final String ARG_SECTION_NUMBER = "section_number";
        public TextView txtNotInTrip;
        public View root;
        RecyclerView rvMembers;
        BasicObjectsAdapter adapter;
        BroadcastReceiver mainAppReceiver;
        BasicObjects data = new BasicObjects();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {

            Log.w(TAG, " !!!!!!! -= onCreateView =- !!!!!!!");

            root = inflater.inflate(R.layout.frag_trip_members, container, false);
            super.onCreateView(inflater, container, savedInstanceState);

            rvMembers = root.findViewById(R.id.rvMembers);
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

            buildList();

            return root;
        }

        @Override
        public void onPause() {
            super.onPause();

            getContext().unregisterReceiver(mainAppReceiver);

        }

        @Override
        public void onResume() {
            super.onResume();

            Log.i(TAG, "onResume | Registered the main app receiver");
            getContext().registerReceiver(mainAppReceiver, new IntentFilter(AppBroadcastHelper.GLOBAL_BROADCAST));
            buildList();

        }

        void destroyList() {
            rvMembers.setAdapter(null);
            txtNotInTrip.setText("Not in a group");
            txtNotInTrip.setVisibility(View.VISIBLE);
        }

        void buildList() {

            if (MyApp.getCurrentTripcode() == null) {
                Log.i(TAG, "buildList | No trip running - clearly we cannot get a list of members!");
                return;
            }

            txtNotInTrip.setVisibility(View.GONE);

            ArrayList<TripReport> rawData = new TripDatasource().getLastXmemberUpdates(1, MyApp.getCurrentTripcode());
            data = new BasicObjects();

            if (rawData == null || rawData.size() == 0) {
                return;
            }

            for (TripReport report : rawData) {
                for (TripReport.MemberUpdate update : report.list) {
                    BasicObjects.BasicObject object = new BasicObjects.BasicObject();
                    object.isSeparator = false;
                    object.title = "Name: " + update.displayName;
                    object.subtitle = "Location type: " + update.locationtype;
                    object.obj = update;
                    data.list.add(object);
                }
            }

            // This line is needed because reasons
            rvMembers.setLayoutManager(new LinearLayoutManager(getContext()));

            // Create an adapter for our basicobjects and recyclerview
            adapter = new BasicObjectsAdapter(getContext(), data);

            // Set on click and long click listeners
            adapter.setOnBasicObjectItemLongClickListener(this);
            adapter.setOnBasicItemClickListener(this);

            // Apply the adapter to the recyclerview
            rvMembers.setAdapter(adapter);

        }

        @Override
        public void onBasicObjectItemClicked(BasicObjects.BasicObject basicObject) {
            Toast.makeText(getContext(), "Clicked: " + basicObject.title, Toast.LENGTH_SHORT).show();

            Intent pendingIntent = new Intent(Frag_Map.SELECTED_MEMBER);
            TripReport.MemberUpdate selectedUser = (TripReport.MemberUpdate) basicObject.obj;
            pendingIntent.putExtra(Frag_Map.SELECTED_MEMBER, selectedUser.userid);
            mViewPager.setCurrentItem(2, pendingIntent);


        }

        @Override
        public void onBasicItemLongClicked(BasicObjects.BasicObject basicObject) {
            Toast.makeText(getContext(), "Long clicked: " + basicObject.title, Toast.LENGTH_SHORT).show();
        }
    }

    public static class Frag_Map extends Fragment implements OnMapReadyCallback {

        private static final String TAG = "Frag_Map";
        public static final String SELECTED_MEMBER = "SELECTED_MEMBER";
        public static final String ARG_SECTION_NUMBER = "section_number";
        public TextView txtNotInTrip;
        BroadcastReceiver mainAppReceiver;
        BroadcastReceiver pageChangedToThisReceiver;
        BroadcastReceiver floatingActionButtonClickReceiver;
        MyMapMarkers mapMarkers = new MyMapMarkers();
        public View root;
        GoogleMap map;
        MapCam mapCam;

        public Frag_Map() {
            super();
        }

        public static class MyMapMarkers  {

            public ArrayList<MyMapMarker> list = new ArrayList<>();

            public static class MyMapMarker {
                public TripReport.MemberUpdate memberUpdate;
                public Marker mapMarker;

                public void removeFromMap() {
                    this.mapMarker.remove();
                }
            }

            public void add(MyMapMarker mapMarker) {
                this.list.add(mapMarker);
            }

            public MyMapMarker find(String googleuserid) {
                for (MyMapMarker myMarker : this.list) {
                    if (myMarker.memberUpdate.userid.equals(googleuserid)) {
                        return myMarker;
                    }
                }
                return null;
            }
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

                        if (mapMarkers.list.size() > 0) {
                            MyMapMarkers.MyMapMarker selectedMarker = mapMarkers.find(selectedUserid);
                            selectedMarker.mapMarker.showInfoWindow();
                            Log.i(TAG, "onReceive ");
                        }

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
        public void onPause() {
            super.onPause();

            getContext().unregisterReceiver(mainAppReceiver);
            getContext().unregisterReceiver(floatingActionButtonClickReceiver);
            getContext().unregisterReceiver(pageChangedToThisReceiver);

        }

        @Override
        public void onResume() {
            super.onResume();

            Log.i(TAG, "onResume | Registered the main app receiver");
            getContext().registerReceiver(mainAppReceiver, new IntentFilter(AppBroadcastHelper.GLOBAL_BROADCAST));
            getContext().registerReceiver(floatingActionButtonClickReceiver, new IntentFilter(MainActivity.FLOATING_BUTTON_CLICKED));
            getContext().registerReceiver(pageChangedToThisReceiver, new IntentFilter(SELECTED_MEMBER));

        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            this.map = googleMap;
            this.mapCam = new MapCam(this.map);

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
            map.clear();
            mapMarkers.list.clear();

            // Loop through each member in the most recent update
            for (TripReport.MemberUpdate member : mostRecentGroupUpdate.list) {
                // Start creating a map marker for this user's location update
                MarkerOptions m = new MarkerOptions();
                m.position(member.toLatLng());
                BitmapDescriptor pinicon = BitmapDescriptorFactory.fromResource(R.drawable.facility_map_pin_4);

                // Flag as the initiator if this member prompted this server location updated broadcast
                boolean wasInitiator = false;
                if (mostRecentGroupUpdate.initiatedbygoogleid != null
                        && mostRecentGroupUpdate.initiatedbygoogleid.equals(member.userid)) {
                    wasInitiator = true;
                }

                // Set the pin icon and add to map
                m.icon(pinicon);
                m.title(member.displayName);
                MyMapMarkers.MyMapMarker myMapMarker = new MyMapMarkers.MyMapMarker();
                myMapMarker.memberUpdate = member;
                myMapMarker.mapMarker = map.addMarker(m);
                mapMarkers.add(myMapMarker);

                mapCam.moveCameraToShowMarkers(mostRecentGroupUpdate);
            }

            // Draw the member history polylines
            // drawAllMembersHistory(lastXTripReport);
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
        private long startedMovingAt = 0;
        public ArrayList<GroundOverlay> circles = new ArrayList<>();

        public MapCam(GoogleMap map) {
            this.map = map;
            this.map.setOnCameraMoveListener(this);
            this.map.setOnCameraMoveStartedListener(this);
            this.map.setOnCameraIdleListener(this);
            this.map.setOnCameraMoveCanceledListener(this);
        }

        //runs without a timer by reposting this handler at the end of the runnable
        Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run | Resetting the camera");
                TripReport updates = new TripDatasource().getMostRecentMemberLocEntry(MyApp.getCurrentTripcode());
                moveCameraToShowMarkers(updates);
            }
        };

        @Override
        public void onCameraMoveStarted(int i) {
            Log.w(TAG, " !!!!!!! -= onCameraMoveStarted =- !!!!!!!");
        }

        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onCameraMove() {
            Log.i(TAG, "onCameraMove Removed any pending camera reset timers");
            if (timerHandler.hasCallbacks(timerRunnable)) {
                timerHandler.removeCallbacks(timerRunnable);
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
            timerHandler.postDelayed(timerRunnable, 5000);
            TripReport updates = new TripDatasource().getMostRecentMemberLocEntry(MyApp.getCurrentTripcode());
            if (updates != null) {
                doPulseEvaluations(updates);
            }
        }

        /** Moves the camera to a position such that both the start and end map markers are viewable on screen. **/
        private void moveCameraToShowMarkers(TripReport updates) {

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

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void doPulseEvaluations(TripReport report) {

            // Remove all pulsing indicators so we don't pile em up ad infinitum.
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

        /**
         * Shows a animated, pulsing circle at the specified position.  Don't forget to invalidate and
         * re-call if the map gets moved at all!
         * @param latLng Where to put the thing on the thing.
         * @return Yo face.
         */
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
            final int radius = (int)(meters_to_pixels * 35);
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
        }

    } // MapCam class

    //endregion *********************************************************************************

    // region PAGER ADAPTER

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public static final int JOIN_CREATE = 0;
        public static final int VIEW_MEMBERS= 1;
        public static final int VIEW_MAP = 2;

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
            return 3;
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
            }
            return null;
        }


    }

    // endregion
}