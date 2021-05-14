package com.fimbleenterprises.whereuat.generic_objs;

import com.fimbleenterprises.whereuat.R;
import com.google.gson.Gson;

import java.util.ArrayList;

public class BasicObjects {

    public ArrayList<BasicObject> list = new ArrayList<>();

    public BasicObjects(){}

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static class BasicObject {
        public String title;
        public String subtitle;
        public Object obj;
        public boolean isSeparator = false;
        public int drawableRef = R.drawable.lead_icon2;

        public BasicObject() {}

        public BasicObject(String gsonstring) {
            Gson gson = new Gson();
            BasicObject object = gson.fromJson(gsonstring, this.getClass());
            this.title = object.title;
            this.subtitle = object.subtitle;
            this.obj = object.obj;
        }

        public String toGson() {
            return new Gson().toJson(this);
        }

    }

}
