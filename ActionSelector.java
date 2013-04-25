package com.cargocult.grantbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

// GRANT'S LINGERING QUESTIONS
// 1 was it right to convert the binary "event" array to boolean?
// 2 were the matrices converted correctly?
// 3 I am shamefully unsure of what to call static/public/etc. please check :(
// 4 I am going to go through and change the names to math Java conventions.
// 5 all weights/connections/baseline activities need to be assigned via experimentation

public class ActionSelector {

	// roles
	public static final String HEAD = "HEAD";
	public static final String LEFTFLANK = "LEFTFLANK";
	public static final String RIGHTFLANK = "RIGHTFLANK";
	public static final String CABOOSE = "CABOOSE";
	public static final String STEERTEST = "STEERTEST";
	public static String currentRole;
	private int botID;

	// state neurons
	public static final int STATE_WALL_FOLLOW = 0;
	public static final int STATE_OPEN_FIELD = 1;
	public static final int STATE_EXPLORE_OBJECT = 2;
	private static int currentState;
	private static int previousState;
	private static boolean maneuverState = false;
	private static boolean isReversing = false;

	// parameters for state neuron activation function
	double N_ACT_GAIN = 2;
	double N_ACT_PERSIST = 0.25;		// state neuron activity decay rate
	double N_ACT_BASECURRENT = -1.0;

	// event neurons
	// Note about 'bots getting bored of getting kicked
	// can get bored of side bumps, as they signify a wall
	// cannot get bored of front bumps, as they represent an
	// obstacle. Hence, front bumps will be a reflex arc.
	// this will be accomplished by disconnecting the
	// front bump sensors from the event neurons, instead connecting
	// them to the pre-existing maneurver state.
	public static final int EVENT_IR = 0;
	public static final int EVENT_BUMP = 1;
	public static final int EVENT_SIDE_IR = 2;
	static final int E = 3;		// number of events
	boolean[] event;			// values of event neurons

	// neural network parameters
	static final int N = 3;		// number of state neurons
	static final int NM = 2;	// number neuromodulators
	double[] n;					// activation values of state neurons
	double[] nprev;
	double[] nm;				// activation values of nm neurons
	double[] nmprev;
	double[] achne;				// activation values of ACHe/NE neurons
	double[] achneprev;

	//neuromodulators
	public static final int NM_DA = 0;	// dopamine
	public static final int NM_5HT = 1;	// serotonin

	// parameters for neuromodulator neuron activation and synaptic plasticity
	public static final double NM_ACT_GAIN = 2;			// gain for sigmoid function
	public static final double NM_ACT_BASECURRENT = -1.0;
	public static final double NM_ACT_PERSIST = 0.25;	// persistence of synaptic current
	public static final double NM_STP_GAIN = 1.1;		// facilitating synapse ("STP" = "short term plasticity")
	public static final double NM_STP_DECAY = 50	;	// recovery time constant
	public static final double NM_STP_MAX = 2;			// weight value ceiling

	// parameters for ACh/NE neuron activity and synaptic plasticity
	public static final double ACHNE_ACT_GAIN = 5;		// gain for sigmoid function
	public static final double ACHNE_ACT_BASECURRENT = -0.5;
	public static final double ACHNE_ACT_PERSIST = 0.25;// persistence of synaptic current
	public static final double ACHNE_STP_GAIN = 0.1;	// depressing synapse
	public static final double ACHNE_STP_DECAY = 50;	// recovery time constant (what is "recovery?)
	public static final double ACHNE_STP_MAX = 1;		// weight value ceiling

	// neural connection weights (the sizes don't change, so arrays are good?)
	double[][] w_n_n_exc;	// state neuron to state neuron excitatory
	double[][] w_n_n_inh;	// state neuron to state neuron inhibitory
	double[][] w_nm_n;		// neuromodulator to state neuron
	double[][] w_e_n;		// event neuron to state neuron
	double[][] w_e_nm;		// event-neuron-to-neuromudulator
	double[] w_e_achne;		// event-neuron-to-achne

	Random rand;

	// Activity level that must be exceeded by a state neuron to set new state.
	public static final double ACTION_SELECTION_THRESHOLD = 0.68;

	// for logging. currently unused.
	double[] n_out;
	double[] nm_out;
	double[] achne_out;

	//IR voltage change
	ArrayList<Double> IRChanges;
	double leftChange;
	double frontChange;
	double rightChange;

	// parameters for IR event
	double IR_CHANGE = 1; // voltage change to trip exploreobject event

	ServoCalculations servos;

	public ActionSelector() {
		servos = new ServoCalculations();
		currentState = STATE_WALL_FOLLOW;
		previousState = currentState;
		currentRole = HEAD;
		IRChanges = new ArrayList<Double>(5);
		event = new boolean[3];
		roomba_net_init();
	}

