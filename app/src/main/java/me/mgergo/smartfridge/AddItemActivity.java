package me.mgergo.smartfridge;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.time.LocalDate;

public class AddItemActivity extends AppCompatActivity {
    private static final String LOG_TAG = AddItemActivity.class.getName();
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText editTextName, editTextAmount, editTextExpiration;
    private ImageView imagePreview;
    private Uri selectedImageUri;

    // Activity Result API-val
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(imagePreview);
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
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void saveItem() {
        try {
            String name = editTextName.getText().toString();
            int amount = Integer.parseInt(editTextAmount.getText().toString());
            LocalDate expiration = LocalDate.parse(editTextExpiration.getText().toString());
            int imageResourceId = R.drawable.apple;

            FridgeItem newItem;
            if (selectedImageUri != null) {
                newItem = new FridgeItem(name, expiration, amount, imageResourceId);
                newItem.setImageUri(selectedImageUri.toString());
            } else {
                newItem = new FridgeItem(name, expiration, amount, imageResourceId);
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
