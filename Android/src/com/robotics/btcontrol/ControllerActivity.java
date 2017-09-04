package com.robotics.btcontrol;

import com.robotics.btcontrol.ControllerView.OnSpeedChangedListener;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ControllerActivity extends Activity implements View.OnTouchListener, View.OnClickListener{
	
	private static final String TAG = "FCA";
	private static boolean D = true;
	private boolean updaterIsEnabled = true;
	
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	
	private ToggleButton tbLed;
	private Button btGripA, btGripB, 
				   btWristA, btWristB, 
				   btElbowA, btElbowB, 
				   btShoulderA, btShoulderB, 
				   btBaseA, btBaseB, btConnect;
	private byte tempId = 0, tempCmd;
	private TextView tvConnectState;
	private boolean ledState = false;
	private ControllerView joystick;
	
	private ArrayAdapter<String> repliesArray;
	private String connectedDeviceName = null;
	private BluetoothAdapter btAdapter = null;
	private BTService btService = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		if (D) Log.d(TAG, "[  ON CREATE  ]");
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.sampleview);
		//setContentView(R.layout.finalscreen);
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		if (btAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		if (D) Log.d(TAG, "[  ON START  ]");
		
		if(!btAdapter.isEnabled()) {
			Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBT, REQUEST_ENABLE_BT);
		} else {
			if (btService == null) setup();
		}
	}

	@Override
	public synchronized void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (D) Log.d(TAG, "[  ON RESUME  ]");
		if (btService != null) {
			if (btService.getState() == BTService.STATE_NONE) {
				btService.start();
			} else if (btService.getState() == BTService.STATE_CONNECTED) {
				mHandler.removeCallbacks(TimerUpdater);
				mHandler.postDelayed(TimerUpdater, Constants.UPDATER_REPEAT);
			}
		}
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D) Log.d(TAG, "[  ON PAUSE  ]");
		mHandler.removeCallbacks(TimerUpdater);
	}
	
	@Override
	public synchronized void onDestroy() {
		super.onDestroy();
		if (D) Log.d(TAG, "[  ON DESTROY  ]");
		if (btService != null) btService.stop();
	}
	
	private void setup() {
		// TODO Auto-generated method stub
		if (D) Log.d(TAG, "setup()");
		initialize();
		btService = new BTService(this, mHandler);
	}

	private void initialize() {
		// TODO Auto-generated method stub
		if (D) Log.d(TAG, "initialize()");
		
		repliesArray = new ArrayAdapter<String>(this, R.layout.tvmessage);
		tvConnectState = (TextView) findViewById(R.id.tv_ConnectState);
		tbLed = (ToggleButton) findViewById(R.id.tb_led);
		btGripA = (Button) findViewById(R.id.button_GA);
		btGripB = (Button) findViewById(R.id.button_GB);
		btWristA = (Button) findViewById(R.id.button_WA);
		btWristB = (Button) findViewById(R.id.button_WB);
		btElbowA = (Button) findViewById(R.id.button_EA);
		btElbowB = (Button) findViewById(R.id.button_EB);
		btShoulderA = (Button) findViewById(R.id.button_SA);
		btShoulderB = (Button) findViewById(R.id.button_SB);
		btBaseA = (Button) findViewById(R.id.button_BA);
		btBaseB = (Button) findViewById(R.id.button_BB);
		btConnect = (Button) findViewById(R.id.button_Connect);
		joystick = (ControllerView) findViewById(R.id.cv_joystick);
		
		
		tbLed.setOnClickListener(this);
		btConnect.setOnClickListener(this);
		btGripA.setOnTouchListener(this);
		btGripB.setOnTouchListener(this);
		btWristA.setOnTouchListener(this);
		btWristB.setOnTouchListener(this);
		btElbowA.setOnTouchListener(this);
		btElbowB.setOnTouchListener(this);
		btShoulderA.setOnTouchListener(this);
		btShoulderB.setOnTouchListener(this);
		btBaseA.setOnTouchListener(this);
		btBaseB.setOnTouchListener(this);
		joystick.setOnSpeedChangedListener(new OnSpeedChangedListener(){
			@Override
			public void OnSpeedChanged(int lfSpd, int rtSpd, int param) {
				// TODO Auto-generated method stub
				if(param == 0) {
					if(lfSpd >= 0 && lfSpd <= 255) {
						sendCommand(Constants.idFL, (byte) lfSpd);
					} else if (lfSpd < 0 && lfSpd >= -255) {
						sendCommand(Constants.idRL, (byte) Math.abs(lfSpd));
					}
					if(rtSpd >= 0 && rtSpd <= 255) {
						sendCommand(Constants.idFR, (byte) rtSpd);
					} else if (rtSpd < 0 && rtSpd >= -255) {
						sendCommand(Constants.idRR, (byte) Math.abs(rtSpd));
					}
				} else if (param == ControllerView.FULL_STOP){
					sendCommand(Constants.idFS, (byte) 0);
				}
				//tvConnectState.setText("L: " + rtSpd + ", R: " + lfSpd);
			}
		});
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch(v.getId()) {
		case R.id.button_GA: case R.id.button_GB:
			tempId = Constants.gripId; break;
		case R.id.button_WA: case R.id.button_WB:
			tempId = Constants.wristId; break;
		case R.id.button_EA: case R.id.button_EB:
			tempId = Constants.elbowId; break;
		case R.id.button_SA: case R.id.button_SB:
			tempId = Constants.shoulderId; break;
		case R.id.button_BA: case R.id.button_BB:
			tempId = Constants.baseId; break;
		}
		
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			//v.requestFocusFromTouch();
			v.setPressed(true);
			switch(v.getId()){
			case R.id.button_GA: case R.id.button_WA:
			case R.id.button_EA: case R.id.button_SA:
			case R.id.button_BA:
				tempCmd = Constants.forwardCmd; break;
			case R.id.button_GB: case R.id.button_WB:
			case R.id.button_EB: case R.id.button_SB:
			case R.id.button_BB:
				tempCmd = Constants.reverseCmd; break;
			}
			
			sendCommand(tempId, tempCmd);
			return true;
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			v.setPressed(false);
			tempCmd = Constants.stopCmd;
			sendCommand(tempId, tempCmd);
			return true;
		}
		return false;
		
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.tb_led:
			if (ledState) {
				sendCommand(Constants.ledId, Constants.ledOffCmd);
				ledState = false;
			} else {
				sendCommand(Constants.ledId, Constants.ledOnCmd);
				ledState = true;
			}
			break;
		case R.id.button_Connect:
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			break;
		}
	}

	private void sendCommand(byte id, byte cmd) {
		// TODO Auto-generated method stub
		if(btService.getState() == BTService.STATE_CONNECTED && id != 0) {
			byte sendable[] = {Constants.token, id, cmd};
			btService.write(sendable);
			tempId = tempCmd = 0;
		}
	}

	private final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case Constants.MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BTService.STATE_CONNECTED:
					tvConnectState.setText("connected: ");
					tvConnectState.append(connectedDeviceName);
					repliesArray.clear();					
					break;
				case BTService.STATE_CONNECTING:
					tvConnectState.setText("connecting...");
					break;
				case BTService.STATE_LISTEN:
					break;
				case BTService.STATE_NONE:
					tvConnectState.setText("not connected");
					break;
				}
				break;
			case Constants.MESSAGE_WRITE:
				break;
			case Constants.MESSAGE_READ:
				byte[] readBuffer = (byte[]) msg.obj;
				String readMsg = new String(readBuffer, 0, msg.arg1);
				repliesArray.add(readMsg);
				break;
			case Constants.MESSAGE_DEVICE_NAME:
				connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
				Toast.makeText(getApplicationContext(), "Connected to: " + 
						connectedDeviceName, Toast.LENGTH_SHORT).show();
				removeCallbacks(TimerUpdater);
				postDelayed(TimerUpdater, 100);
				break;
			case Constants.MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST), 
						Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (D) Log.d(TAG, "[  onActivityResult: " + resultCode + "  ]" );
		switch (requestCode){
		case REQUEST_CONNECT_DEVICE:
			if (resultCode == Activity.RESULT_OK) {
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				BluetoothDevice device = btAdapter.getRemoteDevice(address);
				btService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				setup();
			} else {
				Log.d(TAG, "BT not enbled");
				Toast.makeText(this, "Bluetooth was enabled. Leaving app", Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private Runnable TimerUpdater = new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(D) Log.d(TAG, "[  Updater Running  ]");
			if(btService.getState() == BTService.STATE_CONNECTED && updaterIsEnabled){
				sendCommand(Constants.timerId, Constants.timerCmd);
				mHandler.postDelayed(this, Constants.UPDATER_REPEAT);
			} else if (btService.getState() == BTService.STATE_NONE) {
				if(D){Log.d(TAG, "[  callback removed  ]");}
				mHandler.removeCallbacks(this);
			}
		}
	};
}