package com.epita.marketplace;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.epita.marketplace.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CreateItemActivity extends AppCompatActivity {

    TextInputEditText titleInput, descInput, priceInput, sellerInput;
    MaterialButton btnSubmit;
    ProgressBar loading;
    Spinner spinnerCategory;

    // S009: category options
    private static final String[] CATEGORIES = {
        "Select a category", "Books", "Electronics", "Furniture", "Sports", "Fashion", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        titleInput  = findViewById(R.id.create_title);
        descInput   = findViewById(R.id.create_description);
        priceInput  = findViewById(R.id.create_price);
        sellerInput = findViewById(R.id.create_seller_name);
        btnSubmit   = findViewById(R.id.btn_submit_item);
        loading     = findViewById(R.id.create_loading);

        // S009 BUG FIX: spinnerCategory was commented out so it was always null
        spinnerCategory = findViewById(R.id.spinner_category);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, CATEGORIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    // S010: client-side validation with inline errors
    private void validateAndSubmit() {
        String title    = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
        String priceStr = priceInput.getText() != null ? priceInput.getText().toString().trim() : "";
        String seller   = sellerInput.getText() != null ? sellerInput.getText().toString().trim() : "";
        int spinnerPos  = spinnerCategory.getSelectedItemPosition();
        String category = spinnerPos == 0 ? "" : CATEGORIES[spinnerPos];

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
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (seller.isEmpty()) {
            sellerInput.setError("Seller name required");
            valid = false;
        }

        if (!valid) return;

        submitItem(title,
            descInput.getText() != null ? descInput.getText().toString().trim() : "",
            Double.parseDouble(priceStr), category, seller);
    }

    // S008 + S011: POST /items, finish() on 201
    private void submitItem(String title, String desc, double price,
                            String category, String seller) {
        if (loading != null) loading.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        new Thread(() -> {
            try {
                URL url = new URL(ApiClient.BASE_URL + "/items");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                JSONObject body = new JSONObject();
                body.put("title", title);
                body.put("description", desc);
                body.put("price", price);
                body.put("category", category);
                body.put("image_url", JSONObject.NULL);
                body.put("seller_name", seller);

                conn.getOutputStream().write(body.toString().getBytes("UTF-8"));

                int code = conn.getResponseCode();

                // S007: show backend 400 field errors
                if (code == 400) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject errObj = new JSONObject(sb.toString());

                    runOnUiThread(() -> {
                        if (loading != null) loading.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        Toast.makeText(this,
                            errObj.optString("detail", errObj.toString()),
                            Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // S011: on 201, go back — list refreshes via MainActivity.onResume
                if (code == 201) {
                    runOnUiThread(() -> {
                        if (loading != null) loading.setVisibility(View.GONE);
                        Toast.makeText(this, "Item listed!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (loading != null) loading.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
