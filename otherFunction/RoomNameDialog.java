package com.example.otherFunction;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import java.util.Random;

import com.example.sync_player.ServerRoomManage;

import com.example.sync_player.R;


public class RoomNameDialog extends Activity {

	public static final String SEARCH_QUERY_RESULT_FROM_DIALOG = "SEARCH_DIALOG";
	private Button confirmBtn;
	private Button cancelBtn;
	private EditText roomNameEdit;
	Random myRandom;
	private static final String INPUT_ROOM_NAME="roomName";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_room_name_dialog);
		confirmBtn = (Button) findViewById(R.id.confirm);
		confirmBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				returnSearchQuery();
			}
		});
		cancelBtn = (Button) findViewById(R.id.cancel);
		cancelBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				cancelDialog();
			}
		});
		roomNameEdit = (EditText) findViewById(R.id.room_name);
		myRandom=new Random();
		roomNameEdit.setText("video"+String.valueOf(myRandom.nextInt()) );
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

	}

	private void returnSearchQuery() {
		//Intent resultIntent = new Intent(this, RoomNameDialog.class);
		Intent intent = new Intent(RoomNameDialog.this,ServerRoomManage.class);
		intent.putExtra(INPUT_ROOM_NAME, roomNameEdit.getText().toString());
		startActivity(intent);
		//resultIntent.putExtra(SEARCH_QUERY_RESULT_FROM_DIALOG, roomNameEdit.getText().toString());
		//setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}

	private void cancelDialog() {
		finish();
	}
}//RoomNameDialog Activity
