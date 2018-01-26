package com.example.chandora.rider;

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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback
        , GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = CustomersMapActivity.class.getSimpleName();
    private static final int REQUEST_CODE = 1;





    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationRequest mLocationRequest;
    private LatLng pickupLocation;

    private Button mLogOutBtn, mRequestBtn ,mSettings ,mHistory;
    LinearLayout mDriverLayout;
    ImageView mProfileImage;
    TextView mDriverName, mDriverPhone, mDriverCar;

    private Boolean requestBoolean = false;
    private int radius = 1000;
    private boolean isDriverFound = false;
    private String driverFoundId;
    private Marker mDriverMarker;
    private RadioGroup mRadioGroup;

    String destination;
    private String requestService;
    private LatLng destinationLatLng;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        mLogOutBtn = (Button) findViewById(R.id.btn_logout_cust);
        mRequestBtn = (Button) findViewById(R.id.btn_request);
        mSettings = (Button) findViewById(R.id.btn_settings_cust);
        mProfileImage = (ImageView)findViewById(R.id.driver_profile_view);
        mDriverName = (TextView) findViewById(R.id.driver_name);
        mDriverPhone = (TextView) findViewById(R.id.driver_no);
        mDriverCar = (TextView) findViewById(R.id.driver_car);
        mDriverLayout = (LinearLayout)findViewById(R.id.driver_info);
        mRadioGroup = (RadioGroup) findViewById(R.id.radio_group_cust);
        mHistory = (Button) findViewById(R.id.btn_history_cust);

        destinationLatLng = new LatLng(0.0,0.0);

        mRadioGroup.check(R.id.rider_x_cust);



        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent customerSettingIntent = new Intent(CustomersMapActivity.this,CustomerSettingsActivity.class);
                startActivity(customerSettingIntent);
                return;

            }
        });


        mLogOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(CustomersMapActivity.this, MainActivity.class));
                finish();
                return;
            }
        });

        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomersMapActivity.this,HistoryActivity.class);
                startActivity(intent);
                return;
            }
        });

        mRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestBoolean) {
                    endRide();
                }

                else {
                    int selectId = mRadioGroup.getCheckedRadioButtonId();
                    RadioButton radioButton = (RadioButton) findViewById(selectId);
                    
                    if (radioButton.getText() == null){
                        return;
                    }
                    requestService = radioButton.getText().toString();

                    requestBoolean = true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLocation.getLatitude(), mLocation.getLongitude()));

                    pickupLocation = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());

                    mDriverMarker = mMap.addMarker(new MarkerOptions().title("Pickup location!").position(pickupLocation).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                    mMap.addMarker(new MarkerOptions().title("Pickup location!").position(pickupLocation));


                    mRequestBtn.setText("Getting your driver...");
                    getClosestDriver();

                }


            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (
                PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

    }

    GeoQuery geoQuery;


    private void getClosestDriver() {

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                Log.i(TAG, "onKeyEntered: ");
                if (!isDriverFound && requestBoolean) {
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDatabase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            HashMap<String, Object> driverMap = (HashMap<String, Object>) dataSnapshot.getValue();
                            if (isDriverFound){
                                return;
                            }

                            if (driverMap.get("service").equals(requestService)){

                                isDriverFound = true;
                                driverFoundId = dataSnapshot.getKey();

                                DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                HashMap hmap = new HashMap();
                                hmap.put("customerRideId", userId);
                                hmap.put("destination",destination);
                                hmap.put("destinationLat", destinationLatLng.latitude);
                                hmap.put("destinationLng",destinationLatLng.longitude);
                                driverRef.updateChildren(hmap);

                                getDriverLocation();
                                getDriverInfo();
                                getHasRideEnded();
                                mRequestBtn.setText("Looking for driver's location...");

                            }


                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }

            }

            @Override
            public void onKeyExited(String key) {
                Log.i(TAG, "onKeyExited: ");

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.i(TAG, "onKeyMoved: ");

            }

            @Override
            public void onGeoQueryReady() {
                Log.i(TAG, "onGeoQueryReady: ");
                if (!isDriverFound) {
                    radius++;
                    getClosestDriver();
                    Log.i(TAG, "onGeoQueryReady: " + radius);

                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.i(TAG, "onGeoQueryError: ");

            }
        });
    }

    private void getDriverInfo() {

        mDriverLayout.setVisibility(View.VISIBLE);
        DatabaseReference mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        mDriverDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    HashMap<String, Object> map = (HashMap<String, Object>) dataSnapshot.getValue();

                    if (map.get("name") != null) {
                        String mName = map.get("name").toString();
                        mDriverName.setText(mName);
                    }
                    if (map.get("phone") != null) {
                        String mPhone = map.get("phone").toString();
                        mDriverPhone.setText(mPhone);
                    }
                    if (map.get("car") != null) {
                        String mCar = map.get("car").toString();
                        mDriverCar.setText(mCar);
                    }

                    if (map.get("profileImageUrl") != null) {
                        String mProfileImageUrl = map.get("profileImageUrl").toString();
                        Log.i(TAG, "onDataChange: "+mProfileImageUrl);
                        Picasso.with(getApplicationContext()).load(mProfileImageUrl).into(mProfileImage);


                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private DatabaseReference driverHasEndedRef;

    private ValueEventListener driverHasEndedRefLisetener;

    private void getHasRideEnded() {

        driverHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest").child("customerRideId");
        driverHasEndedRefLisetener = driverHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                } else {
                    endRide();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void endRide() {
        requestBoolean = false;
        geoQuery.removeAllListeners();
        if (driverLocationRef != null)
            driverLocationRef.removeEventListener(driverLocationRefListener);
        if (driverHasEndedRef != null){
            driverHasEndedRef.removeEventListener(driverHasEndedRefLisetener);
        }



        if (driverFoundId != null) {
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
            driverRef.removeValue();
            driverFoundId = null;

        }

        isDriverFound = false;
        radius = 1000;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);

        if (mDriverMarker != null){
            mDriverMarker.remove();
        }


        mRequestBtn.setText("Call uber...");
        mDriverLayout.setVisibility(View.GONE);
        mDriverPhone.setText("");
        mDriverName.setText("");
        mDriverCar.setText("");
        mProfileImage.setImageResource(R.drawable.ic_launcher_background);
    }


    DatabaseReference driverLocationRef;
    ValueEventListener driverLocationRefListener;



    private void getDriverLocation() {

        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBoolean) {
                    List<Object> list = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0.0f;
                    double locationLng = 0.0f;

                    mRequestBtn.setText("Driver found...");
                    if (list.get(0) != null) {
                        locationLat = Double.parseDouble(list.get(0).toString());

                    }
                    if (list.get(1) != null) {
                        locationLng = Double.parseDouble(list.get(1).toString());
                    }

                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    float dis = loc1.distanceTo(loc2);
                    if (dis < 100) {
                        mRequestBtn.setText("Driver is nearby you");

                    } else {
                        mRequestBtn.setText("Driver's found at " + String.valueOf(dis));
                    }

                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your driver").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

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
        Log.i(TAG, "onLocationChanged: " + location.getLatitude() + " " + location.getLongitude());
        mLocation = location;


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
        Log.i(TAG, "onConnectionSuspended: ") ;

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

    private boolean checkPermission() {
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
    }

    @Override
    protected void onStop() {
        super.onStop();

    }
}
