package pl.archeron.opencv_android_lpr;

import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import pl.archeron.opencv_android_lpr.utils.LicensePlateProcessorAsync;
import pl.archeron.opencv_android_lpr.utils.LicensePlateProcessorCallback;
import pl.archeron.opencv_android_lpr.utils.LicensePlateProcessorParameters;
import pl.archeron.opencv_android_lpr.utils.PreviewMode;

public class PhotoActivity extends AppCompatActivity implements LicensePlateProcessorCallback {

    private static final String TAG = "PhotoActivity";
    private String mCurrentPhotoPath;

    static final int REQUEST_IMAGE_CAPTURE = 1;;

    private ImageView imageView;
    private TextView textView;
    private SubMenu myPreviewMenu;
    private MenuItem[] myPreviewItems;

    private LicensePlateProcessorParameters lprParameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);

        lprParameters = new LicensePlateProcessorParameters();

        if(savedInstanceState != null) {
            Bitmap bitmap = savedInstanceState.getParcelable("image");
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(ImageView.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(imageView.getVisibility() == ImageView.VISIBLE) {
            BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
            Bitmap bitmap = drawable.getBitmap();
            outState.putParcelable("image", bitmap);
            super.onSaveInstanceState(outState);
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
        Log.i(TAG, mCurrentPhotoPath);

        return image;
    }

    public void onButtonClickTakePhoto(View v) {
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

    public void onButtonDebugClick(View v) {
        String p = "/storage/emulated/0/Android/data/pl.archeron.opencv_android_lpr/files/Pictures/v1.jpg";
        lprParameters.setPath(p);
        lprParameters.setResizeRatio(0.25f);

        LicensePlateProcessorAsync lprA = new LicensePlateProcessorAsync(this);
        lprA.execute(lprParameters);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            lprParameters.setPath(mCurrentPhotoPath);
            lprParameters.setResizeRatio(0.25f);

            LicensePlateProcessorAsync lprA = new LicensePlateProcessorAsync(this); //TODO parametrize scaling
            lprA.execute(lprParameters);
        }
    }

    @Override
    public void onTaskCompleted(Object result) {
        imageView.setImageBitmap((Bitmap)result);
        imageView.setVisibility(ImageView.VISIBLE);
    }

    @Override
    public void onTaskUpdated(String update) {
        textView.setText(update);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        List<String> previewNames = PreviewMode.getNames();
        myPreviewMenu = menu.addSubMenu("Preview mode");
        myPreviewItems = new MenuItem[previewNames.size()];

        int idx = 0;
        ListIterator<String> effectItr = previewNames.listIterator();
        while(effectItr.hasNext()) {
            String element = effectItr.next();
            myPreviewItems[idx] = myPreviewMenu.add(1, idx, Menu.NONE, element);
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
            lprParameters.setPreviewMode((String) item.getTitle());
            Toast.makeText(this, lprParameters.getPreviewMode().name(), Toast.LENGTH_SHORT).show();
        }

        return true;
    }
}
