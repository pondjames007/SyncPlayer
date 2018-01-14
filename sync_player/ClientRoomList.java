package com.example.sync_player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ClientRoomList extends Activity {
	//private Button createRoomBtn;
	//private Button advanceSettingBtn;
	private Button testBtn;
	private Button refreshButton;

	private BluetoothAdapter bluetooth;
	private BluetoothSocket socket;
	private BluetoothServerSocket btserver;
	private UUID uuid = UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666");

	private static int DISCOVERY_REQUEST = 1;
	private static String SELECTED_ADDRESS = "SELECTADD";

	// private ArrayList<BluetoothDevice> foundDevices=new
	// ArrayList<BluetoothDevice>();
	// private ArrayAdapter<BluetoothDevice> aa;
	private ArrayList<String> foundDeviceAddress;
	private ArrayAdapter<String> foundDevices;
	private ListView list;

	private Handler handler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.client_room_list);
		configureBluetooth();
		setupListView();
		setupRefreshButton();
		setupListenButton();
		setUpBtn();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DISCOVERY_REQUEST) {
			boolean isDiscoverable = resultCode > 0;
			if (isDiscoverable) {
				String name = "bluetoothserver";
				/*try {
					// final BluetoothServerSocket btserver =
					// bluetooth.listenUsingRfcommWithServiceRecord(name, uuid);
					btserver = bluetooth.listenUsingRfcommWithServiceRecord(
							name, uuid);
					AsyncTask<Integer, Void, BluetoothSocket> acceptThread = new AsyncTask<Integer, Void, BluetoothSocket>() {
						@Override
						protected BluetoothSocket doInBackground(
								Integer... params) {

							try {
								socket = btserver.accept(params[0] * 1000);
								return socket;
							} catch (IOException e) {
								Log.d("BLUETOOTH", e.getMessage());
							}

							return null;
						}

						@Override
						protected void onPostExecute(BluetoothSocket result) {
							if (result != null) {
								switchUI();
							}
						}
					};
					acceptThread.execute(resultCode);
				} catch (IOException e) {
					Log.d("BLUETOOTH", e.getMessage());
				}*/
			}
		}
	}// onActivityResult

	/****************************************************
	 * broadcast receiver listen to the scan result
	 ****************************************************/

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
				// Log.e("test","hello2");
				// Log.e("test",foundDevices.toString());
				// foundDevices.add(remoteDevice);
				// foundDevices.clear();
				// foundDeviceAddress.clear();
				foundDevices.add(remoteDevice.getName() + "\n"
						+ remoteDevice.getAddress());
				foundDeviceAddress.add(remoteDevice.getAddress());
				foundDevices.notifyDataSetChanged();
				// setupListView();
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

	/*****************************************************
	 * listening button
	 ******************************************************/

	private void setupListenButton() {
		Button listenButton = (Button) findViewById(R.id.button_listen_msg);
		listenButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				// Intent disc = new
				// Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				// startActivityForResult(disc, DISCOVERY_REQUEST);
			}
		});
	}// end of setupListenButton

	/******************************************************
	 * setUp listView
	 *****************************************************/

	private void setupListView() {
		// aa = new
		// ArrayAdapter<BluetoothDevice>(this,android.R.layout.simple_list_item_1,
		// foundDevices);
		foundDevices = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		foundDeviceAddress = new ArrayList<String>();

		list = (ListView) findViewById(R.id.clientRoomList);
		list.setAdapter(foundDevices);
		list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				/*
				 * AsyncTask<Integer, Void, Void> connectTask = new
				 * AsyncTask<Integer, Void, Void>() {
				 * 
				 * @Override protected Void doInBackground(Integer... params) {
				 * try { BluetoothDevice device = foundDevices.get(params[0]);
				 * socket = device.createRfcommSocketToServiceRecord(uuid);
				 * socket.connect(); } catch (IOException e) {
				 * Log.d("BLUETOOTH_CLIENT", e.getMessage()); } return null; }
				 * 
				 * @Override protected void onPostExecute(Void result) {
				 * //switchUI(); } };//connectTask
				 */
				// connectTask.execute(index);
				Toast.makeText(view.getContext(),
						foundDeviceAddress.get(index), Toast.LENGTH_SHORT)
						.show();
				Intent intent = new Intent(ClientRoomList.this,
						ClientRoomSetting.class);
				intent.putExtra(SELECTED_ADDRESS, foundDeviceAddress.get(index));
				startActivity(intent);
			}// onItemClick
		});
	}// setupListView

	/***********************************************************
	 * set up search btn
	 **********************************************************/

	private void setupRefreshButton() {
		refreshButton = (Button) findViewById(R.id.button_refresh);
		refreshButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				// foundDevices.clear();
				registerReceiver(discoveryResult, new IntentFilter(
						BluetoothDevice.ACTION_FOUND));
				if (!bluetooth.isDiscovering()) {
					foundDevices.clear();
					foundDeviceAddress.clear();
					bluetooth.startDiscovery();
				}

			}
		});

	}// setupSearchButton

	/********************************************************
	 * show up text
	 ********************************************************/

	private void switchUI() {
		/*
		 * final TextView messageText =
		 * (TextView)findViewById(R.id.text_messages); final EditText textEntry
		 * = (EditText)findViewById(R.id.text_message);
		 * messageText.setVisibility(View.VISIBLE);
		 * list.setVisibility(View.GONE); textEntry.setEnabled(true);
		 */
	}

	/***********************************************************
	 * set up search btn
	 **********************************************************/

	private void setUpBtn() {

		testBtn = (Button) findViewById(R.id.test_btn);

		testBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				Intent intent = new Intent(ClientRoomList.this,ClientRoomSetting.class);
				startActivity(intent);

			}
		});// startBtn onClickListener

	}// setUpBtn

}// ClientRoomInfo
