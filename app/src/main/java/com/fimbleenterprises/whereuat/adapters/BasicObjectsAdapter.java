package com.fimbleenterprises.whereuat.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fimbleenterprises.whereuat.R;
import com.fimbleenterprises.whereuat.generic_objs.BasicObjects;
import com.fimbleenterprises.whereuat.preferences.MySettingsHelper;

import androidx.recyclerview.widget.RecyclerView;

public class BasicObjectsAdapter extends RecyclerView.Adapter<BasicObjectsAdapter.ViewHolder> {
    private static final String TAG="TripListRecyclerAdapter";
    public BasicObjects mData;
    private LayoutInflater mInflater;
    private OnBasicObjectClickListener mClickListener;
    private OnBasicObjectLongClickListener mLongClickListener;
    MySettingsHelper options;
    public boolean isInEditMode = false;
    Context context;
    Typeface originalTypeface;
    TextView tvMainText;
    TextView tvSubtext;
    ImageView imgLeftIcon;

    public interface OnBasicObjectClickListener {
        void onBasicObjectItemClicked(BasicObjects.BasicObject basicObject);
    }

    public interface OnBasicObjectLongClickListener {
        void onBasicItemLongClicked(BasicObjects.BasicObject basicObject);
    }

    // data is passed into the constructor
    public BasicObjectsAdapter(Context context, BasicObjects data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.context = context;
        this.options = new MySettingsHelper();
    }

    // data is passed into the constructor
    public BasicObjectsAdapter(Context context, BasicObjects data, OnBasicObjectClickListener clickListener) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.context = context;
        this.options = new MySettingsHelper();
        this.mClickListener = clickListener;
    }

    // data is passed into the constructor
    public BasicObjectsAdapter(Context context, BasicObjects data, OnBasicObjectClickListener clickListener,
                                OnBasicObjectLongClickListener longClickListener) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.context = context;
        this.options = new MySettingsHelper();
        this.mClickListener = clickListener;
        this.mLongClickListener = longClickListener;
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
        final BasicObjects.BasicObject object = mData.list.get(position);

        if (object.isSeparator) {
            holder.txtMainText.setText(object.title);
            holder.txtMainText.setTypeface(originalTypeface, Typeface.BOLD);
        } else {
            holder.txtMainText.setTypeface(originalTypeface);
            holder.imgLeftIcon.setVisibility(View.VISIBLE);
            holder.imgLeftIcon.setImageResource(object.drawableRef);
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)
                    holder.layout.getLayoutParams();
            layoutParams.bottomMargin = 6;
            layoutParams.topMargin = 6;
            holder.layout.setLayoutParams(layoutParams);
            holder.layout.setBackgroundResource(R.drawable.btn_glass_gray_black_border);
            holder.txtMainText.setText(object.title);
            holder.txtSubtext.setText(object.subtitle);
           // holder.chkbxSelectTrip.setVisibility((isInEditMode) ? View.VISIBLE : View.INVISIBLE);
        }

        holder.itemView.setLongClickable(true);

    }

    // convenience method for getting data at click position
    public BasicObjects.BasicObject getItem(int pos) {
        return mData.list.get(pos);
    }

    // allows clicks events to be caught
    public void setOnBasicItemClickListener(OnBasicObjectClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public void setOnBasicObjectItemLongClickListener(OnBasicObjectLongClickListener itemLongClickListener) {
        this.mLongClickListener = itemLongClickListener;
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.list.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
                                                                       View.OnLongClickListener {
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

            BasicObjects.BasicObject clickedItem = mData.list.get(getAdapterPosition());

            if (clickedItem.isSeparator) {
                return;
            }

            if (mClickListener != null) mClickListener.onBasicObjectItemClicked(clickedItem);

        }

        @Override
        public boolean onLongClick(View view) {
            BasicObjects.BasicObject clickedItem = mData.list.get(getAdapterPosition());
            if (clickedItem.isSeparator) {
                return true;
            }
            if (mLongClickListener != null) mLongClickListener.onBasicItemLongClicked(clickedItem);
            return true;
        }
    }
}
