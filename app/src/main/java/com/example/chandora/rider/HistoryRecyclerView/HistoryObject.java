package com.example.chandora.rider.HistoryRecyclerView;

/**
 * Created by chandora on 26/1/18.
 */

public class HistoryObject {
    private String rideId;

    public HistoryObject(String rideId){
        this.rideId = rideId;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }
}
