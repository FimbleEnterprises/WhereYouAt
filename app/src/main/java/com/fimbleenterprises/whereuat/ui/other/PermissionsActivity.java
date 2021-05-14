package com.fimbleenterprises.whereuat.ui.other;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.fimbleenterprises.whereuat.MainActivity;
import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.StaticHelpers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fimbleenterprises.whereuat.R;

public class PermissionsActivity extends AppCompatActivity {
    public static final int REQUEST_PERMS = 998;
    private static final String TAG = "PermissionsActivity";
    Button btnGoToPerms;
    Button btnRequestPerms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate ");
        
        setContentView(R.layout.activity_permissions);
        // Toolbar toolbar = findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

        btnGoToPerms = findViewById(R.id.btnGoToManualPermissions);
        btnGoToPerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);

            }
        });

        btnRequestPerms = findViewById(R.id.btnRequestPermissions);
        btnRequestPerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                StaticHelpers.Permissions.RequestContainer container = new StaticHelpers.Permissions.RequestContainer();
                container.add(StaticHelpers.Permissions.PermissionType.ACCESS_FINE_LOCATION);
                container.add(StaticHelpers.Permissions.PermissionType.ACCESS_COARSE_LOCATION);
                container.add(StaticHelpers.Permissions.PermissionType.ACCESS_BACKGROUND_LOCATION);
                requestPermissions(container.toArray(), MyApp.REQUEST_LOC_PERMS);

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(TAG, " !!!!!!! -= onDestroy  =- !!!!!!!");

        if (!MyApp.hasCoarseLocationPermission() && !MyApp.hasFineLocationPermission()) {
            Log.i(TAG, "onDestroy User is lacking core permissions onDestroy() - presumably they " +
                    "do not want to be pestered any more - setting shouldRequestPerms to false on the main activity.");
            MainActivity.shouldRequestPermissions = false;
        }
        
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(TAG, "onStart ");

        if (MyApp.hasAllLocationPermissions()) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finishAffinity();
        } else {
            Toast.makeText(this, "All location permissions not granted!", Toast.LENGTH_SHORT).show();
        }
        
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.i(TAG, "onRequestPermissionsResult ");
        
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (MyApp.hasAllLocationPermissions()) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finishActivity(REQUEST_PERMS);
        } else {
            Toast.makeText(this, "All location permissions not granted!", Toast.LENGTH_SHORT).show();
            MainActivity.shouldRequestPermissions = false;
        }

    }
}