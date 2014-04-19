package com.eefocus.chipdetect;

import java.io.IOException;

import com.commondavis.app.camera.CameraManager;
import com.commondavis.app.view.ViewfinderView;

import android.os.Bundle;
import android.os.Handler;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class ScanActivity extends ActionBarActivity implements Callback{
	private static final String TAG = ScanActivity.class.getSimpleName();
	//private static final String TAG=ScanActivity.class.getSimpleName();
	private ViewfinderView finderView;
	private boolean hasSurface;
	private ScanActivityHandler mHandler;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);
		ActionBar actionBar=getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		CameraManager.init(this.getApplication());
		finderView=(ViewfinderView)findViewById(R.id.viewfinder_view);
		hasSurface=false;
	}
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume(){
		super.onResume();
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if(hasSurface){
			initCamera(surfaceHolder);
		}else{
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
;		}
		
	}
	@Override
	protected void onPause() {
		
		if (mHandler != null) {
			mHandler.quitSynchronously();
			mHandler = null;
		}
		CameraManager.get().closeDriver();
		if(!hasSurface){
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
		}
		super.onPause();
	}
	@Override
	protected void onDestroy() {
		//inactivityTimer.shutdown();
		super.onDestroy();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.scan, menu);
		return true;
	}
	public Handler getHandler(){
		return mHandler;
	}
	private void initCamera(SurfaceHolder surfaceHolder) {
		if(null==surfaceHolder){
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if(CameraManager.get().isOpen()){
			Log.w(TAG,"initCamera() while camera opened");
			return;
		}
		try {
			CameraManager.get().openDriver(surfaceHolder);
			if(null == mHandler){
				mHandler = new ScanActivityHandler(this,CameraManager.get());
			}
		} catch (IOException ioe) {
			Log.w(TAG,ioe);
			return;
		} catch (RuntimeException e) {
			Log.w(TAG,"Unexpected error when open camera",e);
			return;
		}
//		if (handler == null) {
//			handler = new CaptureActivityHandler(this, decodeFormats,
//				b	characterSet);
//		}
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// TODO Auto-generated method stub
		//CameraManager.get().resumeCamera(holder);
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		if (null == holder) {
			Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
		}
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
			//CameraManager.get().startPreview();
		}
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		hasSurface=false;
	}
	
	public ViewfinderView getViewfinderView(){
		return finderView;
	}
	public void drawViewfinder(){
		finderView.drawViewfinder();
	}
}
