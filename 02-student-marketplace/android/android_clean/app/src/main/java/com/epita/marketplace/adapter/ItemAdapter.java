package com.epita.marketplace.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.epita.marketplace.R;
import com.epita.marketplace.model.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter that displays a list of marketplace items as cards.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    private List<Item> items = new ArrayList<>();
    private OnItemClickListener listener;

    public ItemAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);
        holder.titleText.setText(item.getTitle());
        holder.priceText.setText(item.formattedPrice());
        holder.categoryText.setText(item.getCategory());
        holder.sellerText.setText("by " + item.getSellerName());

        if (item.isSold()) {
            holder.soldBadge.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.soldBadge.setVisibility(View.GONE);
            holder.itemView.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, priceText, categoryText, sellerText, soldBadge;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.item_title);
            priceText = itemView.findViewById(R.id.item_price);
            categoryText = itemView.findViewById(R.id.item_category);
            sellerText = itemView.findViewById(R.id.item_seller);
            soldBadge = itemView.findViewById(R.id.item_sold_badge);
        }
    }
}
