package com.commondavis.app.camera;

import android.hardware.Camera;

public class DefaultOpenCameraInterface implements OpenCameraInterface {

	@Override
	public Camera open() {
		// TODO Auto-generated method stub
		return Camera.open();
	}

}
