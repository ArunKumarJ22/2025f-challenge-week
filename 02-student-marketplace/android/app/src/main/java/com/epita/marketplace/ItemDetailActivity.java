package com.epita.marketplace;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.epita.marketplace.adapter.ItemAdapter;

import android.content.Intent;
import android.os.Bundle;
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

public class ItemDetailActivity extends AppCompatActivity {

    TextView title, price, category, description, seller, date, soldBadge;
    ProgressBar loading;
    Button btnMarkAsSold, btnContact, btnShare;
    View soldOverlay;

    // Added fields
    private RecyclerView similarRecycler;
    private ItemAdapter similarAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        title = findViewById(R.id.detail_title);
        price = findViewById(R.id.detail_price);
        category = findViewById(R.id.detail_category);
        description = findViewById(R.id.detail_description);
        seller = findViewById(R.id.detail_seller);
        date = findViewById(R.id.detail_date);

        soldBadge = findViewById(R.id.detailSoldBadge);

        loading = findViewById(R.id.detailLoading);
        loading.setVisibility(View.VISIBLE);

        btnMarkAsSold = findViewById(R.id.btnMarkAsSold);
        btnContact = findViewById(R.id.btn_contact_seller);
        btnShare = findViewById(R.id.btn_share_item);

        soldOverlay = findViewById(R.id.detailSoldOverlay);

        //  Initialize similar items RecyclerView
        similarRecycler = findViewById(R.id.similar_items_recycler);
        similarRecycler.setLayoutManager(new LinearLayoutManager(this));

        similarAdapter = new ItemAdapter(item -> {
            Intent intent = new Intent(ItemDetailActivity.this, ItemDetailActivity.class);
            intent.putExtra("item_id", item.getId());
            startActivity(intent);
        });

        similarRecycler.setAdapter(similarAdapter);

        int itemId = getIntent().getIntExtra("item_id", -1);

        if (itemId == -1) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadItem(itemId);
    }

    private void loadItem(int id) {
        new Thread(() -> {
            try {
                Thread.sleep(2000);

                URL url = new URL(ApiClient.BASE_URL + "/items/" + id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.connect();

                if (conn.getResponseCode() == 404) {
                    runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);

                JSONObject obj = new JSONObject(json.toString());

                String titleStr = obj.getString("title");
                double priceVal = obj.getDouble("price");
                String categoryStr = obj.getString("category");
                String descriptionStr = obj.getString("description");
                String sellerStr = obj.getString("seller_name");
                String dateStr = obj.getString("created_at");
                boolean isSold = obj.getBoolean("is_sold");

                runOnUiThread(() -> {
                    title.setText(titleStr);
                    price.setText(String.format("%.2f EUR", priceVal));
                    category.setText("Category: " + categoryStr);
                    description.setText(descriptionStr);
                    seller.setText("Seller: " + sellerStr);
                    date.setText("Posted: " + toRelative(dateStr));

                    soldBadge.setVisibility(isSold ? View.VISIBLE : View.GONE);
                    soldOverlay.setVisibility(isSold ? View.VISIBLE : View.GONE);
                    loading.setVisibility(View.GONE);

                    if (isSold) {
                        btnMarkAsSold.setText("Mark as Available");
                        btnMarkAsSold.setOnClickListener(v -> showUnmarkDialog(id));
                    } else {
                        btnMarkAsSold.setText("Mark as Sold");
                        btnMarkAsSold.setOnClickListener(v -> showConfirmDialog(id));
                    }

                    btnContact.setOnClickListener(v -> contactSeller(sellerStr, titleStr));
                    btnShare.setOnClickListener(v -> shareItem(titleStr, priceVal));

                    //  Load similar items
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

    // FINAL version
    private void loadSimilarItems(int id) {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/items/" + id + "/similar");
                org.json.JSONArray array = new org.json.JSONArray(json);
                List<Item> similarItems = Item.fromJsonArray(array);

                runOnUiThread(() -> similarAdapter.setItems(similarItems));
            } catch (Exception e) {
                // fail silently
            }
        }).start();
    }

    private void contactSeller(String sellerName, String itemTitle) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(android.net.Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Interest in: " + itemTitle);
        intent.putExtra(Intent.EXTRA_TEXT,
                "Hi " + sellerName + ",\n\nI am interested in your listing for " + itemTitle + ".");
        startActivity(Intent.createChooser(intent, "Contact Seller"));
    }

    private void shareItem(String title, double price) {
        String shareBody = "Check out this " + title + " for " + price + " EUR!";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showConfirmDialog(int itemId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Mark as sold?")
                .setPositiveButton("Yes", (d, w) -> markItemAsSold(itemId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUnmarkDialog(int itemId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Mark as available?")
                .setPositiveButton("Yes", (d, w) -> unmarkItem(itemId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String toRelative(String isoDate) {
        try {
            LocalDateTime localTime = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            ZonedDateTime time = localTime.atZone(ZoneId.systemDefault());
            ZonedDateTime now = ZonedDateTime.now();

            Duration diff = Duration.between(time, now);

            long minutes = diff.toMinutes();
            long hours = diff.toHours();
            long days = diff.toDays();

            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + " minutes ago";
            if (hours < 24) return hours + " hours ago";
            if (days < 7) return days + " days ago";
            return "Last week";

        } catch (Exception e) {
            return isoDate;
        }
    }

    private void markItemAsSold(int itemId) {
        // unchanged
    }

    private void unmarkItem(int itemId) {
        // unchanged
    }
}