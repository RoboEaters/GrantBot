package com.cargocult.grantbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;

class ServoCalculations {
	// IR sensors
	public static final int IRLEFTSIDE = 0;
	public static final int IRLEFTDIAGONAL = 1;
	public static final int IRFRONT = 2;
	public static final int IRRIGHTDIAGONAL = 3;
	public static final int IRRIGHTSIDE = 4;
	//public static final int IRBACK = 5;
	static final int IRCount = 5; //number of IR sensors

	// motor values
	public static final int MIDDLEPW = 1550;
	public static final int FASTFORWARDMOTOR = 1425;
	public static final int FORWARDMOTOR = 1455;
	public static final int BACKMOTOR = 1600;
	public static final int MOTORSTOP = 1550;
	double motorPW;

	// wheel values
	int MIDWHEEL = 1400;
	static int WHEELMIN = 1900; //Full left	== 26 degrees lol
	static int WHEELMAX = 900; //Full right == 26 degreees!
	double wheelPW;
	public static final double PWPERDEGREE = 192;	// hilariously bad
	//double wheelDegrees;

	// navigation
	//double heading;	
	String wallSide;
	Double sideWallVoltage;
	Double diagWallVoltage;

	// IR
	//IRCalculations irc;

	//double frontIRVoltage, leftIRVoltage, rightIRVoltage, lSideVoltage,
	//rSideVoltage, backIRVoltage, sideWallVoltage, diagWallVoltage;

	// voltage thresholds and targets
	public static final double FRONT_TURN = 0.7;
	public static final double DIAGONAL_TURN = 0.7;
	public static final double FRONT_BUMP = 1.5;		// figure out minturn vs voltage (vs speed?)
	public static final double DIAGONAL_BUMP = 2.0;
	public static final double REAR_BUMP = 2.0;	// change once you can experiment with rear (lol)
	public static final double BEST_SIDE_WALL_VOLTAGE = 1.7;
	public static final double MAX_SIDE_WALL_VOLTAGE = 2.7;
	public static final double MIN_SIDE_WALL_VOLTAGE = 0.7;
	//RANGE FOR WALL FOLLOWING: 0.7 to 2.7
	//TOTAL = 2.0 VOLTS
	//MIDDLE = 1.7

	// for holding IR values
	ArrayList<Double> IRVoltages;
	ArrayList<Double> prevIRVoltages;
	ArrayList<Double> IRChanges;

	// sensor mount angles for navigation
	double[] navArray = new double[5];	

	public void wheelPWCheck() {
		if (wheelPW < WHEELMIN)
			wheelPW = WHEELMIN;
		if (wheelPW > WHEELMAX)
			wheelPW = WHEELMAX;
	}

	public ServoCalculations() {
		//irc = new IRCalculations();
		IRVoltages = new ArrayList<Double>(IRCount);
		prevIRVoltages = new ArrayList<Double>(IRCount);
		IRChanges = new ArrayList<Double>(IRCount);
		motorPW = MOTORSTOP;
		wheelPW = MIDWHEEL;
		navArray[0] = -90;	// left side
		navArray[1] = -45;	// left diagonal
		navArray[2] = 0;	// front
		navArray[3] = 45;	// right diagonal
		navArray[4] = 90;	// right side
	}

	// utility methods for checking and setting up
	public double[] getServoPW() {
		double[] pws = new double[2];
		pws[0] = motorPW;
		pws[1] = wheelPW;
		return pws;
	}

	synchronized double[] getSetupInfo() {
		double[] info = new double[4];
		info[0] = MIDDLEPW;
		info[1] = MIDDLEPW;
		info[2] = MOTORSTOP;
		info[3] = MIDWHEEL;
		return info;
	}

	// converts throttle percentage (negative = reverse) to PW
	public void setThrottle (double percent) {
		motorPW = MOTORSTOP - (percent * (Math.abs((percent < 0 ? BACKMOTOR : FASTFORWARDMOTOR) - MOTORSTOP) / 100));
		motorPW = (percent < 0 ? Math.min(motorPW, BACKMOTOR) : Math.max(motorPW, FASTFORWARDMOTOR)); // just in case.
	}

	// converts requested turn degrees to PW or max if too much
	public void setWheelPW (double degrees) {
		//wheelPW = MIDWHEEL - (degrees * (Math.abs((degrees < 0 ? WHEELMIN : WHEELMAX) - MIDWHEEL) / 90));
		wheelPW = degrees*PWPERDEGREE;
		wheelPW = (degrees < 0 ? Math.min(wheelPW, WHEELMAX) : Math.max(wheelPW, WHEELMIN)); // just in case.
	}

	// has room to turn; used to check if it needs to reverse
	public boolean hasRoom() {
		return (IRVoltages.get(IRFRONT) < FRONT_BUMP || IRVoltages.get(IRLEFTDIAGONAL) < DIAGONAL_BUMP || IRVoltages.get(IRRIGHTDIAGONAL) < DIAGONAL_BUMP);
	}

	/* disabled until back IR sensor is set up
		// checks rear sensor while reversing
		public boolean canReverse() {
			return backIRVoltage < MAXREVERSE;
		}
	 */

