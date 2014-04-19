package com.eefocus.chipdetect;





import com.commondavis.app.camera.CameraManager;

import android.os.Bundle;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
    	if(CameraManager.hasCamera(this.getApplication())){
        	MenuInflater inflater=getMenuInflater();
        	inflater.inflate(R.menu.main, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        case R.id.btn_scan:
            beginScanActivity();
            return true;
        default:
            return super.onOptionsItemSelected(item);
    	}
    }
    public void beginScanActivity(){
    	Intent intent=new Intent();
    	intent.setClass(this,ScanActivity.class);
    	startActivity(intent);
    }
    public void onSearchClick(View view){
    	
    }
    
}
