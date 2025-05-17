package me.mgergo.smartfridge;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class FridgeItemAdapter extends RecyclerView.Adapter<FridgeItemAdapter.ViewHolder> implements Filterable {
    private ArrayList<FridgeItem> fridgeItemsData;
    private ArrayList<FridgeItem> fridgeItemsDataAll;
    private final Context context;
    private int lastPosition = -1;

    private static final String LOG_TAG = FridgeItemAdapter.class.getName();

    FridgeItemAdapter(Context context, ArrayList<FridgeItem> itemsData) {
        this.fridgeItemsData = itemsData;
        this.fridgeItemsDataAll = itemsData;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FridgeItemAdapter.ViewHolder holder, int position) {
        FridgeItem currentItem = fridgeItemsData.get(position);
        holder.bindTo(currentItem);

        holder.itemView.clearAnimation();

        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_row);
            holder.itemView.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return fridgeItemsData.size();
    }

    @Override
    public Filter getFilter() {
        return fridgeFilter;
    }

    private Filter fridgeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<FridgeItem> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            if (charSequence == null || charSequence.length() == 0) {
                results.count = fridgeItemsDataAll.size();
                results.values = fridgeItemsDataAll;
            } else {
                String filterPattern = charSequence.toString().toLowerCase().trim();

                for (FridgeItem item : fridgeItemsDataAll) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }

                results.count = filteredList.size();
                results.values = filteredList;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            fridgeItemsData = (ArrayList) filterResults.values;
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView expirationDateText;
        private TextView amountText;
        private ImageView itemImage;
        private FridgeItem currentItem;
        private ImageView deleteIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            nameText = itemView.findViewById(R.id.itemName);
            expirationDateText = itemView.findViewById(R.id.itemExpirationDate);
            amountText = itemView.findViewById(R.id.itemAmount);
            itemImage = itemView.findViewById(R.id.itemImage);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    FridgeItem item = fridgeItemsData.get(position);
                    Intent intent = new Intent(context, EditItemActivity.class);
                    intent.putExtra("item", item);
                    context.startActivity(intent);
                }
            });

            deleteIcon.setOnClickListener(view -> {
                int position = getAdapterPosition();
                FridgeItem item = fridgeItemsData.get(position);
                ((FridgeListActivity) context).deleteItem(item);
            });

            itemView.findViewById(R.id.increaseAmountButton).setOnClickListener(view -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    FridgeItem currentItem = fridgeItemsData.get(position);
                    if (currentItem.getDocumentId() != null) {
                        currentItem.setAmount(currentItem.getAmount() + 1);
                        ((FridgeListActivity) context).updateItemAmount(currentItem, currentItem.getAmount());
                    } else {
                        Log.e(LOG_TAG, "Error: documentId null in RecyclerView");
                    }
                    notifyItemChanged(position);

                }
            });

            itemView.findViewById(R.id.decreaseAmountButton).setOnClickListener(view -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && currentItem.getAmount() > 0) {
                    FridgeItem currentItem = fridgeItemsData.get(position);
                    if (currentItem.getDocumentId() != null) {
                        currentItem.setAmount(currentItem.getAmount() - 1);
                        ((FridgeListActivity) context).updateItemAmount(currentItem, currentItem.getAmount());
                    } else {
                        Log.e(LOG_TAG, "Error: documentId null in RecyclerView");
                    }
                    notifyItemChanged(position);
                }
            });
        }

        public void bindTo(FridgeItem currentItem) {
            this.currentItem = currentItem;

            nameText.setText(currentItem.getName());
            expirationDateText.setText("Expires on: " + currentItem.getExpirationDate().toString());
            amountText.setText(String.valueOf(currentItem.getAmount()));

            if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(Uri.parse(currentItem.getImageUrl()))
                        .into(itemImage);
            } else {
                Glide.with(context)
                        .load(currentItem.getImageResource())
                        .into(itemImage);
            }
        }
    }

}