	// points wheels towards closest object
	public void steerClosest(String role) {
		setWheelPW(navArray[IRVoltages.indexOf(Collections.max(IRVoltages))]);
	}


	// points wheels towards closest object; used to find walls
	public void steerWall(String role) {
		if (role == "LEFTFLANK") {
			List<Double> leftList = IRVoltages.subList(IRLEFTSIDE, IRFRONT);
			setWheelPW(navArray[leftList.indexOf(Collections.max(leftList))]);//assumes the indices are same in sublist
		}
		else if (role == "RIGHTFLANK") {
			List<Double> rightList = IRVoltages.subList(IRFRONT, IRRIGHTSIDE);
			setWheelPW(navArray[rightList.indexOf(Collections.max(rightList))]);
		}
		else {
			setWheelPW(navArray[IRVoltages.indexOf(Collections.max(IRVoltages))]);
		}			
	}

	// points wheels towards IR with least voltage
	// tweaked values for better steering (help avoid corners, etc.)
	public boolean steerOpen () {
		if (!hasRoom())
			return false;
		// can changes constants to variables dependent on speed?
		//IRVoltages.set(IRLEFTSIDE, IRVoltages.get(IRLEFTSIDE) * 0.5);
		//IRVoltages.set(IRLEFTDIAGONAL, IRVoltages.get(IRLEFTDIAGONAL) * 1.5);
		//IRVoltages.set(IRFRONT, IRVoltages.get(IRFRONT) * 3);
		//IRVoltages.set(IRRIGHTDIAGONAL , IRVoltages.get(IRRIGHTDIAGONAL) * 1.5);
		//IRVoltages.set(IRRIGHTSIDE, IRVoltages.get(IRRIGHTSIDE) * 0.5);
		System.out.print("size of navArray: " + navArray.length);
		System.out.print("IRVoltages.indexOf(Collections.min(IRVoltages): " + IRVoltages.indexOf(Collections.min(IRVoltages)));
		setWheelPW(navArray[IRVoltages.indexOf(Collections.min(IRVoltages))]); // turn towards most openest space
		return true;
	}

	// points wheels towards area with most changed IR values
	public boolean steerChanged() {
		if(!hasRoom())
			return false;
		setWheelPW(navArray[IRVoltages.indexOf(Collections.max(IRChanges))]);
		return true;
	}

	// params were float, changed to double. is that okay?
	// also this is all probably slow ?
	public void setVoltage(double IRFront, double IRLeft, double IRRight,
			double IRLSide, double IRRSide) {

		
		prevIRVoltages = IRVoltages;
		IRVoltages = new ArrayList<Double>();
		IRVoltages.add(IRLEFTSIDE, IRLSide);
		IRVoltages.add(IRLEFTDIAGONAL, IRLeft);
		IRVoltages.add(IRFRONT, IRFront);
		IRVoltages.add(IRRIGHTDIAGONAL, IRRight);
		IRVoltages.add(IRRIGHTSIDE, IRRSide);

		Log.d("setVoltage", "The front IR voltage is " + IRFront);
	}


	public boolean getBump() {
		return (IRVoltages.get(IRFRONT) > FRONT_BUMP || IRVoltages.get(IRLEFTDIAGONAL) > DIAGONAL_BUMP || IRVoltages.get(IRRIGHTDIAGONAL) > DIAGONAL_BUMP);
	}


	public ArrayList<Double> getChange() {
		IRChanges = new ArrayList<Double>(5);
		for (int i=0 ; i<5; i++)			
			IRChanges.add(i, IRVoltages.get(i) - prevIRVoltages.get(i));
		return IRChanges;
	}

	// checks if found wall on proper side for flankers
	// uses closest wall for head and rear
	public boolean foundWall(String role) {
		if(role == "LEFTFLANK" || role == "RIGHTFLANK")
			wallSide = (role == "LEFTFLANK"? "LEFT" : "RIGHT");
		else
			wallSide = (IRVoltages.get(IRLEFTDIAGONAL) < IRVoltages.get(IRRIGHTDIAGONAL)? "LEFT" : "RIGHT");

		sideWallVoltage = IRVoltages.get(wallSide == "LEFT"? IRLEFTSIDE : IRRIGHTSIDE);
		diagWallVoltage = IRVoltages.get(wallSide == "LEFT"? IRLEFTDIAGONAL : IRRIGHTDIAGONAL);

		if(Math.max(sideWallVoltage, diagWallVoltage) < MIN_SIDE_WALL_VOLTAGE)
			return false;
		else
			return true;
	}

	// uses trig to stay close to wall
	public void followWall() {
		sideWallVoltage = sideWallVoltage + (sideWallVoltage - BEST_SIDE_WALL_VOLTAGE);

		if(diagWallVoltage > IRVoltages.get(IRFRONT))
			setWheelPW((Math.toDegrees(Math.atan(IRVoltages.get(IRFRONT)/sideWallVoltage))) * (wallSide == "LEFT" ? 1 : -1));
		else{
			setWheelPW((diagWallVoltage - sideWallVoltage) * 2); // tweak with testing
		}
		setThrottle(100);
	}

}