package com.robotics.btcontrol;

public class Constants {
	
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int UPDATER_REPEAT = 100;
	
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	
	public static final byte token 		= '#';
	public static final byte gripId 	= 48;
	public static final byte wristId 	= 49;
	public static final byte elbowId 	= 50;
	public static final byte shoulderId = 51;
	public static final byte baseId 	= 52;
	public static final byte ledId 		= 53;
	
	public static final byte idFL = 54;
	public static final byte idFR = 55;
	public static final byte idRL = 56;
	public static final byte idRR = 57;
	public static final byte idSL = 58;
	public static final byte idSR = 59;
	public static final byte idFS = 60;
	
	public static final byte timerId 	= 61;
	
	public static final byte forwardCmd = 'f';
	public static final byte reverseCmd = 'r';
	public static final byte stopCmd 	= 's';
	public static final byte ledOnCmd 	= 'h';
	public static final byte ledOffCmd 	= 'l';
	public static final byte timerCmd 	= '$';
	public static final byte empty = 0;
}
