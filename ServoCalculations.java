package com.roboeaters.grantbot;

// Add logging?

class ServoCalculations {
	//states
	public static final String FOLLOW = "FOLLOW";
	public static final String FIND= "FIND";
	public static final String BACKUP = "BACKUP";
	public static final String asfdTURN = "TURN";
	public static final String STOP= "STOP";
	public static String currentState;
	public static String currentRole; // HEAD,etc

	// uncomment for old bot motor values
	//  public static final float MIDDLEPW = 1550f;
	//	public static final int FORWARDMOTOR = 1465;
	//	public static final int BACKMOTOR = 1600;
	//	public static final int ACTUALSTOP = 1550;

	// uncomment for new bot motor values
	public static final int FORWARDMOTOR = 1250;
	public static final int ACTUALSTOP = 1500;
	public static final int BACKMOTOR = 1600;

	// wheel values
	public static final int MIDWHEEL = 1400;
	public static final int WHEELMIN = 1900; //Full left
	public static final int WHEELMAX = 900; //Full right

	static final float autoTurnScale = 300;
	static final float autoSpeedScale = 25;

	private boolean avoidState;
	private boolean wasBumped;
	private boolean foundWall;
	private boolean startReverse;

	// navigation
	String wallSide;
	private float sideWallVoltage;
	private float diagWallVoltage;

	// voltage thresholds and targets
	public static final float FRONT_TURN = 0.7f;
	public static final float DIAGONAL_TURN = 0.7f;
	public static final float FRONT_BUMP = 1.5f;
	public static final float DIAGONAL_BUMP = 2.0f;
	public static final float REAR_BUMP = 2.0f;	// change once you can experiment with rear (lol)
	public static final float BEST_SIDE_WALL_VOLTAGE = 1.7f;
	public static final float MAX_SIDE_WALL_VOLTAGE = 2.7f;
	public static final float MIN_SIDE_WALL_VOLTAGE = 0.7f;
	private static final float IR_CLEAR = 0.7f;
	private static final float IR_COLLISION = 2.0f;
	//RANGE FOR WALL FOLLOWING: 0.7 to 2.7
	//TOTAL = 2.0 VOLTS
	//MIDDLE = 1.7

	private float[] irChanges;

	private float irLeftVal;
	private float irFrontVal;
	private float irRightVal;
	private float irRightSide;
	private float irLeftSide;
	//private float irBackVal;

	private float irLeftValPrev;
	private float irFrontValPrev;
	private float irRightValPrev;
	private float irRightSidePrev;
	private float irLeftSidePrev;
	//private float irBackValPrev;

	private float[] cmdPwm;
	private static final int DIR = 0;
	private static final int TURN = 1;
	private static final int VELO = 2;	


	public ServoCalculations() {
		irChanges = new float[5];
		cmdPwm = new float[3];
		cmdPwm[DIR] = 1;
		cmdPwm[TURN] = (float) MIDWHEEL;
		cmdPwm[VELO] = (float) ACTUALSTOP;
		wasBumped = false;
		startReverse = false;
	}

	public void checkStates(String currentState) {
		if (avoidState){
			if (irLeftVal < IR_CLEAR && irRightVal < IR_CLEAR && irFrontVal < IR_CLEAR) {
				avoidState = false;
				startReverse = true;
				cmdPwm[VELO] = (ACTUALSTOP);
				cmdPwm[DIR] = 1;
			}
			else {
				steerClosest(-1);
			}
		} else {
			if (irRightVal > IR_COLLISION || irLeftVal > IR_COLLISION || irFrontVal > IR_COLLISION) {
				startReverse = true;
				//cmdPwm[VELO] = (ACTUALSTOP);
				//cmdPwm[DIR] = -1;
				avoidState = true;
				wasBumped = true;
				steerClosest(-1);
			} else if (currentState == "wallFollowState") {
				followWall(currentRole);	
			} else if (currentState == "exploreObjectState") {
				steerChanged();
			} else {		// if (behaviorState = "roamState")
				steerOpen();	
			}
		}
	}

	public boolean getReverse() {
		if(!startReverse)
			return false;
		startReverse = false;
		return true;
	}

