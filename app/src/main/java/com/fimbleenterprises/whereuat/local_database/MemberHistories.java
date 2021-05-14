package com.fimbleenterprises.whereuat.local_database;

import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;

/**
 * A container of containers.  See the description for the MemberHistory object for an explanation
 * of what this class is used for.
 */
public class MemberHistories {
   public ArrayList<MemberHistory> list = new ArrayList<>();

    /**
     * Simply checks if this user has any history entries
     * @param userid The user to look for
     * @return bool
     */
    MemberHistory getMemberHistory(String userid) {
        for (MemberHistory history : this.list) {
            if (history.memberid.equals(userid)) {
                return history;
            }
        }
        return null;
    }

    /**
     * Adds a member history entry using an individual MemberUpdate object.  If the user's id is not
     * found in the list of MemberHistory objects a new one is created and will be appended henceforth.
     * @param memberUpdate A valid MemberUpdate object that will be used to create a new MemberHistory
     *                     object or append an existing one with this location.
     */
    public void add(TripReport.MemberUpdate memberUpdate) {

        MemberHistory history = getMemberHistory(memberUpdate.userid);

        if (history == null) {
            this.list.add(new MemberHistory(memberUpdate));
        } else {
            history.entries.add(memberUpdate);
        }
    }

    /**
     * An object designed to show a user's location history throughout a trip.  This object has three
     * properties: <br/><br/>
     * 1. memberid - The user's unique Google id<br/>
     * 2. entries - An ArrayList of this user's MemberUpdate objects<br/>
     * 3. polyline - An optional reference to a polyline on a Google map representing this user's location history.<br/><br/>
     */
    public static class MemberHistory {
        public String memberid;
        public ArrayList<TripReport.MemberUpdate> entries = new ArrayList<>();
        public Polyline polyline;

        /**
         * Creates a new MemberHistory object using a MemberUpdate object.
         * @param memberUpdate
         */
        public MemberHistory(TripReport.MemberUpdate memberUpdate) {
            this.memberid = memberUpdate.userid;
            this.entries.add(memberUpdate);
        }

    }
}


