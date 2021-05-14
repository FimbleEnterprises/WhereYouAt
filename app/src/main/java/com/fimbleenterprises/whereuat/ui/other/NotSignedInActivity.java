package com.fimbleenterprises.whereuat.ui.other;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.fimbleenterprises.whereuat.MainActivity;
import com.fimbleenterprises.whereuat.R;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.googleuser.MyGoogleSignInHelper;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class NotSignedInActivity extends AppCompatActivity {
    private static final String TAG = "NotSignedInActivity";
    public static final int SIGN_IN_REQUEST_CODE = 222333;

    Button btnSignIn;
    Activity context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setContentView(R.layout.activity_not_signed_in);
        btnSignIn = findViewById(R.id.btnSignIn);

        this.setTitle("Sign In");

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick ");

                MyGoogleSignInHelper.signIn(context);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == MyGoogleSignInHelper.GOOGLE_SIGN_IN_REQUEST_CODE) {
            Log.i(TAG, " !!!!!!! -= onActivityResult GOOGLE SIGN IN RESULT =- !!!!!!!");
            Log.i(TAG, "onActivityResult " + resultCode);

            MainActivity.shouldShowSignIn = false;

            // Send the intent to the MyGoogleSignInHelper class and get the user's GoogleAccount in return
            GoogleUser googleUser = MyGoogleSignInHelper.handleSignInResult(data);

            if (googleUser != null) {
                Log.i(TAG, "onActivityResult Google account returned!  Name: " + googleUser.email);
            } else {
                Log.w(TAG, "onActivityResult: No Google account was returned!");
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // Check if the user pressed the back key and close the app if they did while not being signed in.
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!MyGoogleSignInHelper.isSignedIn(context)) {
                finishActivity(SIGN_IN_REQUEST_CODE);
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        // Wait until the activity is fully created before checking if the user is signed in and
        // potentially showing the sign in dialog.

        Log.i(TAG, " !!!!!!! -= onPostCreate  =- !!!!!!!");

        super.onPostCreate(savedInstanceState);

        if (!MyGoogleSignInHelper.isSignedIn(context)) {
            MyGoogleSignInHelper.signIn(context);
        }

    }

    @Override
    protected void onResume() {

        // If the user is signed in we can close this and launch the main application proper.
        if (MyGoogleSignInHelper.isSignedIn(this)) {
            Log.i(TAG, " !!!!!!! -= onPostResume | User is signed in now - closing activity! =- !!!!!!!");
            finishAndRemoveTask();
            startActivity(new Intent(this, MainActivity.class));
        }

        super.onResume();
    }
}