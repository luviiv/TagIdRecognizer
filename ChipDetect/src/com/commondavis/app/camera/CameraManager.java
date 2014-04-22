package com.commondavis.app.camera;


import java.io.IOException;

import com.commondavis.app.camera.luminance.PlanarYUVLuminanceSource;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 */
public class CameraManager {
	private static final String TAG = CameraManager.class.getSimpleName();
	private static final int MIN_FRAME_WIDTH = 240;
	private static final int MIN_FRAME_HEIGHT = 240;
	private static final int MAX_FRAME_WIDTH = 480;
	private static final int MAX_FRAME_HEIGHT = 360;

	static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
	static {
	    int sdkInt;
	    try {
	      sdkInt = Build.VERSION.SDK_INT;
	    } catch (NumberFormatException nfe) {
	      // Just to be safe
	      sdkInt = 10000;
	    }
	    SDK_INT = sdkInt;
	}
	

	
	private final Context context;
	private final CameraConfigurationManager configManager;
	private AutoFocusManager autoFocusManager;
//	private final boolean useOneShotPreviewCallback ;
	private Camera camera;
	private Rect framingRect;
	private Rect framingRectInPreview;
	private Handler cameraActivityHandler;
	private int requestedFramingRectWidth;
	private int requestedFramingRectHeight;
	private boolean initialized;
	private boolean previewing = false;
	/**
	   * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
	   * clear the handler so it will only receive one message.
	   */
	private final PreviewCallback previewCallback;
	
