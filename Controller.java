package com.cargocult.grantbot;
//package com.example.aroboeater;
//
//import ioio.lib.api.AnalogInput;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Vector;
//
//import org.opencv.android.Utils;
//import org.opencv.core.Core;
//import org.opencv.core.Mat;
//import org.opencv.core.MatOfDouble;
//import org.opencv.core.MatOfPoint;
//import org.opencv.core.MatOfPoint2f;
//import org.opencv.core.Point;
//import org.opencv.core.Scalar;
//import org.opencv.highgui.Highgui;
//import org.opencv.highgui.VideoCapture;
//import org.opencv.imgproc.Imgproc;
//import org.opencv.imgproc.Moments;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.os.Bundle;
//import android.os.Debug;
//import android.speech.tts.TextToSpeech;
//import android.util.Log;
//import android.view.SurfaceHolder;
//
//// Imgproc.contourArea(contour) //use this to get the area, compare to default area(which you must create) 
//
//class Controller extends CameraView {
//	private Mat mRgba;
//	private Mat mGray;
//	private Mat mIntermediateMat;
//	private Mat mIntermediateMat2;
//	private Mat mEmpty;
//	public Scalar lo = new Scalar(60, 100, 30);
//	public Scalar hi = new Scalar(130, 255, 255);
//
//	private Scalar bl, wh;
//	private Scalar lo1, lo2, hi1, hi2;
//	Scalar meanColor;
//	MatOfDouble mean, stdDev;
//	Scalar avgText;
//	Point avgTextPoint;
//	Point p;
//
//	int thickness = 4;
//
//	int fontscale = 2;
//
//	double currentMaxArea = -1;
//	double currentPercent = 0;
//	double defaultArea, minArea, currentX, currentY, screenX, screenY;
//	int countOutOfFrame;
//	
//	//for IR backing up
//	boolean doBacking = false;
//	boolean reverseWheels = false;
//	//boolean[] boolReturns = new boolean[2]; //the return from the other method
//
//	ServoCalculations servos;
//	Mat hierarchy;
//
//	// Debug stuff
//	int loopcount = 0;
//	
//	//Text to speech 
//	TextToSpeech tts;
//	
//	public Controller(Context context, TextToSpeech speech) {
//		super(context);
//		// setContentView(R.layout.colorfinder);
//		avgText = new Scalar(255, 0, 0, 255);
//		defaultArea = 100; // set the normal area that the object should be
//		minArea = 200;
//		servos = new ServoCalculations(horizontalViewAngle, verticalViewAngle, this);
//		hierarchy = new Mat();
//		p = new Point();
//		countOutOfFrame = 0;	
//	}
//
//	@Override
//	public void surfaceChanged(SurfaceHolder _holder, int format, int width,
//			int height) {
//		super.surfaceChanged(_holder, format, width, height);
//
//		synchronized (this) {
//			Log.d("PROBLEM",
//					"The surface has changed. You're default points are no longer accurate");
//			// initialize Mats before usage
//			mGray = new Mat();
//			mRgba = new Mat();
//			mIntermediateMat = new Mat();
//			mIntermediateMat2 = new Mat();
//			mEmpty = new Mat();
//			// CHANGE THE DFEAULT VALUES HERE
//			// lo = new Scalar(85, 100, 30);
//			// hi = new Scalar(130, 255, 255);
//
//			bl = new Scalar(0, 0, 0, 255);
//			wh = new Scalar(255, 255, 255, 255);
//		}
//	}
//	
//	
//
//	void processColor() {
//		Imgproc.cvtColor(mRgba, mIntermediateMat, Imgproc.COLOR_RGB2HSV_FULL);
//		Core.inRange(mIntermediateMat, lo, hi, mIntermediateMat2); // green
//		Imgproc.dilate(mIntermediateMat2, mIntermediateMat2, mEmpty);
//		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
//
//		Imgproc.findContours(mIntermediateMat2, contours, hierarchy,
//				Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
//
//		screenX = mRgba.size().width;
//		screenY = mRgba.size().height;
//		currentMaxArea = 0;
//		int indexMaxArea = -1;
//		for (int i = 0; i < contours.size(); i++) {
//			double s = Imgproc.contourArea(contours.get(i));
//			if (s > currentMaxArea) {
//				indexMaxArea = i;
//				currentMaxArea = s;
//			}
//		}
//
//		// Faster then getting moments to find center?
//		// Imgproc.boundingRect(contours.get(indexMaxArea));
//
//		// gets the center point of the image
//		if (!(indexMaxArea == -1))// check to see if there is a point
//		{
//			Moments m = Imgproc.moments(contours.get(indexMaxArea), true);
//			p.x = (m.get_m10() / m.get_m00());
//			p.y = (m.get_m01() / m.get_m00());
//		} else {
//			p.x = -1;
//			p.y = -1;
//		}
//		// can possibly delete to increase speed
//		// mRgba.setTo(bl); //takes 12.4% of processing time, should clear this
//		// up
//		Imgproc.drawContours(mRgba, contours, indexMaxArea, wh);
//	}
//
//	@Override
//	// makes sure it only grabs the variable after it has been assigned a value
//	protected Bitmap processFrame(VideoCapture capture) {
//		/**/
//		loopcount++;
//		switch (AndCarActivity.viewMode) {
//		case AndCarActivity.VIEW_MODE_FIND:
//			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
//			processColor();
//			// check to make sure area is big
//			Core.putText(mRgba, "Current contour  area: " + currentMaxArea,
//					new Point(60, 50), 3, 1, new Scalar(255, 0, 0, 255), 3);
//			Core.putText(mRgba, "X:" + p.x, new Point(60, 100), 3, 1,
//					new Scalar(255, 0, 0, 255), 3);
//			Core.putText(mRgba, "Y:" + p.y, new Point(60, 150), 3, 1,
//					new Scalar(255, 0, 0, 255), 3);
//			// Core.putText(mRgba, "Size X:" + mRgba.size().width, new Point(60,
//			// 200), 3, 1, new Scalar(255, 0, 0, 255), 3);
//			// Core.putText(mRgba, "Size Y:" + mRgba.size().height, new
//			// Point(60, 250), 3, 1, new Scalar(255, 0, 0, 255), 3);
//			mean = new MatOfDouble();
//			stdDev = new MatOfDouble();
//			Core.meanStdDev(mIntermediateMat, mean, stdDev);
//			double[] s = new double[3];
//			s = stdDev.toArray();
//			double[] m = new double[3];
//			m = mean.toArray();
//			Core.putText(mRgba, "Mean Hue:" + m[0], new Point(60, 200),
//					Core.FONT_HERSHEY_PLAIN, 3, avgText, thickness);
//			Core.putText(mRgba, "Mean Sat:" + m[1], new Point(60, 250),
//					Core.FONT_HERSHEY_PLAIN, 3, avgText, thickness);
//			Core.putText(mRgba, "Mean Val:" + m[2], new Point(60, 300),
//					Core.FONT_HERSHEY_PLAIN, 3, avgText, thickness);
//			Core.putText(mRgba, "StdDev Hue:" + s[0], new Point(60, 350),
//					Core.FONT_HERSHEY_PLAIN, 3, avgText, thickness);
//			Core.putText(mRgba, "StdDev Sat:" + s[1], new Point(60, 400),
//					Core.FONT_HERSHEY_PLAIN, 3, avgText, thickness);
//			Core.putText(mRgba, "StdDev Val:" + s[2], new Point(60, 450),
//					Core.FONT_HERSHEY_PLAIN, 3, avgText, thickness);
//			break;
//		case AndCarActivity.VIEW_MODE_RGBA:
//			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
//			servos.motorPW = ServoCalculations.ACTUALSTOP;
//			servos.mountPWx = ServoCalculations.MIDDLEPW;
//			servos.mountPWy = ServoCalculations.MIDDLEPW;
//
//			break;
//		case AndCarActivity.VIEW_MODE_CANNY:
////			 if (loopcount == 100)
////				 Debug.startMethodTracing("calc");
//			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
//			processColor();
//			//calculate, or if lost, scan
//			if (!(currentMaxArea < minArea)) //if it is NOT less than min area
//			{
//				countOutOfFrame = 0;
//				currentX = p.x;
//				currentY = p.y;
//			} else
//				countOutOfFrame++;
//			
//			 if (countOutOfFrame < 20)
//			 {
////				 if (tts.isSpeaking())
////					 tts.stop();
//				 servos.irc.calculateIR(doBacking, reverseWheels);
//
//				 if (doBacking)
//				 {
//					 servos.calculateMountPW(screenX, screenY, currentMaxArea, currentX, currentY);
//				 }
//				 else
//				 {
//					//MUST BE IN THIS ORDER
//		        	servos.calculateMountPW(screenX, screenY, currentMaxArea, currentX, currentY);
//		        	servos.calculateWheelPW(currentMaxArea);
//		        	servos.calculateMotorPW(currentMaxArea);
//		        	servos.irc.calculateSideIR();
//					servos.irc.calculateLeftRightIR(); //must go after calcualteWheel
//
//				 }
//			 }
//			else
//				try 
//	        	{
//					if (AndCarActivity.viewMode == AndCarActivity.VIEW_MODE_CANNY)
//					{
//						//mTts.speak("Android Car Online",  TextToSpeech.QUEUE_FLUSH, null);
////						if(!tts.isSpeaking())
////							tts.speak("Scanning", TextToSpeech.QUEUE_FLUSH, null);
//						
//						servos.scan();
//					}
//				} catch (InterruptedException e1) 
//				{
//					e1.printStackTrace();
//				}
////				if (loopcount == 100)
////				Debug.stopMethodTracing();
////			loopcount++;
//			break;
//		}
//
//		Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(),
//				Bitmap.Config.ARGB_8888);
//		try {
//			Utils.matToBitmap(mRgba, bmp);
//			return bmp;
//		} catch (Exception e) {
//			Log.e("org.opencv.samples.puzzle15",
//					"Utils.matToBitmap() throws an exception: "
//							+ e.getMessage());
//			bmp.recycle();
//			return null;
//		}
//	}
//
//	// FOR NATIVE JNI
//	// public native void processFrame(long matAddrGr, long matAddrRgba);
//	//
//	// static {
//	// System.loadLibrary("opencv_java");
//	// System.loadLibrary("mixed_sample");
//	// }
//
//	synchronized void setVectors(int hl, int hh, int sl, int sh, int vl, int vh) {
//		lo = new Scalar(hl, sl, vl);
//		hi = new Scalar(hh, sh, vh);
//		System.out.println("Done changing vectors");
//	}
//
//	// returns an array of all the values that the IOIO board will need
//
//
//
//	@Override
//	public void run() {
//		super.run();
//
//		synchronized (this) {
//			// Explicitly deallocate Mats
//			if (mRgba != null)
//				mRgba.release();
//			if (mGray != null)
//				mGray.release();
//			if (mIntermediateMat != null)
//				mIntermediateMat.release();
//
//			if (mIntermediateMat2 != null)
//				mIntermediateMat2.release();
//
//			mRgba = null;
//			mGray = null;
//			mIntermediateMat = null;
//			mIntermediateMat2 = null;
//		}
//	}
//}
