package pl.archeron.opencv_android_lpr.utils;


import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;
import android.hardware.Camera.Size;

import java.io.FileOutputStream;
import java.util.List;

public class MyJavaCameraView extends JavaCameraView implements Camera.PictureCallback {

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

    public void takePicture(final String fileName) {
        Log.i(TAG, "Taking picture");
        this.mPictureFileName = fileName;
        // Postview and jpeg are sent in the same buffers if the queue is not empty when performing a capture.
        // Clear up buffers to avoid mCamera.takePicture to be stuck because of a memory issue
        mCamera.setPreviewCallback(null);

        // PictureCallback is implemented by the current class
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i(TAG, "Saving a bitmap to file");
        // The camera preview was automatically stopped. Start it again.
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);

        // Write the image in a file (in jpeg format)
        try {
            FileOutputStream fos = new FileOutputStream(mPictureFileName);

            fos.write(data);
            fos.close();

        } catch (java.io.IOException e) {
            Log.e("PictureDemo", "Exception in photoCallback", e);
        }

    }
}
