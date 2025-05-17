package me.mgergo.smartfridge;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

public class AddItemActivity extends AppCompatActivity {
    private static final String LOG_TAG = AddItemActivity.class.getName();
    private EditText editTextName, editTextAmount, editTextExpiration;
    private ImageView imagePreview;
    private Uri selectedImageUri;
    private Uri photoUri;
    private String currentPhotoPath;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    getContentResolver().takePersistableUriPermission(
                            selectedImageUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                    loadImage(selectedImageUri);
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                getContentResolver(),
                                photoUri
                        );
                        imagePreview.setImageBitmap(bitmap);
                    } catch (IOException ex) {
                        Log.e(LOG_TAG, "Error loading image", ex);
                    }
                    selectedImageUri = photoUri;
                    loadImage(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        editTextName = findViewById(R.id.editTextItemName);
        editTextAmount = findViewById(R.id.editTextItemAmount);
        editTextExpiration = findViewById(R.id.editTextExpirationDate);
        imagePreview = findViewById(R.id.imagePreview);

        findViewById(R.id.buttonGallery).setOnClickListener(v -> openGallery());
        findViewById(R.id.buttonSaveItem).setOnClickListener(v -> saveItem());

        initializeViews();
        setupButtons();
    }

    private void initializeViews() {
        editTextName = findViewById(R.id.editTextItemName);
        editTextAmount = findViewById(R.id.editTextItemAmount);
        editTextExpiration = findViewById(R.id.editTextExpirationDate);
        imagePreview = findViewById(R.id.imagePreview);
    }

    private void setupButtons() {
        Button btnGallery = findViewById(R.id.buttonGallery);
        Button btnCamera = findViewById(R.id.buttonCamera);
        Button btnSave = findViewById(R.id.buttonSaveItem);

        btnGallery.setOnClickListener(v -> openGallery());
        btnCamera.setOnClickListener(v -> openCamera());
        btnSave.setOnClickListener(v -> saveItem());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(this, "me.mgergo.smartfridge.fileprovider", photoFile);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    cameraLauncher.launch(takePictureIntent);
                }
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Error creating image file", ex);
        } catch (Exception ex) {
            Toast.makeText(this, "Unexpected error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Exception", ex);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    private void loadImage(Uri imageUri) {
        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(imagePreview);
    }

    private void saveItem() {
        try {
            String name = editTextName.getText().toString();
            int amount = Integer.parseInt(editTextAmount.getText().toString());
            LocalDate expiration = LocalDate.parse(editTextExpiration.getText().toString());

            FridgeItem newItem = new FridgeItem(name, expiration, amount, R.drawable.apple);
            if (selectedImageUri != null) {
                newItem.setImageUri(selectedImageUri.toString());
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("newItem", newItem);
            setResult(RESULT_OK, resultIntent);
            finish();
        } catch (Exception ex) {
            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Error saving item", ex);
        }
    }
}
