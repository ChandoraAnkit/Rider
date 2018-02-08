package com.example.chandora.rider;

import android.content.Context;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.example.chandora.rider.HistoryRecyclerView.HistoryAdapter;
import com.example.chandora.rider.HistoryRecyclerView.HistoryObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HistoryActivity extends RootAnimActivity {
    private RecyclerView mRecyclerView;
    private HistoryAdapter adapter;
    private String customerOrDriver,userId;
    private ProgressBar mProgressBar;
    private GpsCheckBroadcasteReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        getSupportActionBar().setTitle("Rides history");

        mRecyclerView = findViewById(R.id.recycler_view_history);
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.VISIBLE);

        mRecyclerView.setNestedScrollingEnabled(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(this,getDataSet());
        mRecyclerView.setAdapter(adapter);

        receiver = new GpsCheckBroadcasteReceiver();
        adapter.notifyDataSetChanged();

        customerOrDriver = getIntent().getStringExtra("customerOrDriver");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserHistoryIds();

    }

    private void getUserHistoryIds() {
        DatabaseReference historyDataRef = FirebaseDatabase.getInstance().getReference("Users").child(customerOrDriver).child(userId).child("history");
        historyDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for(DataSnapshot history : dataSnapshot.getChildren() ){
                        fetchRideInformation(history.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void fetchRideInformation(String rideKey) {
        DatabaseReference historyDataRef = FirebaseDatabase.getInstance().getReference("History").child(rideKey);
        historyDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                   String rideId = dataSnapshot.getKey();
                   Long timestamp = 0L;

                   for(DataSnapshot child : dataSnapshot.getChildren()){
                       if (child.getKey().equals("timestamp")){
                           timestamp = Long.valueOf(child.getValue().toString());
                       }
                   }

                   HistoryObject object = new HistoryObject(rideId,getDate(timestamp));
                   resultDataList.add(object);
                   mProgressBar.setVisibility(View.GONE);
                   adapter.notifyDataSetChanged();
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

    private ArrayList<HistoryObject> resultDataList = new ArrayList<>();
    private ArrayList<HistoryObject> getDataSet() {
            return resultDataList;
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver,new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(receiver);
        }catch (Exception e){
            Log.i("Register  exception ",e+"");
        }

    }
}
