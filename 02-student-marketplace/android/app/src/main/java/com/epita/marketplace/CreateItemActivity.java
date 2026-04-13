package com.epita.marketplace;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateItemActivity extends AppCompatActivity {

    private EditText titleInput, priceInput, categoryInput, sellerInput, descriptionInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_item);

        // Bind views
        titleInput = findViewById(R.id.input_title);
        priceInput = findViewById(R.id.input_price);
        categoryInput = findViewById(R.id.input_category);
        sellerInput = findViewById(R.id.input_seller);
        descriptionInput = findViewById(R.id.input_description);

        Button submitBtn = findViewById(R.id.btn_submit);

        //  Handle submit
        submitBtn.setOnClickListener(v -> submitItem());
    }

    private void submitItem() {
        String title = titleInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim();
        String seller = sellerInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(title) ||
            TextUtils.isEmpty(priceStr) ||
            TextUtils.isEmpty(category) ||
            TextUtils.isEmpty(seller)) {

            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create item object (adjust to your model)
        Item item = new Item(title, description, price, category, seller);

        // Call API
        ApiClient.post("/items", item, new ApiClient.Callback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(CreateItemActivity.this, "Item created", Toast.LENGTH_SHORT).show();
                    finish(); // go back
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(CreateItemActivity.this, "Error creating item", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}