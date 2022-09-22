package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PanelItem {

    private ItemStack item;
    private ClickEventListener clickListener = (e, p) -> {};
    private ItemBuilder itemBuilder = (p) -> item;


    public PanelItem(ItemStack item) {
        this.item = item;
    }

    public ItemStack getItemStack() {
        return item;
    }

    public PanelItem setClickListener(ClickEventListener clickListener) {
        this.clickListener = clickListener;
        return this;
    }

    public PanelItem setItemBuilder(ItemBuilder itemBuilder) {
        this.itemBuilder = itemBuilder;
        return this;
    }

    public ClickEventListener getClickListener() {
        return clickListener;
    }

    public ItemBuilder getItemBuilder() {
        return itemBuilder;
    }

    public PanelItem clone() {
        PanelItem item = new PanelItem(this.item.clone());
        item.setClickListener(clickListener);
        item.setItemBuilder(itemBuilder);
        return item;
    }



    public interface ClickEventListener {
        void click(InventoryClickEvent event, Player player);
    }

    public interface ItemBuilder {
        ItemStack build(Player player);
    }


    public static PanelItem createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return new PanelItem(item);
    }


    public static PanelItem createItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(loreLines);
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return new PanelItem(item);
    }

    public static PanelItem createBlankItem() {
        return createItem(Material.BLACK_STAINED_GLASS_PANE, ChatColor.RESET.toString());
    }

}
