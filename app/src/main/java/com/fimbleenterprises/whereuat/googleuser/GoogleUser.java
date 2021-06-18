package com.fimbleenterprises.whereuat.googleuser;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.fimbleenterprises.whereuat.helpers.MySettingsHelper;
import com.fimbleenterprises.whereuat.helpers.StaticHelpers;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.gson.Gson;

public class GoogleUser implements Parcelable {

    public String email;
    public String id;
    public String photourl;
    public String fullname;
    private Bitmap avatar;

    /**
     * Blank constructor.
     */
    public GoogleUser() { }

    /**
     * Build a GoogleUser from Google's sign in server response.
     * @param signInAccount
     */
    public GoogleUser(GoogleSignInAccount signInAccount) {
        this.email = signInAccount.getEmail();
        this.id = signInAccount.getId();
        if (signInAccount.getPhotoUrl() != null) {
            this.photourl = signInAccount.getPhotoUrl().toString();
        }
        this.fullname = signInAccount.getDisplayName();
    }

    /**
     * Rebuild from json
     * @param json
     */
    public GoogleUser(String json) {
        Gson gson = new Gson();
        GoogleUser user = gson.fromJson(json, this.getClass());
        this.email = user.email;
        this.photourl = user.photourl;
        this.fullname = user.fullname;
        this.id = user.id;
    }

    /**
     * Serializes this object to json.
     * @return
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    /**
     * Gets this user's GoogleUser object from shared prefs if it was cached (and it likely has been).
     * @return
     */
    public static GoogleUser getCachedUser() {
        return new MySettingsHelper().getCachedGoogleUser();
    }

    public boolean hasAvatar() {
        return this.avatar != null;
    }

    public Bitmap getAvatar() {
        return this.avatar;
    }

    public void setAvatar(Bitmap bitmap) {
        this.avatar = bitmap;
    }

    public void getAvatar(final StaticHelpers.Bitmaps.GetImageFromUrlListener listener) {

        if (this.avatar != null) {
            listener.onSuccess(this.avatar);
        } else {
            StaticHelpers.Bitmaps.getFromUrl(this.photourl, listener);
        }
    }

    @Override
    public String toString() {
        return this.fullname + ", " + this.id;
    }


    protected GoogleUser(Parcel in) {
        email = in.readString();
        id = in.readString();
        photourl = in.readString();
        fullname = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(email);
        dest.writeString(id);
        dest.writeString(photourl);
        dest.writeString(fullname);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<GoogleUser> CREATOR = new Parcelable.Creator<GoogleUser>() {
        @Override
        public GoogleUser createFromParcel(Parcel in) {
            return new GoogleUser(in);
        }

        @Override
        public GoogleUser[] newArray(int size) {
            return new GoogleUser[size];
        }
    };
}
