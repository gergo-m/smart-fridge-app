package me.mgergo.smartfridge;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ComponentCaller;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class FridgeListActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_POST_NOTIFICATION = 1001;
    private static final String LOG_TAG = FridgeListActivity.class.getName();
    private FirebaseUser user;
    private FirebaseAuth fbAuth;
    private FirebaseFirestore db;

    private RecyclerView recyclerView;
    private ArrayList<FridgeItem> itemList;
    private FridgeItemAdapter adapter;

    private int gridNumber = 1;
    private boolean viewRow = true;
    private String sortMode = "expiration";
    private boolean filterSoon = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fridge_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(LOG_TAG, "Authenticated user.");
        } else {
            Log.d(LOG_TAG, "Unauthenticated user.");
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showPermissionExplanationDialog();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        }

        recyclerView = findViewById(R.id.listRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        itemList = new ArrayList<>();

        adapter = new FridgeItemAdapter(this, itemList);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setHasFixedSize(true);

        // initializeData();
        loadItemsFromFirestore();
        checkAndSendExpiryNotifications();

        SharedPreferences prefs = getSharedPreferences("fridge_prefs", MODE_PRIVATE);
        boolean alarmSet = prefs.getBoolean("alarm_set", false);

        if (!alarmSet) {
            scheduleDailyAlarm();
            prefs.edit().putBoolean("alarm_set", true).apply();
        }

        findViewById(R.id.fab).setOnClickListener(view -> {
            if (user != null) {
                startActivityForResult(new Intent(this, AddItemActivity.class), 1);
            } else {
                Toast.makeText(this, "Please log in first!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSortedItemsFromFirestore();
        checkAndSendExpiryNotifications();
    }

    private void loadSortedItemsFromFirestore() {
        Query query;
        if ("abc".equals(sortMode)) {
            query = db.collection("users").document(user.getUid()).collection("items")
                    .orderBy("name", Query.Direction.ASCENDING);
        } else if ("amount".equals(sortMode)) {
            query = db.collection("users").document(user.getUid()).collection("items")
                    .orderBy("amount", Query.Direction.DESCENDING);
        } else {
            query = db.collection("users").document(user.getUid()).collection("items")
                    .orderBy("expirationDate", Query.Direction.ASCENDING);
        }

        query.get().addOnSuccessListener(value -> {
            List<FridgeItem> newItems = new ArrayList<>();
            for (QueryDocumentSnapshot doc : value) {
                String name = doc.getString("name");
                LocalDate expiration = LocalDate.parse(doc.getString("expirationDate"));
                int amount = doc.getLong("amount").intValue();
                int imageResource = doc.getLong("imageResource").intValue();
                String imageUrl = doc.getString("imageUrl");

                FridgeItem item = new FridgeItem(name, expiration, amount, imageResource);
                item.setDocumentId(doc.getId());
                if (imageUrl != null) {
                    item.setImageUrl(imageUrl);
                }
                newItems.add(item);
            }
            runOnUiThread(() -> {
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FridgeItemDiffCallback(itemList, newItems));
                itemList.clear();
                itemList.addAll(newItems);
                diffResult.dispatchUpdatesTo(adapter);
            });
        });
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            sendTestNotification();
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
        }
    });

    private void sendTestNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "expiry_channel")
                .setContentTitle("Thanks for the permission!")
                .setContentText("We'll notify you when an item is about to expire.")
                .setSmallIcon(R.drawable.fridge_icon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(this).notify(0, builder.build());
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Error sending notification: " + e.getMessage());
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Notification permission")
                .setMessage("We need your permission to send expiry notifications.")
                .setPositiveButton("OK", (dialog, which) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                })
                .show();
    }

    private void checkAndSendExpiryNotifications() {
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(3);

        for (FridgeItem item : itemList) {
            LocalDate expiry = item.getExpirationDate();
            if ((expiry.isAfter(today) || expiry.isEqual(today)) && !expiry.isAfter(soon)) {
                NotificationHelper.sendExpiryNotification(this, item);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.fridge_list_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.searchBar);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                adapter.getFilter().filter(text);
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.viewSelector) {
            if (viewRow) {
                changeSpanCount(item, R.drawable.ic_view_grid, 2); // Switch to grid view
            } else {
                changeSpanCount(item, R.drawable.ic_view_row, 1); // Switch to list view
            }
            return true;
        } else if (itemId == R.id.sortSelector) {
            showSortDialog();
            return true;
        } else if (itemId == R.id.filterSoonBtn) {
            filterSoon = !filterSoon;
            loadItemsFromFirestore();
            String msg = filterSoon ? "Showing soon-to-expire items" : "Showing all items";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.addItem) {
            if (user != null) {
                startActivityForResult(new Intent(this, AddItemActivity.class), 1);
            } else {
                Toast.makeText(this, "Please log in first!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } /* else if (itemId == R.id.settingsBtn) {
            // Intent settingsIntent = new Intent(this, SettingsActivity.class);
            // startActivity(settingsIntent);
            return true;
        }*/ else if (itemId == R.id.logoutBtn) {
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSortDialog() {
        String[] sortOptions = {"expiration", "name", "amount"};
        new AlertDialog.Builder(this)
                .setTitle("Sorting by...")
                .setItems(sortOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            sortMode = "expiration";
                            break;
                        case 1:
                            sortMode = "abc";
                            break;
                        case 2:
                            sortMode = "amount";
                            break;
                    }
                    loadSortedItemsFromFirestore();
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            try {
                FridgeItem newItem = (FridgeItem) data.getSerializableExtra("newItem");
                if (newItem != null) {
                    addItemToFirestore(newItem);
                }
            } catch (Exception ex) {
                Log.e(LOG_TAG, "Error creating new item", ex);
                Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addItemToFirestore(FridgeItem item) {
        db.collection("users").document(user.getUid()).collection("items")
                .add(item.toMap())
                .addOnSuccessListener(documentReference -> {
                    item.setDocumentId(documentReference.getId());
                    Log.d(LOG_TAG, "Item added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error adding item", e));
    }

    private void loadItemsFromFirestore() {
        Query query;
        if (filterSoon) {
            LocalDate today = LocalDate.now();
            LocalDate soon = today.plusDays(3);
            query = db.collection("users").document(user.getUid()).collection("items")
                    .whereGreaterThanOrEqualTo("expirationDate", today.toString())
                    .whereLessThanOrEqualTo("expirationDate", soon.toString())
                    .orderBy("expirationDate", Query.Direction.ASCENDING);
        } else {
            query = db.collection("users").document(user.getUid()).collection("items")
                    .orderBy("expirationDate", Query.Direction.ASCENDING);
        }

        query.get().addOnSuccessListener(value -> {
                List<FridgeItem> newItems = new ArrayList<>();
                for (QueryDocumentSnapshot doc : value) {
                    String name = doc.getString("name");
                    LocalDate expiration = LocalDate.parse(doc.getString("expirationDate"));
                    int amount = doc.getLong("amount").intValue();
                    int imageResource = doc.getLong("imageResource").intValue();
                    String imageUrl = doc.getString("imageUrl");

                    FridgeItem item = new FridgeItem(name, expiration, amount, imageResource);
                    item.setDocumentId(doc.getId());
                    if (imageUrl != null) {
                        item.setImageUrl(imageUrl);
                    }
                    newItems.add(item);
                }
                runOnUiThread(() -> {
                    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FridgeItemDiffCallback(itemList, newItems));
                    itemList.clear();
                    itemList.addAll(newItems);
                    diffResult.dispatchUpdatesTo(adapter);
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        // checkAndSendExpiryNotifications();
    }

    public void updateItemAmount(FridgeItem item, int newAmount) {
        if (item.getDocumentId() == null || item.getDocumentId().isEmpty()) {
            Log.e(LOG_TAG, "Error: documentId null");
            Toast.makeText(this, "Error: documentId null", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(user.getUid()).collection("items")
                .document(item.getDocumentId())
                .update("amount", newAmount)
                .addOnSuccessListener(aVoid -> {
                    int index = itemList.indexOf(item);
                    if (index != -1) {
                        itemList.get(index).setAmount(newAmount);
                    }
                })
                .addOnFailureListener(e -> Log.w(LOG_TAG, "Update failed", e));
    }

    public void deleteItem(FridgeItem item) {
        int position = itemList.indexOf(item);
        if (position != -1) {
            db.collection("users").document(user.getUid()).collection("items")
                    .document(item.getDocumentId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        itemList.remove(position);
                        adapter.notifyItemRemoved(position);
                    });
        }
    }

    private void scheduleDailyAlarm() {
        Intent intent = new Intent(this, FridgeAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Minden nap reggel 8:00
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }



    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);

        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(layoutManager);
    }
}