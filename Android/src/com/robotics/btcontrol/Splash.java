package com.robotics.btcontrol;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class Splash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);		
		setContentView(R.layout.main);
		Thread timer = new Thread(){
			public void run(){
				try{
					sleep(2500);
				}catch(InterruptedException u){
					u.printStackTrace();
				}finally{
					//Intent myIntent = new Intent(Splash.this, PWMSliderBT.class);
					//Intent myIntent = new Intent(Splash.this, ArmControlTest.class);
					//Intent myIntent = new Intent(Splash.this, FinalController.class);
					//Intent myIntent = new Intent(Splash.this, RoverControlTest.class);
					Intent myIntent = new Intent(Splash.this, ControllerActivity.class);
					startActivity(myIntent);
				}
			}
		};
		timer.start();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		finish();
	}
	
}
