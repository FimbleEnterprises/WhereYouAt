package com.fimbleenterprises.whereuat;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.webkit.MimeTypeMap;
import android.widget.ScrollView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class StaticHelpers {
    
    public static class Notifications {

        private static final String TAG = "Helpers.Notifications";
        
        public static Notification showNotification(Context context, String title, String contentText, int importance) {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context.getApplicationContext(), "22");

            Intent i = new Intent(context.getApplicationContext(), MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Intent ii = new Intent(context.getApplicationContext(), MainActivity.class);
            ii.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ii.setAction("");

            PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), 22, i
                    , PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent stopTripIntent = PendingIntent.getActivity(context.getApplicationContext(), 22
                    , ii, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(pendingIntent);
            mBuilder.setOngoing(true);
            mBuilder.setSmallIcon(R.drawable.ic_menu_camera);
            mBuilder.setContentTitle(title);
            mBuilder.setContentText(contentText);
            mBuilder.addAction(R.drawable.ic_menu_gallery, "some action",
                    stopTripIntent);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilder.setSmallIcon(R.drawable.ic_menu_gallery);
                mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_round));
                mBuilder.setColor(Color.WHITE);
            } else {
                Log.i(TAG, "getNotification ");
            }

            NotificationManager mNotificationManager;
            mNotificationManager = (NotificationManager) context.getApplicationContext()
                    .getSystemService(context.getApplicationContext().NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = "22";
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "WHEREYOUAT",
                        importance);
                mNotificationManager.createNotificationChannel(channel);
                mBuilder.setChannelId(channelId);
            }

            Notification notif = mBuilder.build();

            notif.flags = Notification.FLAG_ONGOING_EVENT;

            mNotificationManager.notify(22, notif);

            return notif;
        }
    }

    public static class Permissions extends AppCompatActivity {

        private static final String TAG = "Permissions";
        private static Context context = MyApp.getAppContext();

        /**
         * Checks if the specified permission is currently granted
         * @param type The permission to evaluate
         * @return A boolean result
         */
        public static boolean isGranted(PermissionType type) {
            String permission = Permission.getPermission(type);
            int res = context.checkCallingOrSelfPermission(permission);
            return (res == PackageManager.PERMISSION_GRANTED);
        }

        /**
         * A container to house permissions that will be requested of the OS
         */
        public static class RequestContainer {
            private ArrayList<String> permissions;

            public RequestContainer() {
                permissions = new ArrayList<>();
            }

            /**
             * Adds a permission string to the list if it isn't already present
             * @param permissionType
             */
            public void add(PermissionType permissionType) {
                if (!exists(permissionType)) {
                    this.permissions.add(Permission.getPermission(permissionType));
                }
            }

            /**
             * Checks if a permission is already in the list.
             * @param permissionType The permission to check for
             * @return a bool
             */
            public boolean exists(PermissionType permissionType) {
                for (String p : this.permissions) {
                    if (p.equals(Permission.getPermission(permissionType))) {
                        return true;
                    }
                }
                return false;
            }

            /**
             * Removes a permission from the list
             * @param permissionType
             */
            public void remove(PermissionType permissionType) {
                for (int i = 0; i < this.permissions.size(); i++) {
                    String perm = this.permissions.get(i);
                    if (perm.equals(Permission.getPermission(permissionType))) {
                        this.permissions.remove(i);
                        return;
                    }
                }
            }

            /**
             * Converts the permissions list to a string array consumable by the OS' permission request methodology
             * @return The permissions as a string array.
             */
            public String[] toArray() {
                String[] array = new String[permissions.size()];
                for (int i = 0; i < this.permissions.size(); i++) {
                    array[i] = this.permissions.get(i);
                }
                return array;
            }
        }

        /**
         * An enumeration of permission names to (more easily) enable strongly typed permission handling
         */
        public enum PermissionType {
            ACCEPT_HANDOVER,
            ACCESS_BACKGROUND_LOCATION,
            ACCESS_CHECKIN_PROPERTIES,
            ACCESS_COARSE_LOCATION,
            ACCESS_FINE_LOCATION,
            ACCESS_LOCATION_EXTRA_COMMANDS,
            ACCESS_MEDIA_LOCATION,
            ACCESS_NETWORK_STATE,
            ACCESS_NOTIFICATION_POLICY,
            ACCESS_WIFI_STATE,
            ACCOUNT_MANAGER,
            ACTIVITY_RECOGNITION,
            ADD_VOICEMAIL,
            ANSWER_PHONE_CALLS,
            BATTERY_STATS,
            BIND_ACCESSIBILITY_SERVICE,
            BIND_APPWIDGET,
            BIND_AUTOFILL_SERVICE,
            BIND_CALL_REDIRECTION_SERVICE,
            BIND_CARRIER_MESSAGING_CLIENT_SERVICE,

            BIND_CARRIER_MESSAGING_SERVICE,
            BIND_CARRIER_SERVICES,
            BIND_CHOOSER_TARGET_SERVICE,
            BIND_CONDITION_PROVIDER_SERVICE,
            BIND_DEVICE_ADMIN,
            BIND_DREAM_SERVICE,
            BIND_INCALL_SERVICE,
            BIND_INPUT_METHOD,
            BIND_MIDI_DEVICE_SERVICE,
            BIND_NFC_SERVICE,
            BIND_NOTIFICATION_LISTENER_SERVICE,
            BIND_PRINT_SERVICE,
            BIND_QUICK_SETTINGS_TILE,
            BIND_REMOTEVIEWS,
            BIND_SCREENING_SERVICE,
            BIND_TELECOM_CONNECTION_SERVICE,
            BIND_TEXT_SERVICE,
            BIND_TV_INPUT,
            BIND_VISUAL_VOICEMAIL_SERVICE,
            BIND_VOICE_INTERACTION,
            BIND_VPN_SERVICE,
            BIND_VR_LISTENER_SERVICE,
            BIND_WALLPAPER,
            BLUETOOTH,
            BLUETOOTH_ADMIN,
            BLUETOOTH_PRIVILEGED,
            BODY_SENSORS,
            BROADCAST_PACKAGE_REMOVED,
            BROADCAST_SMS,
            BROADCAST_STICKY,
            BROADCAST_WAP_PUSH,
            CALL_COMPANION_APP,
            CALL_PHONE,
            CALL_PRIVILEGED,
            CAMERA,
            CAPTURE_AUDIO_OUTPUT,
            CHANGE_COMPONENT_ENABLED_STATE,
            CHANGE_CONFIGURATION,
            CHANGE_NETWORK_STATE,
            CHANGE_WIFI_MULTICAST_STATE,
            CHANGE_WIFI_STATE,
            CLEAR_APP_CACHE,
            CONTROL_LOCATION_UPDATES,
            DELETE_CACHE_FILES,
            DELETE_PACKAGES,
            DIAGNOSTIC,
            DISABLE_KEYGUARD,
            DUMP,
            EXPAND_STATUS_BAR,
            FACTORY_TEST,
            FOREGROUND_SERVICE,
            GET_ACCOUNTS,
            GET_ACCOUNTS_PRIVILEGED,
            GET_PACKAGE_SIZE,

            GET_TASKS,
            GLOBAL_SEARCH,
            INSTALL_LOCATION_PROVIDER,
            INSTALL_PACKAGES,
            INSTALL_SHORTCUT,
            INSTANT_APP_FOREGROUND_SERVICE,
            INTERNET,
            KILL_BACKGROUND_PROCESSES,
            LOCATION_HARDWARE,
            MANAGE_DOCUMENTS,
            MANAGE_OWN_CALLS,
            MASTER_CLEAR,
            MEDIA_CONTENT_CONTROL,
            MODIFY_AUDIO_SETTINGS,
            MODIFY_PHONE_STATE,
            MOUNT_FORMAT_FILESYSTEMS,
            MOUNT_UNMOUNT_FILESYSTEMS,
            NFC,
            NFC_TRANSACTION_EVENT,
            PACKAGE_USAGE_STATS,

            PERSISTENT_ACTIVITY,

            PROCESS_OUTGOING_CALLS,
            READ_CALENDAR,
            READ_CALL_LOG,
            READ_CONTACTS,
            READ_EXTERNAL_STORAGE,

            READ_INPUT_STATE,
            READ_LOGS,
            READ_PHONE_NUMBERS,
            READ_PHONE_STATE,
            READ_SMS,
            READ_SYNC_SETTINGS,
            READ_SYNC_STATS,
            READ_VOICEMAIL,
            REBOOT,
            RECEIVE_BOOT_COMPLETED,
            RECEIVE_MMS,
            RECEIVE_SMS,
            RECEIVE_WAP_PUSH,
            RECORD_AUDIO,
            REORDER_TASKS,
            REQUEST_COMPANION_RUN_IN_BACKGROUND,
            REQUEST_COMPANION_USE_DATA_IN_BACKGROUND,
            REQUEST_DELETE_PACKAGES,
            REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            REQUEST_INSTALL_PACKAGES,
            REQUEST_PASSWORD_COMPLEXITY,

            RESTART_PACKAGES,
            SEND_RESPOND_VIA_MESSAGE,
            SEND_SMS,
            SET_ALARM,
            SET_ALWAYS_FINISH,
            SET_ANIMATION_SCALE,
            SET_DEBUG_APP,

            SET_PREFERRED_APPLICATIONS,
            SET_PROCESS_LIMIT,
            SET_TIME,
            SET_TIME_ZONE,
            SET_WALLPAPER,
            SET_WALLPAPER_HINTS,
            SIGNAL_PERSISTENT_PROCESSES,
            SMS_FINANCIAL_TRANSACTIONS,
            STATUS_BAR,
            SYSTEM_ALERT_WINDOW,
            TRANSMIT_IR,
            UNINSTALL_SHORTCUT,
            UPDATE_DEVICE_STATS,
            USE_BIOMETRIC,

            USE_FINGERPRINT,
            USE_FULL_SCREEN_INTENT,
            USE_SIP,
            VIBRATE,
            WAKE_LOCK,
            WRITE_APN_SETTINGS,
            WRITE_CALENDAR,
            WRITE_CALL_LOG,
            WRITE_CONTACTS,
            WRITE_EXTERNAL_STORAGE,
            WRITE_GSERVICES,
            WRITE_SECURE_SETTINGS,
            WRITE_SETTINGS,
            WRITE_SYNC_SETTINGS,
            WRITE_VOICEMAIL,
        }

        public static class Permission {

            /**
             * Returns the Android permission string as stipulated in the Manifest class
             * @param value The permission type to find a string value for
             * @return The official permission string ex: "android.permission.ACCESS_BACKGROUND_LOCATION"
             */
            public static String getPermission(PermissionType value) {
                switch (value) {
                    case ACCEPT_HANDOVER : return ACCEPT_HANDOVER;
                    case ACCESS_BACKGROUND_LOCATION : return ACCESS_BACKGROUND_LOCATION;
                    case ACCESS_CHECKIN_PROPERTIES : return ACCESS_CHECKIN_PROPERTIES;
                    case ACCESS_COARSE_LOCATION : return ACCESS_COARSE_LOCATION;
                    case ACCESS_FINE_LOCATION : return ACCESS_FINE_LOCATION;
                    case ACCESS_LOCATION_EXTRA_COMMANDS : return ACCESS_LOCATION_EXTRA_COMMANDS;
                    case ACCESS_MEDIA_LOCATION : return ACCESS_MEDIA_LOCATION;
                    case ACCESS_NETWORK_STATE : return ACCESS_NETWORK_STATE;
                    case ACCESS_NOTIFICATION_POLICY : return ACCESS_NOTIFICATION_POLICY;
                    case ACCESS_WIFI_STATE : return ACCESS_WIFI_STATE;
                    case ACCOUNT_MANAGER : return ACCOUNT_MANAGER;
                    case ACTIVITY_RECOGNITION : return ACTIVITY_RECOGNITION;
                    case ADD_VOICEMAIL : return ADD_VOICEMAIL;
                    case ANSWER_PHONE_CALLS : return ANSWER_PHONE_CALLS;
                    case BATTERY_STATS : return BATTERY_STATS;
                    case BIND_ACCESSIBILITY_SERVICE : return BIND_ACCESSIBILITY_SERVICE;
                    case BIND_APPWIDGET : return BIND_APPWIDGET;
                    case BIND_AUTOFILL_SERVICE : return BIND_AUTOFILL_SERVICE;
                    case BIND_CALL_REDIRECTION_SERVICE : return BIND_CALL_REDIRECTION_SERVICE;
                    case BIND_CARRIER_MESSAGING_CLIENT_SERVICE : return BIND_CARRIER_MESSAGING_CLIENT_SERVICE;
                    case BIND_CARRIER_MESSAGING_SERVICE : return BIND_CARRIER_MESSAGING_SERVICE;
                    case BIND_CARRIER_SERVICES : return BIND_CARRIER_SERVICES;
                    case BIND_CHOOSER_TARGET_SERVICE : return BIND_CHOOSER_TARGET_SERVICE;
                    case BIND_CONDITION_PROVIDER_SERVICE : return BIND_CONDITION_PROVIDER_SERVICE;
                    case BIND_DEVICE_ADMIN : return BIND_DEVICE_ADMIN;
                    case BIND_DREAM_SERVICE : return BIND_DREAM_SERVICE;
                    case BIND_INCALL_SERVICE : return BIND_INCALL_SERVICE;
                    case BIND_INPUT_METHOD : return BIND_INPUT_METHOD;
                    case BIND_MIDI_DEVICE_SERVICE : return BIND_MIDI_DEVICE_SERVICE;
                    case BIND_NFC_SERVICE : return BIND_NFC_SERVICE;
                    case BIND_NOTIFICATION_LISTENER_SERVICE : return BIND_NOTIFICATION_LISTENER_SERVICE;
                    case BIND_PRINT_SERVICE : return BIND_PRINT_SERVICE;
                    case BIND_QUICK_SETTINGS_TILE : return BIND_QUICK_SETTINGS_TILE;
                    case BIND_REMOTEVIEWS : return BIND_REMOTEVIEWS;
                    case BIND_SCREENING_SERVICE : return BIND_SCREENING_SERVICE;
                    case BIND_TELECOM_CONNECTION_SERVICE : return BIND_TELECOM_CONNECTION_SERVICE;
                    case BIND_TEXT_SERVICE : return BIND_TEXT_SERVICE;
                    case BIND_TV_INPUT : return BIND_TV_INPUT;
                    case BIND_VISUAL_VOICEMAIL_SERVICE : return BIND_VISUAL_VOICEMAIL_SERVICE;
                    case BIND_VOICE_INTERACTION : return BIND_VOICE_INTERACTION;
                    case BIND_VPN_SERVICE : return BIND_VPN_SERVICE;
                    case BIND_VR_LISTENER_SERVICE : return BIND_VR_LISTENER_SERVICE;
                    case BIND_WALLPAPER : return BIND_WALLPAPER;
                    case BLUETOOTH : return BLUETOOTH;
                    case BLUETOOTH_ADMIN : return BLUETOOTH_ADMIN;
                    case BLUETOOTH_PRIVILEGED : return BLUETOOTH_PRIVILEGED;
                    case BODY_SENSORS : return BODY_SENSORS;
                    case BROADCAST_PACKAGE_REMOVED : return BROADCAST_PACKAGE_REMOVED;
                    case BROADCAST_SMS : return BROADCAST_SMS;
                    case BROADCAST_STICKY : return BROADCAST_STICKY;
                    case BROADCAST_WAP_PUSH : return BROADCAST_WAP_PUSH;
                    case CALL_COMPANION_APP : return CALL_COMPANION_APP;
                    case CALL_PHONE : return CALL_PHONE;
                    case CALL_PRIVILEGED : return CALL_PRIVILEGED;
                    case CAMERA : return CAMERA;
                    case CAPTURE_AUDIO_OUTPUT : return CAPTURE_AUDIO_OUTPUT;
                    case CHANGE_COMPONENT_ENABLED_STATE : return CHANGE_COMPONENT_ENABLED_STATE;
                    case CHANGE_CONFIGURATION : return CHANGE_CONFIGURATION;
                    case CHANGE_NETWORK_STATE : return CHANGE_NETWORK_STATE;
                    case CHANGE_WIFI_MULTICAST_STATE : return CHANGE_WIFI_MULTICAST_STATE;
                    case CHANGE_WIFI_STATE : return CHANGE_WIFI_STATE;
                    case CLEAR_APP_CACHE : return CLEAR_APP_CACHE;
                    case CONTROL_LOCATION_UPDATES : return CONTROL_LOCATION_UPDATES;
                    case DELETE_CACHE_FILES : return DELETE_CACHE_FILES;
                    case DELETE_PACKAGES : return DELETE_PACKAGES;
                    case DIAGNOSTIC : return DIAGNOSTIC;
                    case DISABLE_KEYGUARD : return DISABLE_KEYGUARD;
                    case DUMP : return DUMP;
                    case EXPAND_STATUS_BAR : return EXPAND_STATUS_BAR;
                    case FACTORY_TEST : return FACTORY_TEST;
                    case FOREGROUND_SERVICE : return FOREGROUND_SERVICE;
                    case GET_ACCOUNTS : return GET_ACCOUNTS;
                    case GET_ACCOUNTS_PRIVILEGED : return GET_ACCOUNTS_PRIVILEGED;
                    case GET_PACKAGE_SIZE : return GET_PACKAGE_SIZE;
                    case GET_TASKS : return GET_TASKS;
                    case GLOBAL_SEARCH : return GLOBAL_SEARCH;
                    case INSTALL_LOCATION_PROVIDER : return INSTALL_LOCATION_PROVIDER;
                    case INSTALL_PACKAGES : return INSTALL_PACKAGES;
                    case INSTALL_SHORTCUT : return INSTALL_SHORTCUT;
                    case INSTANT_APP_FOREGROUND_SERVICE : return INSTANT_APP_FOREGROUND_SERVICE;
                    case INTERNET : return INTERNET;
                    case KILL_BACKGROUND_PROCESSES : return KILL_BACKGROUND_PROCESSES;
                    case LOCATION_HARDWARE : return LOCATION_HARDWARE;
                    case MANAGE_DOCUMENTS : return MANAGE_DOCUMENTS;
                    case MANAGE_OWN_CALLS : return MANAGE_OWN_CALLS;
                    case MASTER_CLEAR : return MASTER_CLEAR;
                    case MEDIA_CONTENT_CONTROL : return MEDIA_CONTENT_CONTROL;
                    case MODIFY_AUDIO_SETTINGS : return MODIFY_AUDIO_SETTINGS;
                    case MODIFY_PHONE_STATE : return MODIFY_PHONE_STATE;
                    case MOUNT_FORMAT_FILESYSTEMS : return MOUNT_FORMAT_FILESYSTEMS;
                    case MOUNT_UNMOUNT_FILESYSTEMS : return MOUNT_UNMOUNT_FILESYSTEMS;
                    case NFC : return NFC;
                    case NFC_TRANSACTION_EVENT : return NFC_TRANSACTION_EVENT;
                    case PACKAGE_USAGE_STATS : return PACKAGE_USAGE_STATS;
                    case PERSISTENT_ACTIVITY : return PERSISTENT_ACTIVITY;
                    case PROCESS_OUTGOING_CALLS : return PROCESS_OUTGOING_CALLS;
                    case READ_CALENDAR : return READ_CALENDAR;
                    case READ_CALL_LOG : return READ_CALL_LOG;
                    case READ_CONTACTS : return READ_CONTACTS;
                    case READ_EXTERNAL_STORAGE : return READ_EXTERNAL_STORAGE;
                    case READ_INPUT_STATE : return READ_INPUT_STATE;
                    case READ_LOGS : return READ_LOGS;
                    case READ_PHONE_NUMBERS : return READ_PHONE_NUMBERS;
                    case READ_PHONE_STATE : return READ_PHONE_STATE;
                    case READ_SMS : return READ_SMS;
                    case READ_SYNC_SETTINGS : return READ_SYNC_SETTINGS;
                    case READ_SYNC_STATS : return READ_SYNC_STATS;
                    case READ_VOICEMAIL : return READ_VOICEMAIL;
                    case REBOOT : return REBOOT;
                    case RECEIVE_BOOT_COMPLETED : return RECEIVE_BOOT_COMPLETED;
                    case RECEIVE_MMS : return RECEIVE_MMS;
                    case RECEIVE_SMS : return RECEIVE_SMS;
                    case RECEIVE_WAP_PUSH : return RECEIVE_WAP_PUSH;
                    case RECORD_AUDIO : return RECORD_AUDIO;
                    case REORDER_TASKS : return REORDER_TASKS;
                    case REQUEST_COMPANION_RUN_IN_BACKGROUND : return REQUEST_COMPANION_RUN_IN_BACKGROUND;
                    case REQUEST_COMPANION_USE_DATA_IN_BACKGROUND : return REQUEST_COMPANION_USE_DATA_IN_BACKGROUND;
                    case REQUEST_DELETE_PACKAGES : return REQUEST_DELETE_PACKAGES;
                    case REQUEST_IGNORE_BATTERY_OPTIMIZATIONS : return REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
                    case REQUEST_INSTALL_PACKAGES : return REQUEST_INSTALL_PACKAGES;
                    case REQUEST_PASSWORD_COMPLEXITY : return REQUEST_PASSWORD_COMPLEXITY;
                    case RESTART_PACKAGES : return RESTART_PACKAGES;
                    case SEND_RESPOND_VIA_MESSAGE : return SEND_RESPOND_VIA_MESSAGE;
                    case SEND_SMS : return SEND_SMS;
                    case SET_ALARM : return SET_ALARM;
                    case SET_ALWAYS_FINISH : return SET_ALWAYS_FINISH;
                    case SET_ANIMATION_SCALE : return SET_ANIMATION_SCALE;
                    case SET_DEBUG_APP : return SET_DEBUG_APP;
                    case SET_PREFERRED_APPLICATIONS : return SET_PREFERRED_APPLICATIONS;
                    case SET_PROCESS_LIMIT : return SET_PROCESS_LIMIT;
                    case SET_TIME : return SET_TIME;
                    case SET_TIME_ZONE : return SET_TIME_ZONE;
                    case SET_WALLPAPER : return SET_WALLPAPER;
                    case SET_WALLPAPER_HINTS : return SET_WALLPAPER_HINTS;
                    case SIGNAL_PERSISTENT_PROCESSES : return SIGNAL_PERSISTENT_PROCESSES;
                    case SMS_FINANCIAL_TRANSACTIONS : return SMS_FINANCIAL_TRANSACTIONS;
                    case STATUS_BAR : return STATUS_BAR;
                    case SYSTEM_ALERT_WINDOW : return SYSTEM_ALERT_WINDOW;
                    case TRANSMIT_IR : return TRANSMIT_IR;
                    case UNINSTALL_SHORTCUT : return UNINSTALL_SHORTCUT;
                    case UPDATE_DEVICE_STATS : return UPDATE_DEVICE_STATS;
                    case USE_BIOMETRIC : return USE_BIOMETRIC;
                    case USE_FINGERPRINT : return USE_FINGERPRINT;
                    case USE_FULL_SCREEN_INTENT : return USE_FULL_SCREEN_INTENT;
                    case USE_SIP : return USE_SIP;
                    case VIBRATE : return VIBRATE;
                    case WAKE_LOCK : return WAKE_LOCK;
                    case WRITE_APN_SETTINGS : return WRITE_APN_SETTINGS;
                    case WRITE_CALENDAR : return WRITE_CALENDAR;
                    case WRITE_CALL_LOG : return WRITE_CALL_LOG;
                    case WRITE_CONTACTS : return WRITE_CONTACTS;
                    case WRITE_EXTERNAL_STORAGE : return WRITE_EXTERNAL_STORAGE;
                    case WRITE_GSERVICES : return WRITE_GSERVICES;
                    case WRITE_SECURE_SETTINGS : return WRITE_SECURE_SETTINGS;
                    case WRITE_SETTINGS : return WRITE_SETTINGS;
                    case WRITE_SYNC_SETTINGS : return WRITE_SYNC_SETTINGS;
                    case WRITE_VOICEMAIL : return WRITE_VOICEMAIL;
                    default: return READ_EXTERNAL_STORAGE;
                }
            }

            public static final String ACCEPT_HANDOVER = "android.permission.ACCEPT_HANDOVER";
            public static final String ACCESS_BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION";
            public static final String ACCESS_CHECKIN_PROPERTIES = "android.permission.ACCESS_CHECKIN_PROPERTIES";
            public static final String ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
            public static final String ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
            public static final String ACCESS_LOCATION_EXTRA_COMMANDS = "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS";
            public static final String ACCESS_MEDIA_LOCATION = "android.permission.ACCESS_MEDIA_LOCATION";
            public static final String ACCESS_NETWORK_STATE = "android.permission.ACCESS_NETWORK_STATE";
            public static final String ACCESS_NOTIFICATION_POLICY = "android.permission.ACCESS_NOTIFICATION_POLICY";
            public static final String ACCESS_WIFI_STATE = "android.permission.ACCESS_WIFI_STATE";
            public static final String ACCOUNT_MANAGER = "android.permission.ACCOUNT_MANAGER";
            public static final String ACTIVITY_RECOGNITION = "android.permission.ACTIVITY_RECOGNITION";
            public static final String ADD_VOICEMAIL = "com.android.voicemail.permission.ADD_VOICEMAIL";
            public static final String ANSWER_PHONE_CALLS = "android.permission.ANSWER_PHONE_CALLS";
            public static final String BATTERY_STATS = "android.permission.BATTERY_STATS";
            public static final String BIND_ACCESSIBILITY_SERVICE = "android.permission.BIND_ACCESSIBILITY_SERVICE";
            public static final String BIND_APPWIDGET = "android.permission.BIND_APPWIDGET";
            public static final String BIND_AUTOFILL_SERVICE = "android.permission.BIND_AUTOFILL_SERVICE";
            public static final String BIND_CALL_REDIRECTION_SERVICE = "android.permission.BIND_CALL_REDIRECTION_SERVICE";
            public static final String BIND_CARRIER_MESSAGING_CLIENT_SERVICE = "android.permission.BIND_CARRIER_MESSAGING_CLIENT_SERVICE";
            /** @deprecated */
            @Deprecated
            public static final String BIND_CARRIER_MESSAGING_SERVICE = "android.permission.BIND_CARRIER_MESSAGING_SERVICE";
            public static final String BIND_CARRIER_SERVICES = "android.permission.BIND_CARRIER_SERVICES";
            public static final String BIND_CHOOSER_TARGET_SERVICE = "android.permission.BIND_CHOOSER_TARGET_SERVICE";
            public static final String BIND_CONDITION_PROVIDER_SERVICE = "android.permission.BIND_CONDITION_PROVIDER_SERVICE";
            public static final String BIND_DEVICE_ADMIN = "android.permission.BIND_DEVICE_ADMIN";
            public static final String BIND_DREAM_SERVICE = "android.permission.BIND_DREAM_SERVICE";
            public static final String BIND_INCALL_SERVICE = "android.permission.BIND_INCALL_SERVICE";
            public static final String BIND_INPUT_METHOD = "android.permission.BIND_INPUT_METHOD";
            public static final String BIND_MIDI_DEVICE_SERVICE = "android.permission.BIND_MIDI_DEVICE_SERVICE";
            public static final String BIND_NFC_SERVICE = "android.permission.BIND_NFC_SERVICE";
            public static final String BIND_NOTIFICATION_LISTENER_SERVICE = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE";
            public static final String BIND_PRINT_SERVICE = "android.permission.BIND_PRINT_SERVICE";
            public static final String BIND_QUICK_SETTINGS_TILE = "android.permission.BIND_QUICK_SETTINGS_TILE";
            public static final String BIND_REMOTEVIEWS = "android.permission.BIND_REMOTEVIEWS";
            public static final String BIND_SCREENING_SERVICE = "android.permission.BIND_SCREENING_SERVICE";
            public static final String BIND_TELECOM_CONNECTION_SERVICE = "android.permission.BIND_TELECOM_CONNECTION_SERVICE";
            public static final String BIND_TEXT_SERVICE = "android.permission.BIND_TEXT_SERVICE";
            public static final String BIND_TV_INPUT = "android.permission.BIND_TV_INPUT";
            public static final String BIND_VISUAL_VOICEMAIL_SERVICE = "android.permission.BIND_VISUAL_VOICEMAIL_SERVICE";
            public static final String BIND_VOICE_INTERACTION = "android.permission.BIND_VOICE_INTERACTION";
            public static final String BIND_VPN_SERVICE = "android.permission.BIND_VPN_SERVICE";
            public static final String BIND_VR_LISTENER_SERVICE = "android.permission.BIND_VR_LISTENER_SERVICE";
            public static final String BIND_WALLPAPER = "android.permission.BIND_WALLPAPER";
            public static final String BLUETOOTH = "android.permission.BLUETOOTH";
            public static final String BLUETOOTH_ADMIN = "android.permission.BLUETOOTH_ADMIN";
            public static final String BLUETOOTH_PRIVILEGED = "android.permission.BLUETOOTH_PRIVILEGED";
            public static final String BODY_SENSORS = "android.permission.BODY_SENSORS";
            public static final String BROADCAST_PACKAGE_REMOVED = "android.permission.BROADCAST_PACKAGE_REMOVED";
            public static final String BROADCAST_SMS = "android.permission.BROADCAST_SMS";
            public static final String BROADCAST_STICKY = "android.permission.BROADCAST_STICKY";
            public static final String BROADCAST_WAP_PUSH = "android.permission.BROADCAST_WAP_PUSH";
            public static final String CALL_COMPANION_APP = "android.permission.CALL_COMPANION_APP";
            public static final String CALL_PHONE = "android.permission.CALL_PHONE";
            public static final String CALL_PRIVILEGED = "android.permission.CALL_PRIVILEGED";
            public static final String CAMERA = "android.permission.CAMERA";
            public static final String CAPTURE_AUDIO_OUTPUT = "android.permission.CAPTURE_AUDIO_OUTPUT";
            public static final String CHANGE_COMPONENT_ENABLED_STATE = "android.permission.CHANGE_COMPONENT_ENABLED_STATE";
            public static final String CHANGE_CONFIGURATION = "android.permission.CHANGE_CONFIGURATION";
            public static final String CHANGE_NETWORK_STATE = "android.permission.CHANGE_NETWORK_STATE";
            public static final String CHANGE_WIFI_MULTICAST_STATE = "android.permission.CHANGE_WIFI_MULTICAST_STATE";
            public static final String CHANGE_WIFI_STATE = "android.permission.CHANGE_WIFI_STATE";
            public static final String CLEAR_APP_CACHE = "android.permission.CLEAR_APP_CACHE";
            public static final String CONTROL_LOCATION_UPDATES = "android.permission.CONTROL_LOCATION_UPDATES";
            public static final String DELETE_CACHE_FILES = "android.permission.DELETE_CACHE_FILES";
            public static final String DELETE_PACKAGES = "android.permission.DELETE_PACKAGES";
            public static final String DIAGNOSTIC = "android.permission.DIAGNOSTIC";
            public static final String DISABLE_KEYGUARD = "android.permission.DISABLE_KEYGUARD";
            public static final String DUMP = "android.permission.DUMP";
            public static final String EXPAND_STATUS_BAR = "android.permission.EXPAND_STATUS_BAR";
            public static final String FACTORY_TEST = "android.permission.FACTORY_TEST";
            public static final String FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE";
            public static final String GET_ACCOUNTS = "android.permission.GET_ACCOUNTS";
            public static final String GET_ACCOUNTS_PRIVILEGED = "android.permission.GET_ACCOUNTS_PRIVILEGED";
            public static final String GET_PACKAGE_SIZE = "android.permission.GET_PACKAGE_SIZE";
            /** @deprecated */
            @Deprecated
            public static final String GET_TASKS = "android.permission.GET_TASKS";
            public static final String GLOBAL_SEARCH = "android.permission.GLOBAL_SEARCH";
            public static final String INSTALL_LOCATION_PROVIDER = "android.permission.INSTALL_LOCATION_PROVIDER";
            public static final String INSTALL_PACKAGES = "android.permission.INSTALL_PACKAGES";
            public static final String INSTALL_SHORTCUT = "com.android.launcher.permission.INSTALL_SHORTCUT";
            public static final String INSTANT_APP_FOREGROUND_SERVICE = "android.permission.INSTANT_APP_FOREGROUND_SERVICE";
            public static final String INTERNET = "android.permission.INTERNET";
            public static final String KILL_BACKGROUND_PROCESSES = "android.permission.KILL_BACKGROUND_PROCESSES";
            public static final String LOCATION_HARDWARE = "android.permission.LOCATION_HARDWARE";
            public static final String MANAGE_DOCUMENTS = "android.permission.MANAGE_DOCUMENTS";
            public static final String MANAGE_OWN_CALLS = "android.permission.MANAGE_OWN_CALLS";
            public static final String MASTER_CLEAR = "android.permission.MASTER_CLEAR";
            public static final String MEDIA_CONTENT_CONTROL = "android.permission.MEDIA_CONTENT_CONTROL";
            public static final String MODIFY_AUDIO_SETTINGS = "android.permission.MODIFY_AUDIO_SETTINGS";
            public static final String MODIFY_PHONE_STATE = "android.permission.MODIFY_PHONE_STATE";
            public static final String MOUNT_FORMAT_FILESYSTEMS = "android.permission.MOUNT_FORMAT_FILESYSTEMS";
            public static final String MOUNT_UNMOUNT_FILESYSTEMS = "android.permission.MOUNT_UNMOUNT_FILESYSTEMS";
            public static final String NFC = "android.permission.NFC";
            public static final String NFC_TRANSACTION_EVENT = "android.permission.NFC_TRANSACTION_EVENT";
            public static final String PACKAGE_USAGE_STATS = "android.permission.PACKAGE_USAGE_STATS";
            /** @deprecated */
            @Deprecated
            public static final String PERSISTENT_ACTIVITY = "android.permission.PERSISTENT_ACTIVITY";
            /** @deprecated */
            @Deprecated
            public static final String PROCESS_OUTGOING_CALLS = "android.permission.PROCESS_OUTGOING_CALLS";
            public static final String READ_CALENDAR = "android.permission.READ_CALENDAR";
            public static final String READ_CALL_LOG = "android.permission.READ_CALL_LOG";
            public static final String READ_CONTACTS = "android.permission.READ_CONTACTS";
            public static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
            /** @deprecated */
            @Deprecated
            public static final String READ_INPUT_STATE = "android.permission.READ_INPUT_STATE";
            public static final String READ_LOGS = "android.permission.READ_LOGS";
            public static final String READ_PHONE_NUMBERS = "android.permission.READ_PHONE_NUMBERS";
            public static final String READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
            public static final String READ_SMS = "android.permission.READ_SMS";
            public static final String READ_SYNC_SETTINGS = "android.permission.READ_SYNC_SETTINGS";
            public static final String READ_SYNC_STATS = "android.permission.READ_SYNC_STATS";
            public static final String READ_VOICEMAIL = "com.android.voicemail.permission.READ_VOICEMAIL";
            public static final String REBOOT = "android.permission.REBOOT";
            public static final String RECEIVE_BOOT_COMPLETED = "android.permission.RECEIVE_BOOT_COMPLETED";
            public static final String RECEIVE_MMS = "android.permission.RECEIVE_MMS";
            public static final String RECEIVE_SMS = "android.permission.RECEIVE_SMS";
            public static final String RECEIVE_WAP_PUSH = "android.permission.RECEIVE_WAP_PUSH";
            public static final String RECORD_AUDIO = "android.permission.RECORD_AUDIO";
            public static final String REORDER_TASKS = "android.permission.REORDER_TASKS";
            public static final String REQUEST_COMPANION_RUN_IN_BACKGROUND = "android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND";
            public static final String REQUEST_COMPANION_USE_DATA_IN_BACKGROUND = "android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND";
            public static final String REQUEST_DELETE_PACKAGES = "android.permission.REQUEST_DELETE_PACKAGES";
            public static final String REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS";
            public static final String REQUEST_INSTALL_PACKAGES = "android.permission.REQUEST_INSTALL_PACKAGES";
            public static final String REQUEST_PASSWORD_COMPLEXITY = "android.permission.REQUEST_PASSWORD_COMPLEXITY";
            /** @deprecated */
            @Deprecated
            public static final String RESTART_PACKAGES = "android.permission.RESTART_PACKAGES";
            public static final String SEND_RESPOND_VIA_MESSAGE = "android.permission.SEND_RESPOND_VIA_MESSAGE";
            public static final String SEND_SMS = "android.permission.SEND_SMS";
            public static final String SET_ALARM = "com.android.alarm.permission.SET_ALARM";
            public static final String SET_ALWAYS_FINISH = "android.permission.SET_ALWAYS_FINISH";
            public static final String SET_ANIMATION_SCALE = "android.permission.SET_ANIMATION_SCALE";
            public static final String SET_DEBUG_APP = "android.permission.SET_DEBUG_APP";
            /** @deprecated */
            @Deprecated
            public static final String SET_PREFERRED_APPLICATIONS = "android.permission.SET_PREFERRED_APPLICATIONS";
            public static final String SET_PROCESS_LIMIT = "android.permission.SET_PROCESS_LIMIT";
            public static final String SET_TIME = "android.permission.SET_TIME";
            public static final String SET_TIME_ZONE = "android.permission.SET_TIME_ZONE";
            public static final String SET_WALLPAPER = "android.permission.SET_WALLPAPER";
            public static final String SET_WALLPAPER_HINTS = "android.permission.SET_WALLPAPER_HINTS";
            public static final String SIGNAL_PERSISTENT_PROCESSES = "android.permission.SIGNAL_PERSISTENT_PROCESSES";
            public static final String SMS_FINANCIAL_TRANSACTIONS = "android.permission.SMS_FINANCIAL_TRANSACTIONS";
            public static final String STATUS_BAR = "android.permission.STATUS_BAR";
            public static final String SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW";
            public static final String TRANSMIT_IR = "android.permission.TRANSMIT_IR";
            public static final String UNINSTALL_SHORTCUT = "com.android.launcher.permission.UNINSTALL_SHORTCUT";
            public static final String UPDATE_DEVICE_STATS = "android.permission.UPDATE_DEVICE_STATS";
            public static final String USE_BIOMETRIC = "android.permission.USE_BIOMETRIC";
            /** @deprecated */
            @Deprecated
            public static final String USE_FINGERPRINT = "android.permission.USE_FINGERPRINT";
            public static final String USE_FULL_SCREEN_INTENT = "android.permission.USE_FULL_SCREEN_INTENT";
            public static final String USE_SIP = "android.permission.USE_SIP";
            public static final String VIBRATE = "android.permission.VIBRATE";
            public static final String WAKE_LOCK = "android.permission.WAKE_LOCK";
            public static final String WRITE_APN_SETTINGS = "android.permission.WRITE_APN_SETTINGS";
            public static final String WRITE_CALENDAR = "android.permission.WRITE_CALENDAR";
            public static final String WRITE_CALL_LOG = "android.permission.WRITE_CALL_LOG";
            public static final String WRITE_CONTACTS = "android.permission.WRITE_CONTACTS";
            public static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
            public static final String WRITE_GSERVICES = "android.permission.WRITE_GSERVICES";
            public static final String WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS";
            public static final String WRITE_SETTINGS = "android.permission.WRITE_SETTINGS";
            public static final String WRITE_SYNC_SETTINGS = "android.permission.WRITE_SYNC_SETTINGS";
            public static final String WRITE_VOICEMAIL = "com.android.voicemail.permission.WRITE_VOICEMAIL";
        }

        public static class PermissionUtils {

            @RequiresApi(api = Build.VERSION_CODES.M)
            public static boolean neverAskAgainSelected(final Activity activity, final String permission) {
                final boolean prevShouldShowStatus = getRatinaleDisplayStatus(activity,permission);
                final boolean currShouldShowStatus = activity.shouldShowRequestPermissionRationale(permission);
                return prevShouldShowStatus != currShouldShowStatus;
            }

            public static void setShouldShowStatus(final Context context, final String permission) {
                SharedPreferences genPrefs = context.getSharedPreferences("GENERIC_PREFERENCES", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = genPrefs.edit();
                editor.putBoolean(permission, true);
                editor.commit();
            }
            public static boolean getRatinaleDisplayStatus(final Context context, final String permission) {
                SharedPreferences genPrefs =     context.getSharedPreferences("GENERIC_PREFERENCES", Context.MODE_PRIVATE);
                return genPrefs.getBoolean(permission, false);
            }
        }
    }

    public static class Animations {

        public enum AnimationType {
            WOBBLER, PULSE, PULSE_HARDER
        }

        public static void pulseAnimation(View target) {
            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(target,
                    PropertyValuesHolder.ofFloat("scaleX", 1.00f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.15f));
            scaleDown.setDuration(750);

            scaleDown.setRepeatCount(250);
            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);

            scaleDown.start();
        }

        public static void pulseAnimation(View target, float scaleX, float scaleY, int repeatCount, int scaleDownDuration) {
            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(target,
                    PropertyValuesHolder.ofFloat("scaleX", scaleX),
                    PropertyValuesHolder.ofFloat("scaleY", scaleY));
            scaleDown.setDuration(scaleDownDuration);

            if (repeatCount > 0) {
                scaleDown.setRepeatCount(repeatCount);
            }

            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);

            scaleDown.start();
        }

        public static void fadeOut(View view, int duration, Animation.AnimationListener callback) {
            Animation fade = new AlphaAnimation(1, 0);
            fade.setInterpolator(new AccelerateInterpolator()); //and this
            fade.setStartOffset(1000);
            fade.setDuration(duration);

            AnimationSet animation = new AnimationSet(false); //change to false
            animation.addAnimation(fade);

            animation.setAnimationListener(callback);

            view.setAnimation(animation);
        }

        public static void fadeIn(View view, int duration, Animation.AnimationListener callback) {
            Animation fade = new AlphaAnimation(0, 1);
            fade.setInterpolator(new AccelerateInterpolator()); //and this
            fade.setStartOffset(1000);
            fade.setDuration(duration);

            AnimationSet animation = new AnimationSet(false); //change to false
            animation.addAnimation(fade);

            animation.setAnimationListener(callback);

            view.setAnimation(animation);
        }

        public static void fadeOut(View view, int duration) {
            Animation fade = new AlphaAnimation(1, 0);
            fade.setInterpolator(new AccelerateInterpolator()); //and this
            fade.setStartOffset(1000);
            fade.setDuration(duration);

            AnimationSet animation = new AnimationSet(false); //change to false
            animation.addAnimation(fade);

            view.setAnimation(animation);
        }

        public static void fadeIn(View view, int duration) {
            Animation fade = new AlphaAnimation(0, 1);
            fade.setInterpolator(new AccelerateInterpolator()); //and this
            fade.setStartOffset(1000);
            fade.setDuration(duration);

            AnimationSet animation = new AnimationSet(false); //change to false
            animation.addAnimation(fade);

            view.setAnimation(animation);
        }

        public static void animateView(View view, Context context, AnimationType animationType) {
            int resourceId;
            switch (animationType) {
                case WOBBLER:
                    resourceId = R.anim.wobbler;
                    break;
                case PULSE:
                    resourceId = R.anim.pulse;
                    break;
                case PULSE_HARDER:
                    resourceId = R.anim.pulse_harder;
                    break;
                default:
                    resourceId = R.anim.pulse;
                    break;
            }
            final Animation b = AnimationUtils.loadAnimation(context, resourceId);
            b.reset();
            b.setRepeatCount(Animation.INFINITE);
            view.startAnimation(b);
        }

        public static Animation outToLeftAnimation() {
            Animation outtoLeft = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
            );
            outtoLeft.setDuration(175);
            outtoLeft.setInterpolator(new AccelerateInterpolator());
            return outtoLeft;
        }

        public static Animation inFromLeftAnimation() {
            Animation inFromLeft = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
            );
            inFromLeft.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            inFromLeft.setDuration(175);
            inFromLeft.setInterpolator(new AccelerateInterpolator());
            return inFromLeft;
        }

        public static Animation outToRightAnimation() {
            Animation outtoRight = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
            );
            outtoRight.setDuration(175);
            outtoRight.setInterpolator(new AccelerateInterpolator());
            return outtoRight;
        }

        public static Animation inFromRightAnimation() {
            Animation inFromRight = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
            );
            inFromRight.setDuration(175);
            inFromRight.setInterpolator(new AccelerateInterpolator());
            return inFromRight;
        }

        public static Animation outToTop() {
            Animation animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, +0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 1.0f);
            animation.setDuration(175);
            animation.setInterpolator(new AccelerateInterpolator());
            return animation;
        }

        public static Animation inFromTop() {
            Animation animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, +0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f);
            animation.setDuration(175);
            animation.setInterpolator(new AccelerateInterpolator());
            return animation;
        }
    }

    public static class Application {

        private static final String TAG = "Application";

        /**
         * Fully kills the current process and gracefully starts it again after.
         * @param c A valid context.
         */
        public static void restart(Context c) {
            try {
                //check if the context is given
                if (c != null) {
                    //fetch the packagemanager so we can get the default launch activity
                    // (you can replace this intent with any other activity if you want
                    PackageManager pm = c.getPackageManager();
                    //check if we got the PackageManager
                    if (pm != null) {
                        //create the intent with the default start activity for your application
                        Intent mStartActivity = pm.getLaunchIntentForPackage(
                                c.getPackageName()
                        );
                        if (mStartActivity != null) {
                            mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            //create a pending intent so the application is restarted after System.exit(0) was called.
                            // We use an AlarmManager to call this intent in 100ms
                            int mPendingIntentId = 223344;
                            PendingIntent mPendingIntent = PendingIntent
                                    .getActivity(c, mPendingIntentId, mStartActivity,
                                            PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                            //kill the application
                            System.exit(0);
                        } else {
                            Log.e(TAG, "Was not able to restart application, mStartActivity null");
                        }
                    } else {
                        Log.e(TAG, "Was not able to restart application, PM null");
                    }
                } else {
                    Log.e(TAG, "Was not able to restart application, Context null");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Was not able to restart application");
            }
        }
    }

    public static class Listeners {
        public interface DecoderListener {
            public void onSuccess(File decodedFile);
            public void onFailure(String error);
        }

        public interface EncoderListener {
            public void onSuccess(String base64String);
            public void onFailure(String error);
        }
    }

    public static class Bitmaps {
        public static Bitmap getBitmapFromResource(Context context, @DrawableRes int resource) {
            return BitmapFactory.decodeResource(context.getResources(),
                    resource);
        }

        public static File createPngFileFromString(String text, String fileName) throws IOException {

            fileName = fileName.replace(".txt",".png");
            if (!fileName.endsWith(".png")) {
                fileName += ".png";
            }

            final Rect bounds = new Rect();
            TextPaint textPaint = new TextPaint() {
                {
                    setColor(Color.WHITE);
                    setTextAlign(Paint.Align.LEFT);
                    setTextSize(20f);
                    setAntiAlias(true);
                }
            };
            textPaint.getTextBounds(text, 0, text.length(), bounds);
            StaticLayout mTextLayout = new StaticLayout(text, textPaint,
                    bounds.width(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            int maxWidth = -1;
            for (int i = 0; i < mTextLayout.getLineCount(); i++) {
                if (maxWidth < mTextLayout.getLineWidth(i)) {
                    maxWidth = (int) mTextLayout.getLineWidth(i);
                }
            }
            final Bitmap bmp = Bitmap.createBitmap(maxWidth , mTextLayout.getHeight(),
                    Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.BLACK);// just adding black background
            final Canvas canvas = new Canvas(bmp);
            mTextLayout.draw(canvas);
            File outputFile = new File(Files.getAppTempDirectory(), fileName);
            FileOutputStream stream = new FileOutputStream(outputFile); //create your FileOutputStream here
            bmp.compress(Bitmap.CompressFormat.PNG, 85, stream);
            bmp.recycle();
            stream.close();
            return outputFile;
        }

        public static File createJpegFileFromString(String text, String fileName) throws IOException {

            fileName = fileName.replace(".txt",".jpeg");
            if (!fileName.endsWith(".jpeg")) {
                fileName += ".jpeg";
            }

            final Rect bounds = new Rect();
            TextPaint textPaint = new TextPaint() {
                {
                    setColor(Color.WHITE);
                    setTextAlign(Paint.Align.LEFT);
                    setTextSize(20f);
                    setAntiAlias(true);
                }
            };
            textPaint.getTextBounds(text, 0, text.length(), bounds);
            StaticLayout mTextLayout = new StaticLayout(text, textPaint,
                    bounds.width(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            int maxWidth = -1;
            for (int i = 0; i < mTextLayout.getLineCount(); i++) {
                if (maxWidth < mTextLayout.getLineWidth(i)) {
                    maxWidth = (int) mTextLayout.getLineWidth(i);
                }
            }
            final Bitmap bmp = Bitmap.createBitmap(maxWidth , mTextLayout.getHeight(),
                    Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.BLACK);// just adding black background
            final Canvas canvas = new Canvas(bmp);
            mTextLayout.draw(canvas);
            File outputFile = new File(StaticHelpers.Files.getAppTempDirectory(), fileName);
            FileOutputStream stream = new FileOutputStream(outputFile); //create your FileOutputStream here
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream);
            bmp.recycle();
            stream.close();
            return outputFile;
        }

        /**
         * Converts any view to a bitmap.
         */
        public static Bitmap saveScrollViewAsImage(ScrollView scrollView) {
            Bitmap bitmap = Bitmap.createBitmap(
                    scrollView.getChildAt(0).getWidth(),
                    scrollView.getChildAt(0).getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            scrollView.getChildAt(0).draw(c);
            return bitmap;
        }

        public static Bitmap saveViewAsImage(View view) {
            view.setDrawingCacheEnabled(true);
            view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
            view.buildDrawingCache(true);
            Bitmap saveBm = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            return saveBm;
        }

        /**
         * Saves a bitmap to a png file
         * @param bmp The bitmap to save
         * @param file A file to create
         * @return The created file
         */
        public static File bitmapToFile(Bitmap bmp, File file) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
                return file;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static class Files {

        private static final String TAG = "Files";

        /**
         * Encodes a file to a base64 string.
         * @param filePath The file to encode.
         * @return A base64 string representation of the supplied file.
         */
        public static String base64Encode(String filePath) {
            String base64File = "";
            File file = new File(filePath);
            try (FileInputStream imageInFile = new FileInputStream(file)) {
                // Reading a file from file system
                byte fileData[] = new byte[(int) file.length()];
                imageInFile.read(fileData);
                base64File = Base64.getEncoder().encodeToString(fileData);
            } catch (FileNotFoundException e) {
                System.out.println("File not found" + e);
            } catch (IOException ioe) {
                System.out.println("Exception while reading the file " + ioe);
            }
            return base64File;
        }

        /**
         * Encodes a file to a base64 string asynchronously.
         * @param filePath The path to the file to encode.
         * @param listener A listener to monitor the results.
         */
        public static void base64Encode(final String filePath, final Listeners.EncoderListener listener) {

            final AsyncTask<String, String, String> task = new AsyncTask<String, String, String>() {

                boolean wasSuccessful = false;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    Log.i(TAG, "onPreExecute Preparing to encode file at path: " + filePath);
                }

                @Override
                protected String doInBackground(String... args) {
                    String base64File = "";
                    File file = new File(filePath);
                    try (FileInputStream imageInFile = new FileInputStream(file)) {
                        // Reading a file from file system
                        byte fileData[] = new byte[(int) file.length()];
                        imageInFile.read(fileData);
                        base64File = Base64.getEncoder().encodeToString(fileData);
                        wasSuccessful = true;
                        return base64File;
                    } catch (FileNotFoundException e) {
                        System.out.println("File not found" + e);
                        return e.getLocalizedMessage();
                    } catch (IOException ioe) {
                        System.out.println("Exception while reading the file " + ioe);
                        return ioe.getLocalizedMessage();
                    }
                }

                @Override
                protected void onPostExecute(String val) {
                    super.onPostExecute(val);
                    if (wasSuccessful) {
                        Log.i(TAG, "onPostExecute File was encoded!");
                        listener.onSuccess(val);
                    } else {
                        Log.w(TAG, "onPostExecute: Failed to encode\nError: " + val);
                        listener.onFailure(val);
                    }
                }
            };

            // The lack of this check has burned me before.  It's verbose and not always needed for reasons
            // unknown but I'd leave it!
            if(Build.VERSION.SDK_INT >= 11/*HONEYCOMB*/) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                task.execute();
            }


        }

        /**
         * Converts a base64 string into its constituent file.  The file is stored in the a
         * subdirectory of the application's temp directory
         * @param base64string The string to decode
         * @param outputFile The location to store the decoded file.
         * @return A MyFile file that by default exists in the app's temp directory.
         */
        public static File base64Decode(String base64string, File outputFile) {

            try {
                Base64.Decoder dec = Base64.getDecoder();
                byte[] strdec = dec.decode(base64string);
                OutputStream out = new FileOutputStream(outputFile);
                out.write(strdec);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return outputFile;
        }

        /**
         * Converts a base64 string into its constituent file.  The file is stored in the a
         * subdirectory of the application's temp directory
         * @param base64string The string to decode
         * @param listener A listener to monitor completion.
         * @return A File file that by default exists in the app's temp directory.
         */
        public static void base64Decode(final String base64string, final File outputFile, final Listeners.DecoderListener listener) {

            AsyncTask<String, String, String> task = new AsyncTask<String, String, String>() {

                boolean wasSuccessful = false;

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    Log.i(TAG, "onPreExecute Preparing to decode file at: " + outputFile.getPath());
                }

                @Override
                protected String doInBackground(String... args) {
                    try {
                        Base64.Decoder dec = Base64.getDecoder();
                        byte[] strdec = dec.decode(base64string);
                        OutputStream out = new FileOutputStream(outputFile);
                        out.write(strdec);
                        out.close();
                        wasSuccessful = true;
                        return outputFile.getPath();
                    } catch (Exception e) {
                        e.printStackTrace();
                        wasSuccessful = false;
                        return e.getLocalizedMessage();
                    }
                }

                @Override
                protected void onPostExecute(String val) {
                    super.onPostExecute(val);
                    if (wasSuccessful) {
                        Log.i(TAG, "onPostExecute File was decoded!");
                        listener.onSuccess(outputFile);
                    } else {
                        Log.w(TAG, "onPostExecute: File was NOT decoded\nError:" + val);
                        listener.onFailure(val);
                    }
                }
            };

            // The lack of this check has burned me before.  It's verbose and not always needed for reasons
            // unknown but I'd leave it!
            if(Build.VERSION.SDK_INT >= 11/*HONEYCOMB*/) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                task.execute();
            }
        }

        /**
         * Tries to open a file using the default viewer.
         * @param file The file to open.
         * @param mimeType The file's mime type.
         */
        public static void openFile(File file,  String mimeType) {
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            Intent newIntent = new Intent(Intent.ACTION_VIEW);
            newIntent.setDataAndType(Uri.fromFile(file), mimeType);
            newIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            Context context = MyApp.getAppContext();
            try {
                context.startActivity(newIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(context, "No handler for this type of file.", Toast.LENGTH_LONG).show();
            }
        }

        public static void shareFile(Context context, File file) {
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
            File fileWithinMyDir = file;

            if(fileWithinMyDir.exists()) {
                intentShareFile.setType(getMimetype(file));
                intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+file));

                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                        "Sharing File...");
                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");

                context.startActivity(Intent.createChooser(intentShareFile, "Share File"));
            }
        }

        /**
         * Shares a file.  You may have to supply an activity as context if it is failing.
         * @param context A valid context, this may have to be an activity if a basic context is failing.
         * @param file
         * @param subject
         */
        public static void shareFile(Context context, File file, String subject) {
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
            File fileWithinMyDir = file;

            if(fileWithinMyDir.exists()) {
                intentShareFile.setType(getMimetype(file));
                intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+file));
                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                        subject);
                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
                intentShareFile.addFlags(FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(Intent.createChooser(intentShareFile, "Share File"));
            }
        }

        public static boolean copy(File source, File dest) {
            try {
                FileChannel src = new FileInputStream(source).getChannel();
                @SuppressWarnings("resource")
                FileChannel dst = new FileOutputStream(dest).getChannel();
                long bytes = dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                return dest.exists();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public static long convertBytesToKb(long total) {
            return total / (1024);
        }

        public static double convertBytesToKb(double total) {
            return total / (1024);
        }

        public static long convertBytesToMb(long total) {
            return total / (1024 * 1024);
        }

        public static double convertBytesToMb(double total) {
            return total / (1024 * 1024);
        }

        public static float convertBytesToMb(long total, boolean decimals) {
            DecimalFormat df = new DecimalFormat("0.00");
            String strResult =  df.format((float) total / (1024 * 1024));
            return Float.parseFloat(strResult);
        }

        public static long convertBytesToGb(long total) {
            return total / (1024 * 1024 * 1024);
        }

        /**
         * Returns the supplied file's extension (e.g. .png).  Returns null if any errors are thrown.
         *
         * @param fileName Either a fully qualified file or just a file fullname.
         * @return A (always) lowercase string, which includes the period, representing the file's extension. (e.g. .png)
         */
        public static String getExtension(String fileName) {
            String extension = "";

            try {
                int lastPeriod = fileName.lastIndexOf(".");
                extension = fileName.substring(lastPeriod);
                extension = extension.toLowerCase();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;

            }
            return extension;
        }

        /**
         * Attempts to parse out the filename from a url or filesystem file (assuming the filesystem
         * uses a forward slash as the file separator)
         *
         * @param path A url or fully qualified path to the file to parse.
         * @return The filename or whatever comes after the final "/" found in the string
         */
        public static String parseFileNameFromPath(String path) {
            int fSlashIndex = path.lastIndexOf(File.separator);
            String filename = path.substring(fSlashIndex + 1);
            return filename;
        }

        // Checks for the existence of a file. Returns boolean.
        public static boolean fileExists(String path, String filename) {

            boolean result = false;

            java.io.File file = new java.io.File(path, filename);
            if (file.exists()) {
                result = true;
                Log.d("fileExists", "Found the file at: " + path + filename);
                // b.setCompoundDrawablesWithIntrinsicBounds(null, PLAYLOGO , null,
                // null);
                result = true;

            } else {
                Log.d("fileExists", "Couldn't find the file at: " + path + filename);
                result = false;
            }

            return result;
        }

        /**
         * Deletes all the files in the specified directory
         **/
        public static boolean deleteDirectory(String filePath) {
            File path = new File(filePath);
            if (path.exists()) {
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; i++) {
                    try {
                        files[i].delete();
                        Log.d(TAG, "Deleted: " + files[i].getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return (path.delete());
        }// END deleteDirectory()

        public static File getAppDirectory() {
            makeAppDirectory();
            Context context = MyApp.getAppContext();
            File dir = new File(context.getExternalFilesDir(null), "MileBuddy");
            Log.i(TAG, "getAppDirectory: " + dir.getAbsolutePath());
            return dir;
        }

        public static void makeAppDirectory() {

            Context context = MyApp.getAppContext();

            File dir = new File(context.getExternalFilesDir(null).getAbsolutePath());

            if (!dir.exists() || !dir.isDirectory()) {
                Log.i(TAG, "makeAppDirectory: " + dir.mkdirs());
            } else {
                Log.i(TAG, "makeAppDirectory: App directory exists");
            }
        }

        public static String getMimetype(Context context, Uri uri) {
            String mimeType = null;
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                ContentResolver cr = context.getContentResolver();
                mimeType = cr.getType(uri);
            } else {
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                        .toString());
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        fileExtension.toLowerCase());
            }
            return mimeType;
        }

        public static String getMimetype(File file) {
            try {
                String mimetype = URLConnection.guessContentTypeFromName(file.getName());
                if (mimetype != null) {
                    return mimetype;
                } else {
                    return "application/octet-stream";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "application/octet-stream";
            }
        }

        /*
        public static void makeBackupDirectory() {

            Context context = MyApp.getAppContext();

            File dir = new File(getAppDirectory().getPath(), "Backups");

            if (!dir.exists() || !dir.isDirectory()) {
                Log.i(TAG, "makeBackupDirectory: " + dir.mkdirs());;
            } else {
                Log.i(TAG, "makeBackupDirectory: Backup directory exists");
            }
        }
        */

        public static File getAppDownloadDirectory() {
            return new File(MyApp.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString());
        }

        public static File getAppTempDirectory() {
            File tmp = new File(MyApp.getAppContext().getExternalFilesDir(null).toString() + File.separator
                    + "temp");
            if (!tmp.exists()) {
                tmp.mkdirs();
            }
            return tmp;
        }

        public static boolean deleteAppTempDirectory() {
            boolean result = false;

            File tempDir = getAppTempDirectory();
            if (tempDir.exists()) {
                if (tempDir.isDirectory()) {
                    for (File f : tempDir.listFiles()) {
                        result = f.delete();
                        Log.i(TAG, "deleteAppTempDirectory | deleted a file (" + f.getName() + ")");
                    }
                    tempDir.delete();
                }
            }

            Log.i(TAG, "deleteAppTempDirectory " + !tempDir.exists());
            return result;
        }

        public static class AttachmentTempFiles {
            /**
             * Creates a subdirectory to the application's temp directory named, "attachments" if it doesn't
             * already exist.
             */
            public static void makeDirectory() {
                File tmp = new File(getAppDirectory().getAbsolutePath() + File.separator + "attachments");
                if (!tmp.exists()) {
                    Log.i(TAG, "makeDirectory " + tmp.getAbsolutePath() + " doesn't exist, creating...");
                    tmp.mkdirs();
                    Log.i(TAG, "makeDirectory Attachments directory created: " + tmp.exists());
                } else {
                    Log.i(TAG, "makeDirectory " + tmp.getAbsolutePath() + " already exists.");
                }
            }

            /**
             * Clears all files from the application's attachment temp directory (assuming it exists)
             */
            public static void clear() {
                File tmp = new File(getAppDirectory().getAbsolutePath() + File.separator + "attachments");
                Log.i(TAG, "Clearing attachments directory...");
                if (tmp.exists()) {
                    Log.i(TAG, "clear " + tmp.getAbsolutePath() + " exists!");
                    File[] contents = tmp.listFiles();
                    for (int i = 0; i < contents.length; i++) {
                        contents[i].delete();
                        Log.i(TAG, "clearAttachmentTempDirectory Deleted: " + contents[i].getName());
                    }
                } else {
                    Log.i(TAG, "clear " + tmp.getAbsolutePath() + " didn't exist.");
                }
            }

            /**
             * Returns the attachments temp directory.  The directory will be created if it doesn't
             * already exist.
             * @return The application's attachment temp directory.
             */
            public static File getDirectory() {
                makeDirectory();
                File file = new File(getAppDirectory().getAbsolutePath() + File.separator + "attachments");
                Log.i(TAG, "getDirectory " + file.getAbsolutePath() + " exists: " + file.exists());
                Log.i(TAG, "getDirectory Attachments directory: " + file.getAbsolutePath());
                return file;
            }

            /**
             * Searches the application's attachment temp directory for the specified filename
             * @param filename The name of the file to look for.
             * @return The file (or null if not found)
             */
            public static File retrieve(String filename) {
                Log.i(TAG, "retrieve Looking for file in attachments temp dir (" + filename + ")");
                File tmp = getDirectory();
                File[] files = tmp.listFiles();
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.getName().equals(filename)) {
                        Log.i(TAG, file.getAbsolutePath() + " found!");
                        return file;
                    }
                }
                Log.i(TAG, "retrieve File not found in " + tmp.getAbsolutePath() + "!");
                return null;
            }

            /**
             * Checks if the specified filename exists in the application's attachment temp directory.
             * @param filename The filenasme to look for.
             * @return True or false if the file exists.
             */
            public static boolean fileExists(String filename) {
                Log.i(TAG, "fileExists: " + filename + " = " + (retrieve(filename) != null));
                return retrieve(filename) != null;
            }

            /**
             * Returns all files that exist in the application's attachment temp directory.
             * @return
             */
            public static File[] getFiles() {
                File tmp = getDirectory();
                File[] files = tmp.listFiles();
                Log.i(TAG, "getFiles Found " + files.length + " files in the attachments temp directory.");
                return files;
            }
        }

        public static class ExcelTempFiles {
            /**
             * Creates a subdirectory to the application's temp directory named, "spreadsheets" if it doesn't
             * already exist.
             */
            public static void makeDirectory() {
                File tmp = new File(getAppDirectory().getAbsolutePath() + File.separator + "spreadsheets");
                if (!tmp.exists()) {
                    Log.i(TAG, "makeDirectory " + tmp.getAbsolutePath() + " doesn't exist, creating...");
                    tmp.mkdirs();
                    Log.i(TAG, "makeDirectory Attachments directory created: " + tmp.exists());
                } else {
                    Log.i(TAG, "makeDirectory " + tmp.getAbsolutePath() + " already exists.");
                }
            }

            /**
             * Clears all files from the application's attachment temp directory (assuming it exists)
             */
            public static void clear() {
                File tmp = new File(getAppDirectory().getAbsolutePath() + File.separator + "spreadsheets");
                Log.i(TAG, "Clearing spreadsheets directory...");
                if (tmp.exists()) {
                    Log.i(TAG, "clear " + tmp.getAbsolutePath() + " exists!");
                    File[] contents = tmp.listFiles();
                    for (int i = 0; i < contents.length; i++) {
                        contents[i].delete();
                        Log.i(TAG, "clearAttachmentTempDirectory Deleted: " + contents[i].getName());
                    }
                } else {
                    Log.i(TAG, "clear " + tmp.getAbsolutePath() + " didn't exist.");
                }
            }

            /**
             * Returns the spreadsheets temp directory.  The directory will be created if it doesn't
             * already exist.
             * @return The application's attachment temp directory.
             */
            public static File getDirectory() {
                makeDirectory();
                File file = new File(getAppDirectory().getAbsolutePath() + File.separator + "spreadsheets");
                Log.i(TAG, "getDirectory " + file.getAbsolutePath() + " exists: " + file.exists());
                Log.i(TAG, "getDirectory Attachments directory: " + file.getAbsolutePath());
                return file;
            }

            /**
             * Searches the application's attachment temp directory for the specified filename
             * @param filename The name of the file to look for.
             * @return The file (or null if not found)
             */
            public static File retrieve(String filename) {
                Log.i(TAG, "retrieve Looking for file in spreadsheets temp dir (" + filename + ")");
                File tmp = getDirectory();
                File[] files = tmp.listFiles();
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.getName().equals(filename)) {
                        Log.i(TAG, file.getAbsolutePath() + " found!");
                        return file;
                    }
                }
                Log.i(TAG, "retrieve File not found in " + tmp.getAbsolutePath() + "!");
                return null;
            }

            /**
             * Checks if the specified filename exists in the application's attachment temp directory.
             * @param filename The filenasme to look for.
             * @return True or false if the file exists.
             */
            public static boolean fileExists(String filename) {
                Log.i(TAG, "fileExists: " + filename + " = " + (retrieve(filename) != null));
                return retrieve(filename) != null;
            }

            /**
             * Returns all files that exist in the application's attachment temp directory.
             * @return
             */
            public static File[] getFiles() {
                File tmp = getDirectory();
                File[] files = tmp.listFiles();
                Log.i(TAG, "getFiles Found " + files.length + " files in the spreadsheets temp directory.");
                return files;
            }
        }
    }

    public static class Colors {
        public static final String YELLOW = "#EFC353";
        public static final String MEDISTIM_ORANGE = "#AAF37021";
        public static final String GREEN = "#2D9B01";
        public static final String RED = "#FF0000";
        public static final String MAROON = "#7F0000";
        public static final String SOFT_BLACK = "#3C4F5F";
        public static final String BLUE = "#0026FF";
        public static final String DISABLED_GRAY = "#808080";

        public static int getColor(String color) {
            return Color.parseColor(color);
        }
    }

    public static class BytesAndBits {
        public static long convertBytesToKb(long total) {
            return total / (1024);
        }

        public static long convertBytesToMb(long total) {
            return total / (1024 * 1024);
        }

        public static long convertBytesToGb(long total) {
            return total / (1024 * 1024 * 1024);
        }
    }
    
}
