/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.sync_player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.example.otherFunction.RadioGroup1;

/**
 * This is the main Activity that displays the current chat session.
 */
public class ClientRoomSetting extends Activity {
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	
	//MsgHandle
	private static final int RECEIVE_MSG_ASSIGN_DATA=1;
	private static final int START_MESSAGE=2;

	// Layout Views
	private TextView mTitle;	
	//private Button startPlayBtn;
	private Button cancelBtn;
	private Button readyBtn;
	private TextView roleInfo;
	
	private ViewFlipper myViewFilper;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	private String selectedAdd = "";
	private static String SELECTED_ADDRESS = "SELECTADD";
	
	private static final String LOCAL_VIDEO_PATH="localPath";
    private String filePath="";
    
    /** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;	
	//Target we publish for clients to send messages to IncomingHandler	 
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		// setContentView(R.layout.main);
		setContentView(R.layout.client_role_info);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.custom_title);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);
		
		roleInfo=(TextView) findViewById(R.id.roomInfo);		
		myViewFilper = (ViewFlipper) findViewById(R.id.viewFlipper1);
		
		//setUpButton to click
		setupBtn();

		// get previous selecteed address
		Bundle Addextra = getIntent().getExtras();
		if (Addextra != null) {
			selectedAdd = Addextra.getString(SELECTED_ADDRESS);
		}

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}// onCreate

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}// onStart

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		/*
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
				try {
					// connect previous selected devices				
					connectDevice(selectedAdd, false);
				} catch (Exception e) {
					// TODO: handle exception
				}//try connect to remote device				
			}
		}*/
		doBindService();
	}// onResume

	private void setupChat() {
		Log.d(TAG, "setupChat()");
		
		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);
		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
		
		startService(new Intent(ClientRoomSetting.this,ClientSideService.class));
		doBindService();
		//requestConnect(selectedAdd);
	}

	/****************************************************
	 * setUpBtn
	 ****************************************************/
	private void setupBtn() {
		/*
		 * startPlayBtn= (Button) findViewById(R.id.clientPlay_Btn);
		 * startPlayBtn.setOnClickListener(new OnClickListener() { public void
		 * onClick(View v) { //String message = "start"; //sendMessage(message);
		 * mChatService.stop(); Intent intent = new
		 * Intent(ClientRoomSetting.this,ClientSyncPlayer.class);
		 * intent.putExtra(SELECTED_ADDRESS, selectedAdd);
		 * startActivity(intent); } });
		 */
		readyBtn=(Button) findViewById(R.id.ready_Btn);
		readyBtn.setOnClickListener(new OnClickListener() { 
			public void  onClick(View v) { 
				//String message = "start"; //sendMessage(message);
				//mChatService.stop(); 
				Intent intent = new	Intent(ClientRoomSetting.this,ClientSyncPlayer.class);
				intent.putExtra(SELECTED_ADDRESS, selectedAdd);
				intent.putExtra(LOCAL_VIDEO_PATH, filePath);
				startActivity(intent); 
				} 
		});
		
		cancelBtn=(Button) findViewById(R.id.cancel2_Btn);
		cancelBtn.setOnClickListener(new OnClickListener() { 
			public void onClick(View v) {
				//sourceSelect_changeView();
			  } 
			});			 
	}//setUpBtn

	/****************************************************
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 ****************************************************/

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
		stopService(new Intent(ClientRoomSetting.this,ClientSideService.class));
	}

	/*******************************************************************
	 * for bluetooth discovery
	 *******************************************************************/
	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/****************************************************************************
	 * Sends a message. 
	 * @param message
	 *            A string of text to send.
	 ****************************************************************************/
	private void sendMessage_to_remote(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			// mOutStringBuffer.setLength(0);
			// mOutEditText.setText(mOutStringBuffer);
		}
	}

	// The action listener for the EditText widget, to listen for the return key
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				sendMessage(message);
			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);
					// mConversationArrayAdapter.clear();
					break;
				case BluetoothChatService.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				// mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				Log.e("test", readMessage);				
				//receivedMsgHandler(readMessage);
				
				// mConversationArrayAdapter.add(mConnectedDeviceName+":  " +readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// connectDevice(data, true);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// connectDevice(data, false);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	/*******************************************************************
	 * Connect to a remote device
	 *******************************************************************/

	private void connectDevice(String data, boolean secure) {
		// Get the device MAC address
		// String address =
		// data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		String address = data;
		// Get the BLuetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}	
	/*******************************************************************
     * show up client role information
     *******************************************************************/
	private void showRoleInfo(String info){
		String [] roleMsg;
		String displayText="";
		roleMsg=info.split(",");
		filePath=roleMsg[5];
		
		if(roleMsg[1].equals("2")){			
			myViewFilper.setDisplayedChild(1);			
		}else if(roleMsg[1].equals("1")){			
			myViewFilper.setDisplayedChild(0);
		}
		String[] roles={"video","music only","left sound track","right sound track"};
		displayText=displayText+"Room Name:"+roleMsg[4]+"\n";
		displayText=displayText+"Role Assigned:"+roles[ Integer.parseInt(roleMsg[2]) ]+"\n";
		displayText=displayText+"Is connected:"+roleMsg[3]+"\n";
		displayText=displayText+"File path:"+roleMsg[5]+"\n";
		roleInfo.setText(displayText);
	}
	
	/*******************************************************************
     * handle what to do when receive msg
     *******************************************************************/
	private void receivedMsgHandler(String Msg){
		String [] recvMsg=Msg.split(",");
		String cmd=recvMsg[0];
		
		if (cmd.contains(RECEIVE_MSG_ASSIGN_DATA+"")){
			showRoleInfo(Msg);
		}//show AssignedData 
		
		else if (cmd.contains(START_MESSAGE+"")){
			//mChatService.stop();
			doUnbindService();
			
			Intent intent = new Intent(ClientRoomSetting.this,ClientSyncPlayer.class);
			intent.putExtra(SELECTED_ADDRESS, selectedAdd);
			intent.putExtra(LOCAL_VIDEO_PATH, filePath);
			startActivity(intent);			
		}//jump to ClientSyncPlayer			
	}//end of receivedMsgHandler
	/********************************************************************
   	 * Handler of incoming messages from service.
   	 *******************************************************************/
   	class IncomingHandler extends Handler {
   	    @Override
   	    public void handleMessage(Message msg) {
   	        switch (msg.what) {
   	            case ClientSideService.MSG_SET_VALUE:
   	                //mCallbackText.setText("Received from service: " + msg.arg1);   	            	
   	                break;
   	                
	   	         case ClientSideService.MSG_RETURN_RECEIVE_DATA:	   	        	
	   	        	String receivedMsg=(String) msg.obj;
	   	        	Log.e("IncimingHandler",receivedMsg);
	   	        	receivedMsgHandler(receivedMsg);
	             break;
             
   	            default:
   	                super.handleMessage(msg);
   	        }
   	    }
   	}
   	/*********************************************************************
   	 * Class for interacting with the main interface of the service.
   	 ********************************************************************/
   	private ServiceConnection mConnection = new ServiceConnection() {
   	    public void onServiceConnected(ComponentName className,
   	            IBinder service) {
   	        // This is called when the connection with the service has been
   	        // established, giving us the service object we can use to
   	        // interact with the service.  We are communicating with our
   	        // service through an IDL interface, so get a client-side
   	        // representation of that from the raw service object.
   	        mService = new Messenger(service);
   	        //mCallbackText.setText("Attached.");

   	        // We want to monitor the service for as long as we are
   	        // connected to it.
   	        try {
   	            Message msg = Message.obtain(null,
   	            		ClientSideService.MSG_REGISTER_CLIENT);
   	            msg.replyTo = mMessenger;
   	            mService.send(msg);

   	            // Give it some value as an example.
   	            msg = Message.obtain(null,
   	            		ClientSideService.MSG_SET_VALUE, this.hashCode(), 0);
   	            mService.send(msg);
   	        } catch (RemoteException e) {
   	            
   	        }
   	        // As part of the sample, tell the user what happened.
   	        Toast.makeText(ClientRoomSetting.this, "ClientServiceConnected" ,Toast.LENGTH_SHORT).show();
   	        requestConnect(selectedAdd);
   	    }

   	    public void onServiceDisconnected(ComponentName className) {
   	        // This is called when the connection with the service has been
   	        // unexpectedly disconnected -- that is, its process crashed.
   	        mService = null;
   	        //mCallbackText.setText("Disconnected.");
   	        Toast.makeText(ClientRoomSetting.this, "ClientServiceConnected",Toast.LENGTH_SHORT).show();
   	    }
   	};
   	/********************************************************************
   	 * bind and unBind ClientSideService
   	 ********************************************************************/

   	void doBindService() {
   	    // Establish a connection with the service.  We use an explicit
   	    // class name because there is no reason to be able to let other
   	    // applications replace our component.
   	    bindService(new Intent(ClientRoomSetting.this,ClientSideService.class), mConnection, Context.BIND_AUTO_CREATE);
   	    mIsBound = true;
   	    //mCallbackText.setText("Binding.");
   	}   	
   	
   	void doUnbindService() {
   	    if (mIsBound) {
   	        // If we have received the service, and hence registered with
   	        // it, then now is the time to unregister.
   	        if (mService != null) {
   	            try {
   	                Message msg = Message.obtain(null,ClientSideService.MSG_UNREGISTER_CLIENT);
   	                msg.replyTo = mMessenger;
   	                mService.send(msg);
   	            } catch (RemoteException e) {
   	                // There is nothing special we need to do if the service
   	                // has crashed.
   	            }
   	        }

   	        // Detach our existing connection.
   	        unbindService(mConnection);
   	        mIsBound = false;
   	    }
   	}
   	/*********************************************************************************
   	 * abstract sending method
   	 *********************************************************************************/
   	private void sendMessage(String message) {
   		send2Service(message,ClientSideService.MSG_SENT_DATA);
   	}
   	/*********************************************************************************
   	 * abstract connect method
   	 *********************************************************************************/
   	private void requestConnect(String remoteAdd){
   		send2Service(remoteAdd,ClientSideService.MSG_CONNECT_DEVICE);
   	}
   	/********************************************************************************
	 * send Msg to ClientSide Service
	 *******************************************************************************/
   	private void send2Service(String Msg,int msgType){
   		Message msg = Message.obtain(null,msgType,-1,-1,Msg);
   	    msg.replyTo = mMessenger;
   	    try {
   			mService.send(msg);
   		} catch (RemoteException e) {
   			// TODO Auto-generated catch block
   			e.printStackTrace();
   		}//try   		
   	}  
	/*******************************************************************
     * 
     *******************************************************************/
}// ClientRoomSetting Activity
