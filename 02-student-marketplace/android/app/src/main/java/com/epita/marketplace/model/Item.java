package com.epita.marketplace.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Plain Java object representing a marketplace item.
 */
public class Item {
    private int id;
    private String title;
    private String description;
    private double price;
    private String category;
    private String imageUrl;
    private String sellerName;
    private String createdAt;
    private boolean isSold;

    public Item() {}

    /**
     * Parse an Item from a JSON object returned by the API.
     */
    public static Item fromJson(JSONObject json) throws JSONException {
        Item item = new Item();
        item.id = json.getInt("id");
        item.title = json.getString("title");
        item.description = json.optString("description", "");
        item.price = json.getDouble("price");
        item.category = json.getString("category");
        item.imageUrl = json.optString("image_url", null);
        item.sellerName = json.getString("seller_name");
        item.createdAt = json.getString("created_at");
        item.isSold = json.getBoolean("is_sold");
        return item;
    }

    // ---- Getters & setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isSold() { return isSold; }
    public void setSold(boolean sold) { isSold = sold; }

    /**
     * Formatted price string for display (e.g. "12.50 EUR").
     */
    public String formattedPrice() {
        return String.format("%.2f EUR", price);
    }
}
