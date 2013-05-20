package com.roboeaters.grant_car;

// to do:
// - add logging
// - add/modify events/states
// - test test test

import java.util.Random;

import android.util.Log;

public class ActionSelectionThread extends Thread {

	// roles
	public static final String HEAD = "HEAD";
	public static final String LEFTFLANK = "LEFTFLANK";
	public static final String RIGHTFLANK = "RIGHTFLANK";
	public static final String CABOOSE = "CABOOSE";
	public static final String STEERTEST = "STEERTEST";
	public static String currentRole;

	private int botID;

	private boolean isStopped;

	// state neurons
	public static final int STATE_WALL_FOLLOW = 0;
	public static final int STATE_OPEN_FIELD = 1;
	public static final int STATE_EXPLORE_OBJECT = 2;
	private int currentState;
	private int previousState;
	//private boolean maneuverState = false;
	//private boolean isReversing = false;

	// parameters for state neuron activation function
	private static final float N_ACT_GAIN = 2;
	private static final float N_ACT_PERSIST = 0.25f;		// state neuron activity decay rate
	private static final float N_ACT_BASECURRENT = -1.0f;

	// event neurons
	public static final int EVENT_IR = 0;
	public static final int EVENT_BUMP = 1;
	public static final int EVENT_SIDE_IR = 2;
	static final int E = 3;		// number of events
	boolean[] event;			// values of event neurons

	// neural network parameters
	public static final int N = 3;		// number of state neurons
	public static final int NM = 2;	// number neuromodulators
	private float[] n;					// activation values of state neurons
	private float[] nprev;
	private float[] nm;				// activation values of nm neurons
	private float[] nmprev;
	private float[] achne;				// activation values of ACHe/NE neurons
	private float[] achneprev;

	//neuromodulators
	public static final int NM_DA = 0;	// dopamine
	public static final int NM_5HT = 1;	// serotonin

	// parameters for neuromodulator neuron activation and synaptic plasticity
	public static final float NM_ACT_GAIN = 2;			// gain for sigmoid function
	public static final float NM_ACT_BASECURRENT = -1.0f;
	public static final float NM_ACT_PERSIST = 0.25f;	// persistence of synaptic current
	public static final float NM_STP_GAIN = 1.1f;		// facilitating synapse ("STP" = "short term plasticity")
	public static final float NM_STP_DECAY = 50;	// recovery time constant
	public static final float NM_STP_MAX = 2;			// weight value ceiling

	// parameters for ACh/NE neuron activity and synaptic plasticity
	public static final float ACHNE_ACT_GAIN = 5;		// gain for sigmoid function
	public static final float ACHNE_ACT_BASECURRENT = -0.5f;
	public static final float ACHNE_ACT_PERSIST = 0.25f;// persistence of synaptic current
	public static final float ACHNE_STP_GAIN = 0.1f;	// depressing synapse
	public static final float ACHNE_STP_DECAY = 50f;	// recovery time constant (what is "recovery?)
	public static final float ACHNE_STP_MAX = 1;		// weight value ceiling

	// neural connection weights (the sizes don't change, so arrays are good?)
	private float[][] w_n_n_exc;	// state neuron to state neuron excitatory
	private float[][] w_n_n_inh;	// state neuron to state neuron inhibitory
	private float[][] w_nm_n;		// neuromodulator to state neuron
	private float[][] w_e_n;		// event neuron to state neuron
	private float[][] w_e_nm;		// event-neuron-to-neuromudulator
	private float[] w_e_achne;		// event-neuron-to-achne

	Random rand;
	private static final String TAG = "neural_net";

	// Activity level that must be exceeded by a state neuron to set new state.
	private static final float ACTION_SELECTION_THRESHOLD = 0.68f;

	// parameters for IR change event
	// IR change is a concept carried over from roombas
	// will be dropped for better events
	private float[] irChanges;
	private float irChangeThreshold = 1; // voltage change to trip exploreobject event
	private float maxChange;