	public ActionSelector(int ID, String startRole) {
		servos = new ServoCalculations();
		botID = ID;
		currentRole = startRole;

		if (startRole == "HEAD")
			currentState = STATE_OPEN_FIELD;
		else if (startRole == "LEFTFLANK" || startRole == "RIGHTFLANK")
			currentState = STATE_WALL_FOLLOW;
		else
			currentState = STATE_EXPLORE_OBJECT;

		previousState = currentState;
		IRChanges = new ArrayList<Double>(5);
		event = new boolean[3];
		roomba_net_init();
	}


	public void actionLoop() {

		// get sensor info				
		IRChanges = servos.getChange();
		leftChange = (IRChanges.get(0) + IRChanges.get(1)) / 2;
		frontChange = (IRChanges.get(1) + IRChanges.get(2) + IRChanges.get(3)) / 3;
		rightChange = (IRChanges.get(3) + IRChanges.get(4)) / 2;


		// special state for avoiding obstacles
		if (maneuverState){
			if (!servos.hasRoom()) {
				servos.steerClosest(HEAD);
				servos.setThrottle(!isReversing? 0 : -50);	// attempt to save gears
				isReversing = true;
			}
			else {
				servos.steerOpen();
				servos.setThrottle(50);
				maneuverState = false;	
				isReversing = false;
			}
		}
		else {

			// get events.
			event[EVENT_IR] = (frontChange > IR_CHANGE);
			event[EVENT_BUMP] = servos.getBump();			
			event[EVENT_SIDE_IR] = servos.foundWall(HEAD);

			// cycle network
			roomba_net_cycle();

			// handle states
			if (currentState == STATE_WALL_FOLLOW)
				wallFollowState();
			if (currentState == STATE_OPEN_FIELD)
				openState();
			if (currentState == STATE_EXPLORE_OBJECT)
				exploreState();
		}
	}

	// private void victimState {
	// someValue = investigateVictim()
	// calculate stuff
	// assign stuff (servo motor/wheelPW)
	// use stuff
	// communicate stuff
	//}


	private void wallFollowState() {
		if (!servos.hasRoom()) {
			servos.setThrottle(0); // redundant
			maneuverState = true;
		}
		if (!servos.foundWall(currentRole))
			servos.steerWall(currentRole);
		else
			servos.followWall();	
	}


	private void openState() {
		if (!servos.steerOpen()) {
			servos.setThrottle(0);	// redundant
			maneuverState = true;
		}
		else	
			servos.setThrottle(50);
	}


	private void exploreState() {
		if (!servos.steerChanged()) {
			servos.setThrottle(0);	// redundant
			maneuverState = true;
		}
		else
			servos.setThrottle(50);
	}

	public String getCurrentState() {

		switch (currentState) {
		case STATE_WALL_FOLLOW:
			return "STATE_WALL_FOLLOW" + (maneuverState? " + maneuverState" : "");
		case STATE_OPEN_FIELD:
			return "STATE_OPEN_FIELD" + (maneuverState? " + maneuverState" : "");
		case STATE_EXPLORE_OBJECT:
			return "STATE_EXPLORE_OBJECT" + (maneuverState? " + maneuverState" : "");
		default:
			return "STATE_ERROR_LOL" + (maneuverState? " + maneuverState" : "");
		}
	}

	public String getCurrentRole() {
		return currentRole;
	}

	public int getID() {
		return botID;
	}


	public void roomba_net_init () {
		// initial values of neurons
		event = new boolean[] {false,false,false};
		n = new double[E];		// change to represent # of states
		nm = new double[NM];
		achne = new double[E];	// see above	

		// state neuron intrinsic connectivity
		w_n_n_exc = new double[N][N];
		w_n_n_inh = new double[N][N];

		for(int i = 0; i <N; i++) {
			for(int j = 0; j < N; j++){
		w_n_n_exc[i][j] = 0.5;
		w_n_n_inh[i][j] = -1.0;
			}
		}

		// can't excite or inhibit themselves
		for (int i = 0; i<N; i++) {
			w_n_n_exc[i][i] = 0;
			w_n_n_inh[i][i] = 0;
		}

		// neuromodulator to state neuron activity
		w_nm_n = new double[NM][N];
		for(int i = 0; i < NM; i++) {
			for(int j = 0; j < N; j++) {
				w_nm_n[i][j] = 0;
			}
		}
		w_nm_n[NM_5HT][STATE_WALL_FOLLOW] = 5;
		w_nm_n[NM_DA][STATE_EXPLORE_OBJECT] = 5;
		w_nm_n[NM_DA][STATE_OPEN_FIELD] = 5;

		// event neuron to state neuron activity
		w_e_n = new double[E][N];
		for (int i = 0; i < E; i++) {
			for (int j = 0; j < N; j++) {
				w_e_n[i][j] = 1;
			}
		}

		w_e_nm = new double[E][NM];
		// event neuron to state neuron connectivity
		for (int i = 0; i < E; i++) {
			for (int j = 0; j < NM; j++) {
				w_e_nm[i][j] = 0;
			}
		}
		w_e_nm[EVENT_IR][NM_DA] = 1;
		w_e_nm[EVENT_SIDE_IR][NM_DA] = 1;
		w_e_nm[EVENT_BUMP][NM_5HT] = 1; // risk averse behavior (runs away from bumps)
		// w_e_nm[EVENT_BUMP][NM_DA] = 1;  // risk taking behavior (runs towards bumps)

		w_e_achne = new double[E];
		// event neuron to neuromodulator connectivity
		for(int i = 0; i< E; i++)
		w_e_achne[i] = 1;
	}


