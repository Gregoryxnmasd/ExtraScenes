package com.extrascenes.scene;

import org.bukkit.inventory.ItemStack;

public class ClickContext {
    private final int slot;
    private final boolean rightClick;
    private final boolean shiftClick;
    private final ItemStack clickedItem;

    public ClickContext(int slot, boolean rightClick, boolean shiftClick, ItemStack clickedItem) {
        this.slot = slot;
        this.rightClick = rightClick;
        this.shiftClick = shiftClick;
        this.clickedItem = clickedItem;
    }

    public int getSlot() {
        return slot;
    }

    public boolean isRightClick() {
        return rightClick;
    }

    public boolean isShiftClick() {
        return shiftClick;
    }

    public ItemStack getClickedItem() {
        return clickedItem;
    }
}
