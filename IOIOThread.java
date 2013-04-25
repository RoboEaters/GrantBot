package com.cargocult.grantbot;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;

//import java.text.DecimalFormat;

import android.util.Log;

public class IOIOThread extends BaseIOIOLooper 
{
		private PwmOutput pwmOutputx;
		private PwmOutput pwmOutputy;
		private PwmOutput motorOutput;
		private PwmOutput wheelOutput;
		private static GrantBotMain the_gui;
		private static ActionSelector actions;
		
		double[] PWs = new double[4];
		
		//IRs
		private AnalogInput IRFront, IRLeft, IRRight, IRRSide, IRLSide, IRBack;
		
		private DigitalInput hallEffectSensor;
		private int lastMotorPW;

		//passes in a reference to sample2view FROM sample2nativecamera. bad programming?
		public IOIOThread(GrantBotMain ui)
		{
			the_gui = ui;
			
			actions = new ActionSelector();

			Thread.currentThread().setName("IOIOThread");
			Log.d("IOIOThread", "IOIOThread has been created");
		}
		
		@Override
		public void setup() throws ConnectionLostException 
		{
			try {
				Log.d("IOIOThread", "Trying to finish setup of IOIO");
				double[] info = actions.servos.getSetupInfo();
				double motorPW = info[0];
				double wheelPW = info[1];
				
				pwmOutputx = ioio_.openPwmOutput(11, 100);
				pwmOutputy = ioio_.openPwmOutput(12, 100);
				motorOutput = ioio_.openPwmOutput(5, 100);
				wheelOutput = ioio_.openPwmOutput(10,100);
				IRFront = ioio_.openAnalogInput(43);
				IRLeft = ioio_.openAnalogInput(44); //diag
				IRRight = ioio_.openAnalogInput(40);
				IRRSide = ioio_.openAnalogInput(41);
				IRLSide = ioio_.openAnalogInput(42); 
				hallEffectSensor = ioio_.openDigitalInput(9); //normally 9
				
				motorOutput.setPulseWidth((int) motorPW);
				wheelOutput.setPulseWidth((int) wheelPW);
				actions.servos.setVoltage(IRFront.getVoltage(), IRLeft.getVoltage(), IRRight.getVoltage(), IRRSide.getVoltage(), IRLSide.getVoltage());

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
			//we can calculate the PW values right here in the IOIO loop
			//INSTEAD OF from the controller class
			//MUST BE IN THIS ORDER
			//Still using calculations done based on the tracking of the ball (reliant on mountX and mountY)
			//Need to either change or make new methods.
//        	servos.calculateWheelPW();
//        	servos.calculateMotorPW();
			actions.servos.setVoltage(IRFront.getVoltage(), IRLeft.getVoltage(), IRRight.getVoltage(), IRRSide.getVoltage(), IRLSide.getVoltage());
        	actions.actionLoop();
        	
			
			PWs = actions.servos.getServoPW();
			 int motorPW = (int) PWs[0];	// set speed
			 int wheelPW = (int) PWs[1];	// set steering


			wheelOutput.setPulseWidth(wheelPW);		// sends speed to motor

			//for going between backwards and forwards
			//WARNING: MINIMIZE GOING FROM FULL SPEED FORWARD TO FULL SPEED BACKWARDS
			//DOING SO CAN DAMAGE THE GEARS
			if (motorPW <  ServoCalculations.MOTORSTOP)
				motorOutput.setPulseWidth(ServoCalculations.MOTORSTOP);
			
			// grant, add small brake loop here?
			
			motorOutput.setPulseWidth((int) motorPW);	// sends motor command to motor
			lastMotorPW = (int) motorPW;				// saves current motor state... not used for anything?
			
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
			//10:Halifact Sensor
			double[] values = new double[10];
			values[0] = 0;
			values[1] = 0;
			values[2] = motorPW;
			values[3] = wheelPW;
			values[4] = IRFront.getVoltage();
			values[5] = IRLeft.getVoltage();
			values[6] = IRRight.getVoltage();
			values[7] = IRLSide.getVoltage();
			values[8] = IRRSide.getVoltage();
			values[9] = 0;
			
			Boolean halleffect = false;
//
			halleffect = hallEffectSensor.read();

//
//			if(halleffectVolt > 0.35)
//				halifact = true;

			//Need to post PW and IR readings back to the GUI Here!!

			the_gui.setTextFields(values, halleffect, ActionSelector.currentRole);
			
			//determines how fast calculations are done
			Thread.sleep(50);
			
		}
	}