	// should it be void, or should it return state/role/etc?
	public void roomba_net_cycle() {

		double I;
		nprev = n;					// previous values of state neurons
		nmprev = nm;
		achneprev = achne;

		// calculate cholinergic/noradrenergic neural activity
		for (int i = 0; i<E; i++)	// for each event neuron
			achne[i] = activity(ACHNE_ACT_BASECURRENT + (ACHNE_ACT_PERSIST * achneprev[i]) + (event[i]? w_e_achne[i] : 0), ACHNE_ACT_GAIN);
		

		for (int i = 0; i < NM; i++) {	// for each neuromodulator
			I = NM_ACT_BASECURRENT + (NM_ACT_PERSIST * nmprev[i]);
			for (int j = 0; j < E; j++) {	// for each event neuron tied to said event neuron
				I = I + (event[j] ? w_e_nm[j][i] : 0); // I is modified by the activity of the event neuron modulated by the weight of the 		}					// connection between the two
			}
			nm[i] = activity (I, NM_ACT_GAIN);	// neuromodulator neuron activity is I modified by the gain for neuromodulators
		}

		// calculate state neural activity
		for (int i = 0; i < N; i++) {	// for each state neuron
			I = N_ACT_BASECURRENT + (0.5) + (N_ACT_PERSIST * nprev[i]);	// straightforward 
			//(0.5*(double)rand.nextDouble())
			// intrinsic synaptic input // still in above for each state neuron loop
			for (int j = 0; j < NM; j++) {		// (for each state)
				I = I + (nprev[j] * w_n_n_exc[j][i] + (((nm[0]+nm[1]) * nprev[j] * w_n_n_inh[j][i]))); // uses sum total of both neuromodulators  
			}

			// event synaptic input
			for (int j = 0; j<E; j++) {		// for each event neuron
				for (int k = 0; k<NM; k++) {	// for each neuromodulatory neuron for each event neuron of each state neuron
					I = I + nm[k] * w_nm_n[k][i] * achne[j] * w_e_n[j][i]; // figures out what each nm thinks of the current events
				}
			}

			n[i] = activity (I, N_ACT_GAIN);	// activity of the state neuron
		}

		// update plastic weights (?) with short-term plasticity rule. a spike occurs when an event occurs
		for (int i = 0; i< E; i++) { // for each event neuron
			w_e_achne[i] = stp (w_e_achne[i], ACHNE_STP_GAIN, ACHNE_STP_DECAY, ACHNE_STP_MAX, event[i]);
		}

		for (int i = 0; i < E; i++) {			// for each event neuron,
			for (int j = 0; j < NM; j++) {		// for each nm neuron tied to said event neuron
				if (w_e_nm [i][j] > 0)			// if the weight is greater than zero between the two (what the hell?)
					w_e_nm [i][j] = stp (w_e_nm[i][j], NM_STP_GAIN, NM_STP_DECAY, NM_STP_MAX, event[i]); 
			}
		}

		int maxState = 0;
		double maxActivity = n[0];
		for (int i = 1; i < N; i++) {
			if (n[i] > maxActivity) {
				maxActivity = n[i];
				maxState = i;
			}
		}

		// logging stuff:
		//achne_out = achne;
		//n_out = n;
		//nm_out = nm;

		if(maxActivity > ACTION_SELECTION_THRESHOLD) {
			previousState = currentState;
			currentState = maxState;
		}
	}


	// activity function: sigmoid explained above. output increases more or less exponentially but slows with saturation. kinda neat!
	public double activity (double I, double g) {
		return 1/(1+Math.exp(-g*I));	// where Math.exp(-g*I) == e^(-g*I)
	}


	// short term plasticity
	public double stp (double xin, double p, double tau, double max, boolean spk) {
		double x;
		if (spk)					// if there was a spike (an event occurred)
			x = p*xin;				// the current weight is multiplied by the amount by which it should be increased/decreased
		else 						// if there was no spike (no event)
			x = xin + (1-xin)/tau;	// x is equal to (one minus the current weight) divided by the recovery time constant (50 for roombas!)

		return Math.min(max, x);
	}	
}
