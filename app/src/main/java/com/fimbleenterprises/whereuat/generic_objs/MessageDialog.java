package com.fimbleenterprises.whereuat.generic_objs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.R;
import com.fimbleenterprises.whereuat.googleuser.GoogleUser;
import com.fimbleenterprises.whereuat.rest_api.Requests;
import com.fimbleenterprises.whereuat.rest_api.WebApi;

public class MessageDialog {

    public interface MessageSubmitResultListener {
        void onSuccess();
        void onFailure(String msg);
    }

    private static final String TAG = "MessageDialog";
    private Dialog dialog;
    private Context context;
    private MessageSubmitResultListener messageSubmitResultListener;

    public MessageDialog(final Context ctx, final MessageSubmitResultListener listener) {
        this.dialog = new Dialog(ctx);
        this.messageSubmitResultListener = listener;
        this.context = ctx;
        this.dialog.setContentView(R.layout.dialog_send_message);
        this.dialog.setTitle("Send a message to the group");
        this.dialog.setCancelable(true);
        this.dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    return true;
                } else {
                    return false;
                }
            }
        });
        Button btnSend = this.dialog.findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText txtMsg = dialog.findViewById(R.id.editTextMessageBody);
                String msg = txtMsg.getText().toString();

                if (msg == null || msg.length() == 0) {
                    Toast.makeText(ctx, "You must type something!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Dismiss the dialog and show a toast indicating the msg is being sent.
                dialog.dismiss();
                Toast.makeText(ctx, "Sending message...", Toast.LENGTH_SHORT).show();

                // Submit the message to our server-side api using our local api engine.
                WebApi api = new WebApi(ctx);
                Requests.Request request = new Requests.Request(Requests.Request.Function.SEND_MESSAGE);
                request.arguments.add(new Requests.Arguments.Argument("sender", GoogleUser.getCachedUser().id));
                request.arguments.add(new Requests.Arguments.Argument("body", msg));
                request.arguments.add(new Requests.Arguments.Argument("tripcode", MyApp.getCurrentTripcode()));
                api.makeRequest(request, new WebApi.WebApiResultListener() {
                    @Override
                    public void onSuccess(WebApi.OperationResults results) {
                        if (results.list.get(0).wasSuccessful) {
                            listener.onSuccess();
                        }
                    }

                    @Override
                    public void onFailure(String message) {
                        Log.w(TAG, " !!!!!!! -= onFailure | " + message + " =- !!!!!!!");
                        listener.onFailure(message);
                    }
                });
            }
        });
    }

    public void show() {
        dialog.show();
    }



}
