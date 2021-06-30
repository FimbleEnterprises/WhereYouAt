package com.fimbleenterprises.whereuat.generic_objs;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.fimbleenterprises.whereuat.MyApp;
import com.fimbleenterprises.whereuat.R;
import com.google.android.gms.maps.model.LatLng;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/* The basic format from Google's response is something like this:
 * <DirectionsResponse>
	<status>OK</status>
		<route>
		<summary>US-169 N</summary>
		<leg>
			<step>
			<travel_mode>DRIVING</travel_mode>
			<start_location>
				<lat>45.0066313</lat>
				<lng>-93.4594646</lng>
			</start_location>
			<end_location>
				<lat>45.0066456</lat>
				<lng>-93.4614614</lng>
			</end_location>
			<polyline>
				<points>mjuqGrxlyPCnK</points>
			</polyline>
			<duration>
				<value>14</value>
				<text>1 min</text>
			</duration>
			<html_instructions>
				Head <b>west</b> on <b>25th Ave N</b> toward <b>Fernbrook Ln N</b>
			</html_instructions>
			<distance>
				<value>157</value>
				<text>0.2 km</text>
			</distance>
		</step>
		...
 * 
 * See for yourself at this location result:
 * http://maps.googleapis.com/maps/api/directions/xml?origin=45.0070894,-93.459458&destination=45.29241506,-93.41627637&sensor=false&units=metric&mode=driving&key=AIzaSyDHaFYs8cUyI1Ta5RtOxhg1pXPAd2W9RBs
 */


/**
 * This is some old ass code from my MediBuddy days.  It works.  My gut tells me it is probably shitty
 * code (I was SO new) but I'd be lying if I said it hasn't worked a treat for (now) three widely used apps.
 * <br/>Oh, this fucker will aid in drawing polylines on a map using Google's directions api.
 */
public class MyMapRouteHelper {

    public interface OnDirectionsReceivedListener {
        void onDirectionsReceived(ArrayList<LatLng> positions);
        void onFailure(String msg);
    }

    private static OnDirectionsReceivedListener listener;
    public final static String MODE_DRIVING = "driving";
    public final static String MODE_WALKING = "walking";

    public MyMapRouteHelper(OnDirectionsReceivedListener onDirectionsReceivedListener) {
        this.listener = onDirectionsReceivedListener;
    }

     /** This method gets the XML doc from Google which contains all the information about the trip that the rest of the methods need below
      <br/>
      <br/><br/>It goes without saying that you need to call this first to obtain the Document object it returns for use in the other methods.
      @link https://developers.google.com/maps/documentation/directions/get-directions#DirectionsRequests
      @param start Starting position.
      @param end   Destination position.
      @param mode (String) The following travel modes are supported :
             <li>driving (default) indicates standard driving directions using the road network.</li>
             <li>walking requests walking directions via pedestrian paths & sidewalks (where available).</li>
             <li>bicycling requests bicycling directions via bicycle paths & preferred streets (where available).</li>
             <li>transit requests directions via public transit routes (where available). If you set the mode to transit, 
                  you can optionally specify either a departure_time or an arrival_time. If neither time is specified, 
                  the departure_time defaults to now (that is, the departure time defaults to the current time). You 
                  can also optionally include a transit_mode and/or a transit_routing_preference.</li>
      @return A document object that can be fed into constructPositions() method. */
    public void constructRoute(LatLng start, LatLng end, String mode) throws MalformedURLException {

        String addy = "https://maps.googleapis.com/maps/api/directions/xml?"
                + "origin=" + start.latitude + "," + start.longitude
                + "&destination=" + end.latitude + "," + end.longitude
                + "&sensor=false&units=metric&mode=" + mode + "&key=" + MyApp.getAppContext().getResources().getString(R.string.google_maps_key);
        final URL url = new URL(addy);

        @SuppressLint("StaticFieldLeak") AsyncTask<String, String, String> task = new AsyncTask<String, String, String>() {
            @SuppressLint("StaticFieldLeak")

            Document doc;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                // Pre execute stuff

            }

            @Override
            protected String doInBackground(String... args) {
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    doc = builder.parse(in);
                    return doc.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String val) {
                super.onPostExecute(val);
                listener.onDirectionsReceived(buildPositionsArray(doc));
            }
        };