	public void wheelPWCheck() {
		if (cmdPwm[VELO]> WHEELMIN)
			cmdPwm[VELO]= WHEELMIN;
		if (cmdPwm[VELO]< WHEELMAX)
			cmdPwm[VELO]= WHEELMAX;
	}

	// has room to turn; used to check if it needs to reverse
	public boolean hasRoom() {
		return (irFrontVal < FRONT_BUMP || irLeftVal < DIAGONAL_BUMP || irRightVal < DIAGONAL_BUMP);
	}

	/* disabled until back IR sensor is set up
		// checks rear sensor while reversing
		public boolean canReverse() {
			return backIRVoltage < MAXREVERSE;
		}
	 */

	public float[] getcmdPwm() {
		cmdPwm[DIR] = (cmdPwm[VELO] < ACTUALSTOP? -1 : 1);
		return cmdPwm;
	}

	// points wheels towards closest object
	public void steerClosest(int dir) {

		// Create a population code based on the IR positions and the IR
		// values
		// The X or cosine part of the popcode relates to the direction and
		// magnitude of the turn
		double x = irLeftVal * Math.cos(Math.PI * .75) + irFrontVal
				* Math.cos(Math.PI * .5) + irRightVal * Math.cos(Math.PI * .25);

		// The Y or sine part of the popcode relates to the forward speed
		// The side IRs are discounted so that the robot will not slow
		// down as much when something is on its side.
		double y = .5 * irLeftVal * Math.sin(Math.PI * .75) + irFrontVal
				* Math.sin(Math.PI * .5) + .5 * irRightVal
				* Math.sin(Math.PI * .25);

		if (dir > 0) {
			// check if IRs above threshold, and reverse direction
			cmdPwm[DIR] = 1;
			cmdPwm[TURN] = (float) (MIDWHEEL + autoTurnScale * x);
			cmdPwm[VELO] = (float) ((ACTUALSTOP - 150) + autoSpeedScale * y);
			// check for forward speed goes below stationary
			if (cmdPwm[VELO] > ACTUALSTOP) {
				cmdPwm[VELO] = ACTUALSTOP;
			}
			else if (cmdPwm[VELO] < (ACTUALSTOP-150)) {
				cmdPwm[VELO] = ACTUALSTOP - 150;
			}
		} else {
			cmdPwm[DIR] = -1;
			cmdPwm[TURN] = (float) (MIDWHEEL - autoTurnScale * 2.0 * x);
			cmdPwm[VELO] = BACKMOTOR;	// slow down
		}
	}

	// steers towards open space
	public void steerOpen () {
		// Create a population code based on the IR positions and the IR
		// values
		// The X or cosine part of the popcode relates to the direction and
		// magnitude of the turn
				double x = irLeftVal * Math.cos(Math.PI * .75) + irFrontVal
						* Math.cos(Math.PI * .5) + irRightVal * Math.cos(Math.PI * .25);

		// modified to acknowledge side sensors
//		double x = irLeftSide * Math.cos(Math.PI *1) + irLeftVal * Math.cos(Math.PI * .75) + irFrontVal
//				* Math.cos(Math.PI * .5) + irRightVal * Math.cos(Math.PI * .25) + irRightSide * (Math.PI * 0);

		// The Y or sine part of the popcode relates to the forward speed
		// The side IRs are discounted so that the robot will not slow
		// down as much when something is on its side.
				double y = .5 * irLeftVal * Math.sin(Math.PI * .75) + irFrontVal
						* Math.sin(Math.PI * .5) + .5 * irRightVal
						* Math.sin(Math.PI * .25);

		// added side sensors
//		double y = .25 * irLeftSide * Math.sin(Math.PI * 1) + .5 * irLeftVal * Math.sin(Math.PI * .75) + irFrontVal
//				* Math.sin(Math.PI * .5) + .5 * irRightVal
//				* Math.sin(Math.PI * .25) + .25 * irRightSide * Math.sin(Math.PI * 0);

		cmdPwm[DIR] = 1;
		cmdPwm[TURN] = (float) (MIDWHEEL + autoTurnScale * x);
		cmdPwm[VELO] = (float) ((ACTUALSTOP - 150) + autoSpeedScale * y);

		// check for forward speed goes below stationary
		if (cmdPwm[VELO] > ACTUALSTOP) {
			cmdPwm[VELO] = ACTUALSTOP;
		}
		else if (cmdPwm[VELO] < (ACTUALSTOP-150)) {
			cmdPwm[VELO] = ACTUALSTOP - 150;
		}
	}

