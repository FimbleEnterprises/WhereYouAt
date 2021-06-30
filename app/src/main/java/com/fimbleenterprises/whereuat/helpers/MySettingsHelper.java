package com.fimbleenterprises.whereuat.helpers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.gson.Gson;

public class MySettingsHelper {

    SharedPreferences prefs;

    // Preference keys
    public static final String GOOGLE_SIGN_IN_ACCOUNT_OBJECT = "GOOGLE_SIGN_IN_ACCOUNT_OBJECT";
    public static final String FCM_TOKEN = "FCM_TOKEN";

    /**
     * Instantiates a MySettingsHelper object using the app's global context to gain access to the
     * shared preferences service.
     */
    public MySettingsHelper() {
        prefs = PreferenceManager.getDefaultSharedPreferences(MyApp.getAppContext());
    }

    //region ************ GOOGLE ACCOUNT ***************

    /**
     * Checks if a GoogleSignInAccount object is cached as json in shared prefs.
     *
     * @return
     */
    public boolean hasCachedGoogleCredentials() {
        return prefs.getString(GOOGLE_SIGN_IN_ACCOUNT_OBJECT, null) != null;
    }

    /**
     * Saves a json representation of the user's GoogleSignInAccount to shared prefs.
     *
     * @param jsonGoogleUser A JSON representation of the user's GoogleSignInAccount.
     */
    public void cacheGoogleAccountSignIn(String jsonGoogleUser) {
        prefs.edit().putString(GOOGLE_SIGN_IN_ACCOUNT_OBJECT, jsonGoogleUser).commit();
    }

    /**
     * Saves a json representation of the user's GoogleSignInAccount to shared prefs.
     *
     * @param googleSignInUser The user's GoogleSignInAccount object.
     */
    public void cacheGoogleAccountSignIn(GoogleSignInAccount googleSignInUser) {
        Gson gson = new Gson();
        GoogleUser googleUser = new GoogleUser(googleSignInUser);
        String s = gson.toJson(googleUser);
        prefs.edit().putString(GOOGLE_SIGN_IN_ACCOUNT_OBJECT, s).commit();
    }

    /**
     * Saves a json representation of the user's GoogleSignInAccount to shared prefs.
     *
     * @param googleUser The user's GoogleSignInAccount object.
     */
    public void cacheGoogleAccountSignIn(GoogleUser googleUser) {
        Gson gson = new Gson();
        String s = gson.toJson(googleUser);
        prefs.edit().putString(GOOGLE_SIGN_IN_ACCOUNT_OBJECT, s).commit();
    }

    /**
     * Returns the user's GoogleSignInAccountObject as json from shared preferences.
     *
     * @return
     */
    public String getGoogleUserAsJson() {
        return prefs.getString(GOOGLE_SIGN_IN_ACCOUNT_OBJECT, null);
    }

    /**
     * Returns the user's GoogleSignInAccount object by deserializing the json representation of it
     * stored in shared prefs.
     *
     * @return The user's last saved GoogleSignIn object.
     */
    public GoogleUser getCachedGoogleUser() {
        Gson gson = new Gson();
        String json = prefs.getString(GOOGLE_SIGN_IN_ACCOUNT_OBJECT, null);
        GoogleUser cachedAct = null;
        if (json != null) {
            cachedAct = gson.fromJson(json, GoogleUser.class);
        }
        return cachedAct;
    }

    /**
     * Removes the json representation of the user's GoogleSignInAccount from shared prefs.
     */
    public void removeSavedGoogleSignIn() {
        prefs.edit().remove(GOOGLE_SIGN_IN_ACCOUNT_OBJECT).commit();
    }
    //endregion

    //region ************ FCM TOKEN ***************

    /**
     * Checks if a cached FCM token exists in shared prefs.
     *
     * @return
     */
    public boolean hasCachedFcmToken() {
        return prefs.getString(FCM_TOKEN, null) != null;
    }

    /**
     * Saves the FCM token to shared preferences
     *
     * @param token
     */
    public void cacheFcmToken(String token) {
        prefs.edit().putString(FCM_TOKEN, token).commit();
    }

    /**
     * Retrieves the cached FCM token from shared preferences
     *
     * @return The cached token string.
     */
    public String getCachedFcmToken() {
        return prefs.getString(FCM_TOKEN, null);
    }

    //endregion

    //region ************ ALERTS ***************

    public void addProximityAlert(String userid, float distanceInMeters) {

    }

    //endregion



}