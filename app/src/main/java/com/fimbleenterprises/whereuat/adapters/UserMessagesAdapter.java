package com.fimbleenterprises.whereuat.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fimbleenterprises.whereuat.R;
import com.fimbleenterprises.whereuat.generic_objs.ListObjects;
import com.fimbleenterprises.whereuat.generic_objs.UserMessage;
import com.fimbleenterprises.whereuat.helpers.MySettingsHelper;
import com.fimbleenterprises.whereuat.helpers.StaticHelpers;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import androidx.recyclerview.widget.RecyclerView;

public class UserMessagesAdapter extends RecyclerView.Adapter<UserMessagesAdapter.ViewHolder> {
    private static final String TAG="UserMessageAdapter";
    public ArrayList<UserMessage> mData;
    private LayoutInflater mInflater;
    private OnUserMessageClickListener mClickListener;
    private OnUserMessageLongClickListener mLongClickListener;
    MySettingsHelper options;
    Context context;
    Typeface originalTypeface;
    TextView txtMessage;
    TextView txtName;
    TextView txtDate;
    ImageView imgAvatar;
    private Map<String, Bitmap> cachedAvatars = new HashMap<>();

    public interface OnUserMessageClickListener {
        void onClick(UserMessage message);
    }

    public interface OnUserMessageLongClickListener {
        void onLongClick(UserMessage message);
    }

    // data is passed into the constructor
    public UserMessagesAdapter(Context context, ArrayList<UserMessage> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.context = context;
        this.options = new MySettingsHelper();
    }

    // data is passed into the constructor
    public UserMessagesAdapter(Context context, ArrayList<UserMessage> data, OnUserMessageClickListener clickListener) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.context = context;
        this.options = new MySettingsHelper();
        this.mClickListener = clickListener;
    }

    // data is passed into the constructor
    public UserMessagesAdapter(Context context, ArrayList<UserMessage> data, OnUserMessageClickListener clickListener,
                               OnUserMessageLongClickListener longClickListener) {
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
        View view = mInflater.inflate(R.layout.message_row, parent, false);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        txtMessage = view.findViewById(R.id.txtMsg);
        txtName = view.findViewById(R.id.txtName);
        txtDate = view.findViewById(R.id.txtDate);
        originalTypeface = txtMessage.getTypeface();
        return new ViewHolder(view);
    }


    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final UserMessage message = mData.get(position);

        holder.txtMsg.setTypeface(originalTypeface);
        holder.imgAvatar.setVisibility(View.VISIBLE);


        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)
                holder.layout.getLayoutParams();
        layoutParams.bottomMargin = 6;
        layoutParams.topMargin = 6;
        holder.layout.setLayoutParams(layoutParams);
        holder.layout.setBackgroundResource(R.drawable.btn_glass_gray_black_border);
        holder.txtDate.setText(StaticHelpers.DatesAndTimes.getPrettyDateAndTime(message.getMessageLocalDateTime()));
        holder.txtMsg.setText(message.messageBody);
        holder.txtName.setText(message.sender.fullname);
        if (message.sender.hasAvatar()) {
            holder.imgAvatar.setImageBitmap(message.sender.getAvatar());
        } else {
            message.sender.getAvatar(new StaticHelpers.Bitmaps.GetImageFromUrlListener() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    message.sender.setAvatar(bitmap);
                    notifyDataSetChanged();
                }

                @Override
                public void onFailure(String msg) { }
            });
        }

        holder.itemView.setLongClickable(true);

    }

    // convenience method for getting data at click position
    public UserMessage getItem(int pos) {
        return mData.get(pos);
    }

    // allows clicks events to be caught
    public void setOnBasicItemClickListener(OnUserMessageClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public void setOnBasicObjectItemLongClickListener(OnUserMessageLongClickListener itemLongClickListener) {
        this.mLongClickListener = itemLongClickListener;
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
                                                                       View.OnLongClickListener {
        ImageView imgAvatar;
        TextView txtMsg;
        TextView txtName;
        TextView txtDate;
        RelativeLayout layout;


        ViewHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.row);
            txtMsg = itemView.findViewById(R.id.txtMsg);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtName = itemView.findViewById(R.id.txtName);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {

            UserMessage clickedItem = mData.get(getAdapterPosition());

            if (mClickListener != null) mClickListener.onClick(clickedItem);

        }

        @Override
        public boolean onLongClick(View view) {
            UserMessage clickedItem = mData.get(getAdapterPosition());

            if (mLongClickListener != null) mLongClickListener.onLongClick(clickedItem);
            return true;
        }
    }
}
