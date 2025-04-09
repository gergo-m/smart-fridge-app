package me.mgergo.smartfridge;

import android.content.Context;
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
    private Context context;
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

        if (holder.getAdapterPosition() > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_row);
            holder.itemView.startAnimation(animation);
            lastPosition = holder.getAdapterPosition();
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            nameText = itemView.findViewById(R.id.itemName);
            expirationDateText = itemView.findViewById(R.id.itemExpirationDate);
            amountText = itemView.findViewById(R.id.itemAmount);
            itemImage = itemView.findViewById(R.id.itemImage);

            itemView.findViewById(R.id.increaseAmountButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(LOG_TAG, "Increased item amount");
                }
            });
        }

        public void bindTo(FridgeItem currentItem) {
            nameText.setText(currentItem.getName());
            expirationDateText.setText(currentItem.getExpirationDate().toString());
            amountText.setText("asd");

            Glide.with(context).load(currentItem.getImageResource()).into(itemImage);
        }
    }

}


