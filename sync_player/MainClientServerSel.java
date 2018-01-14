package com.example.sync_player;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.support.v4.app.NavUtils;

public class MainClientServerSel extends Activity {
	
	private Button beServerBtn;
	private Button beClientBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_server_client_sel);
        setUpBtn();
    }//onCreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private void setUpBtn(){
    	beServerBtn = (Button) findViewById(R.id.beServer_btn);
    	beClientBtn=(Button) findViewById(R.id.beClient_btn);
    	
    	beServerBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				Intent intent=new Intent(MainClientServerSel.this,ServerRoomSetup.class);
				startActivity(intent);
			}
		});//beServerBtn onClickListener
    	
    	beClientBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Perform action on click
				Intent intent=new Intent(MainClientServerSel.this,ClientRoomList.class);
				startActivity(intent);
			}
		});//beClientBtn onClickListener    	
    	
    	
    }//setUpBtn

    
}//ClientServerSel
