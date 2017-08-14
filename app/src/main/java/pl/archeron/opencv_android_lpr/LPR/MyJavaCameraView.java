package pl.archeron.opencv_android_lpr.LPR;


import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;
import android.hardware.Camera.Size;

import java.io.FileOutputStream;
import java.util.List;

public class MyJavaCameraView extends JavaCameraView {

    private static final String TAG = "MyJavaCameraView";
    private String mPictureFileName;

    public MyJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<String> getFocusModeList() {
        Camera.Parameters params = mCamera.getParameters();
        return params.getSupportedFocusModes();
    }

    public void setFocusMode(String focusMode) {
        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(focusMode);
        mCamera.setParameters(params);
    }

    public List<Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(mMaxWidth, mMaxHeight);
    }

    public Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }
}
