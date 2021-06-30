package com.fimbleenterprises.whereuat.generic_objs;

import android.location.Location;

import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.local_database.TripReport;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

public class MyMapMarkers {

    public ArrayList<MyMapMarker> list = new ArrayList<>();

    public static class MyMapMarker {
        public TripReport.MemberUpdate memberUpdate;
        public Marker mapMarker;

        public void removeFromMap() {
            this.mapMarker.remove();
        }

        /**
         * Calculates the distance, in meters, between two map markers.
         * @param marker The marker to calculate to/from
         * @return The distance in meters.
         */
        public float distanceTo(Marker marker) {
            Location locationA = new Location("LOGICAL");
            locationA.setLatitude(this.mapMarker.getPosition().latitude);
            locationA.setLongitude(this.mapMarker.getPosition().longitude);

            Location locationB = new Location("LOGICAL");
            locationB.setLatitude(marker.getPosition().latitude);
            locationB.setLongitude(marker.getPosition().longitude);

            return locationA.distanceTo(locationB);
        }

        public boolean isMe() {
            return this.memberUpdate.userid.equals(GoogleUser.getCachedUser().id);
        }

        /**
         * Calculates the distance, in meters, between two map markers.
         * @param position The position (LatLng) to calculate to/from
         * @return The distance in meters.
         */
        public float distanceTo(LatLng position) {
            Location locationA = new Location("LOGICAL");
            locationA.setLatitude(this.mapMarker.getPosition().latitude);
            locationA.setLongitude(this.mapMarker.getPosition().longitude);

            Location locationB = new Location("LOGICAL");
            locationB.setLatitude(position.latitude);
            locationB.setLongitude(position.longitude);

            return locationA.distanceTo(locationB);
        }

        /**
         * Calculates the distance, in meters, between two map markers.
         * @param myMapMarker The marker to calculate to/from
         * @return The distance in meters.
         */
        public float distanceTo(MyMapMarker myMapMarker) {
            return distanceTo(myMapMarker.mapMarker);
        }

        public LatLng toLatLng() {
            return mapMarker.getPosition();
        }
    }

    public void add(MyMapMarker mapMarker) {
        this.list.add(mapMarker);
    }

    public MyMapMarker find(String googleuserid) {
        for (MyMapMarker myMarker : this.list) {
            if (myMarker.memberUpdate.userid.equals(googleuserid)) {
                return myMarker;
            }
        }
        return null;
    }

    public MyMapMarker find(Marker marker) {
        for (MyMapMarker myMarker : this.list) {
            if (myMarker.mapMarker.equals(marker)) {
                return myMarker;
            }
        }
        return null;
    }

    public MyMapMarker findMe() {
        for (MyMapMarker myMarker : this.list) {
            if (myMarker.memberUpdate.userid.equals(GoogleUser.getCachedUser().id)) {
                return myMarker;
            }
        }
        return null;
    }
}
