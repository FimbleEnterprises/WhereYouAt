package com.fimbleenterprises.whereuat.generic_objs;

import android.os.Parcel;
import android.os.Parcelable;

import com.fimbleenterprises.whereuat.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * A container class for packaging array lists of objects destined for a listview.
 */
public class ListObjects implements Parcelable {

    public String title;
    public ArrayList<ListObject> list = new ArrayList<>();

    public ListObjects(){}


    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * Downgrades the existing ArrayList of BasicObjects to a regular Java.Util.List
     * @return A java.util.list object.
     */
    public List<ListObject> toList() {
        List<ListObject> newList = new ArrayList<>();
        for (ListObject listObject : this.list) {
            newList.add(listObject);
        }
        return newList;
    }

    /**
     * A versatile though basic container used for displaying objects in a listview.
     */
    public static class ListObject implements Comparable<ListObject>, Parcelable {
        public String title;
        public String subtitle;
        public Object obj;
        public boolean isSeparator = false;
        public int drawableRef = R.drawable.lead_icon2;

        public ListObject() {}

        public ListObject(String gsonstring) {
            Gson gson = new Gson();
            ListObject object = gson.fromJson(gsonstring, this.getClass());
            this.title = object.title;
            this.subtitle = object.subtitle;
            this.obj = object.obj;
        }

        protected ListObject(Parcel in) {
            title = in.readString();
            subtitle = in.readString();
            isSeparator = in.readByte() != 0;
            drawableRef = in.readInt();
        }

        public static final Creator<ListObject> CREATOR = new Creator<ListObject>() {
            @Override
            public ListObject createFromParcel(Parcel in) {
                return new ListObject(in);
            }

            @Override
            public ListObject[] newArray(int size) {
                return new ListObject[size];
            }
        };

        public String toGson() {
            return new Gson().toJson(this);
        }

        @Override
        public int compareTo(ListObject listObject) {
            int lastCmp = title.compareTo(listObject.title);

            if (lastCmp != 0) {
                return lastCmp;
            } else {
                return this.title.compareTo(listObject.title);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(title);
            parcel.writeString(subtitle);
            parcel.writeByte((byte) (isSeparator ? 1 : 0));
            parcel.writeInt(drawableRef);
        }
    }



    protected ListObjects(Parcel in) {
        if (in.readByte() == 0x01) {
            list = new ArrayList<ListObject>();
            in.readList(list, ListObject.class.getClassLoader());
        } else {
            list = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (list == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(list);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ListObjects> CREATOR = new Parcelable.Creator<ListObjects>() {
        @Override
        public ListObjects createFromParcel(Parcel in) {
            return new ListObjects(in);
        }

        @Override
        public ListObjects[] newArray(int size) {
            return new ListObjects[size];
        }
    };
}
