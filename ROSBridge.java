package com.roboeaters.grant_car;

// Uses ROSBridge 2.0 protocol:
// https://github.com/RobotWebTools/rosbridge_suite/blob/groovy-devel/ROSBRIDGE_PROTOCOL.md
// See also: Rosbridge.org

// Autobahn Android webSocket library:
// http://autobahn.ws/android/getstarted

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

public class ROSBridge {

	String hostName;
	String portNumber;
	byte[] ipAddress;
	byte[] ipBytes;

	private static final String TAG = "ros_thread";

	private final WebSocket mConnection = new WebSocketConnection();

	boolean canMessage;

	public ROSBridge(byte[] ip, String port){

		portNumber = port;
		ipAddress = ip;

		try {
			InetAddress addr = InetAddress.getByAddress(ipBytes);			
			hostName = addr.getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}		
		start();
	}

	// opens the websocket connection
	public void start() {

		final String wsuri = "ws://" + hostName + ":" + portNumber;
		Log.d(TAG, "Connecting to: " + wsuri);

		try {
			mConnection.connect(wsuri, new RosbridgeHandler(this, TAG)); 
		} catch (WebSocketException e) {
			Log.d(TAG, e.toString());
		}

	}
	
	// sends JSON as text over websocket
	private boolean sendJSON(String topic, JSONObject message, String verb) {
		try {
			mConnection.sendTextMessage(message.toString());
		} catch (NullPointerException n) {
			Log.d(TAG, "Unable to " + verb + " topic: " + topic + ".");
			return false;
		}
		Log.d(TAG, "Successfully able to " + verb + " topic: " + topic + ".");
		return true;
	}
	
	
	// informs the ROS master that you will publish messages to a topic (required)
	public boolean advertiseToTopic(String topic)
	{
		Log.d("AUTOBAHN", "Attempting to advertise to topic: " + topic + ".");
		JSONObject advertisement = new JSONObject();
		try {
			advertisement.put("op", "advertise");
			//advertisement.put("id", String);				// optional
			advertisement.put("topic", topic);				// topic: "eater_input"
			advertisement.put("type", "std_msgs/String");	// for stringified/toString(ed) JSONs
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return sendJSON(topic, advertisement, "advertise to");
	}

	// stop publishing messages to topic
	public boolean unadvertiseFromTopic(String topic)
	{
		JSONObject unAdvertisement = new JSONObject();
		try {
			unAdvertisement.put("op", "advertise");
			//unAdvertisement.put("id", String);		// optional
			unAdvertisement.put("topic", topic);		// "eater_input"
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return sendJSON(topic, unAdvertisement, "unadvertise from");
	}


	// sends a rosbridge JSON containing a JSON-format ROS message.
	public boolean publishToTopic(String topic, String msg)
	{
		JSONObject publication = new JSONObject();
		JSONObject message = new JSONObject();
		try {
			message.put("data", "message");
			publication.put("op", "publish");
			//publication.put("id", String);	// optional
			publication.put("topic", topic);
			publication.put("msg", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return sendJSON(topic, publication, "publish to");
	}


	// informs the ROS master that you will accept messages that are published to
	// the specified topic.
	public boolean subscribeToTopic(String topic)
	{
		JSONObject subscription = new JSONObject();
		try {
			subscription.put("op", "subscribe");
			subscription.put("topic", topic);
			//subscription.put("type", String)				// topics have type specified by default
			//subscription.put("throttle_rate, int)			// min time (ms) between messages default: 0
			//subscription.put("queue_length, int);			// size of message buffer (due to throttle) default: 1
			//subscription.put("fragment_size, int);		// max message size before being fragmented
			//subscription.put("compression, String);		// "none" or "png"
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return sendJSON(topic, subscription, "subscribe to");
	}


	// stop accepting messages from topic
	public boolean unSubscribeFromTopic(String topic)
	{
		JSONObject unSubscription = new JSONObject();
		try {
			unSubscription.put("op", "unsubscribe");
			//unSubscription.put("id", String);			// optional
			unSubscription.put("topic", topic);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return sendJSON(topic, unSubscription, "unsubscribe from");
	}

	// calls ROS service
	public void callService(String service, List<String> args)
	{
		JSONObject serviceCall = new JSONObject();
		try {
			serviceCall.put("op", "call_service");
			//serviceCall.put("id", String);				// optional
			serviceCall.put("service", service);
			//serviceCall.put("args, list<json>);			// if service has no args, none need be provided
			//serviceCall.put("fragment_size, int);			// max message size before fragmentation
			//serviceCall.put("compression, String);		// "none" or "png"
		} catch (JSONException e) {
			e.printStackTrace();
		}
		// send(serviceCall);
	}

	// responds to a service call send from a ROS node
	public void respondToService (String service, List<String> args)
	{
		JSONObject serviceResponse = new JSONObject();
		try {
			serviceResponse.put("op", "service_response");
			//serviceResponse.put("id", String);				// optional
			serviceResponse.put("service", service);
			//serviceResponse.put("values, list<json>);			// JSON return values
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	// disconnects, though seems to do so unsuccessfully
	public void end() {
		if (mConnection.isConnected()) 
			mConnection.disconnect();
	}
}