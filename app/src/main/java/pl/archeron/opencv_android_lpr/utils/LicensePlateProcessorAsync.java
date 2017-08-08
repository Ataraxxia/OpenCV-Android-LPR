package pl.archeron.opencv_android_lpr.utils;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

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

        Mat mImage = null;

        publishProgress("Loading file");
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

        publishProgress("Morphological transformations"); //Might cause problems, subject to remove?
        Mat mDeNoised = new MatOfByte();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(11,11));
        Imgproc.morphologyEx(mBinary, mDeNoised, Imgproc.MORPH_CLOSE, kernel);

        publishProgress("Generating integral image");
        Mat mIntegralImage = new Mat();
        int[][] iIntegralAccumulator = new int[mBinary.height()][mBinary.width()];
        Imgproc.integral(mDeNoised, mIntegralImage);

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

            case PREVIEW_DENOISED:
                mat = mDeNoised;
                break;

            case PREVIEW_INTEGRAL:
                mat = mIntegralMax;
                break;

            case PREVIEW_FINAL:
                mat = mPlateBinary;
                break;

            default:
                mat = mImage;
        }

        Log.i(TAG, "Size: " +  Integer.toString(mat.cols()) + "x" + Integer.toString(mat.rows()));
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
