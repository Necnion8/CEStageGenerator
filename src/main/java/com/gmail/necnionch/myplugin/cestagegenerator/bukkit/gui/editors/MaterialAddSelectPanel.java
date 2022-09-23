package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.editors;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.MaterialList;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.PanelItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MaterialAddSelectPanel extends Panel {

    private final List<Material> materials;
//    private final Set<Material> selectedMaterials = Sets.newHashSet();
    private final MaterialList materialList;

    public MaterialAddSelectPanel(Player player, List<Material> materials, MaterialList materialList) {
        super(player, Math.min(Math.max(9, (int) Math.ceil(materials.size() / 9d) * 9), 54), ChatColor.DARK_PURPLE + "追加するブロックを選択", null);
        this.materials = materials;
        this.materialList = materialList;
        setOpenParentWhenClosing(true);
    }

    @Override
    public PanelItem[] build() {
        PanelItem[] slots = new PanelItem[getSize()];

        for (int i = 0; i < Math.min(materials.size(), getSize()); i++) {
            Material material = materials.get(i);
            slots[i] = PanelItem.createItem(Material.AIR, "")
                    .setItemBuilder((p) -> {
                        boolean selected = materialList.contains(material);

                        ChatColor color = (selected) ? ChatColor.YELLOW : ChatColor.GRAY;
                        String label = color + material.getKey().getKey();

                        ItemStack itemStack = PanelItem.createItem(material, label).getItemStack();
                        if (selected)
                            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);

                        return itemStack;
                    })
                    .setClickListener((e, p) -> {
                        if (ClickType.LEFT.equals(e.getClick())) {
                            if (!materialList.remove(material)) {
                                materialList.add(material);
                            }
                            this.update();
                        }
                    });
        }
        return slots;
    }

}
