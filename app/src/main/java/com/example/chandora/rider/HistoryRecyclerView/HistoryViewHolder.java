package com.example.chandora.rider.HistoryRecyclerView;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.chandora.rider.R;


/**
 * Created by chandora on 26/1/18.
 */

public class HistoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public TextView mRideId;

    public HistoryViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        mRideId = itemView.findViewById(R.id.ride_id);
    }

    @Override
    public void onClick(View view) {

    }
}
