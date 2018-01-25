package com.example.chandora.rider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriversMapActivity extends FragmentActivity implements OnMapReadyCallback
        , GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener ,RoutingListener{
    private static final String TAG = DriversMapActivity.class.getSimpleName();
    private static final int REQUEST_CODE = 1;
    String userId,destination;
    DatabaseReference databaseReference;
    GeoFire geoFire;
    String customerId = "";
    Boolean isLoggingOut = false;
    LinearLayout mCustomerLayout;
    ImageView mProfileImage;
    TextView mCustomerName, mCustomerPhone, mCustomerDest;
    DatabaseReference assignedCustomerLocationRef;
    ValueEventListener assignedCustomerLocationListener;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationRequest mLocationRequest;
    private Button mLogOutBtn,mSettings;
    private Marker mCustomerMarker;
    private Marker mPickupMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_map);
        if (!checkPermission()) {
            requestPermission();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLogOutBtn = findViewById(R.id.btn_logout);
        mSettings = findViewById(R.id.btn_settings_driv);
        mCustomerLayout = findViewById(R.id.customer_info);
        mCustomerName = findViewById(R.id.customer_name);
        mCustomerPhone = findViewById(R.id.customer_no);
        mProfileImage = findViewById(R.id.customer_profile_view);
        mCustomerDest = findViewById(R.id.customer_destination);

        polylines = new ArrayList<>();


        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }


        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("driversAvailable");
        geoFire = new GeoFire(databaseReference);


        mLogOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                isLoggingOut = true;
                disconnectDriver();


                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(DriversMapActivity.this, MainActivity.class));
                finish();
                return;
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DriversMapActivity.this, DriverSettingsActivity.class));
                finish();
                return;
            }
        });
        getAssignedCustomer();
    }

    private boolean checkPermission() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
    }

    private void disconnectDriver() {

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("driversAvailable");
        geoFire = new GeoFire(databaseReference);
        geoFire.removeLocation(userId);

        mCustomerDest.setText("Destination--");

    }

    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();


                } else {
                    erasePolyLines();
                    customerId = "";
                    if (mPickupMarker != null) {
                        mPickupMarker.remove();
                    }
                    if (assignedCustomerLocationRef != null) {
                        assignedCustomerLocationRef.removeEventListener(assignedCustomerLocationListener);
                    }
                    mCustomerLayout.setVisibility(View.GONE);
                    mCustomerPhone.setText("");
                    mCustomerName.setText("");
                    mProfileImage.setImageResource(R.drawable.ic_launcher_background);

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void getAssignedCustomerDestination() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                if(map.get("destination")!=null){
                    destination = map.get("destination").toString();
                    mCustomerDest.setText("Destination: " + destination);
                }
                else{
                    mCustomerDest.setText("Destination: --");
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerInfo() {
        mCustomerLayout.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    HashMap<String, Object> map = (HashMap<String, Object>) dataSnapshot.getValue();

                    if (map.get("name") != null) {
                        String mName = map.get("name").toString();
                        mCustomerName.setText(mName);
                    }
                    if (map.get("phone") != null) {
                        String mPhone = map.get("phone").toString();
                        mCustomerPhone.setText(mPhone);
                    }
                    if (map.get("profileImageUrl") != null) {
                        String mProfileImageUrl = map.get("profileImageUrl").toString();
                        Picasso.with(getApplicationContext()).load(mProfileImageUrl).into(mProfileImage);

                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerPickupLocation() {

        assignedCustomerLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
        assignedCustomerLocationListener = assignedCustomerLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerId.equals("")) {

                    List<Object> list = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (list.get(0) != null) {
                        locationLat = Double.parseDouble(list.get(0).toString());
                    }
                    if (list.get(1) != null) {
                        locationLng = Double.parseDouble(list.get(1).toString());
                    }
                    LatLng pickUpLatLng = new LatLng(locationLat, locationLng);

                    if (mPickupMarker != null) {
                        mPickupMarker.remove();
                    }
                    mPickupMarker = mMap.addMarker(new MarkerOptions().position(pickUpLatLng).title("Pickup location...").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                    getRouteToMarker(pickUpLatLng);
//                    if (mCustomerMarker != null) {
//                        mCustomerMarker.remove();
//                    }
//                    mCustomerMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Pickup location..."));


                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getRouteToMarker(LatLng pickUpLatLng) {
        Routing routing = new Routing.Builder()
                            .travelMode(AbstractRouting.TravelMode.DRIVING)
                            .alternativeRoutes(false)
                            .withListener(this)
                            .waypoints(new LatLng(mLocation.getLatitude(),mLocation.getLongitude()),pickUpLatLng)
                            .build();
                routing.execute();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady: ");
        mMap = googleMap;
        MapStyleOptions styles = MapStyleOptions.loadRawResourceStyle(this,R.raw.mapstyle_night);
        mMap.setMapStyle(styles);


        buildGoogleApiClient();
        if (checkPermission()) mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "onLocationChanged: ");
        mLocation = location;


        mMap.addMarker(new MarkerOptions().position(new LatLng(mLocation.getLatitude(), mLocation.getLongitude())).title("my position"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 16));


        MarkerOptions mp = new MarkerOptions();
        mp.position(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        mp.title("my position");

        mMap.addMarker(mp);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference driversAvailableRef = FirebaseDatabase.getInstance().getReference("driversAvailable");
        DatabaseReference driversWorkingRef = FirebaseDatabase.getInstance().getReference("driversWorking");
        GeoFire geoFireAvailable = new GeoFire(driversAvailableRef);
        GeoFire geoFireWorking = new GeoFire(driversWorkingRef);

        switch (customerId) {
            case "":
                geoFireWorking.removeLocation(userId);
                geoFireAvailable.setLocation(userId, new GeoLocation(mLocation.getLatitude(), mLocation.getLongitude()));
                break;
            default:
                geoFireAvailable.removeLocation(userId);
                geoFireWorking.setLocation(userId, new GeoLocation(mLocation.getLatitude(), mLocation.getLongitude()));
                break;
        }

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected: ");
        mLocationRequest = createLocationRequest();
        if (checkPermission())
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended: ");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed: ");

    }

    private LocationRequest createLocationRequest() {
        return new LocationRequest().setInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(1000);
    }


    @Override
    protected void onStop() {
        super.onStop();


        if (!isLoggingOut) {
            disconnectDriver();
        }

    }

    @Override
    public void onRoutingFailure(RouteException e) {
        Log.i(TAG, "onRoutingFailure: "+e);
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {
        Log.i(TAG, "onRoutingStart: ");

    }
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        Log.i(TAG, "onRoutingSuccess: ");

        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onRoutingCancelled() {
        Log.i(TAG, "onRoutingCancelled: ");

    }
    private void erasePolyLines(){
        for (Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}
