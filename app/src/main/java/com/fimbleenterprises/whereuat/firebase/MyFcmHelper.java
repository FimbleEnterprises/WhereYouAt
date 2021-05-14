package com.fimbleenterprises.whereuat.firebase;

import android.util.Log;

import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.rest_api.Requests;
import com.fimbleenterprises.whereuat.rest_api.WebApi;
import com.fimbleenterprises.whereuat.preferences.MySettingsHelper;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.messaging.FirebaseMessaging;

public class MyFcmHelper {

    private static final String TAG = "MyFcmHelper";

    public interface UpsertFcmListener {
        public void onSuccess();
        public void onFailure(String msg);
    }

    public interface RequestFcmTokenListener {
        public void onSuccess(String token);
        public void onFailure(String msg);
    }

    /**
     * Silently attempts to retrieve an FCM token from Google, save it locally to shared preferences
     * then finally upload the token to our servers.
     */
    public static void getNewToken() {
        Log.i(TAG, "getNewToken Requesting a new FCM token from Google...");
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.i(TAG, "onSuccess Got new token: " + s);
                Log.i(TAG, "onSuccess Caching fcm token to shared prefs...");
                MySettingsHelper options = new MySettingsHelper();
                options.cacheFcmToken(s);
                Log.i(TAG, "onSuccess Token cached.");
                try {
                    MyFcmHelper.upsertCachedFcm();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Attempts to retrieve an FCM token from Google, save it locally to the shared preferences and then
     * finally upload the token to our servers.
     * @param onFcmReceivedFromGoogle A listener monitoring Google's token response
     */
    public static void getNewToken(final RequestFcmTokenListener onFcmReceivedFromGoogle) {
        Log.i(TAG, "getNewToken Requesting a new FCM token from Google...");
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                Log.i(TAG, "onSuccess Got new token: " + s);
                Log.i(TAG, "onSuccess Caching fcm token to shared prefs...");
                onFcmReceivedFromGoogle.onSuccess(s);
                MySettingsHelper options = new MySettingsHelper();
                options.cacheFcmToken(s);
                Log.i(TAG, "onSuccess Token cached.");
                try {
                    MyFcmHelper.upsertCachedFcm();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Attempts to retrieve an FCM token from Google, save it locally to the shared preferences and then
     * finally upload the token to our servers.
     * @param onFcmReceivedFromGoogle A listener monitoring Google's token response
     * @param onFcmUpsertedListener A listener monitoring our server's upsert response
     */
    public static void getNewToken(final RequestFcmTokenListener onFcmReceivedFromGoogle, final UpsertFcmListener onFcmUpsertedListener) {
        Log.i(TAG, "getNewToken Requesting a new FCM token from Google...");
        try {
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String s) {
                    Log.i(TAG, "onSuccess Got new token: " + s);
                    Log.i(TAG, "onSuccess Caching fcm token to shared prefs...");
                    onFcmReceivedFromGoogle.onSuccess(s);
                    MySettingsHelper options = new MySettingsHelper();
                    options.cacheFcmToken(s);
                    Log.i(TAG, "onSuccess Token cached.");
                    try {
                        MyFcmHelper.upsertCachedFcm(new UpsertFcmListener() {
                            @Override
                            public void onSuccess() {
                                onFcmUpsertedListener.onSuccess();
                            }

                            @Override
                            public void onFailure(String msg) {
                                onFcmUpsertedListener.onFailure(msg);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "getNewToken: | Failed to get a token from Google!  See stacktrace!");
            e.printStackTrace();
        }
    }

    /**
     * Upserts a cached FCM token to our server.
     * @param listener Simple listener for the async request callback.
     * @throws Exception If either the token is not found in shared prefs or there is not a valid
     *                   Google user cached in shared prefs an exception will be thrown.
     */
    public static void upsertCachedFcm(final UpsertFcmListener listener) throws Exception {
        MySettingsHelper options = new MySettingsHelper();

        Log.i(TAG, "upsertCachedFcm Upserting fcm token to our server...");

        if (options.getCachedFcmToken() == null || options.getCachedGoogleUser() == null) {
            throw new Exception("Missing google account or fcm token in cache!");
        }

        if (options.getCachedFcmToken() != null) {
            Requests.Request request = new Requests.Request(Requests.Request.Function.UPSERT_FCM_TOKEN);
            request.arguments.add(new Requests.Arguments.Argument("userid", options.getCachedGoogleUser().id));
            request.arguments.add(new Requests.Arguments.Argument("fcmtoken", options.getCachedFcmToken()));
            WebApi webApi = new WebApi(MyApp.getAppContext());
            webApi.makeRequest(request, new WebApi.WebApiResultListener() {
                @Override
                public void onSuccess(WebApi.OperationResults results) {
                    Log.i(TAG, "onSuccess Token was successfully upserted");
                    listener.onSuccess();
                }

                @Override
                public void onFailure(String message) {
                    listener.onFailure(message);
                    Log.w(TAG, "onFailure: Failed to upsert fcm token " + message);
                }
            });
        }
    }

    /**
     * Upserts a cached FCM token to our server.
     * @throws Exception If either the token is not found in shared prefs or there is not a valid
     *                   Google user cached in shared prefs an exception will be thrown.
     */
    public static void upsertCachedFcm() throws Exception {
        MySettingsHelper options = new MySettingsHelper();

        if (options.getCachedFcmToken() == null || options.getCachedGoogleUser() == null) {
            throw new Exception("Missing google account or fcm token in cache!");
        }

        if (options.getCachedFcmToken() != null) {
            Log.i(TAG, "upsertCachedFcm - Silently upserting FCM token to our server...");
            Requests.Request request = new Requests.Request(Requests.Request.Function.UPSERT_FCM_TOKEN);
            request.arguments.add(new Requests.Arguments.Argument("userid", options.getCachedGoogleUser().id));
            request.arguments.add(new Requests.Arguments.Argument("fcmtoken", options.getCachedFcmToken()));
            WebApi webApi = new WebApi(MyApp.getAppContext());
            webApi.makeRequest(request, new WebApi.WebApiResultListener() {
                @Override
                public void onSuccess(WebApi.OperationResults results) {
                    Log.i(TAG, "onSuccess Successfully upserted the FCM token to our server.");
                }

                @Override
                public void onFailure(String message) {
                    Log.w(TAG, "onFailure: Failed to upsert the FCM token! " + message);
                }
            });
        }
    }

}
