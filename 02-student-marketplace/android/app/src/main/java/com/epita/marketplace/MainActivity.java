package com.epita.marketplace;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.epita.marketplace.adapter.ItemAdapter;
import com.epita.marketplace.api.ApiClient;
import com.epita.marketplace.model.Item;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private ChipGroup chipGroup;
    private LinearLayout emptyState;
    private TextView emptyStateMessage;

    // Tracks the currently selected category — null means "All"
    private String activeCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        recyclerView      = findViewById(R.id.items_recycler_view);
        chipGroup         = findViewById(R.id.chip_group_categories);
        emptyState        = findViewById(R.id.empty_state);
        emptyStateMessage = findViewById(R.id.empty_state_message);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter(this);
        recyclerView.setAdapter(adapter);

        // FAB → Create item screen
        FloatingActionButton fab = findViewById(R.id.fab_create_item);
        fab.setOnClickListener(v -> startActivity(
                new Intent(this, CreateItemActivity.class)));

        // "All" chip resets the filter
        Chip chipAll = findViewById(R.id.chip_all);
        chipAll.setOnClickListener(v -> {
            activeCategory = null;
            loadItems();
        });

        // Load categories from API then load items
        loadCategories();
        loadItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems(); // Refresh list when returning from detail or create screen
    }

    // -----------------------------------------------------------------------
    // S014 — Fetch categories and build chips dynamically
    // -----------------------------------------------------------------------

    private void loadCategories() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/categories");
                JSONArray array = new JSONArray(json);

                List<String> categories = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    categories.add(array.getString(i));
                }

                runOnUiThread(() -> buildCategoryChips(categories));

            } catch (Exception e) {
                // Categories failed to load — list still works without chips
                runOnUiThread(() ->
                    Toast.makeText(this, "Could not load categories", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void buildCategoryChips(List<String> categories) {
        // Remove any chips previously added (keep chip_all at index 0)
        // chipGroup already has the static "All" chip from XML
        // Remove all except "All"
        for (int i = chipGroup.getChildCount() - 1; i >= 1; i--) {
            chipGroup.removeViewAt(i);
        }

        for (String category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChecked(false);
            chip.setChipBackgroundColorResource(
                    com.google.android.material.R.color.mtrl_chip_background_color);

            // S015 — tap a category chip to filter
            chip.setOnClickListener(v -> {
                if (category.equals(activeCategory)) {
                    // Tapping the active chip again → reset to "All"
                    activeCategory = null;
                    Chip chipAll = findViewById(R.id.chip_all);
                    chipAll.setChecked(true);
                } else {
                    activeCategory = category;
                }
                loadItems();
            });

            chipGroup.addView(chip);
        }
    }

    // -----------------------------------------------------------------------
    // S013 + S015 — Load items, optionally filtered by activeCategory
    // -----------------------------------------------------------------------

    private void loadItems() {
        String path = activeCategory != null
                ? "/items?category=" + activeCategory
                : "/items";

        new Thread(() -> {
            try {
                String json = ApiClient.get(path);
                JSONArray array = new JSONArray(json);

                List<Item> items = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    items.add(Item.fromJson(array.getJSONObject(i)));
                }

                runOnUiThread(() -> showItems(items));

            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "Failed to load items: " + e.getMessage(),
                            Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    // -----------------------------------------------------------------------
    // S015 — Show list or empty state depending on results
    // -----------------------------------------------------------------------

    private void showItems(List<Item> items) {
        if (items.isEmpty()) {
            // Show empty state, hide list
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);

            if (activeCategory != null) {
                emptyStateMessage.setText("No items in \"" + activeCategory + "\" yet");
            } else {
                emptyStateMessage.setText("No items listed yet.\nBe the first to sell something!");
            }
        } else {
            // Show list, hide empty state
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            adapter.setItems(items);
        }
    }

    @Override
    public void onItemClick(Item item) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", item.getId());
        startActivity(intent);
    }
}