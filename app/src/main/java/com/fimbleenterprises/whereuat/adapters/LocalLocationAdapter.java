package com.fimbleenterprises.whereuat.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fimbleenterprises.whereuat.R;
import com.fimbleenterprises.whereuat.local_database.LocalUserLocation;
import com.fimbleenterprises.whereuat.helpers.MySettingsHelper;

import java.util.ArrayList;

import androidx.recyclerview.widget.RecyclerView;

public class LocalLocationAdapter extends RecyclerView.Adapter<LocalLocationAdapter.ViewHolder> {
    private static final String TAG="TripListRecyclerAdapter";
    public ArrayList<LocalUserLocation> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    MySettingsHelper options;
    Context context;
    TextView tvMainText;

    // data is passed into the constructor
    public LocalLocationAdapter(Context context, ArrayList<LocalUserLocation> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.context = context;
        this.options = new MySettingsHelper();
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.local_location_logger_row, parent, false);
        tvMainText = view.findViewById(R.id.txtMain);

        return new ViewHolder(view);
    }


    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final LocalUserLocation member = mData.get(position);
        holder.txtMainText.setText(member.provider + " - lat: " + member.lat + ", lon: " + member.lon + " Datetime: " + member.getPrettyDate());

        holder.itemView.setLongClickable(true);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView txtMainText;
        RelativeLayout layout;


        ViewHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.row);
            txtMainText = itemView.findViewById(R.id.txtMain);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {

            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());

        }

        @Override
        public boolean onLongClick(View view) {

            return true;
        }
    }

    // convenience method for getting data at click position
    public LocalUserLocation getItem(int pos) {
        return mData.get(pos);
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
