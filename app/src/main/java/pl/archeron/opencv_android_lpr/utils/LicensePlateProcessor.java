package pl.archeron.opencv_android_lpr.utils;


import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


public class LicensePlateProcessor {

    private static final String TAG = "LicensePlateProcessor";

    private Mat mImage;
    private Mat mGrayscale;

    public LicensePlateProcessor(Bitmap bmp) {
        mImage = new Mat();
        Utils.bitmapToMat(bmp, mImage);
    }

    public Bitmap getGrayscale() {
        return mat2bmp(mGrayscale);
    }

    private Bitmap mat2bmp(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);
        return bmp;
    }

    public void preprocess() {
        mGrayscale = new Mat();
        Imgproc.cvtColor(mImage, mGrayscale, Imgproc.COLOR_RGB2GRAY);
    }


}
