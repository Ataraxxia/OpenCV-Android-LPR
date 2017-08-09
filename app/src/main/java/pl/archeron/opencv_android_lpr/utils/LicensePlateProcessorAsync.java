package pl.archeron.opencv_android_lpr.utils;


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
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LicensePlateProcessorAsync extends AsyncTask<LicensePlateProcessorParameters, String, Object> {
    private static final String TAG = "LPRWorker";

    private LicensePlateProcessorCallback listener;

    public LicensePlateProcessorAsync(LicensePlateProcessorCallback listener) {
        this.listener = listener;
    }

    protected Object doInBackground(LicensePlateProcessorParameters... params) {
        LicensePlateProcessorParameters param = params[0];
        String path = param.getPath();
        float fResizeRatio = param.getResizeRatio();
        PreviewMode previewMode = param.getPreviewMode();
        int iMedianBlurKernelSize = param.getMedianBlurKernelSize(); //Must be odd accordng to opencv documentation
        int iLineLength = param.getLineLength();

        //region Loading image file
        publishProgress("Loading file");
        Mat mImage = null;
        try {
            ExifInterface exif = new ExifInterface(path);
            int rotationCode = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            int rotationDegrees = 0;
            if (rotationCode == ExifInterface.ORIENTATION_ROTATE_90) { rotationDegrees = 90; }
            else if (rotationCode == ExifInterface.ORIENTATION_ROTATE_180) {  rotationDegrees = 180; }
            else if (rotationCode == ExifInterface.ORIENTATION_ROTATE_270) {  rotationDegrees = 270; }

            Matrix matrix = new Matrix();
            if (rotationDegrees != 0) {matrix.preRotate(rotationDegrees);}

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = false;

            Bitmap bitmapTmp = BitmapFactory.decodeFile(path, bmOptions);
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
            bitmap.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //endregion

        Imgproc.resize(mImage, mImage, new Size(), fResizeRatio, fResizeRatio, Imgproc.INTER_LANCZOS4);

        publishProgress("Converting to grayscale");
        Mat mGrayscale = new Mat();
        Imgproc.cvtColor(mImage, mGrayscale, Imgproc.COLOR_RGB2GRAY);

        publishProgress("Applying blur");
        Mat mBlurred = new Mat();
        Imgproc.medianBlur(mGrayscale, mBlurred, iMedianBlurKernelSize); //may need to be done twice or none on small resolutions
        Imgproc.medianBlur(mBlurred, mBlurred, iMedianBlurKernelSize);

        //publishProgress("Equalizing histogram");
        //Mat mEqualizedHistogram = new Mat();
        //Imgproc.equalizeHist(mBlurred, mEqualizedHistogram);

        publishProgress("Extracting edges");
        Mat mEdges = new Mat();
        Mat mD = new Mat();
        Mat mE = new Mat();
        Mat straightLineHorizontal = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1,iLineLength));
        Mat straightLineVertical = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(iLineLength, 1));
        Imgproc.dilate(mBlurred, mD, straightLineHorizontal);
        Imgproc.erode(mBlurred, mE, straightLineVertical);
        Core.subtract(mD, mE, mEdges);

        publishProgress("Converting to binary image");
        Mat mBinary = new Mat();
        Imgproc.threshold(mEdges, mBinary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU); // THRESH_BINARY | THRESH_OTSU

        publishProgress("Generating integral image");
        Mat mIntegralImage = new Mat();
        int[][] iIntegralAccumulator = new int[mBinary.height()][mBinary.width()];
        Imgproc.integral(mBinary, mIntegralImage);

        int[] integralImage = new int[mIntegralImage.width()*mIntegralImage.height()];
        mIntegralImage.get(0,0, integralImage);

        float aspectRatio = 4.666667f; //TODO add scaling? If it won't kill performance?
        int rectWidth = 200;
        int rectHeight = (int) (rectWidth / aspectRatio);

        Mat mIntegralMax = null;
        int iIntegralAccumulatorMaxValue = 0; //TODO private class decribing maxval, with fields as val, 2dpoint
        int iIntegralAccumulatorMaxValueX = 0;
        int iIntegralAccumulatorMaxValueY = 0;

        for (int i = 0; i<mIntegralImage.height() - rectHeight; i++)
            for (int j = 0; j < mIntegralImage.width() - rectWidth; j++)
                if(mIntegralImage.get(i,j)[0] > 0) {
                    iIntegralAccumulator[i][j] =
                            integralImage[(i+rectHeight)*mIntegralImage.width() + (j + rectWidth)] -
                            integralImage[(i+rectHeight)*mIntegralImage.width() + j] -
                            integralImage[i*mIntegralImage.width() + (j + rectWidth)] +
                            integralImage[i*mIntegralImage.width() + j];

                    if(iIntegralAccumulator[i][j] > iIntegralAccumulatorMaxValue) {
                        iIntegralAccumulatorMaxValue = iIntegralAccumulator[i][j];
                        iIntegralAccumulatorMaxValueX = j;
                        iIntegralAccumulatorMaxValueY = i;
                    }
                }

        publishProgress("FoundMaximum");
        try {
            Rect area = new Rect(iIntegralAccumulatorMaxValueY, iIntegralAccumulatorMaxValueX, rectWidth, rectHeight);
            mIntegralMax = new Mat(mGrayscale, area);
        }
        catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }

        Mat mPlateBinary = new Mat();
        Imgproc.threshold(mIntegralMax, mPlateBinary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        Mat mPlateBinaryClosed = new MatOfByte();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5));
        Imgproc.morphologyEx(mPlateBinary, mPlateBinaryClosed, Imgproc.MORPH_CLOSE, kernel);

        //region Empty columns and rows removal
        List<Integer> emptyColumnsList = new ArrayList<Integer>();
        List<Integer> emptyRowsList = new ArrayList<Integer>();

        float emptyRowThreshold = 0.60f;
        float emptyColumnThreshold = 0.60f;

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

        Mat mPlateBinaryWithoutZeros1 = new Mat(rectHeight, rectWidth - emptyColumnsList.size(), CvType.CV_8U);
        Mat mPlateBinaryWithoutZeros2 = new Mat(rectHeight - emptyRowsList.size(), rectWidth - emptyColumnsList.size(), CvType.CV_8U);
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

        publishProgress("Done, generating image");
        Mat mat;
        switch (previewMode) {
            case PREVIEW_NONE:
                mat = mImage;
                break;

            case PREVIEW_GRAYSCALE:
                mat = mGrayscale;
                break;

            case PREVIEW_BLURRED:
                mat = mBlurred;
                break;

            case PREVIEW_EDGES:
                mat = mEdges;
                break;

            case PREVIEW_BINARY:
                mat = mBinary;
                break;

            case PREVIEW_INTEGRAL:
                mat = mIntegralMax;
                break;

            case PREVIEW_FINAL:
                mat = mPlateBinaryWithoutZeros2;
                break;

            default:
                mat = mImage;
        }

        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);
        return bmp;
    }

    protected void onProgressUpdate(String... progress) {
        listener.onTaskUpdated(progress[0]);
    }

    protected void onPostExecute(Object result) {
        listener.onTaskCompleted(result);
    }
}