	/**
	   * Checks if the phone supports camera
	   * @param Context the activity needs to check camera availability
	* */
	public static boolean hasCamera(Context context){
		if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			  return true;
		}else{
			  return false;
		}
	}
	
	/**
	   * Constructor
	   * @param Context the activity needs to check camera availability
	* **/
	public CameraManager(Context context) {

		this.context = context;
	    this.configManager = new CameraConfigurationManager(context);

	    // Camera.setOneShotPreviewCallback() has a race condition in Cupcake, so we use the older
	    // Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later, we need to use
	    // the more efficient one shot callback, as the older one can swamp the system and cause it
	    // to run out of memory. We can't use SDK_INT because it was introduced in the Donut SDK.
	    //useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > Build.VERSION_CODES.CUPCAKE;
	    //useOneShotPreviewCallback = (Build.VERSION.SDK_INT > 3); // 3 = Cupcake

	    previewCallback = new PreviewCallback(configManager);
	}
	/**
	 * @author Davis Lu
	 * @param handler Handler that handles the camera
	 */
	public void setCameraActivityHandler(Handler handler) {
		// TODO Auto-generated method stub
		cameraActivityHandler=handler;
	}
	/**
	   * Opens the camera driver and initializes the hardware parameters.
	   *
	   * @param holder The surface object which the camera will draw preview frames into.
	   * @throws IOException Indicates the camera driver failed to open.
	   */
	public synchronized void openDriver(SurfaceHolder holder) throws IOException {
		Camera theCamera = camera;
        if (theCamera == null) {
            theCamera = new OpenCameraManager().build().open();
            if (theCamera == null) {
                throw new IOException();
            }
            camera = theCamera;
        }
        theCamera.setPreviewDisplay(holder);
        /**
         * set up the preview rect manually
         */
        if (!initialized) {
            initialized = true;
            configManager.initFromCameraParameters(theCamera);
            if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
                setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
                requestedFramingRectWidth = 0;
                requestedFramingRectHeight = 0;
            }
        }
        Camera.Parameters parameters = theCamera.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
        try {
            configManager.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            if (parametersFlattened != null) {
                parameters = theCamera.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    theCamera.setParameters(parameters);
                    configManager.setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }
	}
	public synchronized boolean isOpen() {
        return camera != null;
    }
	/**
	 * Close the camera driver
	 */
	public synchronized void closeDriver(){
		if(null!=camera){
			camera.release();
			camera=null;
			// Make sure to clear these each time we close the camera, so that any scanning rect
            // requested by intent is forgotten.
            framingRect = null;
            framingRectInPreview = null;
		}
	}
	/**
	   * Asks the camera hardware to begin drawing preview frames to the screen.
	   */
	public synchronized void startPreview() {
		Camera theCamera = camera;
		if (theCamera != null && !previewing) {
	      theCamera.startPreview();
	      previewing = true;
	      autoFocusManager = new AutoFocusManager(context,camera,cameraActivityHandler);
	    }
	}
	/**
	 * Stops the camera preview
	 */
	public synchronized void stopPreview(){
		if(autoFocusManager != null){
			autoFocusManager.stop();
			autoFocusManager = null;
		}
		if(camera != null && previewing){
			camera.stopPreview();
			previewCallback.setHandler(null,0);
			previewing = false;
		}
	}
	/**
	 * Resume the camera once displaying surface changes
	 * @param holder the SurfaceHolder of camera
	 */
	public void resumeCamera(SurfaceHolder holder){
		if(null==holder.getSurface())
			return;
		try {
	        camera.stopPreview();
	    } catch (Exception e){
	          // ignore: tried to stop a non-existent preview
	    	Log.e(TAG,e.getMessage());
	    }

	    // set preview size and make any resize, rotate or
	    // reformatting changes here

	    // start preview with new settings
	    try {
	        camera.setPreviewDisplay(holder);
	        camera.startPreview();
	        previewing=true;
	    } catch (Exception e){
	        Log.d(TAG, "Error resuming camera preview: " + e.getMessage());
	    }
	}
	 /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    public synchronized void requestPreviewFrame(Handler handler, int message) {
        Camera theCamera = camera;
        if (theCamera != null && previewing) {
            previewCallback.setHandler(handler, message);
            theCamera.setOneShotPreviewCallback(previewCallback);
        }
    }
	/**
	 * Calculates the framing rect which the UI should draw to show the user where to place the
	 * barcode. This target helps with alignment as well as forces the user to hold the device
	 * far enough away to ensure the image will be in focus.
	 *
	 * @return The rectangle to draw on screen in window coordinates.
	 */
	public synchronized Rect getFramingRect() {
		Point screenResolution = configManager.getScreenResolution();
	    if (framingRect == null) {
	    	if (camera == null || screenResolution == null) {
	    		return null;
	        }
	    	int width = screenResolution.x * 3 / 4;
	    	if (width < MIN_FRAME_WIDTH) {
	    		width = MIN_FRAME_WIDTH;
	    	} else if (width > MAX_FRAME_WIDTH) {
	    		width = MAX_FRAME_WIDTH;
	    	}
	    	int height = screenResolution.y * 1 / 4;
	    	if (height < MIN_FRAME_HEIGHT) {
	    		height = MIN_FRAME_HEIGHT;
	    	} else if (height > MAX_FRAME_HEIGHT) {
	    		height = MAX_FRAME_HEIGHT;
	    	}
	    	int leftOffset = (screenResolution.x - width) / 2;
	    	int topOffset = (screenResolution.y - height) / 4;
	    	framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
	    	Log.d(TAG, "Calculated framing rect: " + framingRect);
	    }
	    return framingRect;
	}
	
	public synchronized Rect getFramingRectInPreview(){
		if(framingRectInPreview == null){
			Rect framingRect = getFramingRect();
			if(framingRect == null){
				return null;
			}
			Rect rect = new Rect(framingRect);
			Point cameraResolution = configManager.getCameraResolution();
			Point screenResolution = configManager.getScreenResolution();
			if(null == cameraResolution || null == screenResolution){
				return null;
			}
			rect.left = rect.left * cameraResolution.x / screenResolution.x;
            rect.right = rect.right * cameraResolution.x / screenResolution.x;
            rect.top = rect.top * cameraResolution.y / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
            framingRectInPreview = rect;
		}
		return framingRectInPreview;
	}
	public synchronized void setManualFramingRect(int width, int height) {
		if (initialized) {
			Point screenResolution = configManager.getScreenResolution();
			if (width > screenResolution.x) {
				width = screenResolution.x;
			}
			if (height > screenResolution.y) {
				height = screenResolution.y;
			}
			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
			Log.d(TAG, "Calculated manual framing rect: " + framingRect);
			framingRectInPreview = null;
		} else {
			requestedFramingRectWidth = width;
			requestedFramingRectHeight = height;
		}
	 }
	/**
	   * A factory method to build the appropriate LuminanceSource object based on the format
	   * of the preview buffers, as described by Camera.Parameters.
	   *
	   * @param data A preview frame.
	   * @param width The width of the image.
	   * @param height The height of the image.
	   * @return A PlanarYUVLuminanceSource instance.
	   */
	  public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
	    Rect rect = getFramingRectInPreview();
	    if (rect == null) {
	      return null;
	    }
	    // Go ahead and assume it's YUV rather than die.
	    return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
	                                        rect.width(), rect.height(), false);
	  }
	
}
