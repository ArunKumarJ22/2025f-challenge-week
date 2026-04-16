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

        chipGroup = findViewById(R.id.chip_group_categories);

        FloatingActionButton fab = findViewById(R.id.fab_create_item);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateItemActivity.class);
            startActivity(intent);
        });

        loadItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload items with the current active filter when returning from detail/create
        loadItems(activeCategory);
    }

    private void loadItems() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/items");
                JSONArray array = new JSONArray(json);
                List<Item> items = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    items.add(Item.fromJson(obj));
                }
                runOnUiThread(() -> adapter.setItems(items));
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to load items: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onItemClick(Item item) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("item_id", item.getId());
        startActivity(intent);
    }
}