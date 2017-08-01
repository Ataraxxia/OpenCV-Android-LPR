package pl.archeron.opencv_android_lpr.utils;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LicensePlateProcessorAsync extends AsyncTask<LicensePlateProcessorParameters, String, Object> {
    public static final int PREVIEW_NONE = 0;
    public static final int PREVIEW_GRAYSCALE = 1;
    public static final int PREVIEW_BLURRED = 2;
    public static final int PREVIEW_EQ_HISTOGRAM = 3;
    public static final int PREVIEW_EDGES = 4;
    public static final int PREVIEW_BINARY = 5;
    public static final int PREVIEW_CONTOURS = 6;

    private LicensePlateProcessorCallback listener;

    public LicensePlateProcessorAsync(LicensePlateProcessorCallback listener) {
        this.listener = listener;
    }

    protected Object doInBackground(LicensePlateProcessorParameters... params) {
        LicensePlateProcessorParameters param = params[0];
        String path = param.getPath();
        float fResizeRatio = param.getResizeRatio();
        int iPreview = param.getPreviewMode();
        int iMedianBlurKernelSize = param.getMedianBlurKernelSize(); //Must be odd accordng to opencv documentation
        int iLineLength = param.getLineLength();

        Mat mImage = null;
        Mat mGrayscale;
        Mat mBlurred;
        Mat mEqualizedHistogram;
        Mat mEdges;
        Mat mBinary;
        Mat mContours;

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
        mGrayscale = new Mat();
        Imgproc.cvtColor(mImage, mGrayscale, Imgproc.COLOR_RGB2GRAY);

        publishProgress("Applying blur");
        mBlurred = new Mat();
        Imgproc.medianBlur(mGrayscale, mBlurred, iMedianBlurKernelSize); //may need to be done twice or none on small resolutions
        Imgproc.medianBlur(mBlurred, mBlurred, iMedianBlurKernelSize);

        publishProgress("Equalizing histogram");
        mEqualizedHistogram = new Mat();
        Imgproc.equalizeHist(mBlurred, mEqualizedHistogram);

        publishProgress("Extracting edges");
        Mat mD = new Mat();
        Mat mE = new Mat();
        mEdges = new Mat();
        Mat straightLineHorizontal = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1,iLineLength));
        Mat straightLineVertical = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(iLineLength, 1));
        Imgproc.dilate(mEqualizedHistogram, mD, straightLineHorizontal);
        Imgproc.erode(mEqualizedHistogram, mE, straightLineVertical);
        Core.subtract(mD, mE, mEdges);

        publishProgress("Converting to binary image");
        mBinary = new Mat();
        Imgproc.threshold(mEdges, mBinary, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU); // THRESH_BINARY | THRESH_OTSU

        publishProgress("Detecting contours");
        mContours = mBinary.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mBinary, contours, hierarchy, Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Imgproc.drawContours(mContours, contours, contourIdx, new Scalar(0, 0, 255), -1);
        }

        publishProgress("Done, generating image");
        Mat mat;
        switch (iPreview) {
            case PREVIEW_NONE:
                mat = mImage;
                break;

            case PREVIEW_GRAYSCALE:
                mat = mGrayscale;
                break;

            case PREVIEW_BLURRED:
                mat = mBlurred;
                break;

            case PREVIEW_EQ_HISTOGRAM:
                mat = mEqualizedHistogram;
                break;

            case PREVIEW_EDGES:
                mat = mEdges;
                break;

            case PREVIEW_BINARY:
                mat = mBinary;
                break;

            case PREVIEW_CONTOURS:
                mat = mContours;
                break;

            default:
                mat = null;
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
