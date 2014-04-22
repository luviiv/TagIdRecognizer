/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commondavis.app.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.regex.Pattern;

final class CameraConfigurationManager {

  private static final String TAG = CameraConfigurationManager.class.getSimpleName();

  public static final String KEY_AUTO_FOCUS = "preferences_auto_focus";
  public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "preferences_disable_continuous_focus";
  public static final String KEY_INVERT_SCAN = "preferences_invert_scan";
  public static final String KEY_FRONT_LIGHT_MODE = "preferences_front_light_mode";
  
  //private static final int TEN_DESIRED_ZOOM = 27;
  private static final int DESIRED_SHARPNESS = 30;

  private static final Pattern COMMA_PATTERN = Pattern.compile(",");

  private final Context context;
  private Point screenResolution;
  private Point cameraResolution;
  private int previewFormat;
  private String previewFormatString;

  CameraConfigurationManager(Context context) {
    this.context = context;
  }

  /**
   * Reads, one time, values from the camera that are needed by the app.
   */
  void initFromCameraParameters(Camera camera) {
    Camera.Parameters parameters = camera.getParameters();
    previewFormat = parameters.getPreviewFormat();
    previewFormatString = parameters.get("preview-format");
    Log.d(TAG, "Default preview format: " + previewFormat + '/' + previewFormatString);
    DisplayMetrics dm=context.getResources().getDisplayMetrics();
    screenResolution = new Point(dm.widthPixels , dm.heightPixels);
    Log.d(TAG, "Screen resolution: " + screenResolution);
    cameraResolution = getCameraResolution(parameters, screenResolution);
    Log.d(TAG, "Camera resolution: " + screenResolution);
  }

