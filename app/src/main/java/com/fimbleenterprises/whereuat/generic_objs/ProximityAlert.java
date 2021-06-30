package com.fimbleenterprises.whereuat.generic_objs;

import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.local_database.TripDatasource;
import com.google.gson.Gson;

import java.util.ArrayList;

public class ProximityAlert {

    public int id;
    public boolean isActive = false;
    public GoogleUser trackedUser;
    public float proximityInMeters;
    public boolean hasFired = false;
    public boolean wasAcknowledged = false;
    public String tripcode;

    public ProximityAlert() { }

    public ProximityAlert(String gson) {
        ProximityAlert alert = new Gson().fromJson(gson, this.getClass());
        this.id = alert.id;
        this.isActive = alert.isActive;
        this.trackedUser = alert.trackedUser;
        this.proximityInMeters = alert.proximityInMeters;
        this.hasFired = alert.hasFired;
        this.wasAcknowledged = alert.wasAcknowledged;
        this.tripcode = alert.tripcode;
    }

    public ProximityAlert(String tripcode, GoogleUser user,  float proximityInMeters) {
        this.trackedUser = user;
        this.tripcode = tripcode;
        this.proximityInMeters = proximityInMeters;
    }

    public void save() {
        new TripDatasource().saveAlert(this);
    }

    public void delete() {
        new TripDatasource().deleteAlert(this.id);
    }

    public String toGson() {
        return new Gson().toJson(this);
    }

}
