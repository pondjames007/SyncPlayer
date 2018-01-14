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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RadioButton;

import com.example.otherFunction.FileDialog;
import com.example.otherFunction.RoomNameDialog;
/**
 * This is the main Activity that displays the current chat session.
 */
public class ServerRoomManage extends Activity {
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_CLOSE = 6;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	private static final int REQUEST_LOAD = 4;
	
	//MsgHandle
	private static final int RECEIVE_MSG_ASSIGN_DATA=1;
	private static final int START_MESSAGE=2;

	// Layout Views
	private TextView mTitle;
	private Button startPlayBtn;
	private Button saveBtn;
	private RadioButton fileRadio;
	private RadioButton youtuRadio;
	private Button localBrowseBtn;
	private Button youtubeBrowseBtn;
	private RadioGroup myRadioGroup;

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

	private ArrayAdapter<String> connectedClients;
	private ArrayList<String> connectedClientArray;
	private ListView list;
	private static final String ROOM_SETTING_PROFILE = "roomProfile2";
	private static final String INPUT_ROOM_NAME = "roomName";
	private static final String LOCAL_VIDEO_PATH="localPath";
	private String roomName;
	private String filePath;
	
	private int closeJumpIndicate=0;
	private Timer timerConnect;
	
	private ListOfClientAdapter listAdapter;
	private ArrayList<Map<String, String>> listOfClient;
	
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
		setContentView(R.layout.server_manage_list);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		// set up list of client and other UI
		setupClientList();
		getRoomName();
		setupRadioButton();
		setUpBtn();

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}// oncreate

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
	}//onStart

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		/*if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}*/
		doBindService();
	}

	/***********************************************************
	 * set up the list of client connected
	 ************************************************************/
	private void setupClientList() {
		/*
		 * connectedClientArray=new ArrayList<String>(); connectedClients=new
		 * ArrayAdapter
		 * <String>(this,android.R.layout.simple_list_item_1,connectedClientArray
		 * );
		 * 
		 * list = (ListView) findViewById(R.id.Server_manage_list);
		 * list.setAdapter(connectedClients); list.setOnItemClickListener(new
		 * OnItemClickListener() {
		 * 
		 * public void onItemClick(AdapterView<?> arg0, View view, int
		 * index,long arg3) {
		 * //Toast.makeText(view.getContext(),foundDeviceAddress
		 * .get(index),Toast.LENGTH_SHORT).show(); //Intent intent = new
		 * Intent(ClientRoomList.this,ClientRoomSetting.class);
		 * //intent.putExtra(SELECTED_ADDRESS, foundDeviceAddress.get(index));
		 * //startActivity(intent); }//onItemClick });
		 */
		list = (ListView) findViewById(R.id.Server_manage_list);

		// 配置适配器
		listAdapter = new ListOfClientAdapter(this, getData()); // 布局里的控件id
		// 添加并且显示
		list.setAdapter(listAdapter);
		listOfClient=new ArrayList<Map<String, String>>();

		// 添加点击
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				setTitle("点击第" + arg2 + "个项目");
				Toast.makeText(ServerRoomManage.this, "点击第" + arg2 + "个项目",
						Toast.LENGTH_LONG).show();
			}
		});

		// 添加长按点击
		/*
		 * list.setOnCreateContextMenuListener(new OnCreateContextMenuListener()
		 * {
		 * 
		 * @Override public void onCreateContextMenu(ContextMenu menu, View v,
		 * ContextMenuInfo menuInfo) { // menu.setHeaderIcon(R.drawable.icon);
		 * //字义图片样式 menu.setHeaderTitle("长按菜单选项:"); menu.add(1, 0, 0, "编辑");
		 * menu.add(0, 1, 0, "删除"); } });
		 */

	}// setupClientList

	/***********************************************************
	 * set up the chat service
	 ************************************************************/
	private void setupChat() {
		Log.d(TAG, "setupChat()");	
		ensureDiscoverable();

		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);
		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
		
		startService(new Intent(ServerRoomManage.this,ServerSideService.class));
	}
	/*******************************************************************
	 * setUp Button 
	 *******************************************************************/
	private void setUpBtn(){
		startPlayBtn = (Button) findViewById(R.id.server_start_play);
		startPlayBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {	
				assignedData();
				sendStartMsg();

				doUnbindService();
				
				Intent intent = new Intent(ServerRoomManage.this,ServerSyncPlayer.class);
				intent.putExtra(LOCAL_VIDEO_PATH, filePath);
				startActivity(intent);					
			}
		});//startPlayBtn onClickListener

		saveBtn = (Button) findViewById(R.id.button_save_setting);
		saveBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// ensureDiscoverable();
				assignedData();				
