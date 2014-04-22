package com.commondavis.app.camera;

/**
 * Selects an appropriate implementation of {@link OpenCameraInterface} based on the device's
 * API level.
 */
public final class OpenCameraManager extends PlatformSupportManager<OpenCameraInterface> {

  public OpenCameraManager() {
    super(OpenCameraInterface.class, new DefaultOpenCameraInterface());
    addImplementationClass(9, "com.commondavis.app.camera.GingerbreadOpenCameraInterface");
  }

}
