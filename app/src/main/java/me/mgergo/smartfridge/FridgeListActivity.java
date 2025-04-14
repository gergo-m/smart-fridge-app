package me.mgergo.smartfridge;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;

public class FridgeListActivity extends AppCompatActivity {

    private static final String LOG_TAG = FridgeListActivity.class.getName();
    private FirebaseUser user;
    private FirebaseAuth fbAuth;

    private RecyclerView recyclerView;
    private ArrayList<FridgeItem> itemList;
    private FridgeItemAdapter adapter;

    private int gridNumber = 1;
    private boolean viewRow = true;

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

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(LOG_TAG, "Authenticated user.");
        } else {
            Log.d(LOG_TAG, "Unauthenticated user.");
            finish();
        }

        recyclerView = findViewById(R.id.listRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        itemList = new ArrayList<>();

        adapter = new FridgeItemAdapter(this, itemList);
        recyclerView.setAdapter(adapter);

        initializeData();
    }

    private void initializeData() {
        String[] itemsList = getResources().getStringArray(R.array.fridge_item_names);
        String[] itemsExpirationDate = getResources().getStringArray(R.array.fridge_item_expiration_dates);
        int[] itemsAmount = getResources().getIntArray(R.array.fridge_item_amounts);
        TypedArray itemsImageResource = getResources().obtainTypedArray(R.array.fridge_item_images);

        itemList.clear();

        for (int i = 0; i < itemsList.length; i++) {
            try {
                LocalDate expirationDate = LocalDate.parse(itemsExpirationDate[i]);
                itemList.add(new FridgeItem(itemsList[i], expirationDate, itemsAmount[i], itemsImageResource.getResourceId(i, 0)));
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error parsing expiration date for item: " + itemsList[i], e);
            }
        }

        itemsImageResource.recycle();
        adapter.notifyDataSetChanged();
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
        } else if (itemId == R.id.settingsBtn) {
            // Intent settingsIntent = new Intent(this, SettingsActivity.class);
            // startActivity(settingsIntent);
            return true;
        } else if (itemId == R.id.logoutBtn) {
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);

        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        recyclerView.setLayoutManager(layoutManager);
    }
}