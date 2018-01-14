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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.example.sync_player.ServerRoomManage.IncomingHandler;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
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
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class ServerSyncPlayer extends Activity {
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

    // Layout Views
    private TextView mTitle;   
    
    private Button btnplay, btnstop, btnpause;
	private SurfaceView surfaceView;
	private MediaPlayer mediaPlayer;
	private int position;

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
    
    private static final String LOCAL_VIDEO_PATH="localPath";
    private String localPath="";
    
    /** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	
	//Target we publish for clients to send messages to IncomingHandler	 
	final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        //setContentView(R.layout.main);
        setContentView(R.layout.sync_server_player);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        
        getFilepath();
        setUpVideoView();

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }//onCreate

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
        	//setUpVideoView();
            if (mChatService == null) 
            	setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        /*
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
        */
        doBindService();
    }
    
    /***********************************************************
	 * Set up chat service
	 ************************************************************/
    private void setupChat() {
        Log.d(TAG, "setupChat()");        

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
    /***********************************************************
	 * SetUp video View and controller button
	 ************************************************************/
    
    private void setUpVideoView(){
    	btnplay = (Button)findViewById(R.id.btnplay);
		btnstop = (Button) findViewById(R.id.btnstop);
		btnpause = (Button) findViewById(R.id.btnpause);
		
		/////////////////////////////////////////////////////////////////////
		
		final Timer timer = new Timer();
				
		btnstop.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {              	
            	Calendar calendar = Calendar.getInstance();
            	//long tmp = calendar.getTimeInMillis()+2000;
            	/*long now = 0;
            	long tmp = 0;
            	if(client.requestTime(ntpaddr, 30000)){
            		now = client.getNtpTime() + System.nanoTime()/600 - client.getNtpTimeReference();
            		tmp = now + 2000000;
            	}
            	Toast.makeText(getApplicationContext(), Long.toString(now),
                        Toast.LENGTH_SHORT).show();
            	String time = Long.toString(tmp);
            	*/
            	long settime = calendar.getTimeInMillis() + 600;
            	String time = Long.toString(settime);
            	sendMessage("stop"+time);
            	//calendar = Calendar.getInstance();
            	/*if(client.requestTime(ntpaddr, 30000)){
            		now = client.getNtpTime() + System.nanoTime()/600 - client.getNtpTimeReference();
            	}*/
            	calendar = Calendar.getInstance();
            	timer.schedule(new TimerTask(){
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (mediaPlayer.isPlaying()) {
		    				mediaPlayer.stop();				
		    			}
					}
            	}, settime-calendar.getTimeInMillis());
            }
        });//btnstop
		btnplay.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {     
            	play();
            	mediaPlayer.pause();
            	Calendar calendar = Calendar.getInstance();
            	//long tmp = calendar.getTimeInMillis()+2000;
            	/*long now = 0;
            	long tmp = 0;
            	if(client.requestTime(ntpaddr, 30000)){
            		now = client.getNtpTime() + System.nanoTime()/600 - client.getNtpTimeReference();
            		tmp = now + 2000000;
            	}
            	String time = Long.toString(tmp);
            	*/
            	long settime = calendar.getTimeInMillis() + 600;
            	String time = Long.toString(settime);
                sendMessage("play"+time);
            	Log.e("test", "play Video");
            	//calendar = Calendar.getInstance();
            	/*if(client.requestTime(ntpaddr, 30000)){
            		now = client.getNtpTime() + System.nanoTime()/600 - client.getNtpTimeReference();
            	}*/
            	calendar = Calendar.getInstance();
            	timer.schedule(new TimerTask(){
					@Override
					public void run() {
						// TODO Auto-generated method stub
						mediaPlayer.start();
					}
            	}, settime-calendar.getTimeInMillis());
            }
        });//btnplay
		btnpause.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Calendar calendar = Calendar.getInstance();
            	//long tmp = calendar.getTimeInMillis()+2000;
            	/*long now = 0;
            	long tmp = 0;
            	if(client.requestTime(ntpaddr, 30000)){
            		now = client.getNtpTime() + System.nanoTime()/600 - client.getNtpTimeReference();
            		tmp = now + 2000000;
            	}
            	String time = Long.toString(tmp);
            	*/
            	final int videoTime = mediaPlayer.getCurrentPosition();
            	long settime = calendar.getTimeInMillis() + 600;
            	String time = Long.toString(settime);
            	sendMessage(time+"pause"+Integer.toString(videoTime));
            	//calendar = Calendar.getInstance();
            	/*if(client.requestTime(ntpaddr, 30000)){
            		now = client.getNtpTime() + System.nanoTime()/600 - client.getNtpTimeReference();
            	}*/
            	calendar = Calendar.getInstance();
            	timer.schedule(new TimerTask(){
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if (mediaPlayer.isPlaying()) {
		    				mediaPlayer.pause();				
		    			} else {
		    				mediaPlayer.seekTo(videoTime);
		    				mediaPlayer.start();				
		    			}
					}
            	}, settime-calendar.getTimeInMillis());            	
            }
        });//btnpause
		
		///////////////////////////////////////////////////////////////////////////////////////

		mediaPlayer = new MediaPlayer();
		surfaceView = (SurfaceView) this.findViewById(R.id.surfaceView);
		
		
		//getClientIP
		//Bundle extras = getIntent().getExtras();
		//clientIP = extras.getString("IPadd");

		//	
		surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceView.getHolder().addCallback(new Callback() {
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {

			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				if (position > 0) {
					try {
						//
						play();
						//
						mediaPlayer.seekTo(position);
						position = 0;
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {

			}
		});// callBack
    }//setUpVideoView
    
    /***********************************************************
	 * Method to play the video
	 ************************************************************/
    
    private void play() {
		try {			
			mediaPlayer.reset();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			//mediaPlayer.setDataSource("/mnt/sdcard/test.mp4");
			mediaPlayer.setDataSource(localPath);
			//SurfaceView
			mediaPlayer.setDisplay(surfaceView.getHolder());
			mediaPlayer.prepare();
			
			mediaPlayer.start();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}// play
    
    /***********************************************************
	 * Activity override events
	 ************************************************************/

    @Override
    public synchronized void onPause() {
    	/*
    	if (mediaPlayer.isPlaying()) {
			//
			position = mediaPlayer.getCurrentPosition();
			mediaPlayer.stop();
		}
		*/
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
        doUnbindService();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services        
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    /***********************************************************
	 * Ensure Discoverability
	 ************************************************************/

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /************************************************************
     * Sends a message.
     * @param message  A string of text to send.
     ***********************************************************/
    private void sendMessage_to_remote(String message) {
        // Check that we're actually connected before trying anything
    	
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }        

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);            
        }
    }
    /***********************************************************
	 * Message handler
	 ************************************************************/
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    //mConversationArrayAdapter.clear();
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
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
         //////////////////////////////////////////////////
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                receivedMsgHandler(readMessage);
                Log.e("test", readMessage);
                break;
        ////////////////////////////////////////////////////
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    /***********************************************************
	 * onActivityResult
	 ************************************************************/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
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
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }//onActivity Result
    
    /*******************************************************************
     * Connect to Remote device
     *******************************************************************/

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }
    
    /*******************************************************************     
     * receivedMsgHandler
     *******************************************************************/   
    private void receivedMsgHandler(String Msg){
    	Calendar calendar = Calendar.getInstance();
    	long tmp1 = calendar.getTimeInMillis();
    	if(Msg.contains("time")){
    		calendar = Calendar.getInstance();
        	long tmp2 = calendar.getTimeInMillis();
        	//sendMessage("timeone"+Long.toString(tmp1));
        	sendMessage("timetwo"+Long.toString(tmp2));
    	}
    }
    /********************************************************************************
	 * get file path
	 *******************************************************************************/
	private void getFilepath() {
		Bundle fileNameExtra = getIntent().getExtras();
		localPath = fileNameExtra.getString(LOCAL_VIDEO_PATH);
		Log.e("roomName", localPath);
	}
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
   	                
	   	         case ServerSideService.MSG_RETURN_RECEIVE_DATA:
	   	        	String receivedMsg=(String) msg.obj;	   	        	
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
   	        Toast.makeText(ServerSyncPlayer.this, "connected" ,Toast.LENGTH_SHORT).show();
   	    }

   	    public void onServiceDisconnected(ComponentName className) {
   	        // This is called when the connection with the service has been
   	        // unexpectedly disconnected -- that is, its process crashed.
   	        mService = null;
   	        //mCallbackText.setText("Disconnected.");

   	        // As part of the sample, tell the user what happened.
   	        Toast.makeText(ServerSyncPlayer.this, "disconnected",Toast.LENGTH_SHORT).show();
   	    }
   	};
   	/********************************************************************
   	 * bind and unBind serverSideService
   	 ********************************************************************/

   	void doBindService() {
   	    // Establish a connection with the service.  We use an explicit
   	    // class name because there is no reason to be able to let other
   	    // applications replace our component.
   	    bindService(new Intent(ServerSyncPlayer.this,ServerSideService.class), mConnection, Context.BIND_AUTO_CREATE);
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
	/********************************************************************************
	 * 
	 *******************************************************************************/
}//end of Activity
