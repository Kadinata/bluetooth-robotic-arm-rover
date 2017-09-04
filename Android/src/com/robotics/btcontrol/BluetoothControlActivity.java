package com.robotics.btcontrol;

import java.util.Random;

import android.app.Activity;
import android.os.Bundle;

//import android.widget.Toast;

public class BluetoothControlActivity extends Activity {
    /** Called when the activity is first created. */
    
	Random stuff = new Random();
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.buttons);

    }
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		finish();
	}
    
    
}