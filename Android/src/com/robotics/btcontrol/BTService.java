package com.robotics.btcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BTService {
	//Debugging
	private static final String TAG = "BTService";
	private static final Boolean D = true;
	
	//private static final String NAME = "BTControl";
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	
	private final BluetoothAdapter btAdapter;
	private final Handler mHandler;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;
	
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote device
	
    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BTService(Context context, Handler handler){
    	btAdapter = BluetoothAdapter.getDefaultAdapter();
    	mState = STATE_NONE;
    	mHandler = handler;
    }
    
    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state){
    	if(D){Log.d(TAG, "setState() "+ mState + " -> " + state);}
    	mState = state;
    	// Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    
    public synchronized int getState() {return mState;}
    
    public synchronized void start(){
    	if (D) {Log.d(TAG, "start");}
    	if(mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
    	if(mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
    	setState(STATE_NONE);
    }
    
    public synchronized void connect(BluetoothDevice btdevice){
    	if(D){Log.d(TAG, "connect to: " + btdevice);}
    	if (mState == STATE_CONNECTING){
    		if(mConnectThread != null){mConnectThread.cancel(); mConnectThread = null;}
    	}
    	if(mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
    	
    	mConnectThread = new ConnectThread(btdevice);
    	mConnectThread.start();
    	setState(STATE_CONNECTING);
    }
    
    public synchronized void connected(BluetoothSocket btsocket, BluetoothDevice device){
    	if(D){Log.d(TAG, "connected");}
    	if(mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
    	if(mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
    	
    	mConnectedThread = new ConnectedThread(btsocket);
    	mConnectedThread.start();
    	
    	Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
    	Bundle bundle = new Bundle();
    	bundle.putString(Constants.DEVICE_NAME, device.getName());
    	msg.setData(bundle);
    	mHandler.sendMessage(msg);
    	
    	setState(STATE_CONNECTED);
    }
    
    public synchronized void stop(){
    	if(D){Log.d(TAG, "stop");}
    	if(mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
    	if(mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
    	setState(STATE_NONE);
    }
    
    public void write(byte[] out){
    	ConnectedThread ct;
    	synchronized (this) {
    		if(mState != STATE_CONNECTED){return;}
    		ct = mConnectedThread;
    	}
    	ct.write(out);
    }
    
    public void write(int out){
    	ConnectedThread ct;
    	synchronized (this) {
    		if(mState != STATE_CONNECTED){return;}
    		ct = mConnectedThread;
    	}
    	ct.write(out);
    }
    
    private void connectionFailed(){
    	setState(STATE_NONE);
    	Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
    	Bundle bundle = new Bundle();
    	bundle.putString(Constants.TOAST, "Unable to connect device");
    	msg.setData(bundle);
    	mHandler.sendMessage(msg);
    }
    
    private void connectionLost(){
    	setState(STATE_NONE);
    	Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
    	Bundle bundle = new Bundle();
    	bundle.putString(Constants.TOAST, "Device connection was lost");
    	msg.setData(bundle);
    	mHandler.sendMessage(msg);
    }
    
	private class ConnectThread extends Thread{
		
		private final BluetoothSocket btSocket;
		private final BluetoothDevice btDevice;
		
		public ConnectThread(BluetoothDevice device) {
			// TODO Auto-generated constructor stub
			btDevice = device;
			BluetoothSocket tmp = null;
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			btSocket = tmp;
		}
		
		public void run(){
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");
			btAdapter.cancelDiscovery();
			
			try {
				btSocket.connect();
			} catch (IOException u) {
				
				connectionFailed();
				try {
					btSocket.close();
				} catch(IOException t) {
					Log.e(TAG, "unable to connect() socket during connection failure", t);
				}
				
				BTService.this.start();
				return;
			}
			
			synchronized (BTService.this){
				mConnectThread = null;
			}
			
			connected(btSocket, btDevice);
		}
		
		public void cancel() {
			// TODO Auto-generated method stub
			try {
				btSocket.close();
			} catch(IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
	
	private class ConnectedThread extends Thread{
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		
		public ConnectedThread(BluetoothSocket btsocket) {
			// TODO Auto-generated constructor stub
			Log.d(TAG,"create ConnectedThread");
			mmSocket = btsocket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			try {
				tmpIn = btsocket.getInputStream();
				tmpOut = btsocket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}
			
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		
		public void run(){
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;
			
			while(true) {
				try {
					bytes = mmInStream.read(buffer);
					 mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                     .sendToTarget();
				} catch(IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
		}
		
		public void write(int out) {
			// TODO Auto-generated method stub
			try{
				mmOutStream.write(out);
				mmOutStream.flush();
				//mHandler.obtainMessage(PWMSliderBT.MESSAGE_WRITE, -1, -1, out).sendToTarget();
			} catch(IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void write(byte[] buffer) {
			// TODO Auto-generated method stub
			try{
				mmOutStream.write(buffer);
				mmOutStream.flush();
				//mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
			} catch(IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			// TODO Auto-generated method stub
			try {
				mmSocket.close();
			} catch(IOException e){
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}