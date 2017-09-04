package com.robotics.btcontrol;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceListActivity extends Activity{

	private static final String TAG = "DeviceListActivity";
	private static boolean D = true;
	
	public static final String EXTRA_DEVICE_ADDRESS = "device_address";
	
	private BluetoothAdapter btAdapter;
	private ArrayAdapter<String> pairedDevicesArray;
	private ArrayAdapter<String> newDeviceArray;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list_layout);
		
		setResult(Activity.RESULT_CANCELED);
		
		Button scanButton = (Button) findViewById(R.id.btn_Scan);
		scanButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doDiscovery();
				v.setVisibility(View.GONE);
			}
		});
		
		pairedDevicesArray = new ArrayAdapter<String>(this, R.layout.device_name);
		newDeviceArray = new ArrayAdapter<String>(this, R.layout.device_name);
		
		ListView pairedListView = (ListView) findViewById(R.id.lv_pairedDevices);
		pairedListView.setAdapter(pairedDevicesArray);
		pairedListView.setOnItemClickListener(mDeviceClickListener);
		
		ListView newDeviceListView = (ListView) findViewById(R.id.lv_newDevices);
		newDeviceListView.setAdapter(newDeviceArray);
		newDeviceListView.setOnItemClickListener(mDeviceClickListener);
		
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);
		
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		
		Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
		
		if(pairedDevices.size() > 0) {
			findViewById(R.id.tv_pairedDevices).setVisibility(View.VISIBLE);
			for (BluetoothDevice device: pairedDevices) {
				pairedDevicesArray.add(device.getName() + "\n" + device.getAddress());
			}
		} else {
			pairedDevicesArray.add("No devices have been paired");
		}
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		if(btAdapter != null) {
			btAdapter.cancelDiscovery();
		}
		
		this.unregisterReceiver(mReceiver);
	}
	
	private void doDiscovery() {
		// TODO Auto-generated method stub
		if (D) {Log.d(TAG, "doDiscovery()");}
		
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.scanning);
		
		findViewById(R.id.tv_newDevices).setVisibility(View.VISIBLE);
		
		if(btAdapter.isDiscovering()) {btAdapter.cancelDiscovery();}
		
		btAdapter.startDiscovery();
	}
	
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			// TODO Auto-generated method stub
			btAdapter.cancelDiscovery();
			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length() - 17);
			
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
			
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	};
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			
			if(BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
					newDeviceArray.add(device.getName() + "\n" + device.getAddress());
				}
				
			} else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.select_device);
				if(newDeviceArray.getCount() == 0) {
					newDeviceArray.add("No devices found");
				}
			}
		}
	};
}