//				String content = "TEMPROOMNAME" + ","
//						+ connectedClientArray.toString();
//				String fileName = ROOM_SETTING_PROFILE;
//				FileOutputStream writer;
//				try {
//					writer = openFileOutput(fileName, Context.MODE_PRIVATE);
//					writer.write(content.getBytes());
//					writer.close();
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

			}// onClick
		});// save onClicklistener
		
		localBrowseBtn = (Button) findViewById(R.id.local_BrowseBtn);
		localBrowseBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {				
				Intent intent = new Intent(getBaseContext(), FileDialog.class);
                intent.putExtra(FileDialog.START_PATH, "/sdcard");
                
                //can user select directories or not
                intent.putExtra(FileDialog.CAN_SELECT_DIR, true);                                
                
                //startActivityForResult(intent, REQUEST_SAVE);
                startActivityForResult(intent, REQUEST_LOAD);
			}
		});//localBrowseBtn
		
		youtubeBrowseBtn = (Button) findViewById(R.id.youtube_BrowseBtn);
		youtubeBrowseBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {				
				//mChatService.stop();
				
				//send Msg to serverSideService
				String cmdMsg="test12345";
				Message msg = Message.obtain(null,ServerSideService.MSG_SENT_DATA,-1,-1,cmdMsg);
	            msg.replyTo = mMessenger;
	            try {
					mService.send(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}//try
			}
		});//youtubeBrowseBtn
		
	}//setUp button
	/*******************************************************************
	 * setUpRadioButton
	 *******************************************************************/
	private void setupRadioButton() {
		fileRadio=(RadioButton) findViewById(R.id.LocalRadio);
		youtuRadio=(RadioButton) findViewById(R.id.youtubeRadio);
		//RadioGroup myRadioGroup;
		myRadioGroup=new RadioGroup(); 
				
		myRadioGroup.addRadioButton(fileRadio);
		myRadioGroup.addRadioButton(youtuRadio);
	}

	/*******************************************************************
	 * Activity life cycle
	 * 
	 * @see android.app.Activity#onPause()
	 *******************************************************************/

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
		//doUnbindService();
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
		stopService(new Intent(ServerRoomManage.this,ServerSideService.class));
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

	/*******************************************************************
	 * Sends a message.	
	 * @param message
	 *            A string of text to send.
	 *******************************************************************/
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

	/********************************************************************************
	 * The Handler that gets information back from the BluetoothChatService
	 *******************************************************************************/

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
				// mConversationArrayAdapter.add(mConnectedDeviceName+":  " +
				// readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				String clientArrayInfo = "";
				Random myRandom=new Random();
				int hashNum=myRandom.nextInt();
				listAdapter.addItem(mConnectedDeviceName,""+hashNum);
				
				Map<String, String> map = new HashMap<String, String>();				
				map.put("id", mConnectedDeviceName+hashNum);
				map.put("itemTitle", mConnectedDeviceName);
				listOfClient.add(map);				
				