        // The lack of this check has burned me before.  It's verbose and not always needed for reasons
        // unknown but I'd leave it!
        if(Build.VERSION.SDK_INT >= 11/*HONEYCOMB*/) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            task.execute();
        }

    }
    
    /** Returns the total driving/walking distance of this trip in meters **/
    public int getTotalDistanceInmeters (Document doc) {
    	
    	if (doc == null) {
    		return -1;
    	}
    	
    	// Gets all of the the <distance> nodes from each of the <step> nodes.
    	NodeList nodeList1 = doc.getElementsByTagName("distance");
        int totalDistanceInMeters = 0;
        
        try {
        	// Loop through each of the <distance> nodes from within each of the <step> nodes
			for (int i = 0; i < nodeList1.getLength() - 1; i++) {
				
				/* Each <distance> node has two child nodes: <value> and <text>.  <text> is the pretty, english variant (e.g. .2 km) 
				 * while <value> is the cold, hard meters value (e.g. 454) */
				
				// Get the <distance> node 
			    Node currentNode = nodeList1.item(i);
			    
			    // Get the child nodes (there's only two)
			    NodeList nl2 = currentNode.getChildNodes();
			    // Get the <value> node.  We're not interested in the <text> node.  We'll do our own pretty conversions (namely to imperial miles)
		        Node node2 = nl2.item(getNodeIndex(nl2, "value"));
		        // Increment our total meters variable from this <step>'s <distance><value> node and then continue to the next <step> by looping again.  
			    totalDistanceInMeters += Integer.parseInt(node2.getTextContent());
			}
		} catch (NumberFormatException e) {
			totalDistanceInMeters = 0;
			e.printStackTrace();
		} catch (DOMException e) {
			e.printStackTrace();
		}
        // Return the total, tallied userSwipedDown meters in this trip
        return totalDistanceInMeters;
    }
    
    /** Returns the total driving/walking duration of this trip in meters **/
    public int getTotalDurationInSeconds (Document doc) {
    	
    	// Gets all of the the <distance> nodes from each of the <step> nodes.
    	NodeList nodeList1 = doc.getElementsByTagName("duration");
        int totalTimeInSeconds = 0;
        
        try {
        	// Loop through each of the <duration> nodes from within each of the <step> nodes
			for (int i = 0; i < nodeList1.getLength() - 1; i++) {
				
				/* Each <duration> node has two child nodes: <value> and <text>.  <text> is the pretty, english variant (e.g. 4 minutes) 
				 * while <value> is the cold, hard seconds value (e.g. 138) */
				
				// Get the <duration> node 
			    Node currentNode = nodeList1.item(i);
			    
			    // Get the child nodes (there's only two)
			    NodeList nl2 = currentNode.getChildNodes();
			    // Get the <value> node.  We're not interested in the <text> node.  We'll do our own pretty conversions (to minutes, hours etc.)
		        Node node2 = nl2.item(getNodeIndex(nl2, "value"));
		        // Increment our total meters variable from this <step>'s <duration><value> node and then continue to the next <step> by looping again.  
			    totalTimeInSeconds += Integer.parseInt(node2.getTextContent());
			}
		} catch (NumberFormatException e) {
			totalTimeInSeconds = 0;
			e.printStackTrace();
		} catch (DOMException e) {
			e.printStackTrace();
		}
        // Return the total, tallied userSwipedDown meters in this trip
        return totalTimeInSeconds;
    }
        
    /* These methods only pull a single <step> of the returned trip details xml.  I don't yet know how to deal with these 
     * individual steps.  Instead I modified two of the below methods to return the aggregate values from the entire trip.
     * I am very much still trying to wrap my head around this code but the two total methods are above.  The below code
     * seems far too valuable to delete just because I don't understand it yet so it stays though unused. */

    public String getDurationText (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("duration");
        Node node1 = nl1.item(0);
        NodeList nl2 = node1.getChildNodes();
        Node node2 = nl2.item(getNodeIndex(nl2, "text"));
        Log.d("DurationText", node2.getTextContent());
        return node2.getTextContent();
    }

    public int getDurationValue (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("duration");
        Node node1 = nl1.item(0);
        NodeList nl2 = node1.getChildNodes();
        Node node2 = nl2.item(getNodeIndex(nl2, "value"));
        Log.d("DurationValue", node2.getTextContent());
        return Integer.parseInt(node2.getTextContent());
    }
    
    public String getDistanceText (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("distance");
        Node node1 = nl1.item(0);
        NodeList nl2 = node1.getChildNodes();
        Node node2 = nl2.item(getNodeIndex(nl2, "text"));
        Log.d("DistanceText", node2.getTextContent());
        return node2.getTextContent();
    }

    public int getDistanceValue (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("distance");
        Node node1 = nl1.item(0);
        NodeList nl2 = node1.getChildNodes();
        Node node2 = nl2.item(getNodeIndex(nl2, "value"));
        Log.d("DistanceValue", node2.getTextContent());
        return Integer.parseInt(node2.getTextContent());
    }

    public String getStartAddress (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("start_address");
        Node node1 = nl1.item(0);
        Log.d("StartAddress", node1.getTextContent());
        return node1.getTextContent();
    }

    public String getEndAddress (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("end_address");
        Node node1 = nl1.item(0);
        Log.d("StartAddress", node1.getTextContent());
        return node1.getTextContent();
    }

    public String getCopyRights (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("copyrights");
        Node node1 = nl1.item(0);
        Log.d("CopyRights", node1.getTextContent());
        return node1.getTextContent();
    }

    private ArrayList<LatLng> buildPositionsArray(Document doc) {
        NodeList nl1, nl2, nl3;
        ArrayList<LatLng> listGeopoints = new ArrayList<LatLng>();
        nl1 = doc.getElementsByTagName("step");
        if (nl1.getLength() > 0) {
            for (int i = 0; i < nl1.getLength(); i++) {
                Node node1 = nl1.item(i);
                nl2 = node1.getChildNodes();

                Node locationNode = nl2.item(getNodeIndex(nl2, "start_location"));
                nl3 = locationNode.getChildNodes();
                Node latNode = nl3.item(getNodeIndex(nl3, "lat"));
                double lat = Double.parseDouble(latNode.getTextContent());
                Node lngNode = nl3.item(getNodeIndex(nl3, "lng"));
                double lng = Double.parseDouble(lngNode.getTextContent());
                listGeopoints.add(new LatLng(lat, lng));

                locationNode = nl2.item(getNodeIndex(nl2, "polyline"));
                nl3 = locationNode.getChildNodes();
                latNode = nl3.item(getNodeIndex(nl3, "points"));
                ArrayList<LatLng> arr = decodePoly(latNode.getTextContent());
                for(int j = 0 ; j < arr.size() ; j++) {
                    listGeopoints.add(new LatLng(arr.get(j).latitude, arr.get(j).longitude));
                }

                locationNode = nl2.item(getNodeIndex(nl2, "end_location"));
                nl3 = locationNode.getChildNodes();
                latNode = nl3.item(getNodeIndex(nl3, "lat"));
                lat = Double.parseDouble(latNode.getTextContent());
                lngNode = nl3.item(getNodeIndex(nl3, "lng"));
                lng = Double.parseDouble(lngNode.getTextContent());
                listGeopoints.add(new LatLng(lat, lng));
            }
        }

        return listGeopoints;
    }

    private int getNodeIndex(NodeList nl, String nodename) {
        for(int i = 0 ; i < nl.getLength() ; i++) {
            if(nl.item(i).getNodeName().equals(nodename))
                return i;
        }
        return -1;
    }

    private ArrayList<LatLng> decodePoly(String encoded) {
        ArrayList<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng position = new LatLng((double) lat / 1E5, (double) lng / 1E5);
            poly.add(position);
        }
        return poly;
    }
}