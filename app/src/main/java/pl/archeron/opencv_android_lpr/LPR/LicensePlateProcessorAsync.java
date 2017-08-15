package pl.archeron.opencv_android_lpr.LPR;


import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class LicensePlateProcessorAsync extends AsyncTask<LicensePlateProcessorParameters, String, Bitmap> {
    private static final String TAG = "LPRWorker";

    private LicensePlateProcessorCallback listener;

    public LicensePlateProcessorAsync(LicensePlateProcessorCallback listener) {
        this.listener = listener;
    }

    protected Bitmap doInBackground(LicensePlateProcessorParameters... params) {
        LicensePlateProcessorParameters param = params[0];
        Mat mImage = param.getMat();

        Mat mBlurred = new Mat();
        Imgproc.blur(mImage, mBlurred, new Size(3,3));

        Mat mEdges = new Mat();
        Imgproc.Canny(mBlurred, mEdges, 1, 3);
/*
        Mat mGrayscale = new Mat();
        Imgproc.cvtColor(mImage, mGrayscale, Imgproc.COLOR_RGB2GRAY);

        Mat mEqualizedHistogram = new Mat();
        Imgproc.equalizeHist(mGrayscale, mEqualizedHistogram);

        publishProgress("Extracting edges");
        Mat mEdges = new Mat();
        Mat mD = new Mat();
        Mat mE = new Mat();
        Mat straightLineHorizontal = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1,5));
        Mat straightLineVertical = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 1));
        Imgproc.dilate(mEqualizedHistogram, mD, straightLineHorizontal);
        Imgproc.erode(mEqualizedHistogram, mE, straightLineVertical);
        Core.subtract(mD, mE, mEdges);
*/


        Mat mPlateBinary = new Mat();
        Imgproc.threshold(mEdges, mPlateBinary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        Mat mPlateBinaryClosed = new MatOfByte();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));
        Imgproc.morphologyEx(mPlateBinary, mPlateBinaryClosed, Imgproc.MORPH_CLOSE, kernel);

        //region Empty columns and rows removal
        publishProgress("Empty rows and columns removal");
        List<Integer> emptyColumnsList = new ArrayList<Integer>();
        List<Integer> emptyRowsList = new ArrayList<Integer>();

        float emptyRowThreshold = 0.90f;
        float emptyColumnThreshold = 0.90f;

        byte plateBinary[] = new byte[mPlateBinary.width() * mPlateBinary.height()];
        mPlateBinaryClosed.get(0,0,plateBinary);

        int plateBinaryWidth = mPlateBinary.width();
        int plateBinaryHeight = mPlateBinary.height();
        int zeroPixCounter = 0;
        for(int x = 0; x < plateBinaryWidth; x++) {
            for(int y = 0; y < plateBinaryHeight; y++) {
                byte pix = plateBinary[y * plateBinaryWidth + x];
                if (pix == 0)
                    zeroPixCounter++;
            }
            if(zeroPixCounter >= plateBinaryHeight*emptyColumnThreshold) { //if more than 95% of pixels are empty, add column to removal list
                emptyColumnsList.add(x);
            }
            zeroPixCounter = 0;
        }

        zeroPixCounter = 0;
        for(int y = 0; y < plateBinaryHeight; y++) {
            for(int x = 0; x < plateBinaryWidth; x++) {
                byte pix = plateBinary[y * plateBinaryWidth + x];
                if(pix == 0)
                    zeroPixCounter++;
            }
            if(zeroPixCounter >= plateBinaryWidth*emptyRowThreshold) {
                emptyRowsList.add(y);
            }
            zeroPixCounter = 0;
        }

        Mat mPlateBinaryWithoutZeros1 = new Mat(mImage.height(), mImage.width() - emptyColumnsList.size(), CvType.CV_8U);
        Mat mPlateBinaryWithoutZeros2 = new Mat(mImage.height() - emptyRowsList.size(), mImage.width() - emptyColumnsList.size(), CvType.CV_8U);
        Log.i(TAG, "ROWS: " + Integer.toString(emptyRowsList.size()));
        Log.i(TAG, "COLS: " + Integer.toString(emptyColumnsList.size()));

        Log.i(TAG, "M1: " + Integer.toString(mPlateBinaryWithoutZeros1.width()) + "x" + Integer.toString(mPlateBinaryWithoutZeros1.height()));
        Log.i(TAG, "M2: " + Integer.toString(mPlateBinaryWithoutZeros2.width()) + "x" + Integer.toString(mPlateBinaryWithoutZeros2.height()));

        int hit = 0;
        for(int i = 0; i < plateBinaryWidth; i++) {
            if(!emptyColumnsList.contains(i)) {
                mPlateBinary.col(i).copyTo(mPlateBinaryWithoutZeros1.col(i - hit));
            } else {
                hit++;
            }
        }

        hit = 0;
        for(int i = 0; i < plateBinaryHeight; i++) {
            if(!emptyRowsList.contains(i)) {
                mPlateBinaryWithoutZeros1.row(i).copyTo(mPlateBinaryWithoutZeros2.row(i - hit));
            } else {
                hit++;
            }
        }
        //endregion

        Mat mat = mPlateBinaryClosed;
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);
        return bmp;
    }

    protected void onProgressUpdate(String... progress) {
        listener.onTaskUpdated(progress[0]);
    }

    protected void onPostExecute(Bitmap result) {
        listener.onTaskCompleted(result);
    }
}
