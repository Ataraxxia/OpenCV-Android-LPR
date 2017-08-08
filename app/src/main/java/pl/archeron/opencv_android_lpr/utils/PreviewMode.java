package pl.archeron.opencv_android_lpr.utils;


import java.util.ArrayList;
import java.util.List;

public enum PreviewMode {
    PREVIEW_NONE,
    PREVIEW_GRAYSCALE,
    PREVIEW_BLURRED,
    PREVIEW_EQ_HISTOGRAM,
    PREVIEW_EDGES,
    PREVIEW_BINARY,
    PREVIEW_DENOISED,
    PREVIEW_FINAL,
    PREVIEW_INTEGRAL;

    public static List<String> getNames() {
        List<String> nameList = new ArrayList<String>();

        for( PreviewMode pm : PreviewMode.values()) {
            nameList.add(pm.name());
        }
        return nameList;
    }
}
