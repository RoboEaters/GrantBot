package com.roboeaters.grant_car;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

import java.text.DecimalFormat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;


public class GrantCarMain extends Activity implements IOIOLooperProvider {
	private static final String TAG = "Sample::Activity";

	private final IOIOAndroidApplicationHelper helper_ = new IOIOAndroidApplicationHelper(
			this, this);

	GrantCarMain app;

	// UI stuff
	TextView mountX, mountY, motorPW, wheelPW, frontIR, backIR, sideRIR,
			sideLIR, diagRIR, diagLIR, halifact;
	TextView mountXValue, mountYValue, motorPWValue, wheelPWValue,
			frontIRValue, backIRValue, sideRIRValue, sideLIRValue,
			diagRIRValue, diagLIRValue, halifactValue;
	TextView stateText;
	TextView state;
	LinearLayout head_parent;
	LinearLayout UI;
	LinearLayout UIValues;

	// Threadings
	IOIOThread ioio_thread;
	ROSBridge ros_thread;
	Cam_thread cam_thread;
	SurfaceView view;	// needed?

	byte[] ip_address;
	String portNumber;
	
	DecimalFormat df = new DecimalFormat("#.####"); // format to print voltages

	// BLUETOOTH

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	// private BluetoothService mChatService = null;
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	private String mConnectedDeviceName = null;
	int menuSelection;
	// makes a "number" into a "dp" value
	// ex (scale) * 3 == "3dp"
	private float scale;

	public GrantCarMain() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@TargetApi(5)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = this;
		scale = getResources().getDisplayMetrics().density;
		Log.i(TAG, "onCreate");
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setLayout();

		helper_.create();

		// BLUETOOTH METHODS
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		// ensureDiscoverable(); // makes the device discoverable
		menuSelection = 0; // set the inital selection
		
		ip_address = new byte[]{10,42,0,1};
		portNumber = "9090";	// default ROSBridge
		
