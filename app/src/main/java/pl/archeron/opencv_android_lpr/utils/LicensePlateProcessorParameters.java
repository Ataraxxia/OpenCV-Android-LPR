package pl.archeron.opencv_android_lpr.utils;

public class LicensePlateProcessorParameters {

    private String sPath;
    private float fResizeRatio;
    private int iMedianBlurKernelSize;
    private int iLineLength;

    private PreviewMode previewMode;

    public LicensePlateProcessorParameters() {
        fResizeRatio = 1.0f;
        previewMode = PreviewMode.PREVIEW_FINAL;
        iMedianBlurKernelSize = 3;
        iLineLength = 9;
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

    public void setPreviewMode(PreviewMode previewMode) {
        previewMode = previewMode;
    }

    public void setPreviewMode(String sPreviewMode) {
        previewMode = PreviewMode.valueOf(sPreviewMode);
    }

    public PreviewMode getPreviewMode() {
        return previewMode;
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