	// points wheels towards area with most changed IR values
	// will probably ditch this later
	public boolean steerChanged() {
		//if(!hasRoom())
		//	return false;
		//setWheelPW(navArray[IRVoltages.indexOf(Collections.max(irChanges))]);
		return true;
	}

	
	public void setVoltage(float IRFront, float IRLeft, float IRRight,
			float IRLSide, float IRRSide) {

		irLeftValPrev = irLeftVal;
		irFrontValPrev = irFrontVal;
		irRightValPrev = irRightVal;
		irRightSidePrev = irRightVal;
		irLeftSidePrev = irLeftSide;
		//irBackValPrev = IRBack;

		// light averaging for slight smoothing
		irLeftVal = (IRLeft + irLeftValPrev) / 2;
		irFrontVal = (IRFront + irFrontValPrev) / 2;
		irRightVal = (IRRight + irRightValPrev) / 2;
		irRightSide = (IRRSide + irRightSidePrev) / 2;
		irLeftSide = (IRLSide + irLeftSidePrev) / 2;
		//irBackVal = (IRBack + irLeftSidePrev) / 2;
	}

	// action selection event
	public boolean getBump() {
		if (!wasBumped)
			return false;
		else
			wasBumped = false;
		return true;
	}

	// action selection event
	public boolean foundWall() {
		if (!foundWall)
			return false;
		else
			foundWall = false;
		return foundWall;
	}

	// action selection data (will probably ditch)
	public float[] getIRChanges() {			
		irChanges[0] = irLeftSide - irLeftSidePrev;
		irChanges[1] = irLeftVal - irLeftValPrev;
		irChanges[2] = irFrontVal - irFrontValPrev;
		irChanges[3] = irRightVal - irRightValPrev;
		irChanges[4] = irRightSide - irRightSidePrev;
		return irChanges;
	}

	
	// checks if found wall on proper side for flankers
	// uses closest wall for head and rear
	public void followWall(String role) {
		if(role == "LEFTFLANK" || role == "RIGHTFLANK")
			wallSide = (role == "LEFTFLANK"? "LEFT" : "RIGHT");
		else
			wallSide = (irLeftSide < irRightSide? "LEFT" : "RIGHT");

		sideWallVoltage = (wallSide == "LEFT"? irLeftSide : irRightSide);
		diagWallVoltage = (wallSide == "LEFT"? irLeftVal : irRightVal);

		foundWall = (sideWallVoltage < MIN_SIDE_WALL_VOLTAGE);

		// adjusts reported value of side sensor to correct for drift
		sideWallVoltage = sideWallVoltage + (sideWallVoltage - BEST_SIDE_WALL_VOLTAGE);

		if(diagWallVoltage < irFrontVal)
			cmdPwm[VELO] = (float) (MIDWHEEL + autoTurnScale * (sideWallVoltage * Math.cos((Math.PI * (Math.atan(irFrontVal/sideWallVoltage))))));
		else{
			if (wallSide == "LEFT")
				cmdPwm[TURN] = MIDWHEEL + (float) (diagWallVoltage * (Math.cos(Math.PI * .75) + sideWallVoltage * Math.cos(Math.PI * 1)));
			else
				cmdPwm[TURN] = MIDWHEEL + (float) (diagWallVoltage * (Math.cos(Math.PI * .25) + sideWallVoltage * Math.cos(Math.PI * 0))); 
		}

		// SERIOUSLY BROKEN. UNSAFE FOR NOW LOL
		cmdPwm[VELO] = (float) (irLeftSide * Math.cos(Math.PI *1) + irLeftVal * Math.cos(Math.PI * .75) + irFrontVal
				* Math.cos(Math.PI * .5) + irRightVal * Math.cos(Math.PI * .25) + irRightSide * (Math.PI * 0));
	}

}