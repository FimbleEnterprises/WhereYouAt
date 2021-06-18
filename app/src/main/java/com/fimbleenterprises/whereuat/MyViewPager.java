package com.fimbleenterprises.whereuat;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.fimbleenterprises.whereuat.helpers.MySettingsHelper;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;


/**
 * Created by mweber on 12/2/2015.
 */
public class MyViewPager extends ViewPager {

    public interface MyPageChangedListener extends OnPageChangeListener {
        void onPageChanged(@Nullable Intent intent);
    }

    private final static String TAG = "MyViewPager";
    private boolean isPagingEnabled = true;
    private MySettingsHelper options;
    private Context context;
    private MyPageChangedListener mPageChangedListener;
    public int currentPosition;
    private int pageCount = -1;
    private Intent pendingIntent;
    OnRealPageChangedListener onRealPageChangedListener;

    public interface OnRealPageChangedListener {
        void onPageActuallyFuckingChanged(int pageIndex);
    }

    public MyViewPager(Context context, MyPageChangedListener mPageChangedListener) {
        super(context);
        this.context = context;
        this.mPageChangedListener = mPageChangedListener;
        options = new MySettingsHelper();
    }

    public MyViewPager(Context context, MyPageChangedListener mPageChangedListener, int pageCount) {
        super(context);
        this.context = context;
        this.pageCount = pageCount;
        this.mPageChangedListener = mPageChangedListener;
        options = new MySettingsHelper();
    }

    public MyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        options = new MySettingsHelper();
    }

    public void addOnPageChangeChangedListener(MyPageChangedListener listener) {
        this.mPageChangedListener = listener;
    }

    @Override
    public void addOnPageChangeListener(OnPageChangeListener listener) {
        Log.w(TAG, "addOnPageChangeListener: Page is: " + getCurrentItem());
        super.addOnPageChangeListener(listener);
    }

    public void setPageCount(int count) {
        this.pageCount = count;
    }

    public int getPageCount() {
        return pageCount;
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout with its current adapter there will be a smooth animated transition between
     * the current item and the specified item.
     *
     * @param item Item index to select
     */
    @Override
    public void setCurrentItem(int item) {

        if (!MyApp.isReportingLocation()) {
            Log.w(TAG, " !!!!!!! -= setCurrentItem | CANNOT SCROLL FROM 0 - TRIP NOT RUNNING! =- !!!!!!!");
            super.setCurrentItem(0);
            return;
        }

        Log.d(TAG, "Page is being set to: " + item);
        currentPosition = item;
        super.setCurrentItem(item);

    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout with its current adapter there will be a smooth animated transition between
     * the current item and the specified item.
     *
     * @param item Item index to select
     */
    public void setCurrentItem(int item, Intent intent) {

        if (!MyApp.isReportingLocation()) {
            Log.w(TAG, " !!!!!!! -= setCurrentItem | CANNOT SCROLL FROM 0 - TRIP NOT RUNNING! =- !!!!!!!");
            super.setCurrentItem(0);
            return;
        }

        Log.d(TAG, "Page is being set to: " + item);
        currentPosition = item;
        pendingIntent = intent;
        mPageChangedListener.onPageChanged(pendingIntent);
        super.setCurrentItem(item);
    }

    @Override
    public int getCurrentItem() {
        Log.w(TAG, "getCurrentItem: GET CURRENT ITEM CALLED! (" + super.getCurrentItem() + ")");

        return super.getCurrentItem();
    }

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);
        currentPosition = position;
        if (onRealPageChangedListener != null) {
            onRealPageChangedListener.onPageActuallyFuckingChanged(currentPosition);
        }
    }

    /**
     * Set the currently selected page. `
     *
     * @param item         Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        Log.w(TAG, "setCurrentItem: CURRENT ITEM: " + item);

        if (item < 0) {
            Log.w(TAG, "setCurrentItem: Can't set pager position less than zero.");
            return;
        }

        if (this.pageCount != -1 && item == this.pageCount) {
            Log.w(TAG, "setCurrentItem: Can't set pager position higher than the page count.");
            return;
        }

        this.currentPosition = item;
        // options.setLastPage(item);
        super.setCurrentItem(item, smoothScroll);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Log.d(TAG, "MyViewPager received a touch event.  Paging enabled: " + this.isPagingEnabled);
        return this.isPagingEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        //Log.d(TAG, "MyViewPager intercepted a touch event.  Paging enabled: " + this.isPagingEnabled);
        return this.isPagingEnabled && super.onInterceptTouchEvent(event);
    }

    public void setPagingEnabled(boolean b) {
        this.isPagingEnabled = b;
        //Log.d(TAG, "Enable paging: " + this.isPagingEnabled);
    }



    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        return false;
        // return super.canScroll(v, checkV, dx, x, y);
    }

}