//				clientArrayInfo = connectedClientArray.toString();
//				if (!clientArrayInfo.contains(mConnectedDeviceName)) {
//					connectedClientArray.add(mConnectedDeviceName);
//				}
//				connectedClients.notifyDataSetChanged();
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;			
			case MESSAGE_CLOSE:				
				break;
			}//switch
		}
	};

	/********************************************************************************
	 * onActivityResult
	 *******************************************************************************/

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, false);
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
			break;
		case REQUEST_LOAD:
			filePath = data.getStringExtra(FileDialog.RESULT_PATH);
			//Log.e("path",filePath);
			break;
		}//switch
	}//end of ActivityResult

	/********************************************************************************
	 * Connect to Bluetooth Device
	 *******************************************************************************/

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BLuetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}	

	/********************************************************************************
	 * get room Name
	 *******************************************************************************/
	private void getRoomName() {
		Bundle roomNameExtra = getIntent().getExtras();
		roomName = roomNameExtra.getString(INPUT_ROOM_NAME);
		Log.e("roomName", roomName);
	}

	/*******************************************************************************
	 * set the data for the client list	 
	 *******************************************************************************/
	private List<Map<String, String>> getData() {
		// 组织数据源
		List<Map<String, String>> mylist = new ArrayList<Map<String, String>>();
		return mylist;
	}

	/*******************************************************************************
	 * Custom RadioGroup
	 *******************************************************************************/
	public class RadioGroup implements RadioButton.OnCheckedChangeListener {
		private CompoundButton checkedButton = null;

		public void addRadioButton(RadioButton rb) {
			rb.setOnCheckedChangeListener(this);
			if (checkedButton == null) {
				checkedButton = rb;
			}
		}

		public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
			if (isChecked) {
				checkedButton.setChecked(false);
				checkedButton = buttonView;
			}
		}

		public CompoundButton getCheckedRadioButton() {
			return checkedButton;
		}
	}
	
	/*******************************************************************************
	 * get Assign Data to client
	 *******************************************************************************/
	private void assignedData(){
		Map<String,Integer> clientSel;
		Map<String,Boolean> clientCheck;
		clientSel=listAdapter.getSelectValues();
		clientCheck=listAdapter.getCheck();
		int SourceView=1; 
		
		if(myRadioGroup.getCheckedRadioButton().getText().toString().contains("Local")){
			SourceView=1;
		}else if(myRadioGroup.getCheckedRadioButton().getText().toString().contains("Youtube")){
			SourceView=2;
		}
		
//		Log.e("cleintsel",listOfClient.toString());
//		Log.e("cleintCheck",clientSel.toString());
		Log.e("CheckRadio",myRadioGroup.getCheckedRadioButton().getText().toString());
		
		for (Map<String,String> clientInfo: listOfClient) {
			String AssignMsg="";
			AssignMsg=AssignMsg+RECEIVE_MSG_ASSIGN_DATA+",";
			AssignMsg=AssignMsg+SourceView+","; //client source browse
			AssignMsg=AssignMsg+clientSel.get(clientInfo.get("id"))+","; //client role
			AssignMsg=AssignMsg+clientSel.get(clientCheck.get("id"))+","; //client checked
			AssignMsg=AssignMsg+roomName+",";
			AssignMsg=AssignMsg+filePath+",";
			//Log.e("assignMsg",AssignMsg);
			//sendMessage(AssignMsg);
			
			//send Msg to serverSide Service
			sendMessage(AssignMsg);			
		}
	}//end of function	
	/*******************************************************************
     * start play msg
     *******************************************************************/
	private void sendStartMsg(){
		String startMsg="";
		startMsg=startMsg+START_MESSAGE+",";
		sendMessage(startMsg);
	}//end of function	
	/********************************************************************************
   	 * connect failed handler and repeat requesting the connection
   	 *******************************************************************************/
   	private void repeatConnectRequest(){
   		timerConnect = new Timer();
   		timerConnect.schedule(new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if(closeJumpIndicate==1){
					closeJumpIndicate=0;
					Intent intent = new Intent(ServerRoomManage.this,ServerSyncPlayer.class);
					intent.putExtra(LOCAL_VIDEO_PATH, filePath);
					startActivity(intent);
				}
			}//run TimerTask
   		}, 3000, 5000);   		
   	}//end of connectFailHandler 
   	/********************************************************************
   	 * Handler of incoming messages from service.
   	 *******************************************************************/
   	class IncomingHandler extends Handler {
   	    @Override
   	    public void handleMessage(Message msg) {
   	        switch (msg.what) {
   	            case ServerSideService.MSG_SET_VALUE:
   	                //mCallbackText.setText("Received from service: " + msg.arg1);   	            	
   	                break;
   	                
	   	         case ServerSideService.MSG_RETURN_DEVICE_NAME:
	   	        	Log.e("serverRoomMsg","in serverRoom");
	             	String DeviceName=(String) msg.obj;
					Random myRandom=new Random();
					int hashNum=myRandom.nextInt();
					listAdapter.addItem(DeviceName,""+hashNum);
					
					Map<String, String> map = new HashMap<String, String>();				
					map.put("id", DeviceName+hashNum);
					map.put("itemTitle", DeviceName);
					listOfClient.add(map);
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
   	                    ServerSideService.MSG_REGISTER_CLIENT);
   	            msg.replyTo = mMessenger;
   	            mService.send(msg);

   	            // Give it some value as an example.
   	            msg = Message.obtain(null,
   	            		ServerSideService.MSG_SET_VALUE, this.hashCode(), 0);
   	            mService.send(msg);
   	        } catch (RemoteException e) {
   	            // In this case the service has crashed before we could even
   	            // do anything with it; we can count on soon being
   	            // disconnected (and then reconnected if it can be restarted)
   	            // so there is no need to do anything here.
   	        }

   	        // As part of the sample, tell the user what happened.
   	        Toast.makeText(ServerRoomManage.this, "connected" ,Toast.LENGTH_SHORT).show();
   	    }

   	    public void onServiceDisconnected(ComponentName className) {
   	        // This is called when the connection with the service has been
   	        // unexpectedly disconnected -- that is, its process crashed.
   	        mService = null;
   	        //mCallbackText.setText("Disconnected.");

   	        // As part of the sample, tell the user what happened.
   	        Toast.makeText(ServerRoomManage.this, "disconnected",Toast.LENGTH_SHORT).show();
   	    }
   	};
   	/********************************************************************
   	 * bind and unBind serverSideService
   	 ********************************************************************/

   	void doBindService() {
   	    // Establish a connection with the service.  We use an explicit
   	    // class name because there is no reason to be able to let other
   	    // applications replace our component.
   	    bindService(new Intent(ServerRoomManage.this,ServerSideService.class), mConnection, Context.BIND_AUTO_CREATE);
   	    mIsBound = true;
   	    //mCallbackText.setText("Binding.");
   	}   	
   	
   	void doUnbindService() {
   	    if (mIsBound) {
   	        // If we have received the service, and hence registered with
   	        // it, then now is the time to unregister.
   	        if (mService != null) {
   	            try {
   	                Message msg = Message.obtain(null,ServerSideService.MSG_UNREGISTER_CLIENT);
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
   		send2Service(message);
   	}
   	/********************************************************************************
	 * send Msg to serverSide Service
	 *******************************************************************************/
   	private void send2Service(String Msg){
   		Message msg = Message.obtain(null,ServerSideService.MSG_SENT_DATA,-1,-1,Msg);
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

}// ServerRoomManage