	ServoCalculations servos;

	// test
	public ActionSelectionThread() {
		servos = new ServoCalculations();
		currentState = STATE_OPEN_FIELD;
		previousState = currentState;
		currentRole = HEAD;
	}

	// real
	public ActionSelectionThread(int ID, String startRole) {
		servos = new ServoCalculations();
		botID = ID;	// numerical ID
		currentRole = startRole;

		if (startRole == "HEAD")
			currentState = STATE_OPEN_FIELD;
		else if (startRole == "LEFTFLANK" || startRole == "RIGHTFLANK")
			currentState = STATE_WALL_FOLLOW;
		else
			currentState = STATE_EXPLORE_OBJECT;

		previousState = currentState;
	}


	@Override
	public void start() {
		isStopped = false;
		irChanges = new float[5];
		maxChange = 0;
		event = new boolean[3];
		roboeater_net_init();
	}

	
	public void stop_thread() {
		isStopped = true;
	}

	
	// called by other threads
	public String getCurrentState(){
		if (currentState == STATE_WALL_FOLLOW)
			return "wallFollowState";
		else if (currentState == STATE_EXPLORE_OBJECT)
			return "exploreObjectState";
		else// (currentState == STATE_OPEN_FIELD)
			return "roamState";
	}

	
	@Override
	public void run() {
		if (!isStopped) {
			
			// get sensor info
			maxChange = 0;
			irChanges = servos.getIRChanges();
			for(float f : irChanges)
				maxChange = Math.max(f, maxChange);
			
			// get events
			event[EVENT_IR] = (maxChange > irChangeThreshold);
			event[EVENT_BUMP] = servos.getBump();			
			event[EVENT_SIDE_IR] = servos.foundWall();
			
			// cycle network
			roboeater_net_cycle();
		}

		// figure out optimal sleep time
		// [robably doesn't have to run too often
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	// everything from this point onwards is 
	// copied from Dr. Krichmar's Roomba network code
	// with minor tweaks to suit roboeater requirements
	
	// initialize network
	private void roboeater_net_init () {
		// initial values of neurons
		event = new boolean[] {false,false,false};
		n = new float[E];		// note: change to represent # of states
		nm = new float[NM];
		achne = new float[E];	// see above
		
		// state neuron intrinsic connectivity
		w_n_n_exc = new float[N][N];
		w_n_n_inh = new float[N][N];
		for(int i = 0; i <N; i++) {
			for(int j = 0; j < N; j++){
				w_n_n_exc[i][j] = 0.5f;
				w_n_n_inh[i][j] = -1.0f;
			}
		}
		for (int i = 0; i<N; i++) {
			w_n_n_exc[i][i] = 0;
			w_n_n_inh[i][i] = 0;
		}
		
		// neuromodulator to state neuron activity
		w_nm_n = new float[NM][N];
		for(int i = 0; i < NM; i++) {
			for(int j = 0; j < N; j++) {
				w_nm_n[i][j] = 0;
			}
		}
		w_nm_n[NM_5HT][STATE_WALL_FOLLOW] = 5;
		w_nm_n[NM_DA][STATE_EXPLORE_OBJECT] = 5;
		w_nm_n[NM_DA][STATE_OPEN_FIELD] = 5;
		
		// event-neuron-to-state-neuron activity
		w_e_n = new float[E][N];
		for (int i = 0; i < E; i++) {
			for (int j = 0; j < N; j++) {
				w_e_n[i][j] = 1;
			}
		}
		
		// event-neuron-to-state-neuron connectivity
		w_e_nm = new float[E][NM];
		for (int i = 0; i < E; i++) {
			for (int j = 0; j < NM; j++) {
				w_e_nm[i][j] = 0;
			}
		}
		w_e_nm[EVENT_IR][NM_DA] = 1;
		w_e_nm[EVENT_SIDE_IR][NM_DA] = 1;
		w_e_nm[EVENT_BUMP][NM_5HT] = 1; // risk averse behavior (runs away from bumps)
		// w_e_nm[EVENT_BUMP][NM_DA] = 1;  // risk taking behavior (runs towards bumps)

		// event-neuron-to-neuromodulator connectivity
		w_e_achne = new float[E];
		for(int i = 0; i< E; i++)
			w_e_achne[i] = 1;
		
		Log.d (TAG, "neural net initialized");
	}


	// main algorithm
	private void roboeater_net_cycle() {
		float I;
		nprev = n;
		nmprev = nm;
		achneprev = achne;
		
		// calculate cholinergic/noradrenergic neural activity
		for (int i = 0; i<E; i++)	// for each event neuron
			achne[i] = activity(ACHNE_ACT_BASECURRENT + (ACHNE_ACT_PERSIST * achneprev[i]) + (event[i]? w_e_achne[i] : 0), ACHNE_ACT_GAIN);
		for (int i = 0; i < NM; i++) {
			I = NM_ACT_BASECURRENT + (NM_ACT_PERSIST * nmprev[i]);
			for (int j = 0; j < E; j++) {
				I = I + (event[j] ? w_e_nm[j][i] : 0);
			}
			nm[i] = activity (I, NM_ACT_GAIN);
		}
		
		// calculate state neural activity
		for (int i = 0; i < N; i++) {
			I = N_ACT_BASECURRENT + (0.5f * rand.nextFloat()) + (N_ACT_PERSIST * nprev[i]);
			for (int j = 0; j < NM; j++) {
				I = I + (nprev[j] * w_n_n_exc[j][i] + (((nm[0]+nm[1]) * nprev[j] * w_n_n_inh[j][i]))); 
			}
			// event synaptic input
			for (int j = 0; j<E; j++) {
				for (int k = 0; k<NM; k++) {
					I = I + nm[k] * w_nm_n[k][i] * achne[j] * w_e_n[j][i];
				}
			}
			n[i] = activity (I, N_ACT_GAIN);	// activity of state neuron i
		}
		
		// update plastic weights with short-term plasticity rule. a spike occurs when an event occurs
		for (int i = 0; i< E; i++) {
			w_e_achne[i] = stp (w_e_achne[i], ACHNE_STP_GAIN, ACHNE_STP_DECAY, ACHNE_STP_MAX, event[i]);
		}
		for (int i = 0; i < E; i++) {
			for (int j = 0; j < NM; j++) {
				if (w_e_nm [i][j] > 0)
					w_e_nm [i][j] = stp (w_e_nm[i][j], NM_STP_GAIN, NM_STP_DECAY, NM_STP_MAX, event[i]); 
			}
		}
		
		// calculate state with maximum activity
		int maxState = 0;
		float maxActivity = n[0];
		for (int i = 1; i < N; i++) {
			if (n[i] > maxActivity) {
				maxActivity = n[i];
				maxState = i;
			}
		}
		
		// changes state if threshold is exceeded
		if(maxActivity > ACTION_SELECTION_THRESHOLD) {
			previousState = currentState;
			currentState = maxState;
			Log.d (TAG, "New state: " + currentState);
		}
	}

	
	// activity function: sigmoid (output increases more or less exponentially but slows with saturation)
	private float activity (float I, float g) {
		return (float) (1/(1+Math.exp(-g*I)));
	}

	
	// short term plasticity
	private float stp (float xin, float p, float tau, float max, boolean spk) {
		float x;
		if (spk)					// if there was a spike (an event occurred)
			x = p*xin;				// the current weight is multiplied by the amount by which it should be increased/decreased
		else 						// if there was no spike (no event)
			x = xin + (1-xin)/tau;	// x is equal to (one minus the current weight) divided by the recovery time constant (50 for roombas)
		return Math.min(max, x);
	}

}
