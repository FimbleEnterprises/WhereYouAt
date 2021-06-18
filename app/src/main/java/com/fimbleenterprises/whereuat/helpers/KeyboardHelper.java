package com.fimbleenterprises.whereuat.helpers;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/*
 *
 * Adapted from khanhamza:
 * https://stackoverflow.com/a/45572278/2097893
 *
 */

/**
 * Helper class for showing/hiding the onscreen keyboard.
 */
public class KeyboardHelper {

    private Context context;
    public int showDelay = 250;
    public int hideDelay = 50;

    /**
     * Constructor for keyboard helper.
     * @param context A valid context that can call the IMM
     */
    public KeyboardHelper(Context context) {
        this.context = context;

    }

    /**
     * Hides the onscreen keyboard
     * @param editText The edit text that needed the keyboard's output.
     */
    public void hideSoftKeyboard(final EditText editText) {
        editText.requestFocus();
        editText.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
        }, hideDelay);
    }


    /**
     * Shows the onscreen keybaord.
     * @param editText The edit text that will receive the keyboard's output.
     */
    public void showSoftKeyboard(final EditText editText) {
        editText.requestFocus();
        editText.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, showDelay);
    }
}
