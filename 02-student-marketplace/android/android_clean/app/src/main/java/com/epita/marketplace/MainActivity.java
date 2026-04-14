package com.epita.marketplace;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.epita.marketplace.adapter.ItemAdapter;
import com.epita.marketplace.api.ApiClient;
import com.epita.marketplace.model.Item;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Main screen — displays a list of marketplace items fetched from the API.
 */
public class MainActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private ChipGroup chipGroup;
    private TextView emptyState;
    private String selectedCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.items_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ItemAdapter(this);
        chipGroup = findViewById(R.id.chip_group_categories);
        emptyState = findViewById(R.id.empty_state);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_create_item);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateItemActivity.class);
            startActivity(intent);
        });

        loadItems();
        loadCategories();
    }
    private void loadCategories() {
    new Thread(() -> {
        try {
            String json = ApiClient.get("/categories");
            JSONArray array = new JSONArray(json);
            List<String> cats = new ArrayList<>();
            cats.add("All");
            for (int i = 0; i < array.length(); i++) {
                cats.add(array.getString(i));
            }
            runOnUiThread(() -> {
                chipGroup.removeAllViews();
                for (String cat : cats) {
                    Chip chip = new Chip(this);
                    chip.setText(cat);
                    chip.setCheckable(true);
                    chip.setChecked(cat.equals("All"));
                    chipGroup.addView(chip);
                }
                chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                    if (checkedIds.isEmpty()) return;
                    Chip active = findViewById(checkedIds.get(0));
                    if (active == null) return;
                    String label = active.getText().toString();
                    selectedCategory = label.equals("All") ? null : label;
                    loadItems();
                });
            });
        } catch (Exception e) {
            runOnUiThread(() ->
                Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show());
        }
    }).start();
}

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

   private void loadItems() {
    new Thread(() -> {
        try {
            String path = selectedCategory == null ? "/items" : "/items?category=" + selectedCategory;
            String json = ApiClient.get(path);  
            JSONArray array = new JSONArray(json);
            List<Item> items = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                items.add(Item.fromJson(obj));
            }
            runOnUiThread(() -> {
    adapter.setItems(items);
    if (items.isEmpty()) {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    } else {
        recyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }
});
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to load items: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onItemClick(Item item) {
        // TODO: Launch ItemDetailActivity with the item id
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", item.getId());
        startActivity(intent);
    }
}
