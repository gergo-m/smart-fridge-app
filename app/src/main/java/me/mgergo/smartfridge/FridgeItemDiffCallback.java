package me.mgergo.smartfridge;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class FridgeItemDiffCallback extends DiffUtil.Callback {
    private final List<FridgeItem> oldList;
    private final List<FridgeItem> newList;


    public FridgeItemDiffCallback(List<FridgeItem> oldList, List<FridgeItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getDocumentId().equals(newList.get(newItemPosition).getDocumentId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        FridgeItem oldItem = oldList.get(oldItemPosition);
        FridgeItem newItem = newList.get(newItemPosition);

        return oldItem.getName().equals(newItem.getName())
                && oldItem.getExpirationDate().equals(newItem.getExpirationDate())
                && oldItem.getAmount() == newItem.getAmount()
                && oldItem.getImageUrl().equals(newItem.getImageUrl());
    }

}
