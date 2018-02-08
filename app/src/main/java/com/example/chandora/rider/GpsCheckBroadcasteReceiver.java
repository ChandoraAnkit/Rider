package com.example.chandora.rider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;

import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * Created by chandora on 7/2/18.
 */

public class GpsCheckBroadcasteReceiver extends BroadcastReceiver {
    public final static String TAG = GpsCheckBroadcasteReceiver.class.getSimpleName();
    private static final int LOCATION_SETTINGS_REQUEST = 99;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if(intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)){
            Log.i(TAG, "onReceive: LOCATION_SETTINGS");

                    isLocationEnable(context);

        }

    }

    private boolean isLocationEnable(Context context){
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.i(TAG, "isLocationEnable: ON");
            return true;
        }else {
            Log.i(TAG, "isLocationEnable: OFF");
            displayPromptForEnablingGPS(context);
            return  false;
        }

    }

    public static void displayPromptForEnablingGPS(final Context context)
    {

        final AlertDialog.Builder builder =  new AlertDialog.Builder(context);
        final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
        final String message = "You need to turn on the GPS setting!";

        builder.setMessage(message)
                .setPositiveButton("SETTINGS",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                context.startActivity(new Intent(action));
                                d.dismiss();
                            }
                        })
                .setNegativeButton("EXIT",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int id) {
                                d.cancel();
                                System.exit(0);
                            }
                        });


                builder.create().show();

    }

}
