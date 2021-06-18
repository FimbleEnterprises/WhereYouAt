package com.fimbleenterprises.whereuat.googleuser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.rest_api.Requests;
import com.fimbleenterprises.whereuat.rest_api.WebApi;
import com.fimbleenterprises.whereuat.helpers.MySettingsHelper;
import com.fimbleenterprises.whereuat.ui.other.NotSignedInActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

/**
 * Methods used to manage Google authentication.
 */
public class MyGoogleSignInHelper {

    public interface UpsertUserListener {
        void onSuccess();
        void onFailure(String msg);
    }

    public interface ValidateUserListener {
        public void isValid();
        public void notValid(String message);
    }

    private static final String TAG = "MyGoogleSignInHelper";
    public static final int GOOGLE_SIGN_IN_REQUEST_CODE = 2000;

    // Configure sign-in to request the user's ID, email address, and basic
    // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
    public static GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build();

    /**
     * Checks if the current user has successfully signed in
     * @param context A valid context for leveraging a GoogleSignIn
     * @return null if the user has not signed in - a GoogleSignInAccount object if they have.
     */
    public static boolean isSignedIn(Context context) {
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account != null) {
            Log.i(TAG, "isSignedIn User is signed in.");
        } else {
            Log.w(TAG, "isSignedIn: User is NOT signed in.");
        }

        return account != null;
    }

    /**
     * Calls the startActivity method using the context supplied.
     * @param context A context capable of calling startActivity()
     */
    public static void showSignInActivity(Activity context) {
        Log.i(TAG, "showSignInActivity | Showing the sign in activity.");
        Intent intent = new Intent(context, NotSignedInActivity.class);
        context.startActivity(intent);
    }

    /**
     * Initiates the standard, Google sign in dialog.
     * @param activity A calling activity that can override the OnActivityResult method.
     */
    public static void signIn(Activity activity) {

        Log.i(TAG, "signin - Showing the sign in dialog");

        // Build a GoogleSignInClient with the options specified by gso.
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE);
    }

    /**
     * Performs a sign out.
     * @param context
     */
    public static void signOut(Context context) {
        // Build a GoogleSignInClient with the options specified by gso.
        try {
            // Check if a trip is running and stop the services if so
            if (MyApp.isReportingLocation()) {
                Log.w(TAG, "signOut: | A trip is running - will stop commands to those services " +
                        "before attempting signout.");
                MyApp.stopAllLocationServices(true, context);
                Log.w(TAG, "signOut: | Stop commands sent, no verification that they succeeded " +
                        "was or will be performed however.");
            }

            Log.i(TAG, "signOut Signing out the user...");
            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
            mGoogleSignInClient.signOut();
            Log.i(TAG, "signOut User signed out.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts the user's GoogleSignInAccount from an intent originally received by the calling
     * activity's OnActivityResult method.  If a Google account is found in the intent data then it
     * will be saved to the local device preferences automatically and then upserted to our server.
     * @param data The intent, verbatim, received by the calling activity's onActivityResult method.
     * @return A GoogleSignInAccount object or null if one wasn't found.
     */
    public static GoogleUser handleSignInResult(Intent data) {
        try {
            MySettingsHelper options = new MySettingsHelper();

            Log.i(TAG, "handleSignInResult Parsing the sign in attempt result...");

            // Extract the GoogleSignInAccount object from the task embedded in the intent as an extra.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount googleSignInAccount = task.getResult(ApiException.class);

            if (googleSignInAccount != null) {
                Log.i(TAG, "handleSignInResult User was signed in.  Caching account to shared prefs");
                // Cache the user's Google sign in account to shared pres.
                options.cacheGoogleAccountSignIn(googleSignInAccount);

                if (options.hasCachedGoogleCredentials()) {
                    Log.i(TAG, "handleSignInResult Account was cached.");
                    // Upsert this account to our server
                    upsertGoogleUserToServer(new UpsertUserListener() {
                        @Override
                        public void onSuccess() {
                            Log.i(TAG, " !!!!!!! -= onSuccess | User was Upserted! =- !!!!!!!");
                        }

                        @Override
                        public void onFailure(String msg) {
                            Log.w(TAG, " !!!!!!! -= onFailure | FAILED TO UPSERT USER! =- !!!!!!!");;
                        }
                    });
                } else {
                    Log.w(TAG, "handleSignInResult: Failed to cache user's account");
                }
            } else {
                Log.w(TAG, "handleSignInResult: Failed to authenticate or user cancelled sign in attempt!");
            }

            // Signed in successfully, return GoogleSignInAccount object to the caller.
            return options.getCachedGoogleUser();

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            return null;
        }
    }

    /**
     * Calls our server api to create or update the user in the server-side database.
     * @param listener A simple callback as it is a separately threaded request.
     */
    public static void upsertGoogleUserToServer(final UpsertUserListener listener) {

        Log.i(TAG, "upsertServerUser Upserting user to our server...");

        MySettingsHelper options = new MySettingsHelper();
        GoogleUser account = options.getCachedGoogleUser();
        Requests.Request request = new Requests.Request(Requests.Request.UPSERT_USER);
        request.arguments.add(new Requests.Arguments.Argument("userid", account.id));
        request.arguments.add(new Requests.Arguments.Argument("email", account.email));
        request.arguments.add(new Requests.Arguments.Argument("photourl", account.photourl));
        request.arguments.add(new Requests.Arguments.Argument("displayname", account.fullname));

        WebApi api = new WebApi(MyApp.getAppContext());
        api.makeRequest(request, new WebApi.WebApiResultListener() {
            @Override
            public void onSuccess(WebApi.OperationResults results) {
                Log.i(TAG, "onSuccess ");
                listener.onSuccess();
            }

            @Override
            public void onFailure(String message) {
                Log.w(TAG, "onFailure: " + message);
                listener.onFailure(message);
            }
        });

    }

    public void validateServerGoogleAccount(ValidateUserListener listener) {

    }

}
