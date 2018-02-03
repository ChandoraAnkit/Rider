package com.example.chandora.rider;

import android.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends RootAnimActivity{

    private static final int REQUEST_CODE =1 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!checkPermission()){
            requestPermission();
        }

        startService(new Intent(MainActivity.this,OnAppKilled.class));
    }
    private boolean checkPermission(){
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED);
    }
    private void requestPermission(){
        ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);
    }


    public void onCustomerBtnClick(View view) {
        Intent customerLoginIntent = new Intent(MainActivity.this,CustomerLoginActivity.class);
        startActivity(customerLoginIntent);
        finish();


    }

    public void OnDriverBtnClick(View view) {
        Intent driverLoginIntent = new Intent(MainActivity.this,DriverLoginActivity.class);
        startActivity(driverLoginIntent);
        finish();


    }
}
