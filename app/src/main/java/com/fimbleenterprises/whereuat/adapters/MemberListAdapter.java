package com.fimbleenterprises.whereuat.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fimbleenterprises.whereuat.helpers.StaticHelpers;
import com.fimbleenterprises.whereuat.local_database.TripReport;
import com.fimbleenterprises.whereuat.helpers.MySettingsHelper;
import com.fimbleenterprises.whereuat.R;

import java.util.ArrayList;
import java.util.logging.LogRecord;

import androidx.recyclerview.widget.RecyclerView;

public class MemberListAdapter extends RecyclerView.Adapter<MemberListAdapter.ViewHolder> {

    public interface OnMemberClickListener {
        void onClick(TripReport.MemberUpdate memberUpdate);
        void onSubrowClick(TripReport.MemberUpdate memberUpdate);
    }

    public interface OnMemberLongClickListener {
        void onLongClick(TripReport.MemberUpdate memberUpdate);
    }

    public interface OnSendMessageClickListener {
        void onClick(TripReport.MemberUpdate memberUpdate);
    }

    public interface OnNavigateToUserClickListener {
        public void onClick(TripReport.MemberUpdate clickedMember);
    }

    static class ItemContainer {
        TripReport.MemberUpdate memberUpdate;
        boolean isExpanded = false;
        Bitmap avatar;

        public ItemContainer(TripReport.MemberUpdate memberUpdate) {
            this.memberUpdate = memberUpdate;
        }

        public TripReport.MemberUpdate toMemberUpdate() {
            return this.memberUpdate;
        }
    }

    private static final String TAG="TripListRecyclerAdapter";

    public ArrayList<ItemContainer> mData = new ArrayList<>();

    private LayoutInflater mInflater;
    private OnMemberClickListener mClickListener;
    private OnMemberLongClickListener mLongClickListener;
    private OnSendMessageClickListener mSendMessageClickListener;
    private OnNavigateToUserClickListener mNavigateToUserClickListener;

    MySettingsHelper options;
    public boolean isInEditMode = false;
    Context context;
    Typeface originalTypeface;
    TextView tvMainText;
    TextView tvSubtext;
    ImageView imgCaret;
    ImageView imgLeftIcon;

    // data is passed into the constructor
    public MemberListAdapter(Context context, TripReport data) {
        this.mInflater = LayoutInflater.from(context);
        for (TripReport.MemberUpdate memberUpdate : data.list) {
            this.mData.add(new ItemContainer(memberUpdate));
        }
        this.context = context;
        this.options = new MySettingsHelper();
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.rv_member_row, parent, false);
        imgLeftIcon = view.findViewById(R.id.imgLeftIcon);
        tvMainText = view.findViewById(R.id.txtMainText);
        tvSubtext = view.findViewById(R.id.txtSubtext);
        imgCaret = view.findViewById(R.id.imgCaret);
        originalTypeface = tvMainText.getTypeface();

