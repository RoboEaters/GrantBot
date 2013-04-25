package com.cargocult.grantbot;
// to be subsumed into other classes as necessary


public class VictimClass {
	
	
	public VictimClass(){
		
	}
	
	
	// spotVictim {
	
	// get thermal sensor info
	// get face info
	//
	// check to see if either exceed threshold (different for each
	// depending on reliability
	//
	// if (threshold exceeded)
	//
	// figure out angle to victim using camera mount angle (if swiveling)
	// and/or visual field in camera
	//
	// notify parent method
	// return event boolean or confidence level(s)?
	// }
	
	
	// investigateVictim {
	//
	// use degrees and distance to victim to get to optimal range for
	// all sensors
	//
	// stay at range until
	//		- confidence threshold(s) are exceeded
	//		- victim counter time expires
	//		- neural network gets bored
	//
	// send victim location
	// or just victim confidence numbers, which whill be
	// passed along to laptop, along with the odometry
	// pose info (which well be being sent anyways)
	// }
	
	
	// boolean or confidence level used by laptop to
	// possibly mark victim on individual (?) or shared 
	// maps
	
	// if camera recognizes same victim & surroundings (tested at laptop)
	// the victim itself would be a great kandmark for ratslam
}
