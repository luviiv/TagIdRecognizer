/**
 * 
 */
package com.commondavis.app.camera;

import android.content.SharedPreferences;

/**
 * @author Davis Lu
 *Enumerates settings of the prefernce controlling the front light.
 */
public enum FrontLightMode {
	/** Always on. */
	  ON,
	  /** On only when ambient light is low. */
	  AUTO,
	  /** Always off. */
	  OFF;

	  private static FrontLightMode parse(String modeString) {
	    return modeString == null ? OFF : valueOf(modeString);
	  }

	  public static FrontLightMode readPref(SharedPreferences sharedPrefs) {
	    return parse(sharedPrefs.getString(CameraConfigurationManager.KEY_FRONT_LIGHT_MODE, null));
	  }

}
