package com.epita.marketplace;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
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
 * Item detail screen.
 *
 * TODO: Load item data from GET /items/{id} and populate the layout.
 * TODO: Display all item fields (title, description, price, category, seller, date).
 * TODO: If the item is sold, show a "SOLD" badge.
 * TODO: Add a "Mark as sold" button that calls PATCH /items/{id}.
 */
public class ItemDetailActivity extends AppCompatActivity {

    TextView title, price, category, description, seller, date, soldBadge;
    ProgressBar loading;
    Button btnMarkAsSold, btnContact, btnShare;
    View soldOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // SMKT-S001: Bind views for item detail screen
        title = findViewById(R.id.detail_title);
        price = findViewById(R.id.detail_price);
        category = findViewById(R.id.detail_category);
        description = findViewById(R.id.detail_description);
        seller = findViewById(R.id.detail_seller);
        date = findViewById(R.id.detail_date);

        // SMKT-S004: SOLD badge
        soldBadge = findViewById(R.id.detailSoldBadge);

        // SMKT-S005: Loading spinner
        loading = findViewById(R.id.detailLoading);
        loading.setVisibility(View.VISIBLE);

        // SMKT-S021 / SMKT-S024: Mark as Sold / Mark as Available button
        btnMarkAsSold = findViewById(R.id.btnMarkAsSold);

        // SMKT-S031 & SMKT-S032: Action buttons
        btnContact = findViewById(R.id.btn_contact_seller);
        btnShare = findViewById(R.id.btn_share_item);

        // SMKT-S022: SOLD overlay
        soldOverlay = findViewById(R.id.detailSoldOverlay);

        // Get item ID from intent
        Intent intent = getIntent();
        int itemId = getIntent().getIntExtra("item_id", -1);

        if (itemId == -1) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // SMKT-S001: Load item details from backend
        loadItem(itemId);

        // TODO: Fetch item details from the API and bind to views
    }

    // SMKT-S001 + SMKT-S005: Load item, handle 404, show spinner
    private void loadItem(int id) {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // force 2 seconds delay
                URL url = new URL(ApiClient.BASE_URL + "/items/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000); // 3 seconds
                conn.setReadTimeout(3000);
                conn.connect();

                // SMKT-S005: 404 handling
                if (conn.getResponseCode() == 404) {
                    runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                // Read response
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);

                JSONObject obj = new JSONObject(json.toString());

                // Extract fields
                String titleStr = obj.getString("title");
                double priceVal = obj.getDouble("price");
                String categoryStr = obj.getString("category");
                String descriptionStr = obj.getString("description");
                String sellerStr = obj.getString("seller_name");
                String dateStr = obj.getString("created_at");
                boolean isSold = obj.getBoolean("is_sold");

                // Update UI
                runOnUiThread(() -> {
                    title.setText(titleStr);
                    price.setText(String.format("%.2f EUR", priceVal));
                    category.setText("Category: " + categoryStr);
                    description.setText(descriptionStr);
                    seller.setText("Seller: " + sellerStr);
                    date.setText("Posted: " + toRelative(dateStr));

                    // SMKT-S036: Image loading logic
                    String imageUrl = obj.optString("image_url", null);
                    if (imageUrl != null && !imageUrl.equals("null")) {
                        // Logic to load image from URL would go here (e.g. using Glide or Picasso)
                        // Glide.with(this).load(imageUrl).into(findViewById(R.id.detail_image));
                    }

                    // SMKT-S004: SOLD badge
                    soldBadge.setVisibility(isSold ? View.VISIBLE : View.GONE);

                    // SMKT-S022: SOLD overlay
                    soldOverlay.setVisibility(isSold ? View.VISIBLE : View.GONE);
                    loading.setVisibility(View.GONE);

                    // SMKT-S021 + SMKT-S024: Button behavior
                    if (isSold) {
                        btnMarkAsSold.setEnabled(true);
                        btnMarkAsSold.setText("Mark as Available");
                        btnMarkAsSold.setOnClickListener(v -> showUnmarkDialog(id));
                    } else {
                        btnMarkAsSold.setEnabled(true);
                        btnMarkAsSold.setText("Mark as Sold");
                        btnMarkAsSold.setOnClickListener(v -> showConfirmDialog(id));
                    }

                    // SMKT-S031: Contact Seller
                    btnContact.setOnClickListener(v -> contactSeller(sellerStr, titleStr));

                    // SMKT-S032: Share Item
                    btnShare.setOnClickListener(v -> shareItem(titleStr, priceVal));

                    // SMKT-S034: Load Similar Items
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

    // SMKT-S031: Contact Seller via Email Intent
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

    // SMKT-S032: Share item details
    private void shareItem(String title, double price) {
        String shareBody = "Check out this " + title + " for " + price + " EUR on Student Marketplace!";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Student Marketplace Item");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    // SMKT-S033 & SMKT-S034: Fetch similar items
    private void loadSimilarItems(int id) {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/items/" + id + "/similar");
                org.json.JSONArray array = new org.json.JSONArray(json);
                List<Item> similarItems = Item.fromJsonArray(array);

                runOnUiThread(() -> {
                    // SMKT-S034 & SMKT-S035: Bind similar items to UI and handle navigation
                    // When a similar item is clicked:
                    // Intent nextDetail = new Intent(ItemDetailActivity.this, ItemDetailActivity.class);
                    // nextDetail.putExtra("item_id", selectedItem.getId());
                    // startActivity(nextDetail);
                });
            } catch (Exception e) {
                // Similar items are non-critical
            }
        }).start();
    }

    // SMKT-S021: Confirm marking item as sold
    private void showConfirmDialog(int itemId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Are you sure you want to mark this item as sold?")
                .setPositiveButton("Yes", (dialog, which) -> markItemAsSold(itemId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // SMKT-S024: Confirm unmarking item
    private void showUnmarkDialog(int itemId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Mark this item as available again?")
                .setPositiveButton("Yes", (dialog, which) -> unmarkItem(itemId))
                .setNegativeButton("Cancel", null)
                .show();
    }

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
            e.printStackTrace();
            return isoDate;
        }
    }

    // SMKT-S021: PATCH is_sold = true
    private void markItemAsSold(int itemId) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiClient.BASE_URL + "/items/" + itemId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String body = "{\"is_sold\": true}";
                conn.getOutputStream().write(body.getBytes());

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        btnMarkAsSold.setEnabled(false);
                        btnMarkAsSold.setText("Sold");
                        soldBadge.setVisibility(View.VISIBLE);
                        soldOverlay.setVisibility(View.VISIBLE);

                        // SMKT-S023: Refresh list when returning
                        loadItem(itemId);
                        Toast.makeText(this, "Item marked as sold", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // SMKT-S024: PATCH is_sold = false
    private void unmarkItem(int itemId) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiClient.BASE_URL + "/items/" + itemId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String body = "{\"is_sold\": false}";
                conn.getOutputStream().write(body.getBytes());

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        soldBadge.setVisibility(View.GONE);
                        soldOverlay.setVisibility(View.GONE);

                        btnMarkAsSold.setEnabled(true);
                        btnMarkAsSold.setText("Mark as Sold");

                        Toast.makeText(this, "Item marked as available", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

}