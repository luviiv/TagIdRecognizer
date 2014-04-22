/**
 * 
 */
package com.commondavis.app.process;

import com.commondavis.app.camera.CameraManager;
import com.commondavis.app.camera.luminance.PlanarYUVLuminanceSource;
import com.eefocus.chipdetect.R;
import com.eefocus.chipdetect.ScanActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * @author Davis Lu
 *This handler handles the recognize process of picture OCRs
 */
public class RecognizeHandler extends Handler {
	private final ScanActivity activity;
	private boolean running=true;
	RecognizeHandler(Context context){
		this.activity=(ScanActivity)context;
	}
	
	@Override
	public void handleMessage(Message message){
		if(!running){
			return;
		}
		switch(message.what){
		case R.id.recognize:
			recognize((byte[])message.obj, message.arg1, message.arg2);
			break;
		case R.id.quit:
			running=false;
			Looper.myLooper().quit();
			break;
		}
	}
	
	private void recognize(byte[] data,int width,int height){
		PlanarYUVLuminanceSource YUVSource = activity.getCameraManager().buildLuminanceSource(data, width, height);
	}
}
