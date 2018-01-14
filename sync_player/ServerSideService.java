package com.example.sync_player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class ServerSideService extends Service {
    /** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;
   
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_VALUE = 3;
    static final int MSG_SENT_DATA=4;
    static final int MSG_RETURN_DEVICE_NAME=5;
    static final int MSG_RETURN_RECEIVE_DATA=6;
    
    private static final String TAG = "serverSideService";
    private static final boolean D = true;
    private BluetoothAdapter mBluetoothAdapter = null;    
	// Member object for the chat services
	private BluetoothChatService mChatService = null;
	
	// Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_CLOSE = 6;
	
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	

    /**************************************************************************
     * Handler of incoming messages from clients.
     **************************************************************************/
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SET_VALUE:
                    mValue = msg.arg1;
                    for (int i=mClients.size()-1; i>=0; i--) {
                        try {
                            mClients.get(i).send(Message.obtain(null,
                                    MSG_SET_VALUE, mValue, 0));
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            mClients.remove(i);
                        }
                    }
                    break;
                    
                case MSG_SENT_DATA:
                	String sentData=(String) msg.obj;
                	sendMessage(sentData);
                break;
                
                default:
                    super.handleMessage(msg);
            }
        }
        
        private void sendMessage(String message) {
    		// Check that we're actually connected before trying anything
    		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
    			//Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
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
    }

    /***************************************************************************
     * Target we publish for clients to send messages to IncomingHandler.
     **************************************************************************/
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",Toast.LENGTH_LONG).show();			
			return;
		}
		
		// Initialize the BluetoothChatService to perform bluetooth connections
		if (mChatService == null){
			mChatService = new BluetoothChatService(this, mHandler);
		}
		
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
				
		
    	
        //mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.
        //showNotification();
    }

    @Override
    public void onDestroy() {
    	if (mChatService != null)
			mChatService.stop();
    	
        // Cancel the persistent notification.
        //mNM.cancel(R.string.remote_service_started);

        // Tell the user we stopped.
        //Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    }

    /***************************************************************************
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     **************************************************************************/
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    /***************************************************************************
     * Show a notification while this service is running.
     **************************************************************************/
    /*
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.remote_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.stat_sample, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, Controller.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.remote_service_label),
                       text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.remote_service_started, notification);
    }
    */
    /*******************************************************************
	 * Sends a message.	
	 * @param message
	 *            A string of text to send.
	 *******************************************************************/
	private void sendMessage(String message) {
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
				Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					//mTitle.setText(R.string.title_connected_to);
					//mTitle.append(mConnectedDeviceName);
					// mConversationArrayAdapter.clear();
					break;
				case BluetoothChatService.STATE_CONNECTING:
					//mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					//mTitle.setText(R.string.title_not_connected);
//					if(closeJumpIndicate==1){
//						closeJumpIndicate=0;
//						Intent intent = new Intent(ServerRoomManage.this,ServerSyncPlayer.class);
//						intent.putExtra(LOCAL_VIDEO_PATH, filePath);
//						startActivity(intent);
//					}
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
				return_receivedData(readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name				
				String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Log.e("serverSideService",mConnectedDeviceName);
				return_deviceName(mConnectedDeviceName);				
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
	/**************************************************************************
     * return device name to manage page
     **************************************************************************/
	void return_deviceName(String deviceName){
		for (int i=mClients.size()-1; i>=0; i--) {
	        try {
	            mClients.get(i).send(Message.obtain(null,MSG_RETURN_DEVICE_NAME,-1, -1, deviceName));
	        } catch (RemoteException e) {
	            // The client is dead.  Remove it from the list;
	            // we are going through the list from back to front
	            // so this is safe to do inside the loop.
	            mClients.remove(i);
	        }
	    }
	}
	/**************************************************************************
     * return device name to manage page
     **************************************************************************/
	void return_receivedData(String data){
		for (int i=mClients.size()-1; i>=0; i--) {
	        try {
	            mClients.get(i).send(Message.obtain(null,MSG_RETURN_RECEIVE_DATA,-1, -1, data));
	        } catch (RemoteException e) {	            
	            mClients.remove(i);
	        }
	    }
	}    
    /**************************************************************************
     * 
     **************************************************************************/
}//end of serverSideService
