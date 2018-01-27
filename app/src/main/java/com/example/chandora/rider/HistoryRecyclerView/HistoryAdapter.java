package com.example.chandora.rider.HistoryRecyclerView;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.chandora.rider.R;

import java.util.ArrayList;

/**
 * Created by chandora on 26/1/18.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {
    private ArrayList<HistoryObject> itemsList;
    private Context context;

    public HistoryAdapter(Context context,ArrayList<HistoryObject> itemsList){
        this.context = context;
        this.itemsList = itemsList;

    }
    @Override
    public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(context).inflate(R.layout.item_history,null,false);
        HistoryViewHolder itemView = new HistoryViewHolder(view);
        return itemView;
    }

    @Override
    public void onBindViewHolder(HistoryViewHolder holder, int position) {
        holder.mRideId.setText(itemsList.get(position).getRideId());
        holder.mTime.setText(itemsList.get(position).getTime());

    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }
}
