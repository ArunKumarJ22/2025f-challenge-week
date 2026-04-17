package com.epita.marketplace;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.epita.marketplace.adapter.ItemAdapter;
import com.epita.marketplace.api.ApiClient;
import com.epita.marketplace.model.Item;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private ProgressBar mainLoading;
    private ChipGroup chipGroup;
    private TextView emptyState;
    private View sortButton;
    TextInputEditText searchInput;
    Handler searchHandler = new Handler();
    private SwipeRefreshLayout swipeRefresh;
    Runnable searchRunnable;

    private int currentPage = 1;
    private final int limit = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int totalCount = 0;

    private String selectedCategory = null;
    private String selectedSort = "date_desc";
    private static final String PREFS_NAME = "MarketplacePrefs";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.items_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mainLoading = findViewById(R.id.mainLoading);
        chipGroup   = findViewById(R.id.chip_group_categories);
        emptyState  = findViewById(R.id.empty_state);

        adapter = new ItemAdapter(this);
        recyclerView.setAdapter(adapter);

        searchInput = findViewById(R.id.searchInput);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(() -> {
        resetPagination();
        loadItems();
        loadCategories();
        swipeRefresh.setRefreshing(false);
        });

        sortButton = findViewById(R.id.btn_sort);
        sortButton.setOnClickListener(this::showSortMenu);


        FloatingActionButton fab = findViewById(R.id.fab_create_item);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateItemActivity.class);
            startActivity(intent);
        });

        loadSortPreference();
        loadCategories();
        resetPagination();
        loadItems();

        // Infinite scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                int visible = lm.getChildCount();
                int total = lm.getItemCount();
                int first = lm.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visible + first) >= total - 2) {
                        currentPage++;
                        loadItems();
                    }
                }
            }
        });

        // Search debounce
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                final String query = s.toString().trim();
                searchRunnable = () -> loadItemsWithSearch(query);

                searchHandler.postDelayed(searchRunnable, 300);
            }
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    resetPagination();
                    loadItems();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetPagination();
        loadItems();
    }

    private void resetPagination() {
        currentPage = 1;
        isLastPage = false;
    }

    private void loadCategories() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/categories");
                JSONArray array = new JSONArray(json);
                List<JSONObject> categories = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    categories.add(array.getJSONObject(i));
                }
                // Re-fetch all items count for the "All" chip
                runOnUiThread(() -> buildChips(categories)); 
            } catch (Exception e) {
                // Chips are non-critical, fail silently
            }
        }).start();
    }

    // S014 + S015: build "All" chip + one chip per category
    private void buildChips(List<JSONObject> categories) {
        chipGroup.removeAllViews();

        Chip allChip = makeChip("All", true);
        allChip.setOnClickListener(v -> {
            selectedCategory = null;
            highlightChip(chipGroup, allChip);
            resetPagination();
            loadItems();
        });
        chipGroup.addView(allChip);

        // One chip per category
        for (JSONObject obj : categories) {
            String catName = obj.optString("category", "Unknown");
            int count = obj.optInt("count", 0);
            // SMKT-S038: Show count in label
            String label = catName + " (" + count + ")";
            Chip chip = makeChip(label, false);
            chip.setOnClickListener(v -> {
                selectedCategory = catName;
                highlightChip(chipGroup, chip);
                resetPagination();
                loadItems();
            });
            chipGroup.addView(chip);
        }
    }

    private Chip makeChip(String text, boolean selected) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(false);
        chip.setClickable(true);
        chip.setChipBackgroundColorResource(
                selected ? android.R.color.holo_blue_light : android.R.color.darker_gray
        );
        chip.setTextColor(Color.WHITE);
        return chip;
    }

    private void highlightChip(ChipGroup group, Chip active) {
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip c = (Chip) group.getChildAt(i);
            c.setChipBackgroundColorResource(
                    c == active ? android.R.color.holo_blue_light : android.R.color.darker_gray
            );
        }
    }

    private void loadItems() {
        if (isLoading || isLastPage) return;

        isLoading = true;
        mainLoading.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                StringBuilder url = new StringBuilder("/items?page=" + currentPage + "&limit=" + limit);

                if (selectedCategory != null) url.append("&category=").append(selectedCategory);
                if (selectedSort != null) url.append("&sort=").append(selectedSort);

                String json = ApiClient.get(url.toString());
                JSONObject obj = new JSONObject(json);

                JSONArray array = obj.getJSONArray("items");
                totalCount = obj.getInt("total_count");

                List<Item> items = Item.fromJsonArray(array);

                runOnUiThread(() -> {
                    if (currentPage == 1) adapter.setItems(items);
                    else adapter.appendItems(items);

                    if (adapter.getItemCount() >= totalCount) isLastPage = true;

                    isLoading = false;
                    mainLoading.setVisibility(View.GONE);

                    // S015: show empty state if no results
                    if (items.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        if (emptyState != null) {
                            // SMKT-S037: CTA for empty state
                            emptyState.setText("No items here yet. Be the first to sell something!");
                            emptyState.setVisibility(View.VISIBLE);
                        }
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    isLoading = false;
                    mainLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load items", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    public void onItemClick(Item item) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", item.getId());
        startActivity(intent);
    }

    // S019: Persist sort preference
    private void loadSortPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedSort = prefs.getString("sort_pref", "date_desc");
    }

    private void saveSortPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString("sort_pref", selectedSort).apply();
    }


    private void loadItemsWithSearch(String query) {
        resetPagination();

        new Thread(() -> {
            try {
                StringBuilder url = new StringBuilder("/items?page=1&limit=" + limit);

                if (selectedCategory != null) url.append("&category=").append(selectedCategory);
                if (!query.isEmpty()) url.append("&q=").append(query);

                String json = ApiClient.get(url.toString());
                JSONObject obj = new JSONObject(json);

                JSONArray arr = obj.getJSONArray("items");
                totalCount = obj.getInt("total_count");

                List<Item> list = Item.fromJsonArray(arr);

                runOnUiThread(() -> adapter.setItems(list));

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void showSortMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, "Newest First");
        popup.getMenu().add(0, 2, 1, "Oldest First");
        popup.getMenu().add(0, 3, 2, "Price: Low to High");
        popup.getMenu().add(0, 4, 3, "Price: High to Low");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: selectedSort = "date_desc"; break;
                case 2: selectedSort = "date_asc"; break;
                case 3: selectedSort = "price_asc"; break;
                case 4: selectedSort = "price_desc"; break;
            }
            saveSortPreference();
            resetPagination();
            loadItems();
            return true;
        });
        popup.show();
    }

}
