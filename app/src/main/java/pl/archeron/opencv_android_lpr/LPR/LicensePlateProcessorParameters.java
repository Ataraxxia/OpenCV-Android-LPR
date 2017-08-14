package pl.archeron.opencv_android_lpr.LPR;

import org.opencv.core.Mat;
import org.opencv.core.Point;


public class LicensePlateProcessorParameters {

    private String path;
    private Point anchor;
    private Point rectangleSize;

    private Mat mat;

    public LicensePlateProcessorParameters(String path, Point anchor, Point rectangleSize) {
        this.path = path;
        this.anchor = anchor;
        this.rectangleSize = rectangleSize;
    }

    public LicensePlateProcessorParameters(Mat mat) {
        this.mat = mat;
    }

    public Mat getMat() {
        return mat;
    }

    public String getPath() {
        return path;
    }

    public Point getAnchor() {
        return anchor;
    }

    public Point getRectangleSize() {
        return rectangleSize;
    }
}
