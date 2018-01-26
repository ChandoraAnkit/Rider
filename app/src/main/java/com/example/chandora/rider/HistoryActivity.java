package com.example.chandora.rider;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.chandora.rider.HistoryRecyclerView.HistoryAdapter;
import com.example.chandora.rider.HistoryRecyclerView.HistoryObject;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        mRecyclerView = findViewById(R.id.recycler_view_history);

        mRecyclerView.setNestedScrollingEnabled(true);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(this,getDataSet());
        mRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    private ArrayList<HistoryObject> resultDataList = new ArrayList<>();
    private ArrayList<HistoryObject> getDataSet() {
            return resultDataList;
    }
}
