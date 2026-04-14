package com.epita.marketplace;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.epita.marketplace.api.ApiClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

public class CreateItemActivity extends AppCompatActivity {

    // Views
    private TextInputLayout layoutTitle, layoutPrice, layoutCategory, layoutSellerName;
    private TextInputEditText etTitle, etDescription, etPrice, etSellerName;
    private AutoCompleteTextView dropdownCategory;
    private Button btnSubmit;

    // S009 — fixed category list
    private static final String[] CATEGORIES = {
        "Books", "Electronics", "Clothing", "Furniture", "Sports", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        // Enable back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sell an Item");
        }

        // Bind views
        layoutTitle      = findViewById(R.id.layout_title);
        layoutPrice      = findViewById(R.id.layout_price);
        layoutCategory   = findViewById(R.id.layout_category);
        layoutSellerName = findViewById(R.id.layout_seller_name);

        etTitle       = findViewById(R.id.create_title);
        etDescription = findViewById(R.id.create_description);
        etPrice       = findViewById(R.id.create_price);
        etSellerName  = findViewById(R.id.create_seller_name);
        dropdownCategory = findViewById(R.id.create_category);
        btnSubmit     = findViewById(R.id.btn_submit_item);

        // S009 — wire up the category dropdown
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                CATEGORIES
        );
        dropdownCategory.setAdapter(categoryAdapter);

        // S008 + S010 + S011 — submit button
        btnSubmit.setOnClickListener(v -> submitForm());
    }

    /**
     * S010 — Validate all fields inline, then POST if valid.
     */
    private void submitForm() {
        // Clear previous errors
        layoutTitle.setError(null);
        layoutPrice.setError(null);
        layoutCategory.setError(null);
        layoutSellerName.setError(null);

        String title      = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String priceStr   = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String category   = dropdownCategory.getText().toString().trim();
        String sellerName = etSellerName.getText() != null ? etSellerName.getText().toString().trim() : "";

        // --- Client-side validation (S010) ---
        boolean valid = true;

        if (title.isEmpty()) {
            layoutTitle.setError("Title is required");
            valid = false;
        }

        double price = 0;
        if (priceStr.isEmpty()) {
            layoutPrice.setError("Price is required");
            valid = false;
        } else {
            try {
                price = Double.parseDouble(priceStr);
                if (price <= 0) {
                    layoutPrice.setError("Price must be greater than 0");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                layoutPrice.setError("Enter a valid number");
                valid = false;
            }
        }

        if (category.isEmpty()) {
            layoutCategory.setError("Please select a category");
            valid = false;
        }

        if (sellerName.isEmpty()) {
            layoutSellerName.setError("Your name is required");
            valid = false;
        }

        if (!valid) return; // Stop here — errors are shown inline

        // Disable button while request is in flight
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Posting...");

        // Build JSON body
        double finalPrice = price;
        String finalTitle = title;
        String finalDescription = description;
        String finalCategory = category;
        String finalSellerName = sellerName;

        new Thread(() -> {
            try {
                // Build JSON manually (no external library needed)
                JSONObject body = new JSONObject();
                body.put("title", finalTitle);
                body.put("description", finalDescription);
                body.put("price", finalPrice);
                body.put("category", finalCategory);
                body.put("seller_name", finalSellerName);

                String response = ApiClient.post("/items", body.toString());

                // S011 — success: go back to the list (onResume reloads it)
                runOnUiThread(() -> {
                    Toast.makeText(this, "Item posted!", Toast.LENGTH_SHORT).show();
                    finish(); // pops this screen → MainActivity.onResume() → loadItems()
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Post Item");
                    Toast.makeText(this,
                            "Failed to post item: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}