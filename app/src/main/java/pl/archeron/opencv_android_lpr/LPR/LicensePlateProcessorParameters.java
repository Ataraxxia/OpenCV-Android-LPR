package pl.archeron.opencv_android_lpr.LPR;

import org.opencv.core.Mat;
import org.opencv.core.Point;


public class LicensePlateProcessorParameters {

    private Mat mat;

    public LicensePlateProcessorParameters(Mat mat) {
        this.mat = mat;
    }

    public Mat getMat() {
        return mat;
    }
}
