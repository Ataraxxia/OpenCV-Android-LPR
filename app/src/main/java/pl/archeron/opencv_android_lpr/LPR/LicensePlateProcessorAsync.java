package pl.archeron.opencv_android_lpr.LPR;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LicensePlateProcessorAsync extends AsyncTask<LicensePlateProcessorParameters, String, String> {
    private static final String TAG = "LPRWorker";

    private LicensePlateProcessorCallback listener;

    public LicensePlateProcessorAsync(LicensePlateProcessorCallback listener) {
        this.listener = listener;
    }

    protected String doInBackground(LicensePlateProcessorParameters... params) {
        LicensePlateProcessorParameters param = params[0];
        String mPath = param.getPath();
        Point mAnchor = param.getAnchor();
        Point mRectangleSize = param.getRectangleSize();
        Rect rect = new Rect(mAnchor, mRectangleSize);

        //region Loading image file
        publishProgress("Loading file");
        Mat mImage = null;
        try {
            ExifInterface exif = new ExifInterface(mPath);
            int rotationCode = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            int rotationDegrees = 0;
            if (rotationCode == ExifInterface.ORIENTATION_ROTATE_90) { rotationDegrees = 90; }
            else if (rotationCode == ExifInterface.ORIENTATION_ROTATE_180) {  rotationDegrees = 180; }
            else if (rotationCode == ExifInterface.ORIENTATION_ROTATE_270) {  rotationDegrees = 270; }

            Matrix matrix = new Matrix();
            if (rotationDegrees != 0) {matrix.preRotate(rotationDegrees);}

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = false;

            Bitmap bitmapTmp = BitmapFactory.decodeFile(mPath, bmOptions);
            Bitmap bitmap = Bitmap.createBitmap(
                    bitmapTmp,
                    0,
                    0,
                    bitmapTmp.getWidth(),
                    bitmapTmp.getHeight(),
                    matrix,
                    true);
            mImage = new Mat();
            Utils.bitmapToMat(bitmap, mImage);
            mImage = new Mat(mImage, rect);
            bitmap.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //endregion

        Mat mGrayscale = new Mat();
        Imgproc.cvtColor(mImage, mGrayscale, Imgproc.COLOR_RGB2GRAY);

        Mat mPlateBinary = new Mat();
        Imgproc.threshold(mGrayscale, mPlateBinary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        Mat mPlateBinaryClosed = new MatOfByte();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5));
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

        Mat mPlateBinaryWithoutZeros1 = new Mat((int)mRectangleSize.y, (int)mRectangleSize.x - emptyColumnsList.size(), CvType.CV_8U);
        Mat mPlateBinaryWithoutZeros2 = new Mat((int)mRectangleSize.y - emptyRowsList.size(), (int)mRectangleSize.x - emptyColumnsList.size(), CvType.CV_8U);
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

        return null;
    }

    protected void onProgressUpdate(String... progress) {
        listener.onTaskUpdated(progress[0]);
    }

    protected void onPostExecute(String result) {
        listener.onTaskCompleted(result);
    }
}