		view = new SurfaceView(this);
		ros_thread = new ROSBridge(ip_address, portNumber);
		cam_thread = new Cam_thread(view, ros_thread, "eater_input");
		cam_thread.start_thread();
	}
		

	// 0:MountX
	// 1:MountY
	// 2:MotorPW
	// 3:WheelPW
	// 4:Front IR
	// 5:Diag Left IR
	// 6:Diag Right IR
	// 7:Side Left IR
	// 8:Side Right IR
	// 9:Back IR
	public void setTextFields(double[] values, Boolean halifact, final String currentState) {
		// This method is being called often by the IOIO thread,
		// aka asking the UI thread to do a lot of work, may lag with more
		// updates.
		final double[] val = values;
		final Boolean hal = halifact;
		runOnUiThread(new Runnable() {
			/* (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 */
			@Override
			public void run() {
				motorPWValue.setText("" + df.format(val[2]));
				wheelPWValue.setText("" + df.format(val[3]));
				frontIRValue.setText("" + df.format(val[4]));
				diagLIRValue.setText("" + df.format(val[5]));
				diagRIRValue.setText("" + df.format(val[6]));
				sideLIRValue.setText("" + df.format(val[7]));
				sideRIRValue.setText("" + df.format(val[8]));
				backIRValue.setText("" + df.format(val[9]));
				halifactValue.setText("" + hal);
				state.setText("" + currentState);
			}
		});
	}

	public void setLayout() {
		head_parent = new LinearLayout(this);
		head_parent.setWeightSum(2.0f);
		LinearLayout.LayoutParams headlp = new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		head_parent.setLayoutParams(headlp);
		head_parent.setOrientation(LinearLayout.HORIZONTAL);

		UI = new LinearLayout(this);
		UI.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams lp;
		lp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
		UI.setLayoutParams(lp);
		UI.setGravity(Gravity.CENTER);

		UIValues = new LinearLayout(this);
		UIValues.setOrientation(LinearLayout.VERTICAL);
		UIValues.setLayoutParams(lp);
		UIValues.setGravity(Gravity.CENTER);

		TextView[] names = new TextView[12];
		TextView[] values = new TextView[12];

		mountXValue = new TextView(this);
		mountYValue = new TextView(this);
		motorPWValue = new TextView(this);
		wheelPWValue = new TextView(this);
		frontIRValue = new TextView(this);
		backIRValue = new TextView(this);
		sideRIRValue = new TextView(this);
		sideLIRValue = new TextView(this);
		diagRIRValue = new TextView(this);
		diagLIRValue = new TextView(this);
		halifactValue = new TextView(this);
		stateText = new TextView(this);

		mountX = new TextView(this);
		mountY = new TextView(this);
		motorPW = new TextView(this);
		wheelPW = new TextView(this);
		frontIR = new TextView(this);
		backIR = new TextView(this);
		sideRIR = new TextView(this);
		sideLIR = new TextView(this);
		diagRIR = new TextView(this);
		diagLIR = new TextView(this);
		halifact = new TextView(this);
		state = new TextView(this);

		values[0] = mountXValue;
		names[0] = mountX;
		values[1] = mountYValue;
		names[1] = mountY;
		values[2] = motorPWValue;
		names[2] = motorPW;
		values[3] = wheelPWValue;
		names[3] = wheelPW;
		values[4] = frontIRValue;
		names[4] = frontIR;
		values[5] = backIRValue;
		names[5] = backIR;
		values[6] = sideRIRValue;
		names[6] = sideRIR;
		values[7] = sideLIRValue;
		names[7] = sideLIR;
		values[8] = diagRIRValue;
		names[8] = diagRIR;
		values[9] = diagLIRValue;
		names[9] = diagLIR;
		values[10] = halifactValue;
		names[10] = halifact;
		values[11] = state;
		names[11] = stateText;

		for (TextView t : names) {
			t.setPadding((int) (5 * scale), (int) (5 * scale),
					(int) (5 * scale), (int) (5 * scale));
			t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
			UI.addView(t);
		}

		for (TextView t : values) {
			t.setPadding((int) (5 * scale), (int) (5 * scale),
					(int) (5 * scale), (int) (5 * scale));
			t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
			UIValues.addView(t);
		}

		mountX.setText("Mount-X PW");
		mountY.setText("Mount-Y PW");
		motorPW.setText("Motor PW");
		wheelPW.setText("Wheel PW");
		frontIR.setText("Front IR");
		backIR.setText("Back IR");
		sideRIR.setText("Side Right IR");
		sideLIR.setText("Side Left IR");
		diagRIR.setText("Diag Right IR");
		diagLIR.setText("Diag Left IR");
		halifact.setText("Halifact Sensor");
		stateText.setText("State");

		// frame.addView(viewScreen);
		head_parent.addView(UI);
		head_parent.addView(UIValues);
		setContentView(head_parent);

	}

	/****************************************************** functions from IOIOActivity *********************************************************************************/

	protected IOIOLooper createIOIOLooper() {

		// perhaps pass in a reference to the main application so the IOIO
		// thread can post the results
		// of the IR and PW calculations to the textViews in the main
		// applications UI
		ioio_thread = new IOIOThread(app, ros_thread);
		Log.d("YOLO TEST", "YOLO TEST");
		return ioio_thread;// send them the viewscreen thread
	}

	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		return createIOIOLooper();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	
	// BLUETOOTH
	@Override
	public synchronized void onResume() {
		super.onResume();
		if (true)
			Log.e(TAG, "+ ON RESUME +");
	}

	@Override
	protected void onStop() {
		helper_.stop();
		ros_thread.end();
		cam_thread.stop_thread();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		ros_thread.end();
		cam_thread.stop_thread();
		helper_.destroy();
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (true)
			Log.e(TAG, "++ ON START ++");
		helper_.start();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
			helper_.restart();
		}
	}
}