package pl.archeron.opencv_android_lpr;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private Intent myIntent;

    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void startPhotoActivity(View v) {
        myIntent = new Intent(this, PhotoActivity.class);
        startActivity(myIntent);
    }
}
