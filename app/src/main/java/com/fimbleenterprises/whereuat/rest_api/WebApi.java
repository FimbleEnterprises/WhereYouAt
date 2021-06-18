package com.fimbleenterprises.whereuat.rest_api;

import android.content.Context;
import android.util.Log;

import com.fimbleenterprises.whereuat.MyApp;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class WebApi {

    private static final String TAG = "WebApi";
    private static AsyncHttpClient client = new AsyncHttpClient();
    public static final String BASE_URL = "https://restapi20210128131129.azurewebsites.net/api/values/PerformAction/";
    // public static final String BASE_URL = "https://restapi20210128131129-apim.azure-api.net/api/values/PerformAction/";
    private RequestHandle handle = new RequestHandle(null);
    Context context;

    /**
     * A callback specifically for use with the server api.
     */
    public interface WebApiResultListener {
        void onSuccess(OperationResults results);
        void onFailure(String message);
    }

    /**
     * This is a container for web api server results.  It is effectively an ArrayList containing one or more OperationResult
     * objects and is structured identically to a C# object of the same name on the server.
     */
    public static class OperationResults {

        // Server returned error strings
        public static final String ERROR_TRIP_NOT_FOUND = "ERROR_TRIP_NOT_FOUND";


        /**
         * An ArrayList of OperationResult objects.
         */
        public ArrayList<OperationResult> list = new ArrayList<>();

        /**
         * Constructs a new OperationResults object using the json string returned by the server.
         * @param serverResponse The json directly returned by the server api.
         */
        public OperationResults(String serverResponse) {
            try {
                JSONObject root = new JSONObject(serverResponse);
                JSONArray rootArray = root.getJSONArray("allResults");
                for (int i = 0; i < rootArray.length(); i++) {
                    this.list.add(new OperationResult(rootArray.getJSONObject(i)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "Operation has " + this.list.size() + " results";
        }

        /**
         * A generic object designed to convey operation results when working with the server api
         */
        public static class OperationResult {
            public boolean wasSuccessful;
            public String operationSummary;
            public String result;

            /**
             * Constructs a new OperationResult object using json received directly from the server api.
             * @param json
             */
            public OperationResult(JSONObject json) {
                try {
                    if (!json.isNull("wasSuccessful")) {
                        this.wasSuccessful = (json.getBoolean("wasSuccessful"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    if (!json.isNull("operationSummary")) {
                        this.operationSummary = (json.getString("operationSummary"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    if (!json.isNull("result")) {
                        this.result = (json.getString("result"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public String toString() {
                return "Operation: " + this.operationSummary + "| Was successful: " + this.wasSuccessful;
            }
        }

    }

    /**
     * Server api object constructor
     * @param context A valid context for web requests
     */
    public WebApi(Context context) {
        this.context = context;
    }

    /**
     * Server api object constructor using the application context instead of using one supplied by the caller.
     */
    public WebApi() {
        this.context = MyApp.getAppContext();
    }

    /**
     * Makes the actual request to our server web API.
     * @param request A valid Request object containing the appropriate arguments in the order the server
     *                expects to get them in for the function being called.
     *                Argument key/value keys can be any name you want - server simply processes the
     *                values in the order they appear in the array ignoring the key names.
     *                Example Request object when converted to JSON:
     *                  {"arguments":[{"name":"this_doesn't_matter","value":"116830684150145127689"},{"name":"this_doesn't_matter","value":"c9bBPcFPQcijcKNXk-jVs1:APA91bH37M_SvqiFIVgb_Wol2BgBR898sURBYcITwYeHXwdpKgmnEIqB_Txo6J0fi7VcRpkEH_Lg4POopeCtAZ79bGYDQpjMURPrJQ8S7-RpAJXH2Uta_HPcYqaYIZ6E_3UO3g5Ac3xi"}],"function":"upsertfcmtoken"}
     * @param listener
     */
    public void makeRequest(Requests.Request request,
                            final WebApiResultListener listener) {

        // Validate the request handle that will be um... handling the request
        if (handle != null && !handle.isFinished()) {
            Log.w(TAG, "makeRequest: Handle is still valid - cancelling!");
            try {
                boolean wasCancelled = handle.cancel(true);
                if (wasCancelled) {
                    Log.w(TAG, "makeRequest: Existing handle was cancelled!");
                } else {
                    Log.w(TAG, "makeRequest: Failed to cancel existing handle!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "makeRequest: Error cancelling existing handle\n" + e.getLocalizedMessage());
            }
        }

        // NOTE: debugArgumentString IS PURELY FOR LOGGING PURPOSES!
        String debugLoggingArgumentString = "";
        for (Requests.Arguments.Argument arg : request.arguments) {
            debugLoggingArgumentString = arg.toString() + "\n";
        }
        // This is the only place debugArgumentString is used!
        Log.i(TAG, "makeCrmRequest: Request function: " + request.function + " Request arguments: " + debugLoggingArgumentString);

        // Put together the json payload that the server will deserialize and evaluate
        StringEntity payload = null;
        try {
            payload = new StringEntity(request.toJson());
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailure(e.getLocalizedMessage());
        }

        client.setTimeout(120000);

        // Make the actual request
        this.handle = client.post(context, BASE_URL, payload, "application/json",
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Log.i(TAG, "onSuccess : code = " + statusCode);
                        String response = new String(responseBody);
                        OperationResults results = new OperationResults(response);
                        listener.onSuccess(results);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.w(TAG, "onFailure: code: " + statusCode);
                        listener.onFailure(error.getLocalizedMessage());
                    }
                });
    }


}