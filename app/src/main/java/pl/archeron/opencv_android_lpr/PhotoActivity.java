package pl.archeron.opencv_android_lpr;

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import pl.archeron.opencv_android_lpr.utils.LicensePlateProcessor;

public class PhotoActivity extends AppCompatActivity {

    private static final String TAG = "PhotoActivity";
    private String mCurrentPhotoPath;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageView imageView;
    private TextView textView;

    private Bitmap bitmapTmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        bitmapTmp = null;

        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            imageView.setImageBitmap(bitmapTmp);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            imageView.setImageBitmap(bitmapTmp);
        }
    }

    private File createImageFile() throws IOException {
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
        return image;
    }

    public void dispatchTakePictureIntent(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(this,
                        "pl.archeron.opencv_android_lpr.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    Log.i(TAG, "Intent URI 1");
                }
                else if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip=
                            ClipData.newUri(getContentResolver(), "A photo", photoUri);

                    takePictureIntent.setClipData(clip);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    Log.i(TAG, "Intent URI 2");
                }
                else {
                    List<ResolveInfo> resInfoList=
                            getPackageManager()
                                    .queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);

                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        grantUriPermission(packageName, photoUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }

                    Log.i(TAG, "Intent URI 3");
                }

                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
                int rotationCode = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                int rotationDegrees = 0;
                if (rotationCode == ExifInterface.ORIENTATION_ROTATE_90) { rotationDegrees = 90; }
                else if (rotationCode == ExifInterface.ORIENTATION_ROTATE_180) {  rotationDegrees = 180; }
                else if (rotationCode == ExifInterface.ORIENTATION_ROTATE_270) {  rotationDegrees = 270; }

                Matrix matrix = new Matrix();
                if (rotationDegrees != 0) {matrix.preRotate(rotationDegrees);}

                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = false;

                Bitmap bitmapTmp = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
                Bitmap bitmap = Bitmap.createBitmap(
                        bitmapTmp,
                        0,
                        0,
                        bitmapTmp.getWidth(),
                        bitmapTmp.getHeight(),
                        matrix,
                        true);

                LicensePlateProcessor lpr = new LicensePlateProcessor(bitmap);
                bitmap.recycle();
                bitmapTmp.recycle();
                lpr.preprocess();

                bitmapTmp = lpr.getGrayscale();
                imageView.setImageBitmap(bitmapTmp);
                textView.setText(String.format("Width: %d Height: %d", bitmapTmp.getWidth(), bitmapTmp.getHeight()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
