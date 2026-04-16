package com.epita.marketplace;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.epita.marketplace.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Create item form.
 *
 * TODO: Read values from the form fields.
 * TODO: Validate that title, price, category, and seller name are not empty.
 * TODO: POST the new item to /items using ApiClient.post().
 * TODO: On success, finish() to return to the list. On error, show a Toast.
 */
public class CreateItemActivity extends AppCompatActivity {

    TextInputEditText titleInput, descInput, priceInput, categoryInput, sellerInput;
    MaterialButton btnSubmit;
    ProgressBar loading;
    Spinner spinnerCategory;

    private static final String[] CATEGORIES = {
            "Select a category", "BOOKS", "ELECTRONICS", "FASHION", "HOME", "OTHER"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        titleInput = findViewById(R.id.create_title);
        descInput = findViewById(R.id.create_description);
        priceInput = findViewById(R.id.create_price);
        categoryInput = findViewById(R.id.create_category);
        sellerInput = findViewById(R.id.create_seller_name);
        btnSubmit = findViewById(R.id.btn_submit_item);

        // SMKT-S009: Setup category dropdown (Spinner)
        // spinnerCategory = findViewById(R.id.spinner_category);

        // NEW: dropdown setup
        if (spinnerCategory != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    CATEGORIES
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);
        }

        // 🔥 Replace click (ONLY ADDITION)
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void validateAndSubmit() {

        String title = titleInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();
        String seller = sellerInput.getText().toString().trim();

        String category = categoryInput.getText().toString().trim();

        // SMKT-S009: Override category with spinner selection if used
        if (spinnerCategory != null &&
                !spinnerCategory.getSelectedItem().toString().equals("Select a category")) {
            category = spinnerCategory.getSelectedItem().toString();
            categoryInput.setText(category); // sync with old field
        }

        boolean valid = true;

        if (title.isEmpty()) {
            titleInput.setError("Title required");
            valid = false;
        }

        if (priceStr.isEmpty()) {
            priceInput.setError("Price required");
            valid = false;
        } else {
            try {
                double p = Double.parseDouble(priceStr);
                if (p <= 0) {
                    priceInput.setError("Price must be > 0");
                    valid = false;
                }
            } catch (Exception e) {
                priceInput.setError("Invalid price");
                valid = false;
            }
        }

        if (category.isEmpty()) {
            Toast.makeText(this, "Select category", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (seller.isEmpty()) {
            sellerInput.setError("Seller required");
            valid = false;
        }

        if (!valid) return;

        // SMKT-S008: Submit to backend
        submitItem();
    }

    // SMKT-S008: Submit item to backend (POST /items)
    private void submitItem() {

        String title = titleInput.getText().toString().trim();
        String desc = descInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim().toUpperCase();
        String seller = sellerInput.getText().toString().trim();

        if (title.isEmpty() || priceStr.isEmpty() || category.isEmpty() || seller.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
            return;
        }

        // SMKT-S005: Show loading spinner during network request
        loading = new ProgressBar(this);
        loading.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                URL url = new URL(ApiClient.BASE_URL + "/items");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                JSONObject json = new JSONObject();
                json.put("title", title);
                json.put("description", desc);
                json.put("price", price);
                json.put("category", category);
                json.put("image_url", JSONObject.NULL);
                json.put("seller_name", seller);

                conn.getOutputStream().write(json.toString().getBytes());

                int code = conn.getResponseCode();

                // SMKT-S007: Display backend validation errors (400 Bad Request)
                if (code == 400) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream())
                    );
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);

                    JSONObject errors = new JSONObject(sb.toString());

                    runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        Toast.makeText(this, errors.toString(), Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // SMKT-S008: On success, show toast and finish()
                if (code == 201) {
                    runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        Toast.makeText(this, "Item created!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    loading.setVisibility(View.GONE);
                    Toast.makeText(this, "Error creating item", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}