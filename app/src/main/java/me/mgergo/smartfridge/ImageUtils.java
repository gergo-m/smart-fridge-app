package me.mgergo.smartfridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageUtils {
    public static void openGallery(Activity activity, ActivityResultLauncher<Intent> galleryLauncher) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    public static void openCamera(Activity activity,
                                  ActivityResultLauncher<Intent> cameraLauncher,
                                  Uri photoUri) {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                cameraLauncher.launch(takePictureIntent);
            }
        } catch (Exception ex) {
            Toast.makeText(activity, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    static File createImageFile(Activity activity) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return File.createTempFile(
                "JPEG_" + timeStamp + "_",
                ".jpg",
                activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        );
    }
}
