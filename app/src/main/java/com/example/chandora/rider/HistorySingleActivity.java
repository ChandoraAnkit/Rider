package com.example.chandora.rider;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback,RoutingListener{
    public final static String TAG  = HistorySingleActivity.class.getSimpleName();
    private GoogleMap mMap;
    private SupportMapFragment mSupportMapFragment;
    private TextView mRideLocation,mRideDistance,mRideDate,mDriverName,mDriverPhone;
    private ImageView  mDriverPhoto;
    private String rideId,currentRideId,customerId="",customerOrDriver,driverId;
    private DatabaseReference historyRideInfo;
    private LatLng destinationLatLng,pickupLatLng;
    private String distance;
    private Double ridePrice;
    private RatingBar mRatingbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        mSupportMapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mSupportMapFragment.getMapAsync(this);

        mRideLocation = findViewById(R.id.ride_location);
        mRideDistance = findViewById(R.id.ride_distance);
        mRideDate = findViewById(R.id.ride_date);
        mDriverName = findViewById(R.id.history_driver_name);
        mDriverPhone = findViewById(R.id.history_driver_phone);
        mDriverPhoto = findViewById(R.id.history_driver_image);
        mRatingbar = findViewById(R.id.rating_bar);


        polylines = new ArrayList<>();

        rideId = getIntent().getExtras().getString("rideId").toString();
        currentRideId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyRideInfo = FirebaseDatabase.getInstance().getReference().child("History").child(rideId);
        getRideInformation();


    }

    private void getRideInformation() {
        historyRideInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        if (child.getKey().equals("Customer")){
                            customerId = child.getValue().toString();
                            if (!customerId.equals(currentRideId)){
                                customerOrDriver = "Drivers";
                                getUserInformation("Customers",customerId);
                            }
                        }
                        if (child.getKey().equals("Driver")){
                            driverId = child.getValue().toString();
                            if (!driverId.equals(currentRideId)){
                                customerOrDriver = "Customers";
                                getUserInformation("Drivers",driverId);
                                displayCustomerRelatedObjects();

                            }
                        }
                        if (child.getKey().equals("Timestamp")){
                            mRideDate.setText(getDate(Long.valueOf(child.getValue().toString())));
                        }
                        if (child.getKey().equals("Destination")){
                            mRideLocation.setText(child.getValue().toString());
                        }
                        if (child.getKey().equals("Distance")){
                            distance = child.getValue().toString();
                            mRideDistance.setText(distance.substring(0,Math.min(distance.length(),5)));

                            ridePrice = Double.valueOf(distance)* 0.5;
                            Log.i(TAG, "onDataChange: "+ridePrice);
                        }
                        if (child.getKey().equals("Rating")){
                            mRatingbar.setRating(Integer.valueOf(child.getValue().toString()));
                        }
                        if (child.getKey().equals("Location")){
                           pickupLatLng = new LatLng(Double.valueOf(child.child("From").child("Lat").getValue().toString()),Double.valueOf(child.child("From").child("Lng").getValue().toString()));
                            destinationLatLng = new LatLng(Double.valueOf(child.child("To").child("Lat").getValue().toString()),Double.valueOf(child.child("To").child("Lng").getValue().toString()));
                        if (destinationLatLng != new LatLng(0.0,0.0)){
                            getRouteToMarker();
                        }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void displayCustomerRelatedObjects() {

        mRatingbar.setVisibility(View.VISIBLE);
        mRatingbar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean b) {
                historyRideInfo.child("Rating").setValue(rating);
                DatabaseReference mDriverRatingDb =  FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("Rating");
                mDriverRatingDb.child(rideId).setValue(rating);
            }
        });
    }

    private void getUserInformation(String customerOrDriver, String customerOrDriverId) {
        DatabaseReference mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child(customerOrDriver).child(customerOrDriverId);
        mDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    HashMap<String,Object> data = (HashMap<String, Object>) dataSnapshot.getValue();

                    if (data.get("name")!=null){
                        mDriverName.setText(data.get("name").toString());
                    }
                    if (data.get("phone")!=null){
                        mDriverPhone.setText(data.get("phone").toString());
                    }
                    if (data.get("profileImageUrl")!=null){
                        Picasso.with(getApplicationContext()).load(data.get("profileImageUrl").toString()).into(mDriverPhoto);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private String getDate(Long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(timestamp*1000);

        String date = DateFormat.format("dd-MM-yyyy hh:mm",calendar).toString();
        return date;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


    }
    private void getRouteToMarker() {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .alternativeRoutes(false)
                .withListener(this)
                .waypoints(pickupLatLng,destinationLatLng)
                .build();
        routing.execute();
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

        LatLngBounds.Builder  builder =  new LatLngBounds.Builder();
        builder.include(pickupLatLng);
        builder.include(destinationLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width*0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,padding);
        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().title("Pickup").position(pickupLatLng).icon(BitmapDescriptorFactory.defaultMarker()));
        mMap.addMarker(new MarkerOptions().title("Destination").position(destinationLatLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));



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



}
