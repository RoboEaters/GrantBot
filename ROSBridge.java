package com.cargocult.grantbot;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;


public class ROSBridge {
	
	
	public ROSBridge(){
	}
	
	
	public void advertiseToTopic(String topic) {
		JSONObject advertisement = new JSONObject();
		try {
			advertisement.put("op", "advertise");
			//advertisement.put("id", String);				// optional
			advertisement.put("topic", topic);				// topic: "eater_input"
			advertisement.put("type", "std_msgs/String");	// for stringified/toString(ed) JSONs
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// send(advertisement);
	}
	
	public void unadvertiseFromTopic(String topic) {
		JSONObject unAdvertisement = new JSONObject();
		try {
			unAdvertisement.put("op", "advertise");
			//unAdvertisement.put("id", String);		// optional
			unAdvertisement.put("topic", topic);		// "eater_input"
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// send(unAdvertisement);
	}
	

	public void publishToTopic(String topic, JSONObject message) {	// already in String format?
		JSONObject publication = new JSONObject();
		try {
			publication.put("op", "publish");
			//publication.put("id", String);	// optional
			publication.put("topic", topic);	// "eater_input"
			publication.put("msg", message.toString());	// JSON (toString/stringify/whatever)
		} catch (JSONException e) {
			e.printStackTrace();
		}
		// send(publication);
	}
		
	public void subscribeToTopic(String topic) {
		JSONObject subscription = new JSONObject();
		try {
			subscription.put("op", "subscribe");
			subscription.put("topic", topic);			// "eater_output"
			//subscription.put("type", String)				// topics have type specified by default
			//subscription.put("throttle_rate, int)			// min time (ms) between messages default: 0
			//subscription.put("queue_length, int);			// size of message buffer (due to throttle) default: 1
			//subscription.put("fragment_size, int);		// max message size before being fragmented
			//subscription.put("compression, String);		// "none" or "png"
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// send(subscription);
	}
	
	public void unSubscribeFromTopic(String topic) {
		JSONObject unSubscription = new JSONObject();
		try {
			unSubscription.put("op", "unsubscribe");
			//unSubscription.put("id", String);			// optional
			unSubscription.put("topic", topic);			// "eater_output"
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// send(unSubscription);
	}
	
	public void callService(String service, List<String> args) {	// list instead of string: JSON objects..
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
	
	public void respondToService (String service, List<String> args) {						// args?
		JSONObject serviceResponse = new JSONObject();
		try {
			serviceResponse.put("op", "service_response");
			//serviceResponse.put("id", String);				// optional
			serviceResponse.put("service", service);
			//serviceResponse.put("values, list<json>);			// JSON return values
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// send(serviceResponse);
	}
	
}