package com.roboeaters.grant_car;

import java.io.ByteArrayOutputStream;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;

// mashup of Richard's CameraPreview and Dr. Oros' Cam_thread

public class Cam_thread
{
	Camera mCamera;
	ROSBridge ros_thread;
	SurfaceView parent_context;

	String encoded_string;
	String topic;

	int width_ima, height_ima;
	private static final String TAG = "cam_thread";
	
	private boolean STOP_THREAD;
	private boolean IS_ADVERTISED;

	
	public Cam_thread(SurfaceView context, ROSBridge r_t, String t) {
		parent_context = context;	
		ros_thread = r_t;
		topic = t;
	}

	private void init() {
		try {			 
			mCamera = Camera.open();        
			Camera.Parameters parameters = mCamera.getParameters(); 
			parameters.setPreviewSize(320, 240);
			parameters.setPreviewFrameRate(30);
			parameters.setSceneMode(Camera.Parameters.SCENE_MODE_SPORTS);
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
			mCamera.setParameters(parameters);
			mCamera.setPreviewDisplay(parent_context.getHolder());			
			mCamera.setPreviewCallback(new cam_PreviewCallback());           
			mCamera.startPreview();
		} catch (Exception exception) {
			Log.e(TAG, "Error: ", exception);
		}
	}
	
	public void start_thread() {
		init();
	}
	
	public void stop_thread() {
		STOP_THREAD = true;
	}

	// Preview callback used whenever new frame is available...send image via ROSBridge !!!
	private class cam_PreviewCallback implements PreviewCallback {
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			if(STOP_THREAD == true) {
				mCamera.setPreviewCallback(null);
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
				return;
			}

			Parameters p = camera.getParameters();
			int width = p.getPreviewSize().width;
			int height = p.getPreviewSize().height;

			ByteArrayOutputStream outstr = new ByteArrayOutputStream();
			Rect rect = new Rect(0, 0, width, height);
			YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
			yuvimage.compressToJpeg(rect, 80, outstr); 
			
			// what else (if anything) is needed here?
			
			String encoded_string = Base64.encodeToString(data,Base64.DEFAULT);
			
			if(!IS_ADVERTISED)
				IS_ADVERTISED = ros_thread.advertiseToTopic(topic);
			if(IS_ADVERTISED)
				ros_thread.publishToTopic(topic, encoded_string);
		}
	}
}