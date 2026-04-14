package com.epita.marketplace;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.epita.marketplace.api.ApiClient;
import com.epita.marketplace.model.Item;

import org.json.JSONObject;

/**
 * Item detail screen — loads a single item from GET /items/{id} and displays all its fields.
 * Includes a "Mark as sold" button that calls PATCH /items/{id}.
 */
public class ItemDetailActivity extends AppCompatActivity {

    private TextView titleText;
    private TextView priceText;
    private TextView categoryText;
    private TextView descriptionText;
    private TextView sellerText;
    private TextView dateText;
    private TextView soldBadge;
    private Button markSoldButton;

    private int itemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        titleText       = findViewById(R.id.detail_title);
        priceText       = findViewById(R.id.detail_price);
        categoryText    = findViewById(R.id.detail_category);
        descriptionText = findViewById(R.id.detail_description);
        sellerText      = findViewById(R.id.detail_seller);
        dateText        = findViewById(R.id.detail_date);
        soldBadge       = findViewById(R.id.detail_sold_badge);
        markSoldButton  = findViewById(R.id.btn_mark_sold);

        itemId = getIntent().getIntExtra("item_id", -1);
        if (itemId == -1) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadItem();
        markSoldButton.setOnClickListener(v -> markAsSold());
    }

    /** Fetch item details from the API and populate the views. */
    private void loadItem() {
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

    /** Bind a loaded Item to all the views. */
    private void bindItem(Item item) {
        titleText.setText(item.getTitle());
        priceText.setText(item.formattedPrice());
        categoryText.setText(item.getCategory());
        descriptionText.setText(item.getDescription());
        sellerText.setText("Seller: " + item.getSellerName());
        dateText.setText("Posted: " + item.getCreatedAt());

        if (item.isSold()) {
            soldBadge.setVisibility(View.VISIBLE);
            markSoldButton.setEnabled(false);
            markSoldButton.setText("Already sold");
        } else {
            soldBadge.setVisibility(View.GONE);
            markSoldButton.setEnabled(true);
            markSoldButton.setText("Mark as sold");
        }
    }

    /** Call PATCH /items/{id} to mark the item as sold. */
    private void markAsSold() {
        markSoldButton.setEnabled(false);
        new Thread(() -> {
            try {
                ApiClient.patch("/items/" + itemId, "{\"is_sold\": true}");
                runOnUiThread(() -> {
                    soldBadge.setVisibility(View.VISIBLE);
                    markSoldButton.setText("Already sold");
                    Toast.makeText(this, "Item marked as sold", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    markSoldButton.setEnabled(true);
                    Toast.makeText(this, "Failed to update item: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
