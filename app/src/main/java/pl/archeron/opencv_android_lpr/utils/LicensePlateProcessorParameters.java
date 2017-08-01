package pl.archeron.opencv_android_lpr.utils;


public class LicensePlateProcessorParameters {

    private String sPath;
    private float fResizeRatio;
    private int iPreviewMode;
    private int iMedianBlurKernelSize;
    private int iLineLength;

    public LicensePlateProcessorParameters() {
        sPath = "";
        fResizeRatio = 1.0f;
        iPreviewMode = LicensePlateProcessorAsync.PREVIEW_NONE;
        iMedianBlurKernelSize = 3;
        iLineLength = 7;
    }

    public void setPath(String path) {
        sPath = path;
    }

    public String getPath() {
        return sPath;
    }

    public void setResizeRatio(float resizeRatio) {
        fResizeRatio = resizeRatio;
    }

    public float getResizeRatio() {
        return fResizeRatio;
    }

    public void setPreviewMode(int previewMode) {
        iPreviewMode = previewMode;
    }

    public int getPreviewMode() {
        return iPreviewMode;
    }

    public void setMedianBlurKernelSize(int i) { //Todo kernel size must be odd
        iMedianBlurKernelSize = i;
    }

    public int getMedianBlurKernelSize() {
        return iMedianBlurKernelSize;
    }

    public void setLineLength(int i ) {
        iLineLength = i;
    }

    public int getLineLength() {
        return iLineLength;
    }
}
