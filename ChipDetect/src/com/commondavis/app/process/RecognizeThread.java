package com.commondavis.app.process;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.CountDownLatch;

import com.eefocus.chipdetect.ScanActivity;
/**
 * 
 * @author Davis Lu
 *hang up the getHandler invoke, until this thread is executed, 
 *and return the RecognizeHandler
 */
public class RecognizeThread extends Thread {
	private final static String TAG=RecognizeThread.class.getSimpleName();
	private final ScanActivity activity;
	private Handler handler;
	private CountDownLatch handlerInitLatch;//wake up the thread when count down to 0
	public RecognizeThread(Context context){
		activity=(ScanActivity)context;
		handlerInitLatch=new CountDownLatch(1);
	}
	public Handler getHandler(){
		try{
			handlerInitLatch.await();//hang up until wake up
		}catch(InterruptedException e){
			Log.e(TAG,e.getMessage());
		}
		return handler;
	}
	
	@Override
	public void run(){
		Looper.prepare();
		handler=new RecognizeHandler(activity);
		handlerInitLatch.countDown();//wake up getHandler
		Looper.loop();
	}
	
}
