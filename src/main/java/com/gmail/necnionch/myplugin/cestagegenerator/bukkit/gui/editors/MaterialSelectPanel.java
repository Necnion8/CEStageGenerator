package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.editors;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.MaterialList;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.PanelItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

public class MaterialSelectPanel extends Panel {

    private final MaterialList materialList;

    public MaterialSelectPanel(Player player, MaterialList materialList, String title) {
        super(player, 54, title, null);
        this.materialList = materialList;
        setOpenParentWhenClosing(true);
    }

    @Override
    public PanelItem[] build() {
        PanelItem[] slots = new PanelItem[getSize()];

        int idx = 0;
        for (Iterator<Material> it = materialList.iterator(); it.hasNext(); idx++) {
            if (idx >= getSize())
                break;
            Material material = it.next();

            slots[idx] = PanelItem.createItem(material, ChatColor.WHITE + material.getKey().getKey())
                    .setClickListener((e, p) -> {
                        if (ClickType.LEFT.equals(e.getClick())) {
                            materialList.remove(material);
                            this.update();
                        }
                    });
        }
        return slots;
    }

    @Override
    public boolean onClick(InventoryClickEvent event) {
        if (event.getAction().name().startsWith("PLACE_") && event.getCursor() != null) {
            if (event.getSlot() < getSize()) {
                ItemStack itemStack = event.getCursor().clone();
                itemStack.setAmount(1);

                materialList.add(itemStack.getType());
                this.update();
            }
            Bukkit.getScheduler().runTaskLater(OWNER, () -> event.getView().setCursor(null), 1);
            getPlayer().getInventory().addItem(event.getCursor());
        }
        return super.onClick(event);
    }

    @Override
    public void onEvent(InventoryClickEvent event) {
        if (InventoryAction.MOVE_TO_OTHER_INVENTORY.equals(event.getAction()) && event.getInventory().equals(getInventory())) {
            ItemStack item = event.getCurrentItem();
            materialList.add(item.getType());
            this.update();
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

}
