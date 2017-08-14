package pl.archeron.opencv_android_lpr.LPR;

public interface LicensePlateProcessorCallback {
    void onTaskCompleted(String output);

    void onTaskUpdated(String update);
}
