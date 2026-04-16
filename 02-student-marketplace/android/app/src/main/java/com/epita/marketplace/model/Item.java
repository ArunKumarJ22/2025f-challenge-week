package com.epita.marketplace.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
        boolean isSold = json.getInt("is_sold") == 1;


        // Handle both boolean and integer (0/1) for is_sold
//        Object isSoldObj = json.get("is_sold");
//        if (isSoldObj instanceof Integer) {
//            item.isSold = (Integer) isSoldObj != 0;
//        } else if (isSoldObj instanceof Boolean) {
//            item.isSold = (Boolean) isSoldObj;
//        } else {
//            item.isSold = false;
//        }

        return item;
    }

    public static List<Item> fromJsonArray(JSONArray array) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject obj = array.getJSONObject(i);
                items.add(Item.fromJson(obj));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return items;
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