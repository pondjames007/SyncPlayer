package com.example.sync_player;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

import com.example.otherFunction.RoomNameDialog;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ServerRoomSetup extends Activity {

	private Button createRoomBtn;
	private Button advanceSettingBtn;
	private Button startBtn;

	private Button joinBtn;
	private Button searchButton;

	private BluetoothAdapter bluetooth;
	private BluetoothSocket socket;
	private UUID uuid = UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666");

	private static int DISCOVERY_REQUEST = 1;
	private static final int MY_CUSTOM_DIALOG = 0;

	private ArrayList<BluetoothDevice> foundDevices = new ArrayList<BluetoothDevice>();
	private ArrayAdapter<String> aa;
	private ListView list;
	private String content = "";

	private Handler handler = new Handler();

	private static final String ROOM_SETTING_PROFILE = "roomProfile2";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_room_setting);
		//configureBluetooth();
		readRoomProfile();
		setupListView();
		// setupSearchButton();
		// setupListenButton();
		setUpBtn();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case (MY_CUSTOM_DIALOG): {
			if (resultCode == Activity.RESULT_OK) {
				Log.d("ANDRO_DIALOG", "Coming back from the search dialog..");
				String searchQuery = data
						.getStringExtra(RoomNameDialog.SEARCH_QUERY_RESULT_FROM_DIALOG);
				Log.d("ANDRO_DIALOG", "Search query result: " + searchQuery);
			}
			break;
		}
		}
	}// onActivityResult

	/****************************************
	 * listen to the scan result
	 *****************************************/

	BroadcastReceiver discoveryResult = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			BluetoothDevice remoteDevice;
			// remoteDevice =
			// intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			String action = intent.getAction();
			Log.e("test", "hello");
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				remoteDevice = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.e("test", "hello2");
				Log.e("test", foundDevices.toString());
				foundDevices.add(remoteDevice);
				setupListView();
			}
			// if (bluetooth.getBondedDevices().contains(remoteDevice)) {
			// foundDevices.add(remoteDevice);
			// aa.notifyDataSetChanged();
			// }
		}
	};// discoveryResult

	/*
	 * setting up funcion
	 */
	private void configureBluetooth() {
		bluetooth = BluetoothAdapter.getDefaultAdapter();
	}// configureBluttooth

	/*
	 * listening button
	 */
	/*
	 * private void setupListenButton() { Button listenButton = (Button)
	 * findViewById(R.id.button_let_listen); listenButton.setOnClickListener(new
	 * OnClickListener() { public void onClick(View view) { // Intent disc;
	 * Intent disc = new Intent( BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	 * startActivityForResult(disc, DISCOVERY_REQUEST); } });
	 * 
	 * }// end of setupListenButton
	 */
	/**********************************************************
	 * setUp listView
	 **********************************************************/
	private void readRoomProfile() {
		String fileName = ROOM_SETTING_PROFILE;
		int readed;
		// String content="";
		byte[] buff = new byte[256]; // input stream buffer
		// Input stream
		try {
			FileInputStream reader = openFileInput(fileName);
			while ((readed = reader.read(buff)) != -1) {
				content += new String(buff).trim();
			}
			Log.e("profile", content);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}// readRoomProfile

	/**********************************************************
	 * setUp listView
	 **********************************************************/

	private void setupListView() {

		aa = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		aa.add(content);
		list = (ListView) findViewById(R.id.Server_manage_list);
		// Log.e("list",aa.toString());
		list.setAdapter(aa);
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {

			}
		});
	}// setupListView

	/**********************************************************************
	 * set up search btn
	 **********************************************************************/

	private void setupSearchButton() {
		searchButton = (Button) findViewById(R.id.button_server_refresh);
		searchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				foundDevices.clear();
				registerReceiver(discoveryResult, new IntentFilter(
						BluetoothDevice.ACTION_FOUND));
				if (!bluetooth.isDiscovering()) {
					foundDevices.clear();
					bluetooth.startDiscovery();
				}

			}
		});

	}// setupSearchButton

	/******************************************************
	 * satrt vieo playing btn
	 ******************************************************/

	private void setUpBtn() {

		startBtn = (Button) findViewById(R.id.video_start1);

		startBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				startCustomDialog();
				//Intent intent = new Intent(ServerRoomSetup.this,ServerRoomManage.class);
				//startActivity(intent);				
			}
		});// startBtn onClickListener

	}// setUpBtn

	/******************************************************
	 * set up room name input dialog
	 ******************************************************/
	private void startCustomDialog() {
		Intent intent = new Intent(this, RoomNameDialog.class);
		startActivityForResult(intent, MY_CUSTOM_DIALOG);
	}

}// serverRoomSetup Activity