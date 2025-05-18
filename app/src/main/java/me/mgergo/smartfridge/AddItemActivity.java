package me.mgergo.smartfridge;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;

public class AddItemActivity extends AppCompatActivity {
    private static final String LOG_TAG = AddItemActivity.class.getName();
    private FirebaseUser user;
    private FirebaseFirestore db;
    private FridgeItemAdapter adapter;

    private EditText editTextName, editTextAmount, editTextExpiration;
    private ImageView imagePreview;
    private Uri selectedImageUri;
    private Uri photoUri;
    private StorageReference storageRef;
    private ProgressBar progressBar;

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
                    /* try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                getContentResolver(),
                                photoUri
                        );
                        imagePreview.setImageBitmap(bitmap);
                    } catch (IOException ex) {
                        Log.e(LOG_TAG, "Error loading image", ex);
                    } */
                    selectedImageUri = photoUri;
                    loadImage(photoUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_item);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        storageRef = FirebaseStorage.getInstance().getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        editTextName = findViewById(R.id.editTextItemName);
        editTextAmount = findViewById(R.id.editTextItemAmount);
        editTextExpiration = findViewById(R.id.editTextExpirationDate);
        imagePreview = findViewById(R.id.imagePreview);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.buttonGallery).setOnClickListener(v -> ImageUtils.openGallery(this, galleryLauncher));
        findViewById(R.id.buttonSaveItem).setOnClickListener(v -> saveItem());
        setupDatePicker();

        initializeViews();
        setupButtons();
    }

    private void setupDatePicker() {
        editTextExpiration.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    LocalDate selectedDate = LocalDate.of(year, month + 1, day);
                    editTextExpiration.setText(selectedDate.toString());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
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

        btnGallery.setOnClickListener(v -> ImageUtils.openGallery(this, galleryLauncher));
        btnCamera.setOnClickListener(v -> {
            try {
                File photoFile = createImageFile();
                photoUri = FileProvider.getUriForFile(this,
                        "me.mgergo.smartfridge.fileprovider",
                        photoFile);
                ImageUtils.openCamera(this, cameraLauncher, photoUri);
            } catch (IOException ex) {
                Toast.makeText(this, "Fájl létrehozási hiba", Toast.LENGTH_SHORT).show();
            }
        });
        btnSave.setOnClickListener(v -> saveItem());
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return File.createTempFile(
                "JPEG_" + timeStamp + "_",
                ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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
            if (editTextName.getText().toString().isEmpty()
                    || editTextAmount.getText().toString().isEmpty()
                    || editTextExpiration.getText().toString().isEmpty()) {
                Toast.makeText(this, "Fill out all fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            findViewById(R.id.buttonSaveItem).setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            String name = editTextName.getText().toString();
            int amount = Integer.parseInt(editTextAmount.getText().toString());
            LocalDate expiration = LocalDate.parse(editTextExpiration.getText().toString());

            FridgeItem newItem = new FridgeItem(name, expiration, amount, R.drawable.image_default);
            if (selectedImageUri != null) {
                uploadImageToFirebase(newItem);
            } else {
                saveItemToFirestore(newItem);
                finish();
            }
        } catch (DateTimeParseException ex) {
            Toast.makeText(this, "Invalid date format!", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Error saving item", ex);
        }
    }

    private void uploadImageToFirebase(FridgeItem item) {
        if (selectedImageUri != null) {
            String imageName = "image_" + item.getName().toLowerCase().replace(" ", "-") + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageRef.child("users/" + user.getUid() + "/images/" + imageName);

            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            item.setImageUrl(uri.toString());
                            saveItemToFirestore(item);
                            findViewById(R.id.buttonSaveItem).setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                            finish();
                        });
                    })
                    .addOnFailureListener(e -> {
                        findViewById(R.id.buttonSaveItem).setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveItemToFirestore(FridgeItem item) {
        db.collection("users").document(user.getUid()).collection("items")
                .add(item.toMap())
                .addOnSuccessListener(documentReference -> {
                    findViewById(R.id.buttonSaveItem).setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Log.d(LOG_TAG, "Item saved: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    findViewById(R.id.buttonSaveItem).setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error saving item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
