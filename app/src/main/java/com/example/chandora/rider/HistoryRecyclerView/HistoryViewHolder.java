package com.example.chandora.rider.HistoryRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.chandora.rider.HistorySingleActivity;
import com.example.chandora.rider.R;


/**
 * Created by chandora on 26/1/18.
 */

public class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public TextView mRideId,mTime;

    public HistoryViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        mRideId = itemView.findViewById(R.id.ride_id);
        mTime = itemView.findViewById(R.id.ride_time);

    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), HistorySingleActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("rideId",mRideId.getText().toString());
        intent.putExtras(bundle);
        view.getContext().startActivity(intent);

    }
}
