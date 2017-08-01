package pl.archeron.opencv_android_lpr.utils;

public interface LicensePlateProcessorCallback {
    void onTaskCompleted(Object output);

    void onTaskUpdated(String update);
}
