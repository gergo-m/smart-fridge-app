package me.mgergo.smartfridge;

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
            itemList.add(new FridgeItem(itemsList[i], LocalDate.parse(itemsExpirationDate[i]), itemsAmount[i], itemsImageResource.getResourceId(i, 0)));
        }

        itemsImageResource.recycle();
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.fridge_list_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.searchBar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(LOG_TAG, s);
                adapter.getFilter().filter(s);
                return false;
            }
        });

        return true;
    }

    /*@Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.viewSelector:
                Log.d(LOG_TAG, "View selector clicked");
                if (viewRow) {
                    changeSpanCount(item, R.drawable.ic_view_grid, 1);
                } else {
                    changeSpanCount(item, R.drawable.ic_view_row, 2);
                }
                return true;
            case R.id.settingsBtn:
                Log.d(LOG_TAG, "Settings clicked");
                return true;
            case R.id.logoutBtn:
                Log.d(LOG_TAG, "Logout clicked");
                FirebaseAuth.getInstance().signOut();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);
        GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
        layoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // cart icon refresh

        return super.onPrepareOptionsMenu(menu);
    }
}