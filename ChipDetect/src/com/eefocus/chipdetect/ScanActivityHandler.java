/**
 * 
 */
package com.eefocus.chipdetect;

import com.commondavis.app.camera.CameraManager;
import com.commondavis.app.process.RecognizeThread;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author v
 *
 */
public class ScanActivityHandler extends Handler {

	private static final String TAG = ScanActivityHandler.class.getSimpleName();

    private State state;
    private final CameraManager cameraManager;
    private final RecognizeThread recognizeThread;
    
    private enum State{
    	PREVIEW,
    	SUCCESS,
    	DONE
    }
	/**
	 * @param context the Activity
	 * @param manager CameraManager
	 */
	public ScanActivityHandler(Context context,CameraManager manager) {
		// TODO Auto-generated constructor stub
		this.cameraManager = manager;
		manager.setCameraActivityHandler(this);
		this.recognizeThread = new RecognizeThread(context);
		recognizeThread.start();
		state=State.SUCCESS;
		cameraManager.startPreview();
		restartPreviewAndRecognize();
		
	}

	public void restartPreviewAndRecognize(){
		if(state ==  State.SUCCESS){
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(recognizeThread.getHandler(), R.id.recognize);
		}
	}
	
	public void quitSynchronously(){
		state = State.DONE;
		cameraManager.stopPreview();
		Message quit = Message.obtain(recognizeThread.getHandler(),R.id.quit);
		quit.sendToTarget();
		try {
			recognizeThread.join(500L);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Be absolutely sure we don't send any queued up messages
		removeMessages(R.id.recognize_succeeded);
		removeMessages(R.id.recognize_failed);
	}
	
	public void handleMessage(Message msg){
		 super.handleMessage(msg);
		 switch (msg.what) {
		 	case R.id.restart_preview:
		 		break;
		 	case R.id.return_scan_result:
		 		break;
		 	case R.id.recognize_succeeded:
		 		break;
		 	case R.id.recognize_failed:
	                //state = State.PREVIEW;
	                //cameraManager.requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
		 		break;
		 	case R.id.focus_succeeded:
		 		Log.d(TAG, "focus succeed");
		 		cameraManager.requestPreviewFrame(recognizeThread.getHandler(), R.id.recognize);
		 		break;
	        }	
	}
}
