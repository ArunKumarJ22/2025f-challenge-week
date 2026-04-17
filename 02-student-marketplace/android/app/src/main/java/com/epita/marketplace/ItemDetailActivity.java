package com.epita.marketplace;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.epita.marketplace.adapter.ItemAdapter;
import com.epita.marketplace.api.ApiClient;
import com.epita.marketplace.model.Item;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ItemDetailActivity displays the complete details of a single marketplace item.
 * It allows users to contact the seller, share the item, and mark it as sold.
 */
public class ItemDetailActivity extends AppCompatActivity {

    // UI components
    private TextView title, price, category, description, seller, date, soldBadge;
    private ProgressBar loading;
    private Button btnMarkAsSold, btnContact, btnShare;
    private View soldOverlay;

    // Similar items components
    private RecyclerView similarRecyclerView;
    private ItemAdapter similarAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Bind UI components (SMKT-S001)
        title = findViewById(R.id.detail_title);
        price = findViewById(R.id.detail_price);
        category = findViewById(R.id.detail_category);
        description = findViewById(R.id.detail_description);
        seller = findViewById(R.id.detail_seller);
        date = findViewById(R.id.detail_date);
        soldBadge = findViewById(R.id.detailSoldBadge);
        loading = findViewById(R.id.detailLoading);
        btnMarkAsSold = findViewById(R.id.btnMarkAsSold);
        btnContact = findViewById(R.id.btn_contact_seller);
        btnShare = findViewById(R.id.btn_share_item);
        soldOverlay = findViewById(R.id.detailSoldOverlay);

        // Setup RecyclerView for similar items (SMKT-S034)
        similarRecyclerView = findViewById(R.id.similar_items_recycler);
        similarRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        similarAdapter = new ItemAdapter(item -> {
            // Navigate to the detail page of the similar item (SMKT-S035)
            Intent nextDetail = new Intent(this, ItemDetailActivity.class);
            nextDetail.putExtra("item_id", item.getId());
            startActivity(nextDetail);
        });
        similarRecyclerView.setAdapter(similarAdapter);

        // Get item ID from intent extras
        int itemId = getIntent().getIntExtra("item_id", -1);

        if (itemId == -1) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initial data load
        loadItem(itemId);
    }

    /**
     * Fetches item details from the backend. (SMKT-S001)
     */
    private void loadItem(int id) {
        loading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                // Perform GET request
                URL url = new URL(ApiClient.BASE_URL + "/items/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                // Handle 404 Not Found (SMKT-S005)
                if (conn.getResponseCode() == 404) {
                    runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                // Parse response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                JSONObject obj = new JSONObject(json.toString());

                // Map JSON to fields
                String titleStr = obj.getString("title");
                double priceVal = obj.getDouble("price");
                String categoryStr = obj.getString("category");
                String descriptionStr = obj.getString("description");
                String sellerStr = obj.getString("seller_name");
                String dateStr = obj.getString("created_at");
                boolean isSold = obj.getBoolean("is_sold");

                // Update UI on main thread
                runOnUiThread(() -> {
                    title.setText(titleStr);
                    price.setText(String.format("%.2f EUR", priceVal));
                    category.setText("Category: " + categoryStr);
                    description.setText(descriptionStr);
                    seller.setText("Seller: " + sellerStr);
                    date.setText("Posted: " + toRelative(dateStr));

                    // Display "SOLD" badge and overlay (SMKT-S004, S022)
                    soldBadge.setVisibility(isSold ? View.VISIBLE : View.GONE);
                    soldOverlay.setVisibility(isSold ? View.VISIBLE : View.GONE);
                    loading.setVisibility(View.GONE);

                    // Toggle button behavior between Mark as Sold/Available (SMKT-S021, S024)
                    if (isSold) {
                        btnMarkAsSold.setText("Mark as Available");
                        btnMarkAsSold.setOnClickListener(v -> showUnmarkDialog(id));
                    } else {
                        btnMarkAsSold.setText("Mark as Sold");
                        btnMarkAsSold.setOnClickListener(v -> showConfirmDialog(id));
                    }

                    // Setup contact and share actions (SMKT-S031, S032)
                    btnContact.setOnClickListener(v -> contactSeller(sellerStr, titleStr));
                    btnShare.setOnClickListener(v -> shareItem(titleStr, priceVal));

                    // Load related content
                    loadSimilarItems(id);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    loading.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading item", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Opens an email client with pre-filled fields to contact the seller. (SMKT-S031)
     */
    private void contactSeller(String sellerName, String itemTitle) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(android.net.Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Interest in: " + itemTitle);
        intent.putExtra(Intent.EXTRA_TEXT, "Hi " + sellerName + ",\n\nI am interested in your listing for " + itemTitle + ".");
        try {
            startActivity(Intent.createChooser(intent, "Contact Seller"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens the system share sheet to share the item details. (SMKT-S032)
     */
    private void shareItem(String title, double price) {
        String shareBody = "Check out this " + title + " for " + price + " EUR on Student Marketplace!";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Student Marketplace Item");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    /**
     * Fetches similar items based on the current item. (SMKT-S034)
     */
    private void loadSimilarItems(int id) {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/items/" + id + "/similar");
                org.json.JSONArray array = new org.json.JSONArray(json);
                List<Item> similarItems = Item.fromJsonArray(array);

                runOnUiThread(() -> similarAdapter.setItems(similarItems));
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Confirmation dialog before marking an item as sold. (SMKT-S021)
     */
    private void showConfirmDialog(int itemId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Are you sure you want to mark this item as sold?")
                .setPositiveButton("Yes", (dialog, which) -> updateItemSoldStatus(itemId, true))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Confirmation dialog before marking an item as available. (SMKT-S024)
     */
    private void showUnmarkDialog(int itemId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Mark this item as available again?")
                .setPositiveButton("Yes", (dialog, which) -> updateItemSoldStatus(itemId, false))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Sends a PATCH request to update the 'is_sold' status of an item.
     */
    private void updateItemSoldStatus(int itemId, boolean sold) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiClient.BASE_URL + "/items/" + itemId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String body = "{\"is_sold\": " + sold + "}";
                conn.getOutputStream().write(body.getBytes());

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        loadItem(itemId); // Refresh UI
                        Toast.makeText(this, sold ? "Item marked as sold" : "Item marked as available", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show();
                    }
                });
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /**
     * Converts an ISO date string to a human-readable relative time (e.g., "2 hours ago").
     */
    private String toRelative(String isoDate) {
        try {
            LocalDateTime localTime = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            ZonedDateTime time = localTime.atZone(ZoneId.systemDefault());
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            Duration diff = Duration.between(time, now);

            long minutes = diff.toMinutes();
            long hours = diff.toHours();
            long days = diff.toDays();

            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + " minutes ago";
            if (hours == 1) return "1 hour ago";
            if (hours < 24) return hours + " hours ago";
            if (days == 1) return "Yesterday";
            if (days < 7) return days + " days ago";
            return "Last week";
        } catch (Exception e) {
            return isoDate;
        }
    }
}