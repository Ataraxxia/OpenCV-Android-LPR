package pl.archeron.opencv_android_lpr;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import pl.archeron.opencv_android_lpr.utils.MyJavaCameraView;

public class PhotoRoiActivity extends AppCompatActivity implements CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG = "PhotoRoiActivity";

    private MyJavaCameraView mOpenCvCameraView;

    private Point mROISize;
    private Point mROIAnchorPoint;

    private List<android.hardware.Camera.Size> mResolutionList;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;

    private List<String> mFocusModeList;
    private MenuItem[] mFocusModeMenuItems;
    private SubMenu mFocusModeMenu;

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 0.25f;

    private float mAspectRatio = 4.666667f;
    private int mMinROIWidth = 140;
    private int mMaxROIWidth = 400;

    private Point mPreviewResolution;

    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_roi);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mPreviewResolution = null;

        mROISize = new Point(200, (int) (200 / mAspectRatio));

        mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());

        mOpenCvCameraView = (MyJavaCameraView) findViewById(R.id.photo_roi_camera_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.setOnTouchListener(PhotoRoiActivity.this);

        mOpenCvCameraView.enableView();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mROIAnchorPoint = new Point(width/2 - mROISize.x/2, height/2 - mROISize.y/2);

        mPreviewResolution = new Point(width, height);
    }

    @Override
    public void onCameraViewStopped() {

    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();

        Imgproc.rectangle(frame, mROIAnchorPoint, new Point(mROIAnchorPoint.x + mROISize.x, mROIAnchorPoint.y + mROISize.y), new Scalar(255, 0, 0), 1);

        return frame;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.i(TAG,"onTouch event");

        mScaleDetector.onTouchEvent(motionEvent);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<android.hardware.Camera.Size> resolutionItr = mResolutionList.listIterator();
        int idx = 0;
        while(resolutionItr.hasNext()) {
            android.hardware.Camera.Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(1, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
        }

        mFocusModeMenu = menu.addSubMenu("Focus");
        mFocusModeList = mOpenCvCameraView.getFocusModeList();
        mFocusModeMenuItems = new MenuItem[mFocusModeList.size()];

        ListIterator<String> focusItr = mFocusModeList.listIterator();
        idx = 0;
        while(focusItr.hasNext()) {
            String element = focusItr.next();
            mFocusModeMenuItems[idx] = mFocusModeMenu.add(2, idx, Menu.NONE, element);
            idx++;

        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1)
        {
            int id = item.getItemId();
            android.hardware.Camera.Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            mROIAnchorPoint = new Point(resolution.width/2 - mROISize.x/2, resolution.height/2 - mROISize.y/2);
            mPreviewResolution = new Point(resolution.width, resolution.height);
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        } else if(item.getGroupId() == 2) {
            int id = item.getItemId();
            String focusMode = mFocusModeList.get(id);
            mOpenCvCameraView.setFocusMode(focusMode);

            Toast.makeText(this, focusMode, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public void onTapTakePhoto(View v) {
        try {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = image.getAbsolutePath();

            mOpenCvCameraView.takePicture(mCurrentPhotoPath);

            Toast.makeText(this, "Picture taken", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {

        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            // TODO Auto-generated method stub
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.i(TAG, "Scale gesture detected");

            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.95f, Math.min(mScaleFactor, 1.05f));

            int rectWidth = Math.max(mMinROIWidth, Math.min((int) (mROISize.x * mScaleFactor), mMaxROIWidth));
            int rectHeight = (int) (rectWidth / mAspectRatio);

            mROISize = new Point( rectWidth, rectHeight);
            mROIAnchorPoint = new Point(mPreviewResolution.x/2 - mROISize.x/2, mPreviewResolution.y/2 - mROISize.y/2);

            return true;
        }
    }

}
