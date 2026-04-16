package com.epita.marketplace;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.epita.marketplace.api.ApiClient;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

/**
 * Create item form — collects item details and POSTs them to /items.
 * Validates that title, price, category, and seller name are not empty.
 */
public class CreateItemActivity extends AppCompatActivity {

    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText priceInput;
    private TextInputEditText categoryInput;
    private TextInputEditText sellerNameInput;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        titleInput      = findViewById(R.id.create_title);
        descriptionInput = findViewById(R.id.create_description);
        priceInput      = findViewById(R.id.create_price);
        categoryInput   = findViewById(R.id.create_category);
        sellerNameInput = findViewById(R.id.create_seller_name);
        submitButton    = findViewById(R.id.btn_submit_item);

        submitButton.setOnClickListener(v -> submitItem());
    }

    /** Validate fields, build JSON body, and POST to /items. */
    private void submitItem() {
        String title      = getText(titleInput);
        String description = getText(descriptionInput);
        String priceStr   = getText(priceInput);
        String category   = getText(categoryInput);
        String sellerName = getText(sellerNameInput);

        // Validate required fields
        if (title.isEmpty()) { showError("Title is required"); return; }
        if (priceStr.isEmpty()) { showError("Price is required"); return; }
        if (category.isEmpty()) { showError("Category is required"); return; }
        if (sellerName.isEmpty()) { showError("Seller name is required"); return; }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Enter a valid price");
            return;
        }

        submitButton.setEnabled(false);

        final double finalPrice = price;
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("title", title);
                body.put("description", description);
                body.put("price", finalPrice);
                body.put("category", category);
                body.put("seller_name", sellerName);

                ApiClient.post("/items", body.toString());

                runOnUiThread(() -> {
                    Toast.makeText(this, "Item listed!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    submitButton.setEnabled(true);
                    showError("Failed to create item: " + e.getMessage());
                });
            }
        }).start();
    }

    /** Return trimmed text from an EditText, or empty string if null. */
    private String getText(TextInputEditText input) {
        CharSequence text = input.getText();
        return text != null ? text.toString().trim() : "";
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
