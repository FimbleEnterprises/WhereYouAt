package com.fimbleenterprises.whereuat.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fimbleenterprises.whereuat.local_database.TripReport;
import com.fimbleenterprises.whereuat.preferences.MySettingsHelper;
import com.fimbleenterprises.whereuat.R;

import androidx.recyclerview.widget.RecyclerView;

public class MemberListAdapter extends RecyclerView.Adapter<MemberListAdapter.ViewHolder> {
    private static final String TAG="TripListRecyclerAdapter";
    public TripReport mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    MySettingsHelper options;
    public boolean isInEditMode = false;
    Context context;
    Typeface originalTypeface;
    TextView tvMainText;
    TextView tvSubtext;
    ImageView imgLeftIcon;

    // data is passed into the constructor
    public MemberListAdapter(Context context, TripReport data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
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
        originalTypeface = tvMainText.getTypeface();
        return new ViewHolder(view);
    }


    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final TripReport.MemberUpdate member = mData.list.get(position);

        if (member.isSeparator) {
            holder.txtMainText.setText(member.displayName);
            holder.txtMainText.setTypeface(originalTypeface, Typeface.BOLD);
        } else {
            holder.txtMainText.setTypeface(originalTypeface);
            holder.imgLeftIcon.setVisibility(View.VISIBLE);
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)
                    holder.layout.getLayoutParams();
            layoutParams.bottomMargin = 6;
            layoutParams.topMargin = 6;
            holder.layout.setLayoutParams(layoutParams);
            holder.layout.setBackgroundResource(R.drawable.btn_glass_gray_black_border);
            holder.txtMainText.setText(member.displayName);
            holder.txtSubtext.setText(member.email);
           // holder.chkbxSelectTrip.setVisibility((isInEditMode) ? View.VISIBLE : View.INVISIBLE);
        }

        holder.itemView.setLongClickable(true);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.list.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView imgLeftIcon;
        TextView txtMainText;
        TextView txtSubtext;
        RelativeLayout layout;


        ViewHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.row);
            txtMainText = itemView.findViewById(R.id.txtMainText);
            txtSubtext = itemView.findViewById(R.id.txtSubtext);
            imgLeftIcon = itemView.findViewById(R.id.imgLeftIcon);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {

            if (mData.list.get(getAdapterPosition()).isSeparator) {
                return;
            }

            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());

        }

        @Override
        public boolean onLongClick(View view) {

            return true;
        }
    }

    // convenience method for getting data at click position
    public TripReport.MemberUpdate getItem(int pos) {
        return mData.list.get(pos);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
