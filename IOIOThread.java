package com.roboeaters.grant_car;

// mashup of some iteration of kevinbot and LeCarlDrive

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;

import java.text.DecimalFormat;

import android.util.Log;

public class IOIOThread extends BaseIOIOLooper 
{
	private String currentState;
	private String currentRole;
	private int botID;

	private static final int DIR = 0;		// 1 == forward, -1 == reverse
	private static final int TURN = 1;
	private static final int VELO = 2;

	float speed;
	float targetSpeed;
	float prevSpeed;
	float[] cmdPwm;
	float[] cmdPwmPrev;

	//	ROSBridge tests:
	boolean advertised;
	boolean published;
	boolean unadvertised;

	private PwmOutput motorOutput;
	private PwmOutput wheelOutput;
	private GrantCarMain the_gui;
	private static ServoCalculations servos;
	private ActionSelectionThread actions;
	private ROSBridge ros_thread;

	//IRs
	private AnalogInput IRFront, IRLeft, IRRight, IRRSide, IRLSide, IRBack;

	//private DigitalInput hallEffectSensor;

	public IOIOThread(GrantCarMain ui, ROSBridge r_t)
	{
		the_gui = ui;
		servos = new ServoCalculations();
		actions = new ActionSelectionThread();	//empty constructor for single-bot test
		ros_thread = r_t;

		Thread.currentThread().setName("IOIOThread");
		Log.d("IOIOThread", "IOIOThread has been created");
	}

	@Override
	public void setup() throws ConnectionLostException 
	{
		try {
			Log.d("IOIOThread", "Trying to finish setup of IOIO");

			actions.start();

			cmdPwm = new float[3];
			cmdPwm[DIR] = 1;
			cmdPwm[TURN] = ServoCalculations.MIDWHEEL;
			cmdPwm[VELO] = ServoCalculations.ACTUALSTOP;
			cmdPwmPrev = cmdPwm;

			motorOutput = ioio_.openPwmOutput(5, 100);
			wheelOutput = ioio_.openPwmOutput(10,100);
			IRFront = ioio_.openAnalogInput(43);
			IRLeft = ioio_.openAnalogInput(44); //diag
			IRRight = ioio_.openAnalogInput(40);
			IRRSide = ioio_.openAnalogInput(41);
			IRLSide = ioio_.openAnalogInput(42); 
			//hallEffectSensor = ioio_.openDigitalInput(9);

			motorOutput.setPulseWidth(ServoCalculations.ACTUALSTOP);
			wheelOutput.setPulseWidth(ServoCalculations.MIDWHEEL);
			servos.setVoltage(IRFront.getVoltage(), IRLeft.getVoltage(), IRRight.getVoltage(), IRRSide.getVoltage(), IRLSide.getVoltage());

			// ROSBridge
			advertised = false;
			published = false;
			unadvertised = false;

		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		finally
		{
			Log.d("IOIO_Tread", "IOIO thread sucessfully set up");
		}
	}

	public void loop() throws ConnectionLostException, InterruptedException
	{	
		currentState = actions.getCurrentState();
		//we can calculate the PW values right here in the IOIO loop
		//INSTEAD OF from the controller class
		//MUST BE IN THIS ORDER
		servos.setVoltage(IRFront.getVoltage(), IRLeft.getVoltage(), IRRight.getVoltage(), IRRSide.getVoltage(), IRLSide.getVoltage());
		servos.checkStates(currentState);
		cmdPwmPrev = cmdPwm;
		cmdPwm = servos.getcmdPwm();

		if (cmdPwm[VELO] > ServoCalculations.BACKMOTOR)
			cmdPwm[VELO] = ServoCalculations.BACKMOTOR;
		if (cmdPwm[VELO] < ServoCalculations.FORWARDMOTOR)
			cmdPwm[VELO] = ServoCalculations.FORWARDMOTOR;

		try {
			motorOutput.setPulseWidth((int) cmdPwm[VELO]);
			wheelOutput.setPulseWidth((int) cmdPwm[TURN]);
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		// overcome esc breaking function
		if (servos.getReverse()) {
			try {
				motorOutput.close();
				Thread.sleep(1000);
				motorOutput= ioio_.openPwmOutput(5, 100);
				Log.d("IOIO", "reversed direction");
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		} 


		// ROSBridge test:
		// --------------------------------------------------------------
		if (!advertised)
			advertised = ros_thread.advertiseToTopic("eater_input");

		//if (advertised)
		//	published = ros_thread.publishToTopic("eater_input", "YOLO");

		//if (published && !unadvertised)
		//	unadvertised = ros_thread.unadvertiseFromTopic("eater_input");
		// ---------------------------------------------------------------


		//values represent all of the newly calculated values done by the ServoCalculation class.
		//All of these values will be transported to the Main activity so that the text fields can be updated
		//in the UI.
		//0:MountX
		//1:MountY
		//2:MotorPW
		//3:WheelPW
		//4:Front IR
		//5:Diag Left IR
		//6:Diag Right IR
		//7:Side Left IR
		//8:Side Right IR
		//9:Back IR
		//11:Hall Effect Sensor
		double[] values = new double[10];
		values[0] = 0;
		values[1] = 0;
		values[2] = cmdPwm[VELO];
		values[3] = cmdPwm[TURN];
		values[4] = IRFront.getVoltage();
		values[5] = IRLeft.getVoltage();
		values[6] = IRRight.getVoltage();
		values[7] = IRLSide.getVoltage();
		values[8] = IRRSide.getVoltage();
		values[9] = 0;	// back IR voltage

		Boolean hallEffect = false;
		//
		//hallEffect = hallEffectSensor.read();

		//Need to post PW and IR readings back to the GUI Here!!

		the_gui.setTextFields(values, hallEffect, currentState);

		//determines how fast calculations are done
		Thread.sleep(100);

	}
}