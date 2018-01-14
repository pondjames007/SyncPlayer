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
import java.util.Timer;
import java.util.TimerTask;

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
public class ClientSyncPlayer extends Activity {
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
    private TextView mTitle;    ;
    
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
    
    private String selectedAdd="";
	private static String SELECTED_ADDRESS="SELECTADD"; 
	
	private static final int VIDEO_PLAY_SIGNAL=1;
	private static final int VIDEO_PAUSE_SIGNAL=2;
	private static final int VIDEO_STOP_SIGNAL=3;
	
	private static final String LOCAL_VIDEO_PATH="localPath";
    private String localPath="";
        
    Timer timerConnect;
    
    /** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;	
	//Target we publish for clients to send messages to IncomingHandler	 
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	
	/////////////////////////////////////////////
	private long timediff = 0;
	private long t1 = 0, t2 = 0, t3 = 0, t4 = 0;
	private int playState = 0;
	private long serverTime = 0;
	/////////////////////////////////////////////
	


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        //setContentView(R.layout.main);
        setContentView(R.layout.sync_client_player);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        
        Bundle Addextra=getIntent().getExtras();
        selectedAdd=Addextra.getString(SELECTED_ADDRESS);        
        Log.e("sAddress",selectedAdd);
        
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
    }

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
            //if (mChatService == null) 
            	//setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        /*
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
        	if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();				
				repeatConnectRequest();				
				//connectDevice(selectedAdd, false);							
			}
        }*/
        doBindService();               
    }//onResume
    
    /***********************************************************
	 * set up chat service
	 ************************************************************/

    private void setupChat() {
        Log.d(TAG, "setupChat()");        
        

        // Initialize the BluetoothChatService to perform bluetooth connections
        //mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        //mOutStringBuffer = new StringBuffer("");
        
        Calendar calendar = Calendar.getInstance();
    	t1 = calendar.getTimeInMillis();
        sendMessage("time");
    }
    
    /***********************************************************
	 * set up video view and btn
	 ************************************************************/
    
    private void setUpVideoView(){
    	btnplay = (Button)findViewById(R.id.btnplay);
		btnstop = (Button) findViewById(R.id.btnstop);
		btnpause = (Button) findViewById(R.id.btnpause);
		
		/////////////////////////////////////////////////////////////
		//*		
		btnstop.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {                
                //String message = "start";
                //sendMessage(message);
            	//videoController(VIDEO_STOP_SIGNAL);
            	
            	//connectDevice(selectedAdd,false);    	
            	//callServer();
    			//*
        		Calendar calendar = Calendar.getInstance();
            	t1 = calendar.getTimeInMillis();
                sendMessage("time");
                //*/
            }
        });//btnstop
		btnplay.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //String message = "play";
                //sendMessage(message);
            	videoController(VIDEO_PLAY_SIGNAL);
            }
        });//btnplay
		btnpause.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	videoController(VIDEO_PAUSE_SIGNAL);
            }
        });//btnpause
        //*/
		////////////////////////////////////////////////////////

		mediaPlayer = new MediaPlayer();
		surfaceView = (SurfaceView) this.findViewById(R.id.surfaceView);
		
		
		//getClientIP
		//Bundle extras = getIntent().getExtras();
		//clientIP = extras.getString("IPadd");

		
		surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceView.getHolder().addCallback(new Callback() {
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {

			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				if (position > 0) {
					try {
						// ��
						play();
						// ��
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
	 * method to play video
	 ************************************************************/
    
    private void play() {
		try {			
			mediaPlayer.reset();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			//mediaPlayer.setDataSource("/mnt/sdcard/test.mp4");
			mediaPlayer.setDataSource(localPath);
			// SurfaceView
			mediaPlayer.setDisplay(surfaceView.getHolder());
			mediaPlayer.prepare();
			//
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
    	
    	if (mediaPlayer.isPlaying()) {
		
			position = mediaPlayer.getCurrentPosition();
			mediaPlayer.stop();
		}		
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

    /*****************************************************************
     * Sends a message.
     * @param message  A string of text to send.
     *****************************************************************/  
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
            	
            // Reset out string buffer to zero and clear the edit text field
            //mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }
    /***********************************************************
	 * Message Handler
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
                    callServer();
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
          //////////////////////////////////////////////////////////////
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
        //////////////////////////////////////////////////////////////////
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                //Log.e("test", readMessage);
                receivedMsgHandler(readMessage);                
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
            	//handle the case when connection failed            	
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    /***********************************************************
	 * onActivityResult call back
	 ************************************************************/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                //connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                //connectDevice(data, false);
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
    }
    
    /***********************************************************
	 * Connect to an remote device
	 ************************************************************/
    private void connectDevice(String data, boolean secure) {
        // Get the device MAC address
        //String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
    	String address =data;
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }
    
    /***********************************************************
	 * video controller method
	 ************************************************************/    
    private void videoController(int ctl){
    	switch (ctl) {
		case VIDEO_PLAY_SIGNAL:			
			play();
			break;

		case VIDEO_PAUSE_SIGNAL:			
			if (mediaPlayer.isPlaying()) {
				mediaPlayer.pause();				
			} else {
				mediaPlayer.start();				
			}
			break;

		case VIDEO_STOP_SIGNAL:			
			if (mediaPlayer.isPlaying()) {
				mediaPlayer.stop();				
			}

			break;
		default:
			break;
		}
    }//videoController
    
    /***********************************************************
	 * Message handle when receive message from remote
	 * device
	 ************************************************************/    
    ////////////////////////////////////////////////////////////////////////////
    private void receivedMsgHandler(String Msg){
    	Calendar calendar = Calendar.getInstance();
    	//long ntime = calendar.getTimeInMillis();
    	Timer timer = new Timer();
    	long now = 0;
    	
    	/*SntpClient client = new SntpClient();
    	if(client.requestTime(ntpaddr, 30000)){
    		now = client.getNtpTime() + System.nanoTime()/1000 - client.getNtpTimeReference();
    	}*/
    	
    	if(Msg.contains("play")){
    		playState = 1;
    		play();
    		mediaPlayer.pause();
    		String time = Msg.substring(4);
    		serverTime = Long.valueOf(time);
    		/*Calendar calendar = Calendar.getInstance();
        	t1 = calendar.getTimeInMillis();
            sendMessage("time");
    		*/
    		calendar = Calendar.getInstance();
    		t1 = calendar.getTimeInMillis();
            sendMessage("time");
    		/*now = calendar.getTimeInMillis();
    		timer.schedule(new TimerTask(){
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mediaPlayer.start();
				}
    		}, times-(now - timediff));
    		*/
    	}
    	else if(Msg.contains("pause")){
    		playState = 2;
    		int word = Msg.indexOf("pause");
    		String time = Msg.substring(0,word);
    		String pauseTime = Msg.substring(word+5);
    		if(!mediaPlayer.isPlaying())
    			mediaPlayer.seekTo(Integer.valueOf(pauseTime));
    		serverTime = Long.valueOf(time);
    		calendar = Calendar.getInstance();
    		t1 = calendar.getTimeInMillis();
            sendMessage("time");
            /*
    		now = calendar.getTimeInMillis();
    		timer.schedule(new TimerTask(){
				@Override
				public void run() {
					// TODO Auto-generated method stub
		    		videoController(VIDEO_PAUSE_SIGNAL);  
				}
    		}, times-(now - timediff));  	*/	
    	}
    	else if(Msg.contains("stop")){
    		String time = Msg.substring(4);
    		long times = Long.valueOf(time);
    		calendar = Calendar.getInstance();
    		now = calendar.getTimeInMillis();
    		timer.schedule(new TimerTask(){
				@Override
				public void run() {
					// TODO Auto-generated method stub
					videoController(VIDEO_STOP_SIGNAL); 
				}
    		}, times-(now - timediff));  
    	}
    	/*else if(Msg.contains("timeone")){
    		t2 = Long.valueOf(Msg.substring(7));
    		Toast.makeText(this, Long.toString(t2), Toast.LENGTH_LONG).show();
    	}*/
    	else if(Msg.contains("timetwo")){
    		t3 = Long.valueOf(Msg.substring(7));
    		calendar = Calendar.getInstance();
        	t4 = calendar.getTimeInMillis();
        	long tmpdif = (t4-t1)/2;
        	timediff = t4 - (t3 + tmpdif);
        	
        	if(playState == 1){
        		now = calendar.getTimeInMillis();
        		timer.schedule(new TimerTask(){
        			@Override
        			public void run() {
        				// TODO Auto-generated method stub
        				mediaPlayer.start();
        			}
        		}, serverTime-(now - timediff));
        	}
        	else if(playState == 2){
        		now = calendar.getTimeInMillis();
        		timer.schedule(new TimerTask(){
    				@Override
    				public void run() {
    					// TODO Auto-generated method stub
    		    		videoController(VIDEO_PAUSE_SIGNAL);  
    				}
        		}, serverTime-(now - timediff));
        	}
        	
        	//Toast.makeText(this, "t1 "+Long.toString(t1), Toast.LENGTH_LONG).show();
        	//Toast.makeText(this, "t2 "+Long.toString(t2), Toast.LENGTH_LONG).show();
        	//Toast.makeText(this, "t3 "+Long.toString(t3), Toast.LENGTH_LONG).show();
        	//Toast.makeText(this, "t4 "+Long.toString(t4), Toast.LENGTH_LONG).show();
        	Toast.makeText(this, "tmpdif "+Long.toString(tmpdif), Toast.LENGTH_LONG).show();
        	Toast.makeText(this, "timediff "+Long.toString(timediff), Toast.LENGTH_LONG).show();
    	}
    }//receivedMsgHandler
    
    /********************************************************************************
   	 * get file path
   	 *******************************************************************************/
   	private void getFilepath() {
   		Bundle fileNameExtra = getIntent().getExtras();
   		localPath = fileNameExtra.getString(LOCAL_VIDEO_PATH);
   		Log.e("fileName", localPath);
   	}
    /********************************************************************************
   	 * connect failed handler and repeat requesting the connection
   	 *******************************************************************************/
   	private void repeatConnectRequest(){
   		timerConnect = new Timer();
   		timerConnect.schedule(new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
		        	connectDevice(selectedAdd,false);
		        }
				else{
					timerConnect.cancel();
				}
			}//run TimerTask
   		}, 4000, 2000);   		
   	}//end of connectFailHandler 
   	
   	/********************************************************************************
   	 *
   	 *******************************************************************************/
   	private void callServer(){
   		sendMessage("testtest");
   	}
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
	   	        	//Log.e("clientPlayer",receivedMsg);
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
   	        Toast.makeText(ClientSyncPlayer.this, "ClientServiceConnected" ,Toast.LENGTH_SHORT).show();
   	        setupChat();
   	    }//onServiceConnected

   	    public void onServiceDisconnected(ComponentName className) {
   	        // This is called when the connection with the service has been
   	        // unexpectedly disconnected -- that is, its process crashed.
   	        mService = null;
   	        //mCallbackText.setText("Disconnected.");
   	        Toast.makeText(ClientSyncPlayer.this, "ClientServiceConnected",Toast.LENGTH_SHORT).show();
   	    }
   	};
   	/********************************************************************
   	 * bind and unBind ClientSideService
   	 ********************************************************************/

   	void doBindService() {
   	    // Establish a connection with the service.  We use an explicit
   	    // class name because there is no reason to be able to let other
   	    // applications replace our component.
   	    bindService(new Intent(ClientSyncPlayer.this,ClientSideService.class), mConnection, Context.BIND_AUTO_CREATE);
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
   	/********************************************************************************
  	 *
  	 *******************************************************************************/

}//CLientSyncPlayer
