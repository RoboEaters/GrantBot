package com.roboeaters.grant_car;

// tweak to suit rosbridge

import android.util.Log;
import de.tavendo.autobahn.WebSocketConnectionHandler;

public class RosbridgeHandler extends WebSocketConnectionHandler {

	ROSBridge ros_thread;
	private String TAG;

	public RosbridgeHandler(ROSBridge r_t, String t) {
		ros_thread = r_t;
		TAG = t;
	}

	/**
	 * Fired when the WebSockets connection has been established.
	 * After this happened, messages may be sent.
	 */
	public void onOpen() {
		Log.d(TAG, "Status: Connected to ws://" + ros_thread.hostName + ":" + ros_thread.portNumber);
		ros_thread.canMessage = true;
	}

	/**
	 * Fired when the WebSockets connection has deceased (or could
	 * not established in the first place).
	 *
	 * @param code       Close code.
	 * @param reason     Close reason (human-readable).
	 */
	public void onClose(int code, String reason) {
		Log.d(TAG, "Connection lost.");
		ros_thread.canMessage = false;
	}

	/**
	 * Fired when a text message has been received (and text
	 * messages are not set to be received raw).
	 *
	 * @param payload    Text message payload or null (empty payload).
	 */
	public void onTextMessage(String payload) {
		Log.d(TAG, "onTextMessage: " + payload);
	}

	/**
	 * Fired when a text message has been received (and text
	 * messages are set to be received raw).
	 *
	 * @param payload    Text message payload as raw UTF-8 or null (empty payload).
	 */
	public void onRawTextMessage(byte[] payload) {
		Log.d(TAG, "onRawTextMessage: " + payload.toString());
	}

	/**
	 * Fired when a binary message has been received.
	 *
	 * @param payload    Binar message payload or null (empty payload).
	 */
	public void onBinaryMessage(byte[] payload) {
		Log.d(TAG, "onBinaryMessage: " + payload.toString());
	}



}
