package com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.editors;

import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.Game;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.game.GameSetting;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.Panel;
import com.gmail.necnionch.myplugin.cestagegenerator.bukkit.gui.PanelItem;
import com.google.common.collect.Lists;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GamePanel extends Panel {

    private final Game game;

    public GamePanel(Player player, Game game) {
        super(player, 27, "ゲーム: " + game.getName());
        this.game = game;
    }

    public GameSetting getSetting() {
        return game.getSetting();
    }


    @Override
    public PanelItem[] build() {
        PanelItem[] slots = new PanelItem[getSize()];

        GameSetting.MaterialList breakList = getSetting().getBreakWhitelists();
        slots[9*1+2-1] = PanelItem.createItem(Material.AIR, "").setItemBuilder((p) -> {
            String label = ChatColor.GOLD + "破壊可能ブロック " + ChatColor.GRAY + "- ";

            if (breakList.isAll()) {
                label += ChatColor.WHITE + "全て";
            } else if (breakList.isEmpty()) {
                label += ChatColor.WHITE + "なし";
            } else {
                label += ChatColor.YELLOW.toString() + breakList.size() + " ブロック";
            }
            return PanelItem.createItem(Material.IRON_PICKAXE, label, Lists.newArrayList(
                    ChatColor.GRAY + "左クリック: 一覧の表示",
                    ChatColor.GRAY + "右クリック: ID検索による一括追加"
            )).getItemStack();
        }).setClickListener((e, p) -> {
            if (ClickType.LEFT.equals(e.getClick())) {
                new MaterialSelectPanel(p, breakList, ChatColor.DARK_RED + "破壊可能ブロック").open(this);

            } else if (ClickType.RIGHT.equals(e.getClick())) {
                new AnvilGUI.Builder()
                        .plugin(OWNER)
                        .title("ブロックを検索")
                        .text("")
                        .onLeftInputClick((p2) -> {
                            this.open();
                        })
                        .onComplete((p2, text) -> {
                            Pattern rex;
                            try {
                                rex = Pattern.compile(text);
                            } catch (PatternSyntaxException ignored) {
                                rex = null;
                            }
                            List<Material> finds = findMaterials(rex, text);

                            Bukkit.getScheduler().runTaskLater(OWNER, () -> {
                                if (finds.isEmpty()) {
                                    this.open();
                                } else {
                                    new MaterialAddSelectPanel(p, finds, breakList)
                                            .open(new MaterialSelectPanel(p, breakList, ChatColor.DARK_RED + "破壊可能ブロック").setBackPanel(this));
                                }
                            }, 2);

                            return AnvilGUI.Response.text("");
                        })
                        .open(p);
            }
        });

        GameSetting.MaterialList placeList = getSetting().getPlaceBlacklists();
        slots[9*1+3-1] = PanelItem.createItem(Material.AIR, "").setItemBuilder((p) -> {
            String label = ChatColor.GOLD + "設置禁止ブロック " + ChatColor.GRAY + "- ";

            if (placeList.isAll()) {
                label += ChatColor.WHITE + "全て";
            } else if (placeList.isEmpty()) {
                label += ChatColor.WHITE + "なし";
            } else {
                label += ChatColor.YELLOW.toString() + placeList.size() + " ブロック";
            }
            return PanelItem.createItem(Material.NETHER_STAR, label, Lists.newArrayList(
                    ChatColor.GRAY + "左クリック: 一覧の表示",
                    ChatColor.GRAY + "右クリック: ID検索による追加"
            )).getItemStack();
        }).setClickListener((e, p) -> {
            if (ClickType.LEFT.equals(e.getClick())) {
                new MaterialSelectPanel(p, placeList, ChatColor.DARK_RED + "設置禁止ブロック").open(this);

            } else if (ClickType.RIGHT.equals(e.getClick())) {
                new AnvilGUI.Builder()
                        .plugin(OWNER)
                        .title("ブロックを検索")
                        .text("")
                        .onLeftInputClick((p2) -> {
                            this.open();
                        })
                        .onComplete((p2, text) -> {
                            Pattern rex;
                            try {
                                rex = Pattern.compile(text);
                            } catch (PatternSyntaxException ignored) {
                                rex = null;
                            }
                            List<Material> finds = findMaterials(rex, text);

                            Bukkit.getScheduler().runTaskLater(OWNER, () -> {
                                if (finds.isEmpty()) {
                                    this.open();
                                } else {
                                    new MaterialAddSelectPanel(p, finds, placeList)
                                            .open(new MaterialSelectPanel(p, placeList, ChatColor.DARK_RED + "設置禁止ブロック").setBackPanel(this));
                                }
                            }, 2);

                            return AnvilGUI.Response.text("");
                        })
                        .open(p);
            }
        });

        slots[9*1+5-1] = PanelItem.createItem(Material.AIR, "").setItemBuilder((p) -> {
            if (game.isWorldEditing()) {
                return PanelItem.createItem(
                        Material.GOLDEN_SHOVEL, ChatColor.YELLOW + "ステージ編集: " + ChatColor.GOLD + game.getEditingStageName()
                ).getItemStack();
            } else {
                return PanelItem.createItem(
                        Material.GRASS_BLOCK, ChatColor.YELLOW + "ステージ一覧"
                ).getItemStack();
            }
        }).setClickListener((e, p) -> {
            if (ClickType.LEFT.equals(e.getClick())) {
                if (game.isWorldEditing()) {
                    // open stage edit
                } else {
                    // open list
                }
            }
        });

        return slots;
    }

    private List<Material> findMaterials(@Nullable Pattern regex, String find) {
        return Stream.of(Material.values())
                .filter(Material::isBlock)
                .filter(m -> {
                    String id = m.getKey().getKey();
                    if (regex != null) {
                        return regex.matcher(id).find();
                    } else {
                        return id.contains(find.toLowerCase(Locale.ROOT));
                    }
                })
                .sorted((o1, o2) -> o1.name().compareToIgnoreCase(o2.name()))
                .collect(Collectors.toList());
    }

    @Override
    public void onEvent(InventoryCloseEvent event) {
        if (event.getInventory().equals(getInventory())) {
            game.saveSettings();
        }
    }

}
