package me.mgergo.smartfridge;

import static me.mgergo.smartfridge.ImageUtils.createImageFile;

import me.mgergo.smartfridge.AddItemActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public class EditItemActivity extends AppCompatActivity {
    private static final String LOG_TAG = EditItemActivity.class.getName();
    private FridgeItem currentItem;
    private EditText editTextName, editTextAmount, editTextExpiration;
    private ImageView imagePreview;
    private Uri selectedImageUri;
    private StorageReference storageRef;
    private FirebaseUser user;
    private Uri photoUri;
    private boolean isUploading = false;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_item);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        currentItem = (FridgeItem) getIntent().getSerializableExtra("item");

        editTextName = findViewById(R.id.editTextItemName);
        editTextAmount = findViewById(R.id.editTextItemAmount);
        editTextExpiration = findViewById(R.id.editTextExpirationDate);
        imagePreview = findViewById(R.id.imagePreview);
        storageRef = FirebaseStorage.getInstance().getReference();
        user = FirebaseAuth.getInstance().getCurrentUser();

        editTextName.setText(currentItem.getName());
        editTextAmount.setText(String.valueOf(currentItem.getAmount()));
        editTextExpiration.setText(currentItem.getExpirationDate().toString());

        if (currentItem.getImageUrl() != null) {
            Glide.with(this)
                    .load(currentItem.getImageUrl())
                    .into(imagePreview);
        }

        findViewById(R.id.buttonGallery).setOnClickListener(v -> ImageUtils.openGallery(this, galleryLauncher));
        findViewById(R.id.buttonCamera).setOnClickListener(v -> {
            try {
                File photoFile = createImageFile(this);
                photoUri = FileProvider.getUriForFile(this,
                        "me.mgergo.smartfridge.fileprovider",
                        photoFile);
                ImageUtils.openCamera(this, cameraLauncher, photoUri);
            } catch (IOException ex) {
                Toast.makeText(this, "Fájl létrehozási hiba", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.buttonSaveItem).setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        if (isUploading) return;
        isUploading = true;
        findViewById(R.id.buttonSaveItem).setEnabled(false);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        String name = editTextName.getText().toString();
        int amount = Integer.parseInt(editTextAmount.getText().toString());
        LocalDate expiration = LocalDate.parse(editTextExpiration.getText().toString());

        currentItem.setName(name);
        currentItem.setAmount(amount);
        currentItem.setExpirationDate(expiration);

        if (selectedImageUri != null) {
            uploadNewImage(name);
        } else {
            updateFirestore();
        }
    }

    private void uploadNewImage(String name) {
        String imageName = "image_" + name.toLowerCase().replace(" ", "-") + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child("users/" + user.getUid() + "/images/" + imageName);
        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        currentItem.setImageUrl(uri.toString());
                        selectedImageUri = null;
                        updateFirestore();
                    });
                })
                .addOnFailureListener(ex -> {
                    isUploading = false;
                    findViewById(R.id.buttonSaveItem).setEnabled(true);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    Toast.makeText(this, "Image update failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFirestore() {
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .collection("items")
                .document(currentItem.getDocumentId())
                .set(currentItem.toMap())
                .addOnSuccessListener(aVoid -> {
                    isUploading = false;
                    findViewById(R.id.buttonSaveItem).setEnabled(true);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    finish();
                })
                .addOnFailureListener(ex -> {
                    isUploading = false;
                    findViewById(R.id.buttonSaveItem).setEnabled(true);
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    Toast.makeText(this, "Update failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadImage(Uri imageUri) {
        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(imagePreview);
    }
}
