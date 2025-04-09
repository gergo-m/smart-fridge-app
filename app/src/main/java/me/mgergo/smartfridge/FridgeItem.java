package me.mgergo.smartfridge;

import java.time.LocalDate;
import java.util.Date;

public class FridgeItem {
    private String name;
    private LocalDate expirationDate;
    private int amount;
    private final int imageResource;

    public FridgeItem(String name, LocalDate expirationDate, int amount, int imageResource) {
        this.name = name;
        this.expirationDate = expirationDate;
        this.amount = amount;
        this.imageResource = imageResource;
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

}