        return new ViewHolder(view);
    }

    /**
     * Called from the activity/frag that is hosting the adapter and is used to highlight a specific
     * user in the recyclerview.
     * @param selectedUserid The Google userid of the user to highlight.
     */
    public void highlightUser(String selectedUserid) {

        // First check if the current user is already selected and collapse them if so
        for (int i = 0; i < mData.size(); i++) {

            // Get the item at this position
            ItemContainer item = mData.get(i);

            if (item.memberUpdate.userid.equals(selectedUserid) && item.isExpanded) {
                item.isExpanded = false;

                // Leave and do no more.
                return;
            }
        }

        // Current user was not selected or was not currently expanded
        for (int i = 0; i < mData.size(); i++) {

            // Get the item at this position
            ItemContainer item = mData.get(i);

            // Collapse this item
            item.isExpanded = false;

            // If the item matches the supplied id
            if (item.memberUpdate.userid.equals(selectedUserid)) {
                item.isExpanded = true;
            }
        }

        // Notify the recycler that we have made a change (collapsing all then expanding
        // the selected user).
        this.notifyDataSetChanged();
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final TripReport.MemberUpdate member = mData.get(position).memberUpdate;
        final ItemContainer memberContainer = mData.get(position);

        holder.memberUpdateForThisRow = member;

        // If this entry was created simply to act as an aesthetic separator then hide any value views
        if (member.isSeparator) {
            holder.txtMainText.setText(member.displayName);
            holder.txtMainText.setTypeface(originalTypeface, Typeface.BOLD);

        } else { // Entry is not a separator so populate its views with pertinent values!

            // Set font
            holder.txtMainText.setTypeface(originalTypeface);

            // Show avatar image view
            holder.imgLeftIcon.setVisibility(View.VISIBLE);

            // Get the layout params so we can set things like padding/margins
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)
                    holder.layout.getLayoutParams();

            // Adjust margins
            layoutParams.bottomMargin = 6;
            layoutParams.topMargin = 6;

            // Apply the layout params
            holder.layout.setLayoutParams(layoutParams);


            // Start setting actual values
            holder.txtMainText.setText(member.displayName);
            holder.txtSubtext.setText(member.getDistanceInRelevantUnits() + "\n" + member.getUpdatedLastInRelevantUnits());
            holder.txtDistance.setText(member.getDistanceInRelevantUnits());
            holder.txtLastReported.setText(member.getUpdatedLastInRelevantUnits());
            holder.txtLocationType.setText(member.locationtype);
            holder.txtAccuracy.setText("+/- " + member.getAccuracy() + " meters");
            holder.txtSpeed.setText(member.getSpeedInMilesPerHour() + " mph");

            // If this entry is "expanded" then show the table with the more esoteric values, otherwise hide/collapse it.
            if (mData.get(position).isExpanded) {
                holder.subRow.setVisibility(View.VISIBLE);
                holder.imgCaret.setImageResource(R.drawable.about);
                holder.itemView.setBackgroundColor(Color.parseColor("#44FF9249"));
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                holder.subRow.setVisibility(View.GONE);
                holder.imgCaret.setImageResource(R.drawable.arrow_right);
            }

            // See if the user has an image.
            if (memberContainer.avatar == null && member.photoUrl != null) {
                Log.i(TAG, "onBindViewHolder | This member does not have an avatar - will retrieve it...");
                getAvatar(mData.get(position));
                memberContainer.avatar = StaticHelpers.Bitmaps.getBitmapFromResource(context, R.drawable.car2);
            } else if (memberContainer.avatar == null && member.photoUrl == null) {
                memberContainer.avatar = StaticHelpers.Bitmaps.getBitmapFromResource(context, R.drawable.car2);
                Log.i(TAG, "onBindViewHolder | This member has no associated avatar url - will use stock.");
            } else {
                Log.i(TAG, "onBindViewHolder ");
            }

            // Set the avatar (either the user's Google image or a stock image)
            holder.avatar = memberContainer.avatar;
            holder.imgLeftIcon.setImageBitmap(holder.avatar);

            holder.subRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mClickListener.onSubrowClick(member);
                }
            });

            holder.btnSendMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSendMessageClickListener.onClick(member);
                }
            });

            holder.btnNavigateTo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mNavigateToUserClickListener.onClick(member);
                }
            });

        }

        holder.itemView.setLongClickable(true);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void updateAdapterData(TripReport report) {

        if (mData == null || mData.size() == 0) {
            Log.w(TAG, "updateAdapterData: | NO DATA TO UPDATE!");
            return;
        }

        if (report.list.size() != mData.size()) {
            mData.clear();
            for (TripReport.MemberUpdate memberUpdate : report.list) {
                this.mData.add(new ItemContainer(memberUpdate));
            }
        }

        for (TripReport.MemberUpdate memberUpdate : report.list) {
            for (ItemContainer container : this.mData) {
                if (container.memberUpdate.userid.equals(memberUpdate.userid)) {
                    container.memberUpdate = memberUpdate;
                    Log.i(TAG, "updateAdapterData | Updated " + container.memberUpdate.displayName);
                }
            }
        }

    }

    private void getAvatar(final ItemContainer container) {
        StaticHelpers.Bitmaps.getFromUrl(container.memberUpdate.photoUrl, new StaticHelpers.Bitmaps.GetImageFromUrlListener() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                container.avatar = bitmap;
                container.memberUpdate.avatar = bitmap;
                notifyDataSetChanged();
            }

            @Override
            public void onFailure(String msg) {
                Bitmap defaultBitmap = StaticHelpers.Bitmaps.getBitmapFromResource(context, R.drawable.car2);
                container.avatar = defaultBitmap;
                container.memberUpdate.avatar = defaultBitmap;
                notifyDataSetChanged();
            }
        });
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        TripReport.MemberUpdate memberUpdateForThisRow;

        // ************************************  Main views **************************************
        RelativeLayout layout; // The main container holding all the row's views.
        ImageView imgLeftIcon;
        TextView txtMainText;
        TextView txtSubtext;
        ImageView imgCaret;
        Bitmap avatar; // Holds a bitmap constructed from the user's photoUrl

        // ********************************  Expanded row views  *********************************
        RelativeLayout subRow; // The container for additional member trip metrics
        TextView txtDistance;
        TextView txtLastReported;
        TextView txtLocationType;
        TextView txtAccuracy;
        TextView txtSpeed;
        Button btnSendMsg;
        Button btnNavigateTo;

        ViewHolder(View itemView) {

            // Locate all of the row's views and make references to them.
            super(itemView);
            layout = itemView.findViewById(R.id.row);
            txtMainText = itemView.findViewById(R.id.txtMainText);
            txtSubtext = itemView.findViewById(R.id.txtSubtext);
            imgLeftIcon = itemView.findViewById(R.id.imgLeftIcon);
            subRow = itemView.findViewById(R.id.subRow);
            txtDistance = itemView.findViewById(R.id.txtDistanceValue);
            txtLastReported = itemView.findViewById(R.id.txtLastReportedValue);
            txtLocationType = itemView.findViewById(R.id.txtLocationTypeValue);
            txtAccuracy = itemView.findViewById(R.id.txtAccuracyValue);
            txtSpeed = itemView.findViewById(R.id.txtSpeedValue);
            imgCaret = itemView.findViewById(R.id.imgCaret);
            btnSendMsg = itemView.findViewById(R.id.btnMessageMember);
            btnNavigateTo = itemView.findViewById(R.id.btnNavigateTo);

            // Click listeners that will ultimately percolate down to the calling activity/fragment
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {

            try {

                // If this is a separator we do nuthin.
                if (mData.get(getAdapterPosition()).memberUpdate.isSeparator) { return; }

                // Highlight the selected user
                highlightUser(mData.get(getAdapterPosition()).memberUpdate.userid);

                // Call the onClickListener for the frag/activity hosting the list
                mClickListener.onClick(mData.get(getAdapterPosition()).memberUpdate);

                // Notify the recyclerview that we have made changes and it should redraw
                notifyDataSetChanged();

            } catch (Exception e) { e.printStackTrace(); }

        }

        @Override
        public boolean onLongClick(View view) {

            if (mData.get(getAdapterPosition()).memberUpdate.isSeparator) {
                return true;
            }

            mLongClickListener.onLongClick(mData.get(getAdapterPosition()).memberUpdate);
            return true;

        }

    }

    // convenience method for getting data at click position
    public TripReport.MemberUpdate getItem(int pos) {
        return mData.get(pos).memberUpdate;
    }

    // allows clicks events to be caught
    public void setClickListener(OnMemberClickListener memberClickListener) {
        this.mClickListener = memberClickListener;


    }

    public void setLongClickListener(OnMemberLongClickListener longClickListener) {
        this.mLongClickListener = longClickListener;
    }

    public void setSendMessageClickListener(OnSendMessageClickListener onSendMessageClickListener) {
        this.mSendMessageClickListener = onSendMessageClickListener;
    }

    public void setNavigateToUserClickListener(OnNavigateToUserClickListener onNavigateToUserClickListener) {
        this.mNavigateToUserClickListener = onNavigateToUserClickListener;
    }
}
