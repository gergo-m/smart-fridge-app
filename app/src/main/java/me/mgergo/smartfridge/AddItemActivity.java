package me.mgergo.smartfridge;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDate;

public class AddItemActivity extends AppCompatActivity {
    private EditText editTextName, editTextAmount, editTextExpiration;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_item);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextName = findViewById(R.id.editTextItemName);
        editTextAmount = findViewById(R.id.editTextItemAmount);
        editTextExpiration = findViewById(R.id.editTextExpirationDate);

        findViewById(R.id.buttonSaveItem).setOnClickListener(view -> saveItem());
    }

    private void saveItem() {
        String name = editTextName.getText().toString();
        int amount = Integer.parseInt(editTextAmount.getText().toString());
        LocalDate expiration = LocalDate.parse(editTextExpiration.getText().toString());

        FridgeItem newItem = new FridgeItem(name, expiration, amount, R.drawable.apple);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("newItem", newItem);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
