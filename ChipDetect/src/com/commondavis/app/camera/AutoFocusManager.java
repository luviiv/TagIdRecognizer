/**
 * 
 */
package com.commondavis.app.camera;

import java.util.ArrayList;
import java.util.Collection;

import com.eefocus.chipdetect.R;
import com.eefocus.chipdetect.ScanActivityHandler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author Davis
 *Manage the AutoFocus behavior
 */
public class AutoFocusManager implements AutoFocusCallback {
	private static final String TAG = AutoFocusManager.class.getSimpleName();
	private static final long AUTO_FOCUS_INTERVAL_MS = 2000L;
    private static final Collection<String> FOCUS_MODES_CALLING_AF;
    static {
        FOCUS_MODES_CALLING_AF = new ArrayList<String>(2);
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO);
        FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO);
    }
    
    private boolean active;
    private final boolean useAutoFocus;
	private final Camera camera;
	private AutoFocusTask outstandingTask;
    private final AsyncTaskExecInterface taskExec;
	private final ScanActivityHandler cameraActivityHandler;
    /**
     * Constructor
     * @param context the Activity calling this method
     * @param camera the reference of Camera
     * @param handler the reference of Handler
     */
    @SuppressLint("NewApi")
	public AutoFocusManager(Context context, Camera camera, Handler handler){
    	this.camera = camera;
    	cameraActivityHandler = (ScanActivityHandler)handler;
    	taskExec = new AsyncTaskExecManager().build();
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    	String currentFocusMode = camera.getParameters().getFocusMode();
        useAutoFocus =
                sharedPrefs.getBoolean(CameraConfigurationManager.KEY_AUTO_FOCUS, true) &&
                        FOCUS_MODES_CALLING_AF.contains(currentFocusMode);
        Log.i(TAG, "Current focus mode '" + currentFocusMode + "'; use auto focus? " + useAutoFocus);
        int version = android.provider.Settings.System.getInt(context
                .getContentResolver(),
                android.provider.Settings.System.SYS_PROP_SETTING_VERSION,
                3);
        if (version >= 16)
            camera.setAutoFocusMoveCallback(new Camera.AutoFocusMoveCallback() {
            @Override
            public void onAutoFocusMoving(boolean b, Camera camera) {
                if (b == false) {
                    Message msg = cameraActivityHandler.obtainMessage(R.id.focus_succeeded);
                    msg.sendToTarget();
                }
            }
        });

        start();
    }
	/* (non-Javadoc)
	 * @see android.hardware.Camera.AutoFocusCallback#onAutoFocus(boolean, android.hardware.Camera)
	 */
	@Override
	public synchronized void onAutoFocus(boolean success, Camera theCamera) {
		// TODO Auto-generated method stub
		if(success){
			Message msg = cameraActivityHandler.obtainMessage(R.id.focus_succeeded);
			msg.sendToTarget();
		}
		if(active){
			outstandingTask = new AutoFocusTask();
			taskExec.execute(outstandingTask);
		}
	}
	protected synchronized void start() {
		// TODO Auto-generated method stub
		if (useAutoFocus) {
            active = true;
            try {
                camera.autoFocus(this);
            } catch (RuntimeException re) {
                // Have heard RuntimeException reported in Android 4.0.x+; continue?
                Log.w(TAG, "Unexpected exception while focusing", re);
            }
        }
	}
	protected synchronized void stop(){
		if (useAutoFocus) {
            try {
                camera.cancelAutoFocus();
            } catch (RuntimeException re) {
                // Have heard RuntimeException reported in Android 4.0.x+; continue?
                Log.w(TAG, "Unexpected exception while cancelling focusing", re);
            }
        }
        if (outstandingTask != null) {
            outstandingTask.cancel(true);
            outstandingTask = null;
        }
        active = false;
	}
	private final class AutoFocusTask extends AsyncTask<Object,Object,Object> {
		@Override
		protected Object doInBackground(Object... voids) {
			try {
				Thread.sleep(AUTO_FOCUS_INTERVAL_MS);
			} catch (InterruptedException e) {
				// continue
			}
			synchronized (AutoFocusManager.this) {
				if (active) {
					start();
				}
			}
			return null;
		}
	}
}