  /**
   * Sets the camera up to take preview images which are used for both preview and decoding.
   * We detect the preview format here so that buildLuminanceSource() can build an appropriate
   * LuminanceSource subclass. In the future we may want to force YUV420SP as it's the smallest,
   * and the planar Y can be used for barcode scanning without a copy in some cases.
   */
  void setDesiredCameraParameters(Camera camera, boolean safeMode) {
	  Camera.Parameters parameters = camera.getParameters();

      if (parameters == null) {
          Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
          return;
      }

      Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

      if (safeMode) {
          Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
      }

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

      initializeTorch(parameters, prefs, safeMode);

      String focusMode = null;
      if (prefs.getBoolean(KEY_AUTO_FOCUS, true)) {
          if (safeMode || prefs.getBoolean(KEY_DISABLE_CONTINUOUS_FOCUS, false)) {
              focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                      Camera.Parameters.FOCUS_MODE_AUTO);
          } else {
              focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                      "continuous-picture", // Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE in 4.0+
                      "continuous-video",   // Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO in 4.0+
                      Camera.Parameters.FOCUS_MODE_AUTO);
          }
      }
      // Maybe selected auto-focus but not available, so fall through here:
      if (!safeMode && focusMode == null) {
          focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                  Camera.Parameters.FOCUS_MODE_MACRO,
                  "edof"); // Camera.Parameters.FOCUS_MODE_EDOF in 2.2+
      }
      if (focusMode != null) {
          parameters.setFocusMode(focusMode);
      }

      if (prefs.getBoolean(KEY_INVERT_SCAN, false)) {
          String colorMode = findSettableValue(parameters.getSupportedColorEffects(),
                  Camera.Parameters.EFFECT_NEGATIVE);
          if (colorMode != null) {
              parameters.setColorEffect(colorMode);
          }
      }
      parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
      setDisplayOrientation(camera, 90);
      Log.d(TAG, "preview size is (" + cameraResolution.x+","+cameraResolution.y+")");
      camera.setParameters(parameters);
  }

  Point getCameraResolution() {
    return cameraResolution;
  }

  Point getScreenResolution() {
    return screenResolution;
  }

  int getPreviewFormat() {
    return previewFormat;
  }

  String getPreviewFormatString() {
    return previewFormatString;
  }
  boolean getTorchState(Camera camera) {
      if (camera != null) {
          Camera.Parameters parameters = camera.getParameters();
          if (parameters != null) {
              String flashMode = camera.getParameters().getFlashMode();
              return flashMode != null &&
                      (Camera.Parameters.FLASH_MODE_ON.equals(flashMode) ||
                              Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode));
          }
      }
      return false;
  }

  void setTorch(Camera camera, boolean newSetting) {
      Camera.Parameters parameters = camera.getParameters();
      doSetTorch(parameters, newSetting, false);
      camera.setParameters(parameters);
  }

  private void initializeTorch(Camera.Parameters parameters, SharedPreferences prefs, boolean safeMode) {
      boolean currentSetting = FrontLightMode.readPref(prefs) == FrontLightMode.ON;
      doSetTorch(parameters, currentSetting, safeMode);
  }

  private void doSetTorch(Camera.Parameters parameters, boolean newSetting, boolean safeMode) {
      String flashMode;
      if (newSetting) {
          flashMode = findSettableValue(parameters.getSupportedFlashModes(),
                  Camera.Parameters.FLASH_MODE_TORCH,
                  Camera.Parameters.FLASH_MODE_ON);
      } else {
          flashMode = findSettableValue(parameters.getSupportedFlashModes(),
                  Camera.Parameters.FLASH_MODE_OFF);
      }
      if (flashMode != null) {
          parameters.setFlashMode(flashMode);
      }
  }
  private static String findSettableValue(Collection<String> supportedValues,
          String... desiredValues) {
		Log.i(TAG, "Supported values: " + supportedValues);
		String result = null;
		if (supportedValues != null) {
		for (String desiredValue : desiredValues) {
		if (supportedValues.contains(desiredValue)) {
		result = desiredValue;
		break;
		}
		}
		}
		Log.i(TAG, "Settable value: " + result);
		return result;
  }
  private static Point getCameraResolution(Camera.Parameters parameters, Point screenResolution) {

    String previewSizeValueString = parameters.get("preview-size-values");
    // saw this on Xperia
    if (previewSizeValueString == null) {
      previewSizeValueString = parameters.get("preview-size-value");
    }

    Point cameraResolution = null;

    if (previewSizeValueString != null) {
      Log.d(TAG, "preview-size-values parameter: " + previewSizeValueString);
      cameraResolution = findBestPreviewSizeValue(previewSizeValueString, screenResolution);
    }

    if (cameraResolution == null) {
      // Ensure that the camera resolution is a multiple of 8, as the screen may not be.
      cameraResolution = new Point(
          (screenResolution.x >> 3) << 3,
          (screenResolution.y >> 3) << 3);
    }

    return cameraResolution;
  }

  private static Point findBestPreviewSizeValue(CharSequence previewSizeValueString, Point screenResolution) {
    int bestX = 0;
    int bestY = 0;
    int diff = Integer.MAX_VALUE;
    for (String previewSize : COMMA_PATTERN.split(previewSizeValueString)) {

      previewSize = previewSize.trim();
      int dimPosition = previewSize.indexOf('x');
      if (dimPosition < 0) {
        Log.w(TAG, "Bad preview-size: " + previewSize);
        continue;
      }

      int newX;
      int newY;
      try {
        newX = Integer.parseInt(previewSize.substring(0, dimPosition));
        newY = Integer.parseInt(previewSize.substring(dimPosition + 1));
      } catch (NumberFormatException nfe) {
        Log.w(TAG, "Bad preview-size: " + previewSize);
        continue;
      }

      int newDiff = Math.abs(newX - screenResolution.x) + Math.abs(newY - screenResolution.y);
      if (newDiff == 0) {
        bestX = newX;
        bestY = newY;
        break;
      } else if (newDiff < diff) {
        bestX = newX;
        bestY = newY;
        diff = newDiff;
      }

    }

    if (bestX > 0 && bestY > 0) {
      return new Point(bestX, bestY);
    }
    return null;
  }

  public static int getDesiredSharpness() {
		return DESIRED_SHARPNESS;
	}
	
	/**
	 * compatible  1.6
	 * @param camera
	 * @param angle
	 */
	protected void setDisplayOrientation(Camera camera, int angle){  
        Method downPolymorphic;  
        try  
        {  
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });  
            if (downPolymorphic != null)  
                downPolymorphic.invoke(camera, new Object[] { angle });  
        }  
        catch (Exception e1)  
        {  
        }  
   }  

}
