package com.epita.marketplace;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.epita.marketplace.api.ApiClient;
import com.epita.marketplace.model.Item;
import org.json.JSONObject;

public class ItemDetailActivity extends AppCompatActivity {

    private TextView titleView, priceView, categoryView,
                     descriptionView, sellerView, dateView, soldBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Wire views
        titleView       = findViewById(R.id.detail_title);
        priceView       = findViewById(R.id.detail_price);
        categoryView    = findViewById(R.id.detail_category);
        descriptionView = findViewById(R.id.detail_description);
        sellerView      = findViewById(R.id.detail_seller);
        dateView        = findViewById(R.id.detail_date);
        soldBadge       = findViewById(R.id.detail_sold_badge);  // we'll add this below

        int itemId = getIntent().getIntExtra("item_id", -1);
        if (itemId == -1) { finish(); return; }

        // Network must be off the main thread
        new Thread(() -> {
            try {
                String json = ApiClient.get("/items/" + itemId);
                JSONObject obj = new JSONObject(json);
                Item item = Item.fromJson(obj);

                runOnUiThread(() -> bindItem(item));
            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "Failed to load item: " + e.getMessage(),
                                   Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void bindItem(Item item) {
        titleView.setText(item.getTitle());
        priceView.setText(item.formattedPrice());
        categoryView.setText("Category: " + item.getCategory());
        descriptionView.setText(item.getDescription());
        sellerView.setText("Seller: " + item.getSellerName());
        dateView.setText("Posted: " + item.getCreatedAt());

        if (item.isSold()) {
            soldBadge.setVisibility(View.VISIBLE);
        } else {
            soldBadge.setVisibility(View.GONE);
        }
    }
}