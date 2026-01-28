package com.extrascenes.scene;

import org.bukkit.inventory.Inventory;

public interface EditorGui {
    Inventory build(EditorSession session);

    void handleClick(EditorSession session, ClickContext ctx);

    void refresh(EditorSession session);
}
