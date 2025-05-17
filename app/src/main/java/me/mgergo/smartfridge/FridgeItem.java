package me.mgergo.smartfridge;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.firebase.firestore.Exclude;

public class FridgeItem implements Serializable {
    private String name;
    private LocalDate expirationDate;
    private int amount;
    private final int imageResource;
    private String documentId;
    private String imageUri;

    public FridgeItem(String name, LocalDate expirationDate, int amount, int imageResource) {
        this.name = name;
        this.expirationDate = expirationDate;
        this.amount = amount;
        this.imageResource = imageResource;
        this.imageUri = null;
        this.documentId = null;
    }

    public String getName() {
        return name;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public int getAmount() {
        return amount;
    }

    public int getImageResource() {
        return imageResource;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("expirationDate", expirationDate.toString());
        result.put("amount", amount);
        result.put("imageResource", imageResource);
        if (imageUri != null) {
            result.put("imageUri", imageUri);
        }
        return result;
    }
}
