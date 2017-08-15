package pl.archeron.opencv_android_lpr.LPR;

import android.graphics.Bitmap;

public interface LicensePlateProcessorCallback {
    void onTaskCompleted(Bitmap output);

    void onTaskUpdated(String update);
}
