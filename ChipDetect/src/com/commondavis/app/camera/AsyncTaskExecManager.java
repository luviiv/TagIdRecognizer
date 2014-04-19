package com.commondavis.app.camera;

public class AsyncTaskExecManager extends PlatformSupportManager<AsyncTaskExecInterface>{
	public AsyncTaskExecManager(){
		super(AsyncTaskExecInterface.class, new DefaultAsyncTaskExecInterface());
	    addImplementationClass(11, "com.commondavis.app.camera.HoneycombAsyncTaskExecInterface");
	}
}
